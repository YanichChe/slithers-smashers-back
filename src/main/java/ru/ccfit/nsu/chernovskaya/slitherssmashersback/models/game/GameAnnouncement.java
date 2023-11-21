package ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.InetAddress;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GameAnnouncement {
    private GameConfig config;
    private boolean canJoin;
    private String gameName;
    private InetAddress masterAddress;
    private int masterPort;
}
