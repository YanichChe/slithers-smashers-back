package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.casts;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master.MasterService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@Service
@Log4j2
public class MulticastSenderService {

    private final GameInfo gameInfo;
    private final MasterService masterService;
    @Value("${multicast.sender.address}")
    String groupAddress;

    @Value("${multicast.sender.port}")
    int groupPort;

    @Autowired
    public MulticastSenderService(GameInfo gameInfo, MasterService masterService) {
        this.gameInfo = gameInfo;
        this.masterService = masterService;
    }

    /**
     * Задача которая выполняется раз в ${multicast.sender.period}.
     * Если была создана игра и текущая роль - мастер, то рассылается
     * сообщение с существованием игры на мультикаст адрес.
     */
    @Scheduled(fixedDelay = 1000)
    @Async
    public void sendAnnouncementMsgPeriodic() {
        if (gameInfo.getGameConfig() != null && gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {
            try (DatagramSocket socket = new DatagramSocket()) {

                SnakesProto.GameMessage gameMessage = masterService.generateAnnouncementMessage();
                byte[] buf = gameMessage.toByteArray();

                DatagramPacket packet = new DatagramPacket(buf, buf.length,
                        InetAddress.getByName(groupAddress), groupPort);

                socket.send(packet);

            } catch (IOException e) {
                log.atLevel(Level.ERROR).log(e.getMessage());
            }
        }
    }
}
