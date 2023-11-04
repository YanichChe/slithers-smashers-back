package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class GameStateMsg {

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    private static class Coord {
        private int x;
        private int y;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    private static class GamePlayer {
        private String name;
        private int score;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    private static class Snake {
        List<Coord> coordList;
    }

    public GameStateMsg(List<SnakesProto.GamePlayer> gamePlayers, List<SnakesProto.GameState.Snake> snakes) {
        for (SnakesProto.GamePlayer gamePlayer : gamePlayers) {
            GamePlayer newGamePlayer = new GamePlayer(gamePlayer.getName(), gamePlayer.getScore());
            this.gamePlayers.add(newGamePlayer);
        }

        for (SnakesProto.GameState.Snake snake : snakes) {
            List<Coord> newCoords = new ArrayList<>();
            for (SnakesProto.GameState.Coord coord : snake.getPointsList()) {
                newCoords.add(new Coord(coord.getX(), coord.getY()));
            }

            Snake newSnake = new Snake(newCoords);
            this.snakeList.add(newSnake);
        }

    }

    List<GamePlayer> gamePlayers = new ArrayList<>();
    List<Snake> snakeList = new ArrayList<>();
}
