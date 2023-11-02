package ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameRequestDTO {
    private int height;
    private int width;
    private int foodStatic;
    private int stateDelayMs;
    private String gameName;
    private String username;
    private String ipAddress;
    private int port;
}