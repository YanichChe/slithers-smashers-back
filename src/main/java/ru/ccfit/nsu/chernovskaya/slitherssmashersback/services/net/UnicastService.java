package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.net;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.GamePlayer;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.mapper.ProtobufMapper;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.ID_ENUM;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.Snake;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.State;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master.GameControlService;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master.MasterService;

import java.io.IOException;
import java.net.*;
import java.time.LocalTime;
import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class UnicastService {

    Set<Long> receivedAckSet = new HashSet<>();

    @Value("${state.delay.ms}")
    private long stateDelayMs;

    @Value("${multicast.sender.address}")
    String groupAddress;

    @Value("${multicast.sender.port}")
    int groupPort;

    private DatagramSocket datagramSocket;

    private final GameInfo gameInfo;
    private final GameControlService gameControlService;
    private final MasterService masterService;
    private final ProtobufMapper protobufMapper;

    private final Map<Integer, LocalTime> pingTable= Collections.synchronizedMap(new HashMap<>());

    @PostConstruct
    public void initSocket() throws SocketException {
        datagramSocket = new DatagramSocket();
    }

    @PreDestroy
    public void closeSocket() {
        datagramSocket.close();
    }

    /**
     * Оправка сообщения на конкретный адрес по конкретному порту.
     *
     * @param gameMessage сообщение
     * @param inetAddress адрес получателя
     * @param port        порт получателя
     * @param wait        флаг, нужно ли ожидать ответ от получателя
     */
    public void sendMessage(SnakesProto.GameMessage gameMessage, InetAddress inetAddress, int port, boolean wait) {
        try {
            byte[] buf = gameMessage.toByteArray();

            DatagramPacket packet = new DatagramPacket(buf, buf.length,
                    inetAddress, port);
            datagramSocket.send(packet);

            if (wait) {
                waitAck(gameMessage.getMsgSeq(), gameMessage, inetAddress, port);
            }

        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Отправка подтверждающего сообщения.
     *
     * @param msgSeq  номер сообщения
     * @param address адрес получателя
     * @param port    порт получателя
     */
    private void sendAckMessage(long msgSeq, InetAddress address, int port) {
        SnakesProto.GameMessage message = SnakesProto.GameMessage
                .newBuilder()
                .setAck(SnakesProto.GameMessage.AckMsg.newBuilder().build())
                .setMsgSeq(msgSeq)
                .build();

        sendMessage(message, address, port, false);
    }

    /**
     * Обработчик полученных сообщений.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void receiveMessageTask() throws IOException {

        byte[] buf = new byte[2048];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buf, 0, buf.length);
            datagramSocket.receive(packet);

            var data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.parseFrom(data);

            switch (gameMessage.getTypeCase()) {
                case STATE -> {
                    if (gameInfo.getNodeRole() != null && !gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {
                        gameControlService.updateState(gameMessage.getState());
                        sendAckMessage(gameMessage.getMsgSeq(), packet.getAddress(), packet.getPort());
                    }
                }

                case DISCOVER -> {
                    SnakesProto.GameMessage gameMessageNew = generateAnnouncementMessage();
                    sendMessage(gameMessageNew, packet.getAddress(), packet.getPort(), false);
                }

                case JOIN -> {
                    SnakesProto.GameMessage gameMessageNew =
                            masterService.joinHandler(gameMessage.getJoin().getPlayerName(),
                                    packet.getAddress().getHostAddress(), packet.getPort(), gameMessage.getMsgSeq());

                    sendMessage(gameMessageNew, packet.getAddress(), packet.getPort(), true);
                }

                case ACK -> {
                    receivedAckSet.add(gameMessage.getMsgSeq());
                    if (gameInfo.getPlayerId() == ID_ENUM.UNDEFINED.getValue())
                        gameInfo.setPlayerId(gameMessage.getReceiverId());
                }

                case ERROR -> {
                    receivedAckSet.add(gameMessage.getMsgSeq());

                    if (gameInfo.getPlayerId() != ID_ENUM.UNDEFINED.getValue()) break;

                    gameInfo.setPlayerId(ID_ENUM.NOT_JOIN.getValue());
                    sendAckMessage(gameMessage.getMsgSeq(), packet.getAddress(), packet.getPort());
                }

                case STEER -> {
                    masterService.changeSnakeDirection(gameMessage.getSenderId(),
                            gameMessage.getSteer().getDirection());
                    sendAckMessage(gameMessage.getMsgSeq(), packet.getAddress(), packet.getPort());
                }

                case ROLE_CHANGE -> {
                    deletePlayerById(gameMessage.getSenderId());
                    makeSnakeZombie(gameMessage.getSenderId());
                }

                case PING -> {
                    updatePingTable(gameMessage.getSenderId());
                }
                case TYPE_NOT_SET -> log.error("wrong type message");
            }

            log.debug(gameMessage);
        }
    }

    /**
     * Отправка всем участникам игры сообщение о состоянии игры.
     */
    @Async
    @Scheduled(fixedRateString = "${state.delay.ms}")
    public void sendStateMsgPeriodic() throws UnknownHostException {


        if (gameInfo.getGameConfig() != null && gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {

            gameInfo.setStateOrder(gameInfo.getStateOrder() + 1);

            SnakesProto.GamePlayers gamePlayers = SnakesProto.GamePlayers
                    .newBuilder()
                    .addAllPlayers(protobufMapper.mapFromGamePlayerDTO(gameInfo.getGamePlayers()))
                    .build();

            SnakesProto.GameState gameState = SnakesProto.GameState.newBuilder()
                    .setPlayers(gamePlayers)
                    .setStateOrder(gameInfo.getStateOrder())
                    .addAllSnakes(protobufMapper.map(gameInfo.getSnakes()))
                    .addAllFoods(protobufMapper.mapToFoodProto(gameInfo.getFoods()))
                    .build();

            SnakesProto.GameMessage.StateMsg stateMsg = SnakesProto.GameMessage.StateMsg.newBuilder()
                    .setState(gameState)
                    .build();

            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                    .setMsgSeq(gameInfo.getIncrementMsgSeq())
                    .setState(stateMsg)
                    .build();

            List<GamePlayer> gamePlayerList = gameInfo.getGamePlayers();

            synchronized (gamePlayerList) {
                for (GamePlayer gamePlayer : gamePlayerList) {
                    if (!gamePlayer.getAddress().equals("localhost"))
                        sendMessage(gameMessage, InetAddress.getByName(gamePlayer.getAddress()), gamePlayer.getPort(),
                                true);
                }
            }
        }
    }

    @Async
    public void waitAck(long expectedMsgSeq, SnakesProto.GameMessage gameMessage, InetAddress address, int port)
            throws UnknownHostException, InterruptedException {

        /*for (int i = 0; i < 10; i++) {

            if (receivedAckSet.contains(expectedMsgSeq)) {
                receivedAckSet.remove(expectedMsgSeq);
                return;
            }

            sendMessage(gameMessage, address, port, false);

            Thread.sleep(stateDelayMs);
        }*/
    }

    /**
     * Задача которая выполняется раз в ${multicast.sender.period}.
     * Если была создана игра и текущая роль - мастер, то рассылается
     * сообщение с существованием игры на мультикаст адрес.
     */
    @Scheduled(fixedDelay = 1000)
    @Async
    public void sendAnnouncementMsgPeriodic() throws IOException {
        if (gameInfo.getGameConfig() != null && gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {

            SnakesProto.GameMessage gameMessage = generateAnnouncementMessage();
            byte[] buf = gameMessage.toByteArray();

            DatagramPacket packet = new DatagramPacket(buf, buf.length,
                    InetAddress.getByName(groupAddress), groupPort);

            datagramSocket.send(packet);
        }
    }

    /**
     * Генерация сообщения-уведомления об игре
     *
     * @return новое сообщение с @AnnouncementMessage
     */
    private SnakesProto.GameMessage generateAnnouncementMessage() {

        SnakesProto.GamePlayers gamePlayers = SnakesProto.GamePlayers
                .newBuilder()
                .addAllPlayers(protobufMapper.mapFromGamePlayerDTO(gameInfo.getGamePlayers()))
                .build();

        SnakesProto.GameAnnouncement gameAnnouncement = SnakesProto.GameAnnouncement.newBuilder()
                .setPlayers(gamePlayers)
                .setConfig(protobufMapper.map(gameInfo.getGameConfig()))
                .setCanJoin(gameInfo.isCanJoin())
                .setGameName(gameInfo.getGameName())
                .build();

        SnakesProto.GameMessage.AnnouncementMsg announcementMsg =
                SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                        .addGames(gameAnnouncement)
                        .build();

        return SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(gameInfo.getIncrementMsgSeq())
                .setAnnouncement(announcementMsg)
                .build();
    }

    /**
     * Отправляет ping сообщение
     *
     * @throws UnknownHostException
     */
    @Async
    @Scheduled(fixedDelayString = "${ping.delay.ms}")
    public void sendPingMessage() throws UnknownHostException {
        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage
                .newBuilder()
                .setMsgSeq(gameInfo.getIncrementMsgSeq())
                .setPing(SnakesProto.GameMessage.PingMsg.newBuilder().build())
                .build();

        sendMessage(gameMessage, InetAddress.getByName(gameInfo.getMasterInetAddress()),
                gameInfo.getMasterPort(), true);
    }

    /**
     * Удаление игрока по id.
     *
     * @param id индификатор игрока, которого хотят удалить.
     */
    private void deletePlayerById(int id) {
        gameInfo.getGamePlayers().removeIf(gamePlayer -> gamePlayer.getId() == id);
    }


    /**
     * Устанавливает статус змейки - Зомби
     *
     * @param playerId индификатор хозяина змейки
     */
    private void makeSnakeZombie(int playerId) {
        for (Snake snake: gameInfo.getSnakes()) {
            if (snake.getPlayerId() == playerId) {
                snake.setState(State.Zombie);
            }
        }
    }

    /**
     * Обновляет время игрока.
     *
     * @param playerId индификатор игрока
     */
    public void updatePingTable(int playerId) {
        pingTable.put(playerId, LocalTime.now());
    }

    /**
     * Задание по удалению неактивных игроков.
     */
    @Async
    @Scheduled(fixedDelayString = "${state.delay.ms}")
    public void deleteNotActivePlayers() {
        pingTable.forEach((key, value) -> {
            LocalTime currentTime = LocalTime.now();
            LocalTime fiveSecondsAgo = currentTime.minusSeconds(1);

            if (value.isBefore(fiveSecondsAgo)) {
                pingTable.remove(key);
                deletePlayerById(key);
            }
        });
    }
}
