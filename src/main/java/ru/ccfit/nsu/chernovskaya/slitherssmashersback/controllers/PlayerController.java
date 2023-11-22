package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.GamesListMsg;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.GameStateMsg;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.JoinMsg;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.GameAnnouncement;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.Steer;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GamesInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.GamePlayer;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.ID_ENUM;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master.MasterService;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.net.UnicastService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/player")
@RequiredArgsConstructor
@Log4j2
public class PlayerController {

    private final GameInfo gameInfo;
    private final GamesInfo gamesInfo;
    private final UnicastService sender;
    private final MasterService masterService;

    /**
     * @return состояние игры
     */
    @GetMapping("/game-state")
    public ResponseEntity<GameStateMsg> getGameState() {

        boolean isAlive = false;
        for (GamePlayer gamePlayer: gameInfo.getGamePlayers()) {
            if (gamePlayer.getId() == gameInfo.getPlayerId()) {
                isAlive = true;
                break;
            }
        }

        if (gameInfo.getPlayerId() == ID_ENUM.UNDEFINED.getValue()) isAlive = true;
        log.info(gameInfo.getPlayerId());

        GameStateMsg gameStateMsg = new GameStateMsg(gameInfo.getGamePlayers(),
                gameInfo.getSnakes(),
                gameInfo.getFoods(),
                isAlive,
                gameInfo.getScore());

        log.info(gameStateMsg);
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
            //sender.waitAck(gameInfo.getMsqSeq(), gameMessage);
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
        for (GameAnnouncement gameAnnouncement : gamesInfo.getGameAnnouncementMap().values()) {
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


        GameAnnouncement gameAnnouncement = gamesInfo.getGameAnnouncementMap().get(joinMsg.getGameName());

        sender.sendMessage(gameMessage, gameAnnouncement.getMasterAddress(),
                gameAnnouncement.getMasterPort());

        /*int error = unicastMessageService.waitAck(gameInfo.getMsqSeq(), gameMessage);
        if (error == 0) {
            return ResponseEntity.ok("not found place");
        }*/

        gameInfo.setMasterInetAddress(gameAnnouncement.getMasterAddress().getHostAddress());
        gameInfo.setMasterPort(gameAnnouncement.getMasterPort());

        gameInfo.setNodeRole(SnakesProto.NodeRole.NORMAL);
        gameInfo.setGameConfig(gameAnnouncement.getConfig());
        gameInfo.setCanJoin(gameAnnouncement.isCanJoin());
        gameInfo.setGameName(gameAnnouncement.getGameName());
        gameInfo.setAlive(true);

        return ResponseEntity.ok("join");
    }
}
