package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.GamesListMsg;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.GameStateMsg;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.JoinMsg;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto.GameAnnouncementDTO;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto.Steer;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GamesInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.info.GameInfoService;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.info.GamesInfoService;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master.MasterService;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.net.SenderService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/player")
@Log4j2
public class PlayerController {

    private final GameInfo gameInfo;
    private final GameInfoService gameInfoService;
    private final GamesInfo gamesInfo;
    private final GamesInfoService gamesInfoService;
    private final SenderService sender;
    private final MasterService masterService;

    @Autowired
    public PlayerController(GameInfo gameInfo, GameInfoService gameInfoService, GamesInfo gamesInfo, GamesInfoService gamesInfoService, SenderService sender,
                            MasterService masterService) {
        this.gameInfo = gameInfo;
        this.gameInfoService = gameInfoService;
        this.gamesInfo = gamesInfo;
        this.gamesInfoService = gamesInfoService;
        this.sender = sender;
        this.masterService = masterService;
    }

    /**
     * @return состояние игры
     */
    @GetMapping("/game-state")
    public ResponseEntity<GameStateMsg> getGameState() {
        if (gameInfo.getPlayerId() == -1) {
            GameStateMsg gameStateMsg = new GameStateMsg(gameInfo.getGamePlayers(),
                    gameInfo.getSnakes(),
                    gameInfo.getFoods(),
                    gameInfo.isAlive(),
                    0);

            return ResponseEntity.ok()
                    .body(gameStateMsg);
        }
        SnakesProto.GamePlayer gamePlayer = gameInfo.getGamePlayers()
                .get(gameInfoService.findPlayerIndexById(gameInfo.getPlayerId()));
        GameStateMsg gameStateMsg = new GameStateMsg(gameInfo.getGamePlayers(),
                gameInfo.getSnakes(),
                gameInfo.getFoods(),
                gameInfo.isAlive(),
                gamePlayer.getScore());

        log.debug(gameInfo.getSnakes());
        return ResponseEntity.ok()
                .body(gameStateMsg);
    }

    /**
     * Обновление напрвления змейки.
     * Если змейка не является MASTER то отправляется сообщение о смене направления
     * серверу.
     *
     * @param steer - новое направление змейки
     * @return сообщение об успехе
     * @throws UnknownHostException
     */
    @PatchMapping("/change-direction")
    synchronized public ResponseEntity<String> updateDirection(@RequestBody Steer steer) throws UnknownHostException,
            InterruptedException {

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

            sender.sendMessage(gameMessage, InetAddress.getByName(gameInfo.getMasterInetAddress()),
                    gameInfo.getMasterPort());
            sender.waitAck(gameInfo.getMsqSeq(), gameMessage);
        }

        return ResponseEntity.ok("update request");
    }

    /**
     * Список в текущих доступных игр.
     *
     * @return сообщение со списком названий игр.
     */
    @GetMapping("/games-list")
    public ResponseEntity<GamesListMsg> getGamesList() {

        List<String> gameNames = new ArrayList<>();
        for (GameAnnouncementDTO gameAnnouncement : gamesInfo.getGameAnnouncementList()) {
            if (gameAnnouncement.isCanJoin()) {
                gameNames.add(gameAnnouncement.getGameName());
            }
        }

        GamesListMsg gamesListMsg = new GamesListMsg(gameNames);

        return ResponseEntity.ok().body(gamesListMsg);
    }

    /**
     * Запрос на подключение к игре
     *
     * @param joinMsgRequest данные запроса на подключения.
     * @return сообщение об успехе присоединения.
     */
    @PostMapping("/join")
    public ResponseEntity<String> sendJoinMessage(@RequestBody JoinMsg joinMsgRequest) {

        SnakesProto.GameMessage.JoinMsg joinMsg = SnakesProto.GameMessage.JoinMsg
                .newBuilder()
                .setPlayerName(joinMsgRequest.getPlayerName())
                .setGameName(joinMsgRequest.getGameName())
                .setRequestedRole(SnakesProto.NodeRole.NORMAL)
                .build();

        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage
                .newBuilder()
                .setJoin(joinMsg)
                .setMsgSeq(gameInfo.getIncrementMsgSeq())
                .setReceiverId(0)
                .build();

        GameAnnouncementDTO gameAnnouncement = gamesInfoService.getAnnouncementDTOByName(joinMsgRequest.getGameName());

        sender.sendMessage(gameMessage, gameAnnouncement.getMasterAddress(),
                gameAnnouncement.getMasterPort());

        /*int error = unicastMessageService.waitAck(gameInfo.getMsqSeq(), gameMessage);
        if (error == 0) {
            return ResponseEntity.ok("not found place");
        }*/

        gameInfo.setMasterInetAddress(String.valueOf(gameAnnouncement.getMasterAddress()));
        gameInfo.setMasterPort(gameAnnouncement.getMasterPort());

        gameInfo.setNodeRole(SnakesProto.NodeRole.NORMAL);
        gameInfo.setGameConfig(gameAnnouncement.getConfig());
        gameInfo.setCanJoin(gameAnnouncement.isCanJoin());
        gameInfo.setGameName(gameAnnouncement.getGameName());
        gameInfo.setAlive(true);

        return ResponseEntity.ok("join");
    }
}
