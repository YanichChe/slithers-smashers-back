package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages;

import lombok.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.Coord;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.GamePlayer;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.Snake;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GameStateMsg {

    private List<GamePlayer> gamePlayers = new ArrayList<>();
    private List<Snake> snakeList = new ArrayList<>();
    private List<Coord> foods = new ArrayList<>();
    private boolean isAlive;
    private int score;
}
