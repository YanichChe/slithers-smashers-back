package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

@Service
@Log4j2
public class MulticastReceiverService {
    private final GameInfo gameInfo;
    @Value("${multicast.sender.address}")
    String groupAddress;

    @Value("${multicast.sender.port}")
    int groupPort;

    public MulticastReceiverService(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }

    @Async
    @Scheduled(fixedDelay = 1000)
    public void getMsg() {
        if (gameInfo.getGameConfig() != null && !gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {
            try (MulticastSocket socket = new MulticastSocket(groupPort)) {

                byte[] buf = new byte[2048];

                socket.joinGroup(InetAddress.getByName(groupAddress));

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buf, 0, buf.length);
                    socket.receive(packet);

                    var data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                    SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.parseFrom(data);

                    log.info(gameMessage);

                    if (gameMessage.hasState()) {
                        gameInfo.setGamePlayers(gameMessage.getState().getState().getPlayers().getPlayersList());
                        gameInfo.setSnakes(gameMessage.getState().getState().getSnakesList());
                        log.info(gameInfo.getSnakes());
                    }
                }

            } catch (IOException e) {
                log.atLevel(Level.ERROR).log(e.getMessage());
            }
        }
    }
}
