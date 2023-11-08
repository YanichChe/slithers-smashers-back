package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

@Service
@Log4j2
public class GameControlService {

    private final GameInfo gameInfo;

    public GameControlService(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }

    public void updateState(SnakesProto.GameMessage.StateMsg stateMsg) {
        gameInfo.setGamePlayers(stateMsg.getState().getPlayers().getPlayersList());
        gameInfo.setSnakes(stateMsg.getState().getSnakesList());
        gameInfo.setStateOrder(stateMsg.getState().getStateOrder());
        gameInfo.setFoods(stateMsg.getState().getFoodsList());
    }
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
                for (int j = 0; j < snake.getPointsList().size() - 1; j++) {
                    SnakesProto.GameState.Coord point = snake.getPoints(j);
                    modifiedSnakeBuilder.addPoints(point);
                }

                SnakesProto.GameState.Snake modifiedSnake = modifiedSnakeBuilder.build();

                gameInfo.getSnakes().remove(i);
                gameInfo.getSnakes().add(i, modifiedSnake);

                log.debug(modifiedSnake.getPointsList());
            }
        }
    }
}
