package ru.ccfit.nsu.chernovskaya.slitherssmashersback.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GameState {
    int stateOrder;
    List<Snake> snakes;
    List<Coord> foods;
    List<GamePlayer> players;
}
