package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GamesInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

@Service
@Log4j2
public class MulticastReceiverService {
    private final GameInfo gameInfo;
    private final GamesInfo gamesInfo;
    @Value("${multicast.sender.address}")
    String groupAddress;

    @Value("${multicast.sender.port}")
    int groupPort;

    public MulticastReceiverService(GameInfo gameInfo, GamesInfo gamesInfo) {
        this.gameInfo = gameInfo;
        this.gamesInfo = gamesInfo;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void getMsg() {
        try (MulticastSocket socket = new MulticastSocket(groupPort)) {

            byte[] buf = new byte[2048];

            socket.joinGroup(InetAddress.getByName(groupAddress));

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, 0, buf.length);
                socket.receive(packet);

                var data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.parseFrom(data);

                log.debug(gameMessage);

                if (gameMessage.hasAnnouncement()) {
                    gamesInfo.setGameAnnouncementList(gameMessage.getAnnouncement().getGamesList());
                }
            }

        } catch (IOException e) {
            log.atLevel(Level.ERROR).log(e.getMessage());
        }
    }
}

