package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.Coord;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.GameConfig;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages.GameRequest;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameInfo;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.master.ConnectionService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/master")
@Log4j2
public class MasterController {

    private final GameInfo gameInfo;
    private final ConnectionService connectionService;
    @Value(value = "${state.delay.ms}")
    private int stateDelayMs;

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

        GameConfig gameConfig = new GameConfig();
        gameConfig.setHeight(gameRequest.getHeight());
        gameConfig.setWidth(gameRequest.getWidth());
        gameConfig.setFoodStatic(gameRequest.getFoodStatic());
        gameConfig.setStateDelayMs(stateDelayMs);

        gameInfo.setGameConfig(gameConfig);

        gameInfo.setGameName(gameRequest.getGameName());
        gameInfo.setHeight(gameRequest.getHeight());
        gameInfo.setWidth(gameRequest.getWidth());
        gameInfo.setNodeRole(SnakesProto.NodeRole.MASTER);

        int id = connectionService.createNewGamePlayer(gameRequest.getUsername(), SnakesProto.NodeRole.MASTER);
        gameInfo.setPlayerId(id);

        if (gameRequest.getHaveSnake() == 1) {
            Coord[] coords = connectionService.searchPlace();
            connectionService.createNewSnake(coords, id);

        }

        log.info("Game config " + gameConfig);
        return ResponseEntity.ok("start game");
    }
}
