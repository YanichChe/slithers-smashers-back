package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.net;

import com.google.protobuf.InvalidProtocolBufferException;
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
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master.GameControlService;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master.MasterService;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

@Service
@Log4j2
@RequiredArgsConstructor
public class UnicastService {

    private long receivedAck = -1;

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
     */
    public void sendMessage(SnakesProto.GameMessage gameMessage, InetAddress inetAddress, int port) {
        try {
            byte[] buf = gameMessage.toByteArray();

            DatagramPacket packet = new DatagramPacket(buf, buf.length,
                    inetAddress, port);
            datagramSocket.send(packet);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Обработчик полученных сообщений.
     *
     * @throws IOException
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
                case STATE -> gameControlService.updateState(gameMessage.getState());

                case DISCOVER -> {
                    SnakesProto.GameMessage gameMessageNew = generateAnnouncementMessage();
                    sendMessage(gameMessageNew, packet.getAddress(), packet.getPort());
                }

                case JOIN -> {
                    SnakesProto.GameMessage gameMessageNew =
                            masterService.joinHandler(gameMessage.getJoin().getPlayerName(),
                                    packet.getAddress().getHostAddress(), packet.getPort());

                    sendMessage(gameMessageNew, packet.getAddress(), packet.getPort());
                }

                case ACK -> {
                    receivedAck = gameMessage.getMsgSeq();
                    if (gameInfo.getPlayerId() == -1) gameInfo.setPlayerId(gameMessage.getReceiverId());
                }

                case ERROR -> {
                    receivedAck = -2;
                    gameInfo.setPlayerId(-1);
                }

                case STEER -> {
                    masterService.changeSnakeDirection(gameMessage.getSenderId(),
                            gameMessage.getSteer().getDirection());
                }
                case TYPE_NOT_SET -> log.error("wrong type message");
            }

            log.debug(gameMessage);
        }
    }

    /**
     * Отправка всем участникам игры сообщение о состоянии игры.
     *
     * @throws UnknownHostException
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

            for (GamePlayer gamePlayer : gameInfo.getGamePlayers()) {
                if (!gamePlayer.getAddress().equals("localhost"))
                    sendMessage(gameMessage, InetAddress.getByName(gamePlayer.getAddress()), gamePlayer.getPort());
            }
        }
    }

    public int waitAck(long expectedMsgSeq, SnakesProto.GameMessage gameMessage) throws UnknownHostException,
            InterruptedException {


        for (int i = 0; i < 10; i++) {

            if (receivedAck == -2) {
                receivedAck = -1;
                return 0;
            }

            if (expectedMsgSeq == receivedAck) {
                return 1;
            }

            sendMessage(gameMessage, InetAddress.getByName(gameInfo.getMasterInetAddress()),
                    gameInfo.getMasterPort());

            wait(stateDelayMs / 10);
        }
        return 0;
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

        SnakesProto.GameMessage gameMessageNew = SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(gameInfo.getIncrementMsgSeq())
                .setAnnouncement(announcementMsg)
                .build();

        return gameMessageNew;
    }
}
