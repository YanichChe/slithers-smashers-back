package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages;

import lombok.*;
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

    public GameStateMsg(List<SnakesProto.GamePlayer> gamePlayers,
                        List<SnakesProto.GameState.Snake> snakes,
                        List<SnakesProto.GameState.Coord> foods,
                        boolean isAlive
    ) {
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

        for (SnakesProto.GameState.Coord food: foods) {
            this.foods.add(new Coord(food.getX(), food.getY()));
        }

        this.isAlive = isAlive;
    }

    private List<GamePlayer> gamePlayers = new ArrayList<>();
    private List<Snake> snakeList = new ArrayList<>();
    private List<Coord> foods = new ArrayList<>();
    private boolean isAlive;
    private int score;
}
