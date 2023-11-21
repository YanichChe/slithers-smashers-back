package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.Coord;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.Snake;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
public class FoodService {
    private final GameInfo gameInfo;

    @Autowired
    public FoodService(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }

    /**
     * @param count количество еды, которое должно быть на поле
     */
    public void generateFood(int count) {
        List<Integer> occupiedCoords = new ArrayList<>();
        int height = gameInfo.getHeight();
        int width = gameInfo.getWidth();

        for (Snake snake : gameInfo.getSnakes()) {
            for (Coord coord : snake.getCoordList()) {
                occupiedCoords.add(coord.getY() * height + coord.getX());
            }
        }

        for (Coord coord : gameInfo.getFoods()) {
            occupiedCoords.add(coord.getY() * height + coord.getX());
        }

        for (int i = 0; i < count; i++) {

            if (occupiedCoords.size() == height * width) break;

            int coordNewFood = (int) (Math.random() * (height * width + 1));

            while (occupiedCoords.contains(coordNewFood)) {
                coordNewFood = (int) (Math.random() * (height * width + 1));
            }

            occupiedCoords.add(coordNewFood);

            Coord coordNewFoodXY = new Coord();
            coordNewFoodXY.setX(coordNewFood % width);
            coordNewFoodXY.setY(coordNewFood / width);

            gameInfo.getFoods().add(coordNewFoodXY);
        }
    }

    /**
     * @param coords координаты, в которых может сгенерироваться еда
     */
    public void generateFoodFromList(List<Integer> coords) {
        List<Integer> occupiedCoords = new ArrayList<>();
        int height = gameInfo.getHeight();
        int width = gameInfo.getWidth();

        for (Snake snake : gameInfo.getSnakes()) {
            for (Coord coord : snake.getCoordList()) {
                occupiedCoords.add(coord.getY() * height + coord.getX());
            }
        }

        for (Coord coord : gameInfo.getFoods()) {
            occupiedCoords.add(coord.getY() * height + coord.getX());
        }

        for (Integer coord : coords) {
            int create = (int) (Math.random() * 2);
            if (create == 1) {
                if (!occupiedCoords.contains(coord)) {

                    Coord coordNewFoodXY = new Coord();
                    coordNewFoodXY.setX(coord % width);
                    coordNewFoodXY.setY(coord / width);

                    gameInfo.getFoods().add(coordNewFoodXY);
                    log.info(coordNewFoodXY);
                }
            }
        }
    }
}
