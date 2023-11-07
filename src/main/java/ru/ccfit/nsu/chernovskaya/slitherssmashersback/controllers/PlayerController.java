package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.GameStateMsg;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto.Steer;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

import java.util.Iterator;

@RestController
@RequestMapping("/player")
public class PlayerController {

    private final GameInfo gameInfo;

    @Autowired
    public PlayerController(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
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
}
