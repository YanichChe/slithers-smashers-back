package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.GameAnnouncement;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GamesInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.GamePlayer;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.ID_ENUM;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.Role;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master.MasterService;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.net.UnicastService;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.player.PlayerService;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
    private final PlayerService playerService;

    /**
     * @return состояние игры
     */
    @GetMapping("/game-state")
    public ResponseEntity<GameStateMsg> getGameState() {

        return ResponseEntity.ok()
                .body(playerService.generateGameStateMessage());
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
            masterService.changeSnakeDirection(gameInfo.getPlayerId(), steer.getheadDirection());
        } else {
            sender.sendMessage(playerService.generateSteerMessage(steer),
                    InetAddress.getByName(gameInfo.getMasterInetAddress()),
                    gameInfo.getMasterPort(), true);
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

        return ResponseEntity.ok().body(playerService.generateGamesListMsg());
    }

    /**
     * @return сообщение со списком игроков и их очков
     */
    @GetMapping("/players-table")
    public ResponseEntity<GamePlayersTable> getPlayersTable() {

        return ResponseEntity.ok().body(playerService.generateGamePlayersTable());
    }

    /**
     * Запрос на подключение к игре
     *
     * @param joinMsgRequest данные запроса на подключения.
     * @return сообщение об успехе/не успехе присоединения.
     */
    @PostMapping("/join")
    public ResponseEntity<String> sendJoinMessage(@RequestBody JoinMsg joinMsgRequest) {

        GameAnnouncement gameAnnouncement = gamesInfo.getGameAnnouncementMap().get(joinMsgRequest.getGameName());
        sender.sendMessage(playerService.generateJoinMessage(joinMsgRequest.getPlayerName(),
                        joinMsgRequest.getGameName()), gameAnnouncement.getMasterAddress(),
                gameAnnouncement.getMasterPort(), true);

        while (true) {
            if (gameInfo.getPlayerId() != ID_ENUM.UNDEFINED.getValue()) break;
        }
        if (gameInfo.getPlayerId() == ID_ENUM.NOT_JOIN.getValue()) return ResponseEntity.ok("not enough place");

        playerService.setGameData(gameAnnouncement);
        log.info("You are in game");
        log.info(gameInfo.getGameConfig());
        return ResponseEntity.ok("join");
    }

    /**
     * Отправка сообщения мастеру о том, что хочешь выйти из игры
     *
     * @return сообщение о выходе из игры
     * @throws UnknownHostException
     */
    @PostMapping("/exit")
    public ResponseEntity<String> exit() throws UnknownHostException {
        if (gameInfo.getNodeRole().equals(SnakesProto.NodeRole.MASTER)) {

            gameInfo.setNodeRole(SnakesProto.NodeRole.VIEWER);

            int deputyId = -1;

            List<GamePlayer> gamePlayerList = gameInfo.getGamePlayers();
            GamePlayer deputy = null;
            for (int i = 0; i < gameInfo.getGamePlayers().size(); i++) {
                if (gamePlayerList.get(i).getRole().equals(Role.DEPUTY)) {
                    deputyId = gamePlayerList.get(i).getId();
                    deputy = gamePlayerList.get(i);
                    break;
                }
            }

            SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg
                    .newBuilder()
                    .setSenderRole(SnakesProto.NodeRole.VIEWER)
                    .setReceiverRole(SnakesProto.NodeRole.MASTER)
                    .build();
            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage
                    .newBuilder()
                    .setRoleChange(roleChangeMsg)
                    .setSenderId(deputyId)
                    .setReceiverId(gameInfo.getPlayerId())
                    .setMsgSeq(gameInfo.getIncrementMsgSeq())
                    .build();

            if (deputyId != -1)
                sender.sendMessage(gameMessage,
                    InetAddress.getByName(deputy.getAddress()), deputy.getPort(), true);
        } else {
            sender.sendMessage(playerService.generateRoleChangeMessageExit(),
                    InetAddress.getByName(gameInfo.getMasterInetAddress()), gameInfo.getMasterPort(), true);
        }

        playerService.clearGameInfoData();
        return ResponseEntity.ok("exit");
    }
}
