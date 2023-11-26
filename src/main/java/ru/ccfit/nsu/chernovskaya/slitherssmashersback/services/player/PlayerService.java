package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.player;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.GamePlayersTable;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.GameStateMsg;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.GamesListMsg;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.Steer;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GamesInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.GameAnnouncement;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.GamePlayer;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.ID_ENUM;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final GameInfo gameInfo;
    private final GamesInfo gamesInfo;

    public void setGameData(GameAnnouncement gameAnnouncement) {
        gameInfo.setMasterInetAddress(gameAnnouncement.getMasterAddress().getHostAddress());
        gameInfo.setMasterPort(gameAnnouncement.getMasterPort());

        gameInfo.setNodeRole(SnakesProto.NodeRole.NORMAL);
        gameInfo.setGameConfig(gameAnnouncement.getConfig());
        gameInfo.setCanJoin(gameAnnouncement.isCanJoin());
        gameInfo.setGameName(gameAnnouncement.getGameName());
        gameInfo.setAlive(true);
    }

    public SnakesProto.GameMessage generateJoinMessage(String playerName, String gameName) {
        SnakesProto.GameMessage.JoinMsg joinMsg = SnakesProto.GameMessage.JoinMsg
                .newBuilder()
                .setPlayerName(playerName)
                .setGameName(gameName)
                .setRequestedRole(SnakesProto.NodeRole.NORMAL)
                .build();

        return SnakesProto.GameMessage
                .newBuilder()
                .setJoin(joinMsg)
                .setMsgSeq(gameInfo.getIncrementMsgSeq())
                .build();
    }

    public GameStateMsg generateGameStateMessage() {
        boolean isAlive = false;
        for (GamePlayer gamePlayer : gameInfo.getGamePlayers()) {
            if (gamePlayer.getId() == gameInfo.getPlayerId()) {
                isAlive = true;
                break;
            }
        }

        if (gameInfo.getPlayerId() == ID_ENUM.UNDEFINED.getValue()) isAlive = true;

        return new GameStateMsg(gameInfo.getGamePlayers(),
                gameInfo.getSnakes(),
                gameInfo.getFoods(),
                isAlive,
                gameInfo.getScore());
    }

    public SnakesProto.GameMessage generateSteerMessage(Steer steer) {
        SnakesProto.GameMessage.SteerMsg steerMsg = SnakesProto.GameMessage.SteerMsg
                .newBuilder()
                .setDirection(steer.getheadDirection())
                .build();

        return SnakesProto.GameMessage
                .newBuilder()
                .setSteer(steerMsg)
                .setMsgSeq(gameInfo.getIncrementMsgSeq())
                .setSenderId(gameInfo.getPlayerId())
                .build();
    }

    public GamesListMsg generateGamesListMsg() {
        List<String> gameNames = new ArrayList<>();
        for (GameAnnouncement gameAnnouncement : gamesInfo.getGameAnnouncementMap().values()) {
            if (gameAnnouncement.isCanJoin()) {
                gameNames.add(gameAnnouncement.getGameName());
            }
        }

        return new GamesListMsg(gameNames);
    }

    public GamePlayersTable generateGamePlayersTable() {
        GamePlayersTable gamePlayersTable = new GamePlayersTable();

        for (GamePlayer gamePlayer : gameInfo.getGamePlayers()) {
            gamePlayersTable.getGamePlayerTable().put(gamePlayer.getName(), gamePlayer.getScore());
        }

        return gamePlayersTable;
    }

    public SnakesProto.GameMessage generateRoleChangeMessageExit() {
        SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg
                .newBuilder()
                .setSenderRole(SnakesProto.NodeRole.VIEWER)
                .build();
       return SnakesProto.GameMessage
               .newBuilder()
               .setRoleChange(roleChangeMsg)
               .setSenderId(gameInfo.getPlayerId())
               .setMsgSeq(gameInfo.getIncrementMsgSeq())
               .build();
    }

    public void clearGameInfoData() {
        gameInfo.setGameConfig(null);
        gameInfo.setCanJoin(true);
        gameInfo.setGameName(null);
        gameInfo.getSnakes().clear();
        gameInfo.getGamePlayers().clear();;
        gameInfo.setHeight(0);
        gameInfo.setWidth(0);
        gameInfo.setMasterInetAddress("");
        gameInfo.setMasterPort(0);
        gameInfo.setPlayerId(ID_ENUM.UNDEFINED.getValue());
        gameInfo.setScore(0);
        gameInfo.setAlive(true);
        gameInfo.setStateOrder(0);
        gameInfo.setMsqSeq(-1);
    }
}
