package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

import java.util.Iterator;

@Service
public class MasterService {

    private final GameInfo gameInfo;
    private final ConnectionService connectionService;

    @Autowired
    public MasterService(GameInfo gameInfo, ConnectionService connectionService) {
        this.gameInfo = gameInfo;
        this.connectionService = connectionService;
    }

    /**
     * Генерация сообщения-уведомления об игре
     *
     * @return новое сообщение с @AnnouncementMessage
     */
    public SnakesProto.GameMessage  generateAnnouncementMessage() {

        SnakesProto.GamePlayers gamePlayers = SnakesProto.GamePlayers
                .newBuilder()
                .addAllPlayers(gameInfo.getGamePlayers())
                .build();

        SnakesProto.GameAnnouncement gameAnnouncement = SnakesProto.GameAnnouncement.newBuilder()
                .setPlayers(gamePlayers)
                .setConfig(gameInfo.getGameConfig())
                .setCanJoin(gameInfo.isCanJoin())
                .setGameName(gameInfo.getGameName())
                .build();

        SnakesProto.GameMessage.AnnouncementMsg announcementMsg =
                SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                        .addGames(gameAnnouncement)
                        .build();

        SnakesProto.GameMessage gameMessageNew = SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(gameInfo.getIncrementMsgSeq())
                .setAnnouncement(announcementMsg)
                .build();

        return gameMessageNew;
    }

    /**
     * Обработчик подсоединения нового игрока к игре.
     *
     * @param playerName имя нового игрока
     * @param ipAddress адресс нового игрока
     * @param port порт нового игрока
     * @return Сообщение об ошибке или ответный успех с id игроком
     */
    public SnakesProto.GameMessage joinHandler(String playerName, String ipAddress, int port) {
        int id = connectionService.createNewGamePlayer(playerName, SnakesProto.NodeRole.NORMAL, ipAddress, port);

        SnakesProto.GameState.Coord[] coords = connectionService.searchPlace();
        if (coords == null) {
            SnakesProto.GameMessage.ErrorMsg errorMsg = SnakesProto.GameMessage.ErrorMsg
                    .newBuilder()
                    .setErrorMessage("not found place")
                    .build();
            SnakesProto.GameMessage gameMessageNew = SnakesProto.GameMessage
                    .newBuilder()
                    .setError(errorMsg)
                    .build();

            return gameMessageNew;

        } else {
            SnakesProto.GameMessage gameMessageNew = SnakesProto.GameMessage
                    .newBuilder()
                    .setAck(SnakesProto.GameMessage.AckMsg.newBuilder().build())
                    .setReceiverId(id)
                    .build();
            connectionService.createNewSnake(coords, id);
            return gameMessageNew;
        }
    }

    /**
     * Изменение направления змеи.
     *
     * @param playerId id игрока
     * @param direction новое направление
     */
    public void changeSnakeDirection(int playerId, SnakesProto.Direction direction) {
        Iterator<SnakesProto.GameState.Snake> iterator = gameInfo.getSnakes().iterator();
        while (iterator.hasNext()) {
            SnakesProto.GameState.Snake snake = iterator.next();
            if (snake.getPlayerId() == playerId) {
                if (snake.getHeadDirection().equals(SnakesProto.Direction.UP) &&
                        direction.equals(SnakesProto.Direction.DOWN)
                        || snake.getHeadDirection().equals(SnakesProto.Direction.DOWN) &&
                        direction.equals(SnakesProto.Direction.UP)
                        || snake.getHeadDirection().equals(SnakesProto.Direction.RIGHT) &&
                        direction.equals(SnakesProto.Direction.LEFT)
                        || snake.getHeadDirection().equals(SnakesProto.Direction.LEFT) &&
                        direction.equals(SnakesProto.Direction.RIGHT)
                )
                    break;
                iterator.remove();
                gameInfo.getSnakes().add(snake.toBuilder().setHeadDirection(direction).build());
                break;
            }
        }
    }
}
