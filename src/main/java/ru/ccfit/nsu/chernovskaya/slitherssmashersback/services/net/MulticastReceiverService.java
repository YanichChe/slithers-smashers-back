package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.net;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.GameAnnouncement;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.mapper.ProtobufMapper;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GamesInfo;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

@Service
@Log4j2
@RequiredArgsConstructor
public class MulticastReceiverService {
    private final GamesInfo gamesInfo;
    @Value("${multicast.sender.address}")
    String groupAddress;

    @Value("${multicast.sender.port}")
    int groupPort;

    private final ProtobufMapper protobufMapper;


    /**
     * Получение сообщеения по мультикасту о списке текущих игр.
     * При получении новго сообщения, старый список полностью обновляется
     * новыми значенями.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void getMsg() {
        try (MulticastSocket socket = new MulticastSocket(groupPort)) {

            byte[] buf = new byte[2048];

            socket.joinGroup(new InetSocketAddress(groupAddress, groupPort),
                    NetworkInterface.getByInetAddress(InetAddress.getByName(groupAddress)));

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, 0, buf.length);
                socket.receive(packet);

                var data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.parseFrom(data);

                if (gameMessage.hasAnnouncement()) {
                    GameAnnouncement gameAnnouncement = new GameAnnouncement(
                            protobufMapper.map(gameMessage.getAnnouncement().getGames(0).getConfig()),
                            gameMessage.getAnnouncement().getGames(0).getCanJoin(),
                            gameMessage.getAnnouncement().getGames(0).getGameName(),
                            packet.getAddress(),
                            packet.getPort()
                    );
                    gamesInfo.getGameAnnouncementMap().put(gameAnnouncement.getGameName(), gameAnnouncement);
                }
            }

        } catch (IOException e) {
            log.atLevel(Level.ERROR).log(e.getMessage());
        }
    }
}

