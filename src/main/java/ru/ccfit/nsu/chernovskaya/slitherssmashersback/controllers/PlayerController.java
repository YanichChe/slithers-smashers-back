package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.GamesListMsg;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.GameStateMsg;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto.Steer;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GamesInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.ConnectionService;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.UnicastMessageService;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RestController
@RequestMapping("/player")
public class PlayerController {

    private final GameInfo gameInfo;
    private final GamesInfo gamesInfo;
    private final UnicastMessageService unicastMessageService;
    private final ConnectionService connectionService;

    @Autowired
    public PlayerController(GameInfo gameInfo, GamesInfo gamesInfo, UnicastMessageService unicastMessageService,
                            ConnectionService connectionService) {
        this.gameInfo = gameInfo;
        this.gamesInfo = gamesInfo;
        this.unicastMessageService = unicastMessageService;
        this.connectionService = connectionService;
    }

    @GetMapping("/game-state")
    public ResponseEntity<GameStateMsg> getGameState() {
        GameStateMsg gameStateMsg = new GameStateMsg(gameInfo.getGamePlayers(), gameInfo.getSnakes());

        return ResponseEntity.ok()
                .body(gameStateMsg);
    }

    @PatchMapping("/change-direction")
    synchronized public ResponseEntity<String> updateDirection(@RequestBody Steer steer) {

        if (gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {
            Iterator<SnakesProto.GameState.Snake> iterator = gameInfo.getSnakes().iterator();
            while (iterator.hasNext()) {
                SnakesProto.GameState.Snake snake = iterator.next();
                if (snake.getPlayerId() == 0) {
                    iterator.remove();
                    gameInfo.getSnakes().add(snake.toBuilder().setHeadDirection(steer.getheadDirection()).build());
                    return ResponseEntity.ok("update request");
                }
            }
        }

        return ResponseEntity.ok("update request");
    }

    @GetMapping("/games-list")
    public ResponseEntity<GamesListMsg> getGamesList() {

        List<String> gameNames = new ArrayList<>();
        for (SnakesProto.GameAnnouncement gameAnnouncement : gamesInfo.getGameAnnouncementList()) {
            if (gameAnnouncement.getCanJoin()) {
                gameNames.add(gameAnnouncement.getGameName());
            }
        }

        GamesListMsg gamesListMsg = new GamesListMsg(gameNames);

        return ResponseEntity.ok().body(gamesListMsg);
    }

    @PostMapping("/join")
    public ResponseEntity<String> sendJoinMessage(@RequestBody String gameName) {
        SnakesProto.GameMessage.JoinMsg joinMsg = SnakesProto.GameMessage.JoinMsg
                .newBuilder()
                .setPlayerName(gameInfo.getPlayerName())
                .setGameName(gameName)
                .setRequestedRole(SnakesProto.NodeRole.NORMAL)
                .build();

        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage
                .newBuilder()
                .setJoin(joinMsg)
                .setMsgSeq(gameInfo.getIncrementMsgSeq())
                .setReceiverId(0)
                .build();

        try {
            unicastMessageService.sendMessage(gameMessage, InetAddress.getByName(gameInfo.getMasterInetAddress()),
                    gameInfo.getMasterPort());
        } catch (IOException e) {
            return ResponseEntity.status(504).body("io error");
        }

        //TODO получение результата подсоединения

        SnakesProto.GameAnnouncement gameAnnouncement = gamesInfo.getAnnouncementByName(gameName);

        gameInfo.getGamePlayers().addAll(gameAnnouncement.getPlayers().getPlayersList());
        gameInfo.setGameConfig(gameAnnouncement.getConfig());
        gameInfo.setCanJoin(gameAnnouncement.getCanJoin());
        gameInfo.setGameName(gameAnnouncement.getGameName());

        return ResponseEntity.ok("join");
    }
}
