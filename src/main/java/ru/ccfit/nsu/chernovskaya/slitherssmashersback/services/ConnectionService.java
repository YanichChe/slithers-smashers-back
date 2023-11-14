package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import com.google.protobuf.ByteString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

import java.util.UUID;

@Service
@Log4j2
public class ConnectionService {

    private final GameInfo gameInfo;
    private int id = 0;
    @Autowired
    public ConnectionService(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }

    /**
     * Генерация индификатора.
     *
     * @return id
     */
    synchronized private int generateUniqueId() {
        return (id++);
    }

    /**
     * Создает нового игрока.
     *
     * @param username имя юзера
     * @param nodeRole роль в игре
     * @param ipAddress адресс юзера
     * @param port порт юзера
     * @return уникальный инфификатор игрока
     */
    public int createNewGamePlayer(String username, SnakesProto.NodeRole nodeRole, String ipAddress, int port) {
        int id = generateUniqueId();
        SnakesProto.GamePlayer gamePlayer = SnakesProto.GamePlayer
                .newBuilder()
                .setName(username)
                .setScore(0)
                .setId(id)
                .setIpAddress(ipAddress)
                .setPort(port)
                .setRole(nodeRole)
                .build();
        gameInfo.getGamePlayers().add(gamePlayer);
        log.info("New player " + gamePlayer.toString());
        return id;
    }

    /**
     * Создает нового игрока.
     *
     * @param username имя юзера
     * @param nodeRole роль в игре
     * @return уникальный инфификатор игрока
     */
    public int createNewGamePlayer(String username, SnakesProto.NodeRole nodeRole) {
        int id = generateUniqueId();
        SnakesProto.GamePlayer gamePlayer = SnakesProto.GamePlayer
                .newBuilder()
                .setName(username)
                .setScore(0)
                .setId(id)
                .setRole(nodeRole)
                .build();
        gameInfo.getGamePlayers().add(gamePlayer);
        log.info("New player " + gamePlayer.toString());
        return id;
    }

    /**
     * Создает змейку на заданных координатах для игрока с id
     *
     * @param coords коордиинаты змейки
     * @param id инфикатор владельца змейки
     */
    public void createNewSnake(SnakesProto.GameState.Coord[] coords, int id) {
        SnakesProto.GameState.Snake snake = SnakesProto.GameState.Snake
                .newBuilder()
                .setState(SnakesProto.GameState.Snake.SnakeState.ALIVE)
                .setPlayerId(id)
                .addPoints(coords[0])
                .addPoints(coords[1])
                .setHeadDirection(SnakesProto.Direction.RIGHT)
                .build();

        gameInfo.getSnakes().add(snake);
        log.info("New snake " + snake.toString());
    }

    /**
     * Алгоритм поиска свободного квадрат 5х5
     *
     * @return свободные координаты
     */
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
            gameField[coord.getY() * width + coord.getX()] = 1;
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
