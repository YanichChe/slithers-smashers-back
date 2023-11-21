package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.info;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.Coord;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.Snake;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master.FoodService;

import java.util.ArrayList;
import java.util.List;

@Service
public class GameInfoService {

    private final GameInfo gameInfo;
    private final FoodService foodService;

    @Autowired
    public GameInfoService(GameInfo gameInfo, FoodService foodService) {
        this.gameInfo = gameInfo;
        this.foodService = foodService;
    }

    public int findPlayerIndexById(long id) {
        for (int i = 0; i < gameInfo.getGamePlayers().size(); i++) {
            if (gameInfo.getGamePlayers().get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    public int findFoodIndexByInt(int coord) {
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
    public void addPointToGamePlayer(int index) {
        gameInfo.getGamePlayers().get(index).setScore(gameInfo.getGamePlayers().get(index).getScore() + 1);
    }

    public void deleteGamePlayer(int index) {
        gameInfo.getGamePlayers().remove(index);
    }

    public void killSnake(int i) {
        int gamePlayerIndex = gameInfo.getSnakes().get(i).getPlayerId();
        Snake copySnake = gameInfo.getSnakes().get(i);
        gameInfo.getSnakes().remove(i);
        generateRandomFoodAfterSnakeDeath(copySnake);
        deleteGamePlayer(gamePlayerIndex);
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
