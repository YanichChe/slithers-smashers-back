package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.ConnectionService;

@RestController
@RequestMapping("/master")
@Log4j2
public class MasterController {

    private final GameInfo gameInfo;
    private final ConnectionService connectionService;

    @Autowired
    public MasterController(GameInfo gameInfo, ConnectionService connectionService) {
        this.gameInfo = gameInfo;
        this.connectionService = connectionService;
    }

    @GetMapping("/start")
    public void startNewGame(@RequestParam int height,
                             @RequestParam int width,
                             @RequestParam int foodStatic,
                             @RequestParam int stateDelayMs,
                             @RequestParam String gameName,
                             @RequestParam String username,
                             @RequestParam String ipAddress,
                             @RequestParam int port) {

        SnakesProto.GameConfig gameConfig = SnakesProto.GameConfig
                .newBuilder()
                .setHeight(height)
                .setWidth(width)
                .setFoodStatic(foodStatic)
                .setStateDelayMs(stateDelayMs)
                .build();

        gameInfo.setGameName(gameName);
        gameInfo.setHeight(height);
        gameInfo.setWidth(width);

        connectionService.createNewGamePlayer(username, ipAddress, port, SnakesProto.NodeRole.MASTER);
        SnakesProto.GameState.Coord[] coords = connectionService.searchPlace();
        connectionService.createNewSnake(coords);

        gameInfo.setGameConfig(gameConfig);

        log.info("Game config " + gameConfig.toString());
    }
}
