package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@Service
@Log4j2
public class MulticastSenderService {

    private final GameInfo gameInfo;
    @Value("${multicast.sender.address}")
    String groupAddress;

    @Value("${multicast.sender.port}")
    int groupPort;

    @Autowired
    public MulticastSenderService(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }

    @Async
    @Scheduled(fixedRateString = "${state.delay.ms}")
    public void sendStateMsgPeriodic() {
        if (gameInfo.getGameConfig() != null && gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {

            try (DatagramSocket socket = new DatagramSocket()) {
                try {
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


                    byte[] buf = gameMessage.toByteArray();

                    DatagramPacket packet = new DatagramPacket(buf, 0, buf.length,
                            InetAddress.getByName(groupAddress), groupPort);

                    log.debug("sent state message" + stateMsg.toString());
                    socket.send(packet);

                } catch (IOException e) {
                    socket.close();
                }

            } catch (IOException e) {
                log.atLevel(Level.ERROR).log(e.getMessage());
            }
        }
    }

    @Async
    @Scheduled(fixedRateString = "${multicast.sender.period}")
    public void sendAnnouncementMsgPeriodic() {
        if (gameInfo.getGameConfig() != null && gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {

            try (DatagramSocket socket = new DatagramSocket()) {
                try {
                    SnakesProto.GamePlayers gamePlayers = SnakesProto.GamePlayers
                            .newBuilder()
                            .addAllPlayers(gameInfo.getGamePlayers())
                            .build();

                    SnakesProto.GameAnnouncement gameAnnouncement = SnakesProto.GameAnnouncement.newBuilder()
                            .setPlayers(gamePlayers)
                            .setConfig(gameInfo.getGameConfig())
                            .setCanJoin(gameInfo.isCanJoin())
                            .setGameName(gameInfo.getGameName())
                            .build();

                    SnakesProto.GameMessage.AnnouncementMsg announcementMsg =
                            SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                                    .addGames(gameAnnouncement)
                                    .build();

                    SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                            .setMsgSeq(gameInfo.getIncrementMsgSeq())
                            .setAnnouncement(announcementMsg)
                            .build();

                    byte[] buf = gameMessage.toByteArray();

                    DatagramPacket packet = new DatagramPacket(buf, buf.length,
                            InetAddress.getByName(groupAddress), groupPort);

                    log.debug("sent AnnouncementMsg" + announcementMsg);
                    socket.send(packet);

                } catch (IOException e) {
                    socket.close();
                }

            } catch (IOException e) {
                log.atLevel(Level.ERROR).log(e.getMessage());
            }

        }
    }
}
