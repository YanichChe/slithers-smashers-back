package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Log4j2
public class GameControlService {

    private final GameInfo gameInfo;
    private final FoodService foodService;

    public GameControlService(GameInfo gameInfo, FoodService foodService) {
        this.gameInfo = gameInfo;
        this.foodService = foodService;
    }

    /**
     * Обновление данных о игре в @GameInfo через полученное сообщение от сервера.
     *
     * @param stateMsg сообщение о состоянии игры.
     */
    public void updateState(SnakesProto.GameMessage.StateMsg stateMsg) {
        gameInfo.setGamePlayers(stateMsg.getState().getPlayers().getPlayersList());
        gameInfo.setSnakes(stateMsg.getState().getSnakesList());
        gameInfo.setStateOrder(stateMsg.getState().getStateOrder());
        gameInfo.setFoods(stateMsg.getState().getFoodsList());
    }

    /**
     * Шаг игры.
     * 1. По результатам поворота головы змеи, змея за каждый шаг продвигается вперед.
     */
    @Scheduled(fixedDelay = 1000)
    @Async
    public void gameStep() {
        if (gameInfo.getSnakes() != null) {
            for (int i = 0; i < gameInfo.getSnakes().size(); i++) {

                SnakesProto.GameState.Snake snake = gameInfo.getSnakes().get(i);
                int headX = snake.getPoints(0).getX();
                int headY = snake.getPoints(0).getY();
                SnakesProto.Direction headDirection = snake.getHeadDirection();

                int newHeadX = headX;
                int newHeadY = headY;

                if (headDirection == SnakesProto.Direction.UP) {
                    newHeadY--;
                } else if (headDirection == SnakesProto.Direction.DOWN) {
                    newHeadY++;
                } else if (headDirection == SnakesProto.Direction.LEFT) {
                    newHeadX--;
                } else if (headDirection == SnakesProto.Direction.RIGHT) {
                    newHeadX++;
                }

                newHeadX = (newHeadX + gameInfo.getWidth()) % gameInfo.getWidth();
                newHeadY = (newHeadY + gameInfo.getHeight()) % gameInfo.getHeight();

                SnakesProto.GameState.Coord newHeadCoord = SnakesProto.GameState.Coord.newBuilder()
                        .setX(newHeadX)
                        .setY(newHeadY)
                        .build();

                SnakesProto.GameState.Snake.Builder modifiedSnakeBuilder = snake.toBuilder();
                modifiedSnakeBuilder.clearPoints();

                modifiedSnakeBuilder.addPoints(newHeadCoord);

                int to = snake.getPointsList().size() - 1;
                if (gameInfo.isIncrease()) to += 1;
                for (int j = 0; j < to; j++) {
                    SnakesProto.GameState.Coord point = snake.getPoints(j);
                    modifiedSnakeBuilder.addPoints(point);
                }

                SnakesProto.GameState.Snake modifiedSnake = modifiedSnakeBuilder.build();

                gameInfo.getSnakes().remove(i);
                gameInfo.getSnakes().add(i, modifiedSnake);

                log.debug(modifiedSnake.getPointsList());

                handlerEatenFood();
                intersectionHandler();

                foodService.generateFood(gameInfo.getGameConfig().getFoodStatic() + getAliveSnakesCount()
                        - gameInfo.getFoods().size());
            }
        }
    }

    /**
     * Обработчик съеденной еды
     */
    private void handlerEatenFood() {
        gameInfo.setIncrease(false);
        List<Integer> foodCoords = new ArrayList<>();
        for (SnakesProto.GameState.Coord coord : gameInfo.getFoods()) {
            foodCoords.add(coord.getY() * gameInfo.getWidth() + coord.getX());
        }

        for (SnakesProto.GameState.Snake snake : gameInfo.getSnakes()) {
            for (int x : foodCoords) {
                if (snake.getPoints(0).getY() * gameInfo.getWidth() +
                        snake.getPoints(0).getX() == x) {
                    int indexPlayer = gameInfo.findPlayerIndexById(snake.getPlayerId());
                    SnakesProto.GamePlayer gamePlayer = gameInfo.getGamePlayers().get(indexPlayer);

                    SnakesProto.GamePlayer.Builder modifiedPlayerGameBuilder = gamePlayer.toBuilder();
                    modifiedPlayerGameBuilder.setScore(gamePlayer.getScore() + 1);

                    gameInfo.getGamePlayers().remove(indexPlayer);
                    gameInfo.getGamePlayers().add(indexPlayer, modifiedPlayerGameBuilder.build());

                    int index = gameInfo.findFoodIndexByInt(x);
                    gameInfo.getFoods().remove(index);

                    log.debug(modifiedPlayerGameBuilder.getScore());
                    gameInfo.setIncrease(true);
                    break;
                }
            }
        }
    }

    /**
     * Обработчик столкновений.
     */
    private void intersectionHandler() {
        List<List<Integer>> snakesCoords = new ArrayList<>();
        for (SnakesProto.GameState.Snake snake : gameInfo.getSnakes()) {
            List<Integer> newList = new ArrayList<>();
            for (SnakesProto.GameState.Coord coord : snake.getPointsList()) {
                newList.add(coord.getY() * gameInfo.getWidth() + coord.getX());
            }
            snakesCoords.add(newList);
        }

        for (int i = 0; i < snakesCoords.size(); i++) {
            for (int j = 0; j < snakesCoords.size(); j++) {
                if (i != j) {
                    //случай когда две разные змейки
                    for (int k = 0; k < snakesCoords.get(j).size(); k++) {
                        if (snakesCoords.get(i).get(0) == snakesCoords.get(j).get(k)) {
                            if (k != 0) {
                                gameInfo.setAlive(false);
                                int gamePlayerIndex = gameInfo.getSnakes().get(i).getPlayerId();

                                gameInfo.updateGamePlayer(gamePlayerIndex, SnakesProto.NodeRole.VIEWER);

                                gameInfo.getSnakes().remove(i);
                            } else {
                                //случай когда врезались две головы
                                gameInfo.setAlive(false);

                                int gamePlayerIndex = gameInfo.getSnakes().get(i).getPlayerId();
                                gameInfo.updateGamePlayer(gamePlayerIndex, SnakesProto.NodeRole.VIEWER);

                                int gamePlayerIndex_ = gameInfo.getSnakes().get(j).getPlayerId();
                                gameInfo.updateGamePlayer(gamePlayerIndex_, SnakesProto.NodeRole.VIEWER);

                                gameInfo.getSnakes().remove(i);
                                gameInfo.getSnakes().remove(j);
                            }
                        }
                    }
                } else {
                    // обработка столкновения змейки с самой собой
                    Set<Integer> set = new HashSet<>();
                    List<Object> duplicates = new ArrayList<>();
                    snakesCoords.get(i).forEach(n -> {
                        if (!set.add(n)) {
                            duplicates.add(n);
                        }
                    });

                    if (!duplicates.isEmpty()) {
                        gameInfo.setAlive(false);

                        log.info("Snake died cause: itself " + gameInfo.getSnakes().get(i));

                        int gamePlayerIndex = gameInfo.getSnakes().get(i).getPlayerId();

                        gameInfo.updateGamePlayer(gamePlayerIndex, SnakesProto.NodeRole.VIEWER);

                        generateRandomFoodAfterSnakeDeath(gameInfo.getSnakes().get(i));
                        gameInfo.getSnakes().remove(i);
                    }
                }
            }
        }
    }

    /**
     * @return количество живых змей в игре
     */
    private int getAliveSnakesCount() {
        int total = 0;

        for (SnakesProto.GameState.Snake snake : gameInfo.getSnakes()) {
            if (snake.getState().equals(SnakesProto.GameState.Snake.SnakeState.ALIVE)) {
                total++;
            }
        }
        return total;
    }

    /**
     * Генерация еды с вероятностью 0,5 в месте погибшей змейки.
     *
     * @param snake погибшая змейка
     */
    private void generateRandomFoodAfterSnakeDeath(SnakesProto.GameState.Snake snake) {
        List<Integer> snakeCoords = new ArrayList<>();

        for (SnakesProto.GameState.Coord coord : snake.getPointsList()) {
            snakeCoords.add(coord.getY() * gameInfo.getWidth() + coord.getY());
        }

        foodService.generateFoodFromList(snakeCoords);
    }
}

