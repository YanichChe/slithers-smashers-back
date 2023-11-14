package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

import java.util.ArrayList;
import java.util.List;

@Service
public class FoodService {
    private final GameInfo gameInfo;

    @Autowired
    public FoodService(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }

    public void generateFood() {
        List<Integer> occupiedCoords = new ArrayList<>();
        int height = gameInfo.getHeight();
        int width = gameInfo.getWidth();

        for (SnakesProto.GameState.Snake snake : gameInfo.getSnakes()) {
            for (SnakesProto.GameState.Coord coord : snake.getPointsList()) {
                occupiedCoords.add(coord.getY() * height + coord.getX());
            }
        }

        for (SnakesProto.GameState.Coord coord : gameInfo.getFoods()) {
            occupiedCoords.add(coord.getY() * height + coord.getX());
        }

        for (int i = 0; i < gameInfo.getGameConfig().getFoodStatic() - gameInfo.getFoods().size(); i++) {

            if (occupiedCoords.size() == height * width) break;

            int  coordNewFood = (int) (Math.random() * (height * width + 1));
            
            while (occupiedCoords.contains(coordNewFood)) {
                coordNewFood = (int) (Math.random() * (height * width + 1));
            }

            occupiedCoords.add(coordNewFood);

            SnakesProto.GameState.Coord coordNewFoodXY = SnakesProto.GameState.Coord
                    .newBuilder()
                    .setY(coordNewFood  / width)
                    .setX(coordNewFood % width)
                    .build();
            
            gameInfo.getFoods().add(coordNewFoodXY);
        }
    }
}
