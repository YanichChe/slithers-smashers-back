package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto.GameRequestDTO;
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

    @PostMapping("/start")
    public ResponseEntity<String> startGame(@RequestBody GameRequestDTO gameRequest) {

        SnakesProto.GameConfig gameConfig = SnakesProto.GameConfig
                .newBuilder()
                .setHeight(gameRequest.getHeight())
                .setWidth(gameRequest.getWidth())
                .setFoodStatic(gameRequest.getFoodStatic())
                .setStateDelayMs(gameRequest.getStateDelayMs())
                .build();

        gameInfo.setGameName(gameRequest.getGameName());
        gameInfo.setHeight(gameRequest.getHeight());
        gameInfo.setWidth(gameRequest.getWidth());
        gameInfo.setNodeRole(SnakesProto.NodeRole.MASTER);

        int id = connectionService.createNewGamePlayer(gameRequest.getUsername(), gameRequest.getIpAddress(),
                gameRequest.getPort(), SnakesProto.NodeRole.MASTER);

        SnakesProto.GameState.Coord[] coords = connectionService.searchPlace();
        connectionService.createNewSnake(coords, id);

        gameInfo.setGameConfig(gameConfig);

        log.info("Game config " + gameConfig.toString());
        return ResponseEntity.ok("Игра начата");
    }
}
