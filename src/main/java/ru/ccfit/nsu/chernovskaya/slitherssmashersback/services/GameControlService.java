package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

@Service
public class GameControlService {

    private final GameInfo gameInfo;

    public GameControlService(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }

    @Scheduled(fixedDelay = 1000)
    public void gameStep() {
        if (gameInfo.getSnakes() != null) {
            for (SnakesProto.GameState.Snake snake : gameInfo.getSnakes()) {
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

                SnakesProto.GameState.Coord newHeadCoord = SnakesProto.GameState.Coord.newBuilder()
                        .setX(newHeadX)
                        .setY(newHeadY)
                        .build();

                snake.getPointsList().set(0, newHeadCoord);
                snake.getPointsList().removeLast();
            }
        }
    }
}
