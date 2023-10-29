package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@Service
@Log4j2
public class MulticastSenderService implements Runnable {

    private final GameInfo gameInfo;
    @Value("${multicast.sender.address}")
    String groupAddress;

    @Value("${multicast.sender.port}")
    int groupPort;

    @Autowired
    public MulticastSenderService(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            while (true) {
                try {
                    SnakesProto.GameAnnouncement gameAnnouncement = SnakesProto.GameAnnouncement.newBuilder()
                            .setPlayers(gameInfo.getGamePlayers())
                            .setConfig(gameInfo.getGameConfig())
                            .setCanJoin(gameInfo.isCanJoin())
                            .setGameName(gameInfo.getGameName())
                            .build();

                    SnakesProto.GameMessage.AnnouncementMsg announcementMsg =
                            SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                                    .setGames(0, gameAnnouncement)
                                    .build();

                    SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                            .setMsgSeq(gameInfo.getIncrementMsgSeq())
                            .setAnnouncement(announcementMsg)
                            .build();

                    byte[] buf = gameMessage.toByteArray();

                    DatagramPacket packet = new DatagramPacket(buf, buf.length,
                            InetAddress.getByName(groupAddress), groupPort);

                    log.info("sent AnnouncementMsg");
                    socket.send(packet);

                    Thread.sleep(1000);
                } catch (IOException | InterruptedException e) {
                    socket.close();
                }
            }
        } catch (IOException e) {
            log.atLevel(Level.ERROR).log(e.getMessage());
        }
    }
}
