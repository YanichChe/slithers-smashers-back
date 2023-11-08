package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

@Service
@Log4j2
public class UnicastMessageService {

    private DatagramSocket datagramSocket;

    private final GameInfo gameInfo;
    private final GameControlService gameControlService;
    private final MasterService masterService;

    public UnicastMessageService(GameInfo gameInfo, GameControlService gameControlService,
                                 MasterService masterService) {
        this.gameInfo = gameInfo;
        this.gameControlService = gameControlService;
        this.masterService = masterService;
    }

    @PostConstruct
    public void initSocket() throws SocketException {
        datagramSocket = new DatagramSocket();
    }

    @PreDestroy
    public void closeSocket() {
        datagramSocket.close();
    }

    public int sendMessage(SnakesProto.GameMessage gameMessage, InetAddress inetAddress, int port) {
        try {
            byte[] buf = gameMessage.toByteArray();

            DatagramPacket packet = new DatagramPacket(buf, 0, buf.length,
                    inetAddress, port);
            datagramSocket.send(packet);
            return 0;
        } catch (IOException e) {
            return 1;
        }
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void receiveMessageTask() throws IOException {
        if (gameInfo.getGameConfig() != null) {
            byte[] buf = new byte[2048];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, 0, buf.length);
                datagramSocket.receive(packet);

                var data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.parseFrom(data);

                if (gameMessage.hasState()) {
                    gameControlService.updateState(gameMessage.getState());
                } else if (gameMessage.hasDiscover() & gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {
                    SnakesProto.GameMessage gameMessageNew = masterService.generateAnnouncementMessage();
                    sendMessage(gameMessageNew, packet.getAddress(), packet.getPort());
                } else if (gameMessage.hasJoin()) {
                    SnakesProto.GameMessage gameMessageNew =
                            masterService.joinHandler(gameMessage.getJoin().getPlayerName());
                    sendMessage(gameMessageNew, packet.getAddress(), packet.getPort());
                } else if (gameMessage.hasAck()) {
                    gameInfo.setPlayerId(gameMessage.getReceiverId());
                } else if (gameMessage.hasError()) {
                    gameInfo.setPlayerId(-1);
                } else if (gameMessage.hasSteer() & gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {
                    masterService.changeSnakeDirection(gameMessage.getSenderId(),
                            gameMessage.getSteer().getDirection());
                }

                log.debug(gameMessage);
            }
        }
    }

    @Async
    @Scheduled(fixedRateString = "${state.delay.ms}")
    public void sendStateMsgPeriodic() throws UnknownHostException {
        if (gameInfo.getGameConfig() != null && gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {


            gameInfo.setStateOrder(gameInfo.getStateOrder() + 1);

            SnakesProto.GamePlayers gamePlayers = SnakesProto.GamePlayers
                    .newBuilder()
                    .addAllPlayers(gameInfo.getGamePlayers())
                    .build();

            SnakesProto.GameState gameState = SnakesProto.GameState.newBuilder()
                    .setPlayers(gamePlayers)
                    .setStateOrder(gameInfo.getStateOrder())
                    .addAllSnakes(gameInfo.getSnakes())
                    .build();

            SnakesProto.GameMessage.StateMsg stateMsg = SnakesProto.GameMessage.StateMsg.newBuilder()
                    .setState(gameState)
                    .build();

            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                    .setMsgSeq(gameInfo.getIncrementMsgSeq())
                    .setState(stateMsg)
                    .build();

            for (SnakesProto.GamePlayer gamePlayer : gameInfo.getGamePlayers()) {
                sendMessage(gameMessage, InetAddress.getByName(gamePlayer.getIpAddress()), gamePlayer.getPort());
            }
        }
    }
}
