package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.GameStateMsg;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;

@RestController
@RequestMapping("/player")
public class PlayerController {

    private final GameInfo gameInfo;

    @Autowired
    public PlayerController(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }

    @GetMapping("/game-state")
    public ResponseEntity<GameStateMsg> refreshTokens() {
        GameStateMsg gameStateMsg = new GameStateMsg(gameInfo.getGamePlayers(), gameInfo.getSnakes());

        return ResponseEntity.ok()
                .body(gameStateMsg);
    }
}
