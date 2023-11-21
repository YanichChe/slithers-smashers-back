package ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GamePlayer {
    private String name;
    private int score = 0;
    private int id;
    private boolean isAlive;
    private String address = "localhost";
    private int port;
    private Role role;
}
