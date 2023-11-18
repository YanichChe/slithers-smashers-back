package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto.GameRequest;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.ConnectionService;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.FoodService;

@RestController
@RequestMapping("/master")
@Log4j2
public class MasterController {

    private final GameInfo gameInfo;
    private final ConnectionService connectionService;
    @Value(value = "${state.delay.ms}")
    private int stateDelayMs;

    @Autowired
    public MasterController(GameInfo gameInfo, ConnectionService connectionService) {
        this.gameInfo = gameInfo;
        this.connectionService = connectionService;
    }

    /**
     * Инициализация запуска игры.
     * 1. Принимает config игры
     * 2. Заполняет поля game info
     * 3. Устанавливает id
     * 4. Находит свободные координаты для змейки
     * 5. Создает новую змейку
     *
     * @param gameRequest параметры игры.
     * @return ответ о успехе запуска игры
     */
    @PostMapping("/start")
    public ResponseEntity<String> startGame(@RequestBody GameRequest gameRequest) {

        gameInfo.setAlive(true);
        SnakesProto.GameConfig gameConfig = SnakesProto.GameConfig
                .newBuilder()
                .setHeight(gameRequest.getHeight())
                .setWidth(gameRequest.getWidth())
                .setFoodStatic(gameRequest.getFoodStatic())
                .setStateDelayMs(stateDelayMs)
                .build();

        gameInfo.setGameConfig(gameConfig);

        gameInfo.setGameName(gameRequest.getGameName());
        gameInfo.setHeight(gameRequest.getHeight());
        gameInfo.setWidth(gameRequest.getWidth());
        gameInfo.setNodeRole(SnakesProto.NodeRole.MASTER);

        int id = connectionService.createNewGamePlayer(gameRequest.getUsername(), SnakesProto.NodeRole.MASTER);
        gameInfo.setPlayerId(id);

        if (gameRequest.getHaveSnake() == 1) {
            SnakesProto.GameState.Coord[] coords = connectionService.searchPlace();
            connectionService.createNewSnake(coords, id);

        }

        log.info("Game config " + gameConfig.toString());
        return ResponseEntity.ok("start game");
    }
}
