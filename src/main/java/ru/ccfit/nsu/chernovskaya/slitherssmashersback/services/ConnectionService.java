package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

import java.util.UUID;

@Service
public class ConnectionService {

    private final GameInfo gameInfo;
    private int id;

    @Autowired
    public ConnectionService(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }

    public void generateUniqueId(){
        id = Integer.parseInt(UUID.randomUUID().toString());
    }

    public void createNewGamePlayer(String username, String ipAddress, int port) {
        SnakesProto.GamePlayer gamePlayer = SnakesProto.GamePlayer
                .newBuilder()
                .setName(username)
                .setIpAddressBytes(ByteString.copyFromUtf8(ipAddress))
                .setPort(port)
                .setScore(0)
                .setId(id)
                .build();
        gameInfo.getGamePlayers().getPlayersList().add(gamePlayer);
    }

    public void createNewSnake(SnakesProto.GameState.Coord[] coords) {
        SnakesProto.GameState.Snake snake = SnakesProto.GameState.Snake
                .newBuilder()
                .setState(SnakesProto.GameState.Snake.SnakeState.ALIVE)
                .setPlayerId(id)
                .setPoints(0, coords[0])
                .setPoints(1, coords[1])
                .setHeadDirection(SnakesProto.Direction.RIGHT)
                .build();
    }

    public SnakesProto.GameState.Coord[] searchPlace() {
        SnakesProto.GameState.Coord[] coords = new SnakesProto.GameState.Coord[2];

        int height = gameInfo.getHeight();
        int width = gameInfo.getWidth();

        int[] gameField = new int[height * width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                gameField[i * width + j] = 0;
            }
        }

        for (SnakesProto.GameState.Snake snake : gameInfo.getSnakes()) {
            for (SnakesProto.GameState.Coord coord : snake.getPointsList()) {
                gameField[coord.getX() * width + coord.getY()] = 1;
            }
        }

        for (SnakesProto.GameState.Coord coord : gameInfo.getFoods()) {
            gameField[coord.getX() * width + coord.getY()] = 1;
        }

        for (int i = 0; i <= height - 5; i++) {
            for (int j = 0; j <= width - 5; j++) {
                boolean isZeroSquare = true;
                for (int row = i; row < i + 5; row++) {
                    for (int col = j; col < j + 5; col++) {
                        if (gameField[row * width + col] != 0) {
                            isZeroSquare = false;
                            break;
                        }
                    }
                    if (!isZeroSquare) {
                        break;
                    }
                }
                if (isZeroSquare) {
                    coords[0] = SnakesProto.GameState.Coord.newBuilder().setX(i + 2).setY(j + 2).build();
                    coords[1] = SnakesProto.GameState.Coord.newBuilder().setX(i + 2).setY(j + 3).build();
                    return coords;
                }
            }
        }

        return null;
    }
}
