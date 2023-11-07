package ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto;

import lombok.Getter;

@Getter
public class GameRequest {
    private int height;
    private int width;
    private int foodStatic;
    private String gameName;
    private String username;
}