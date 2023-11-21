package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
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