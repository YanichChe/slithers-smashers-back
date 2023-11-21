package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.mapper.ProtobufMapper;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class ConnectionService {

    private final GameInfo gameInfo;
    private final ProtobufMapper protobufMapper;
    private int id = 0;


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

        GamePlayer gamePlayer = new GamePlayer();
        gamePlayer.setName(username);
        gamePlayer.setId(id);
        gamePlayer.setRole(protobufMapper.map(nodeRole));
        gamePlayer.setAddress(ipAddress);
        gamePlayer.setPort(port);

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

        GamePlayer gamePlayer = new GamePlayer();
        gamePlayer.setName(username);
        gamePlayer.setId(id);
        gamePlayer.setRole(protobufMapper.map(nodeRole));

        gameInfo.getGamePlayers().add(gamePlayer);
        log.info("New player " + gamePlayer);
        return id;
    }

    /**
     * Создает змейку на заданных координатах для игрока с id
     *
     * @param coords коордиинаты змейки
     * @param id инфикатор владельца змейки
     */
    public void createNewSnake(Coord[] coords, int id) {

        Snake snake = new Snake();
        snake.setState(State.Alive);
        snake.getCoordList().add(coords[0]);
        snake.getCoordList().add(coords[1]);
        snake.setPlayerId(id);
        snake.setHeadDirection(Direction.RIGHT);

        gameInfo.getSnakes().add(snake);
        log.info("New snake " + snake);
    }

    /**
     * Алгоритм поиска свободного квадрат 5х5
     *
     * @return свободные координаты
     */
    public Coord[] searchPlace() {
        Coord[] coords = new Coord[2];

        int height = gameInfo.getHeight();
        int width = gameInfo.getWidth();

        int[] gameField = new int[height * width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                gameField[i * width + j] = 0;
            }
        }

        for (Snake snake : gameInfo.getSnakes()) {
            for (Coord coord : snake.getCoordList()) {
                gameField[coord.getX() * width + coord.getY()] = 1;
            }
        }

        for (Coord coord : gameInfo.getFoods()) {
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
                    coords[0] = new Coord(i + 2, j + 2);
                    coords[1] = new Coord(i + 3, j + 2);
                    return coords;
                }
            }
        }

        return null;
    }
}
