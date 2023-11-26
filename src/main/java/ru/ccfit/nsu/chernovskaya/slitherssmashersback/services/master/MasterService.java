package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.*;
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
                if ((snake.getHeadDirection().equals(Direction.UP) &&
                        direction.equals(SnakesProto.Direction.DOWN))
                        || (snake.getHeadDirection().equals(Direction.DOWN) &&
                        direction.equals(SnakesProto.Direction.UP))
                        || (snake.getHeadDirection().equals(Direction.RIGHT) &&
                        direction.equals(SnakesProto.Direction.LEFT))
                        || (snake.getHeadDirection().equals(Direction.LEFT) &&
                        direction.equals(SnakesProto.Direction.RIGHT))
                ) break;
                snake.setHeadDirection(mapper.map(direction));

                break;
            }
        }
    }

    public GamePlayer findNewDeputy() {
        boolean hasDeputy = false;
        for (GamePlayer gamePlayer: gameInfo.getGamePlayers()) {
            if (gamePlayer.getRole().equals(Role.DEPUTY)) {
                hasDeputy = true;
                break;
            }
        }

        if (hasDeputy) return null;

        for(GamePlayer gamePlayer_: gameInfo.getGamePlayers()) {
            if (!gamePlayer_.getRole().equals(Role.MASTER)) {
                gamePlayer_.setRole(Role.DEPUTY);
                return gamePlayer_;
            }
        }
        return null;
    }

    public SnakesProto.GameMessage generateRoleChangeMessageAboutNewMaster() {
        SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg
                .newBuilder()
                .setSenderRole(SnakesProto.NodeRole.MASTER)
                .build();

        return SnakesProto.GameMessage
                .newBuilder()
                .setMsgSeq(gameInfo.getIncrementMsgSeq())
                .setRoleChange(roleChangeMsg)
                .build();
    }

    public SnakesProto.GameMessage generateRoleChangeMessageNewDeputy() {
        SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg
                .newBuilder()
                .setSenderRole(SnakesProto.NodeRole.MASTER)
                .setReceiverRole(SnakesProto.NodeRole.DEPUTY)
                .build();

        return SnakesProto.GameMessage
                .newBuilder()
                .setMsgSeq(gameInfo.getIncrementMsgSeq())
                .setRoleChange(roleChangeMsg)
                .build();
    }
}
