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
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.MasterService;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.UnicastMessageService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RestController
@RequestMapping("/player")
public class PlayerController {

    private final GameInfo gameInfo;
    private final GamesInfo gamesInfo;
    private final UnicastMessageService unicastMessageService;
    private final MasterService masterService;
    @Autowired
    public PlayerController(GameInfo gameInfo, GamesInfo gamesInfo, UnicastMessageService unicastMessageService,
                            MasterService masterService) {
        this.gameInfo = gameInfo;
        this.gamesInfo = gamesInfo;
        this.unicastMessageService = unicastMessageService;
        this.masterService = masterService;
    }

    @GetMapping("/game-state")
    public ResponseEntity<GameStateMsg> getGameState() {
        GameStateMsg gameStateMsg = new GameStateMsg(gameInfo.getGamePlayers(), gameInfo.getSnakes());

        return ResponseEntity.ok()
                .body(gameStateMsg);
    }

    @PatchMapping("/change-direction")
    synchronized public ResponseEntity<String> updateDirection(@RequestBody Steer steer) throws UnknownHostException {

        if (gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {
            masterService.changeSnakeDirection(0, steer.getheadDirection());
        } else {
            SnakesProto.GameMessage.SteerMsg steerMsg = SnakesProto.GameMessage.SteerMsg
                    .newBuilder()
                    .setDirection(steer.getheadDirection())
                    .build();

            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage
                    .newBuilder()
                    .setSteer(steerMsg)
                    .setMsgSeq(gameInfo.getIncrementMsgSeq())
                    .setSenderId(gameInfo.getPlayerId())
                    .build();

            unicastMessageService.sendMessage(gameMessage, InetAddress.getByName(gameInfo.getMasterInetAddress()),
                    gameInfo.getMasterPort());
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
