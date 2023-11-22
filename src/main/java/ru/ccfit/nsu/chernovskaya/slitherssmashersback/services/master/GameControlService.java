package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.mapper.ProtobufMapper;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.*;

import java.util.*;

import static ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.Direction.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class GameControlService {

    private final GameInfo gameInfo;
    private final FoodService foodService;
    private final ProtobufMapper mapper;

    private final Set<Integer> isIncrease = new HashSet<>();

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
                Coord newHeadCoord = getHeadCoord(snake);

                List<Coord> copySnakeCoords = new ArrayList<>(snake.getCoordList());
                snake.getCoordList().clear();

                snake.getCoordList().add(newHeadCoord);
                int to = copySnakeCoords.size() - 1;

                if (isIncrease.contains(snake.getPlayerId())) {
                    log.info(to);
                    to += 1;
                }

                for (int j = 0; j < to; j++) {
                    Coord point = copySnakeCoords.get(j);
                    snake.getCoordList().add(point);
                }

            }

            handlerEatenFood();
            intersectionHandler();

            foodService.generateFood(gameInfo.getGameConfig().getFoodStatic() + getAliveSnakesCount()
                    - gameInfo.getFoods().size());
        }
    }

    private Coord getHeadCoord(Snake snake) {
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

        return new Coord(newHeadX, newHeadY);
    }

    /**
     * Обработчик съеденной еды
     */
    private void handlerEatenFood() {

        isIncrease.clear();

        List<Integer> foodCoords = new ArrayList<>();
        for (Coord coord : gameInfo.getFoods()) {
            foodCoords.add(coord.getY() * gameInfo.getWidth() + coord.getX());
        }

        for (Snake snake : gameInfo.getSnakes()) {
            int indexGamePlayer = findPlayerIndexById(snake.getPlayerId());

            for (int x : foodCoords) {
                if (snake.getCoordList().get(0).getY() * gameInfo.getWidth() +
                        snake.getCoordList().get(0).getX() == x) {

                    GamePlayer gamePlayer = gameInfo.getGamePlayers().get(indexGamePlayer);
                    gamePlayer.setScore(gamePlayer.getScore() + 1);

                    int index = findFoodIndexByInt(x);
                    gameInfo.getFoods().remove(index);

                    isIncrease.add(gamePlayer.getId());
                    break;
                }
            }
        }
    }

    /**
     * Обработчик столкновений.
     */
    private void intersectionHandler() {
        List<List<Integer>> snakesCoordsLists = new ArrayList<>();

        for (Snake snake : gameInfo.getSnakes()) {
            List<Integer> newList = new ArrayList<>();
            for (Coord coord : snake.getCoordList()) {
                newList.add(coord.getY() * gameInfo.getWidth() + coord.getX());
            }
            snakesCoordsLists.add(newList);
        }

        Set<Integer> killedSnakesPlayersId = new HashSet<>();

        for (int i = 0; i < snakesCoordsLists.size(); i++) {
            for (int j = 0; j < snakesCoordsLists.size(); j++) {
                if (i != j) {
                    //случай когда две разные змейки
                    for (int k = 0; k < snakesCoordsLists.get(j).size(); k++) {

                        if (Objects.equals(snakesCoordsLists.get(i).get(0), snakesCoordsLists.get(j).get(k))) {

                            int gamePlayerId = gameInfo.getSnakes().get(j).getPlayerId();
                            int gamePlayerId_ = gameInfo.getSnakes().get(i).getPlayerId();

                            if (k != 0) {
                                addPointToGamePlayer(gamePlayerId);
                            } else {
                                killedSnakesPlayersId.add(gamePlayerId_);
                            }

                            killedSnakesPlayersId.add(gamePlayerId);
                        }
                    }
                } else {
                    // обработка столкновения змейки с самой собой
                    Set<Integer> set = new HashSet<>();
                    List<Object> duplicates = new ArrayList<>();
                    snakesCoordsLists.get(i).forEach(n -> {
                        if (!set.add(n)) {
                            duplicates.add(n);
                        }
                    });

                    if (!duplicates.isEmpty()) {
                        killedSnakesPlayersId.add(gameInfo.getSnakes().get(j).getPlayerId());
                    }
                }
            }
        }

        for (int id : killedSnakesPlayersId.stream().toList()) {
            killSnake(id);
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

    private int findPlayerIndexById(long id) {
        for (int i = 0; i < gameInfo.getGamePlayers().size(); i++) {
            if (gameInfo.getGamePlayers().get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    private int findSnakeIndexByPlayerId(long id) {
        for (int i = 0; i < gameInfo.getSnakes().size(); i++) {
            if (gameInfo.getSnakes().get(i).getPlayerId() == id) {
                return i;
            }
        }
        return -1;
    }

    private int findFoodIndexByInt(int coord) {
        for (int i = 0; i < gameInfo.getFoods().size(); i++) {
            Coord food = gameInfo.getFoods().get(i);
            if (food.getY() * gameInfo.getWidth() + food.getX() == coord) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param index индекс игрока
     */
    private void addPointToGamePlayer(int index) {
        gameInfo.getGamePlayers().get(index).setScore(gameInfo.getGamePlayers().get(index).getScore() + 1);
    }

    private void deleteGamePlayer(int index) {
        gameInfo.getGamePlayers().remove(index);
    }

    private void killSnake(int playerId) {

        Snake copySnake = gameInfo.getSnakes().get(findSnakeIndexByPlayerId(playerId));
        removeSnakeById(playerId);
        generateRandomFoodAfterSnakeDeath(copySnake);
        deleteGamePlayer(findPlayerIndexById(playerId));
    }

    private void removeSnakeById(int gamePlayerId) {
        for (int i = 0; i < gameInfo.getSnakes().size(); i++) {
            if (gameInfo.getSnakes().get(i).getPlayerId() == gamePlayerId) {
                gameInfo.getSnakes().remove(i);
                break;
            }
        }
    }

    /**
     * Генерация еды с вероятностью 0,5 в месте погибшей змейки.
     *
     * @param snake погибшая змейка
     */
    private void generateRandomFoodAfterSnakeDeath(Snake snake) {
        List<Integer> snakeCoords = new ArrayList<>();

        for (Coord coord : snake.getCoordList()) {
            snakeCoords.add(coord.getY() * gameInfo.getWidth() + coord.getX());
        }

        foodService.generateFoodFromList(snakeCoords);
    }
}


