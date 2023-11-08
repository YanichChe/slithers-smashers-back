package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

@Service
public class MasterService {

    private final GameInfo gameInfo;
    private final ConnectionService connectionService;

    @Autowired
    public MasterService(GameInfo gameInfo, ConnectionService connectionService) {
        this.gameInfo = gameInfo;
        this.connectionService = connectionService;
    }

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

    public SnakesProto.GameMessage joinHandler(String playerName) {
        int id = connectionService.createNewGamePlayer(playerName, SnakesProto.NodeRole.NORMAL);

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
}
