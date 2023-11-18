package ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto;

import lombok.Getter;

@Getter
public class GameRequest {
    private int height;
    private int width;
    private int foodStatic;
    private String gameName;
    private String username;
    private int haveSnake;

    public int getNodeRole() {
        return haveSnake;
    }
}