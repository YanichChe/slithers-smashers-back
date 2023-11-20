package ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;

import java.net.InetAddress;

@AllArgsConstructor
@Getter
@Setter
public class GameAnnouncementDTO {
    private SnakesProto.GameConfig config;
    private boolean canJoin;
    private String gameName;
    private InetAddress masterAddress;
    private int masterPort;
}
