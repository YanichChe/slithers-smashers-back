package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.mapper.ProtobufMapper;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.info.GameInfoService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.Direction.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class GameControlService {

    private final GameInfo gameInfo;
    private final GameInfoService gameInfoService;
    private final FoodService foodService;
    private final ProtobufMapper mapper;

    /**
     * Обновление данных о игре в @GameInfo через полученное сообщение от сервера.
     *
     * @param stateMsg сообщение о состоянии игры.
     */
    public void updateState(SnakesProto.GameMessage.StateMsg stateMsg) {
        GameState gameState = new GameState();
        mapper.map(stateMsg, gameState);

        gameInfo.setGamePlayers(gameState.getPlayers());
        gameInfo.setSnakes(gameState.getSnakes());
        gameInfo.setStateOrder(gameState.getStateOrder());
        gameInfo.setFoods(gameState.getFoods());
    }

    /**
     * Шаг игры.
     * 1. По результатам поворота головы змеи, змея за каждый шаг продвигается вперед.
     */
    @Scheduled(fixedDelay = 1000)
    @Async
    public void gameStep() {
        if (gameInfo.getSnakes() != null && gameInfo.getNodeRole() != null
                && gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {
            for (int i = 0; i < gameInfo.getSnakes().size(); i++) {

                Snake snake = gameInfo.getSnakes().get(i);
                int headX = snake.getCoordList().get(0).getX();
                int headY = snake.getCoordList().get(0).getY();
                Direction headDirection = snake.getHeadDirection();

                int newHeadX = headX;
                int newHeadY = headY;

                if (headDirection == UP) {
                    newHeadY--;
                } else if (headDirection == DOWN) {
                    newHeadY++;
                } else if (headDirection == LEFT) {
                    newHeadX--;
                } else if (headDirection == RIGHT) {
                    newHeadX++;
                }

                newHeadX = (newHeadX + gameInfo.getWidth()) % gameInfo.getWidth();
                newHeadY = (newHeadY + gameInfo.getHeight()) % gameInfo.getHeight();

                Coord newHeadCoord = new Coord(newHeadX, newHeadY);

                List<Coord> copySnakeCoords = new ArrayList<>();
                copySnakeCoords.addAll(snake.getCoordList());

                snake.getCoordList().clear();

                snake.getCoordList().add(newHeadCoord);

                int to = copySnakeCoords.size() - 1;
                if (gameInfo.isIncrease()) to += 1;
                for (int j = 0; j < to; j++) {
                    Coord point = copySnakeCoords.get(j);
                    snake.getCoordList().add(point);
                }

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
        for (Coord coord : gameInfo.getFoods()) {
            foodCoords.add(coord.getY() * gameInfo.getWidth() + coord.getX());
        }

        for (Snake snake : gameInfo.getSnakes()) {
            for (int x : foodCoords) {
                if (snake.getCoordList().get(0).getY() * gameInfo.getWidth() +
                        snake.getCoordList().get(0).getX() == x) {
                    int indexPlayer = gameInfoService.findPlayerIndexById(snake.getPlayerId());
                    GamePlayer gamePlayer = gameInfo.getGamePlayers().get(indexPlayer);
                    gamePlayer.setScore(gamePlayer.getScore() + 1);
                    gameInfo.setScore(gamePlayer.getScore() + 1);
                    log.info(gamePlayer.getScore());

                    int index = gameInfoService.findFoodIndexByInt(x);
                    gameInfo.getFoods().remove(index);

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
        for (Snake snake : gameInfo.getSnakes()) {
            List<Integer> newList = new ArrayList<>();
            for (Coord coord : snake.getCoordList()) {
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
                                int gamePlayerIndex = gameInfo.getSnakes().get(j).getPlayerId();
                                gameInfoService.addPointToGamePlayer(gamePlayerIndex);

                                gameInfoService.killSnake(i);

                            } else {
                                gameInfoService.killSnake(i);
                                gameInfoService.killSnake(j);
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
                        gameInfoService.killSnake(i);
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

        for (Snake snake : gameInfo.getSnakes()) {
            if (snake.getState().equals(State.Alive)) {
                total++;
            }
        }
        return total;
    }
}

