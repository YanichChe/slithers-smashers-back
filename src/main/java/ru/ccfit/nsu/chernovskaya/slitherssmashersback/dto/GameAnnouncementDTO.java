package ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;

@AllArgsConstructor
@Getter
@Setter
public class GameAnnouncementDTO {
    private SnakesProto.GameConfig config;
    private boolean canJoin;
    private String gameName;
    private String masterAddress;
    private int masterPort;
}
