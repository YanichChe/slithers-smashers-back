package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.Coord;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.Direction;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.GamePlayer;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.Snake;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.mapper.ProtobufMapper;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MasterService {

    private final GameInfo gameInfo;
    private final ConnectionService connectionService;
    private final ProtobufMapper mapper;


    /**
     * Обработчик подсоединения нового игрока к игре.
     *
     * @param playerName имя нового игрока
     * @param ipAddress  адресс нового игрока
     * @param port       порт нового игрока
     * @return Сообщение об ошибке или ответный успех с id игроком
     */
    public SnakesProto.GameMessage joinHandler(String playerName, String ipAddress, int port, long msgSeq) {

        List<GamePlayer> gamePlayerList = gameInfo.getGamePlayers();
        synchronized (gamePlayerList) {
            for (GamePlayer gamePlayer: gamePlayerList) {
                if (gamePlayer.getAddress().equals(ipAddress) && gamePlayer.getPort() == port) {
                    SnakesProto.GameMessage.ErrorMsg errorMsg = SnakesProto.GameMessage.ErrorMsg
                            .newBuilder()
                            .setErrorMessage("you are in game")
                            .build();

                    return SnakesProto.GameMessage
                            .newBuilder()
                            .setError(errorMsg)
                            .setMsgSeq(gameInfo.getIncrementMsgSeq())
                            .build();
                }
            }
        }

        int id = connectionService.createNewGamePlayer(playerName, SnakesProto.NodeRole.NORMAL, ipAddress, port);

        Coord[] coords = connectionService.searchPlace();
        if (coords == null) {
            SnakesProto.GameMessage.ErrorMsg errorMsg = SnakesProto.GameMessage.ErrorMsg
                    .newBuilder()
                    .setErrorMessage("not found place")
                    .build();

            return SnakesProto.GameMessage
                    .newBuilder()
                    .setError(errorMsg)
                    .setMsgSeq(gameInfo.getIncrementMsgSeq())
                    .build();

        } else {
            SnakesProto.GameMessage gameMessageNew = SnakesProto.GameMessage
                    .newBuilder()
                    .setAck(SnakesProto.GameMessage.AckMsg.newBuilder().build())
                    .setReceiverId(id)
                    .setMsgSeq(msgSeq)
                    .build();
            connectionService.createNewSnake(coords, id);
            return gameMessageNew;
        }
    }

    /**
     * Изменение направления змеи.
     *
     * @param playerId  id игрока
     * @param direction новое направление
     */
    public void changeSnakeDirection(int playerId, SnakesProto.Direction direction) {
        List<Snake> snakes = gameInfo.getSnakes();
        for (Snake snake : snakes) {

            if (snake.getPlayerId() == playerId) {
                if (snake.getHeadDirection().equals(Direction.UP) &&
                        direction.equals(Direction.DOWN)
                        || snake.getHeadDirection().equals(Direction.DOWN) &&
                        direction.equals(SnakesProto.Direction.UP)
                        || snake.getHeadDirection().equals(Direction.RIGHT) &&
                        direction.equals(Direction.LEFT)
                        || snake.getHeadDirection().equals(Direction.LEFT) &&
                        direction.equals(Direction.RIGHT)
                ) break;
                snake.setHeadDirection(mapper.map(direction));

                break;
            }
        }
    }


}
