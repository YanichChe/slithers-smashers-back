package ru.ccfit.nsu.chernovskaya.slitherssmashersback.mapper;

import org.springframework.stereotype.Component;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.*;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProtobufMapper {
    public void map(SnakesProto.GameMessage.StateMsg stateMsg, GameState gameState) {

        List<GamePlayer> gamePlayerList = new ArrayList<>();
        List<Coord> foods = new ArrayList<>();
        List<Snake> snakes = new ArrayList<>();

        for (SnakesProto.GamePlayer gamePlayer : stateMsg.getState().getPlayers().getPlayersList()) {
            GamePlayer gamePlayerDTO = new GamePlayer(
                    gamePlayer.getName(),
                    gamePlayer.getScore(),
                    gamePlayer.getId(),
                    true,
                    gamePlayer.getIpAddress(),
                    gamePlayer.getPort(),
                    map(gamePlayer.getRole())
            );

            gamePlayerList.add(gamePlayerDTO);
        }

        for (SnakesProto.GameState.Coord foodCoord : stateMsg.getState().getFoodsList()) {
            Coord coord = new Coord(foodCoord.getX(), foodCoord.getY());
            foods.add(coord);
        }

        for (SnakesProto.GameState.Snake snake : stateMsg.getState().getSnakesList()) {
            Snake snakeDTO = new Snake();
            snakeDTO.setState(map(snake.getState()));
            snakeDTO.setPlayerId(snake.getPlayerId());
            snakeDTO.setHeadDirection(map(snake.getHeadDirection()));

            List<Coord> coordList = new ArrayList<>();

            for (SnakesProto.GameState.Coord coord : snake.getPointsList()) {
                Coord coordDTO = new Coord(coord.getX(), coord.getY());
                coordList.add(coordDTO);
            }
            snakeDTO.setCoordList(coordList);
            snakes.add(snakeDTO);
        }

        gameState.setPlayers(gamePlayerList);

        gameState.setStateOrder(stateMsg.getState().getStateOrder());
        gameState.setFoods(foods);
        gameState.setSnakes(snakes);
    }

    public Role map(SnakesProto.NodeRole nodeRole) {
        Role role;
        if (nodeRole.equals(SnakesProto.NodeRole.MASTER)) role = Role.MASTER;
        else if (nodeRole.equals(SnakesProto.NodeRole.VIEWER)) role = Role.VIEWER;
        else if (nodeRole.equals(SnakesProto.NodeRole.NORMAL)) role = Role.NORMAL;
        else role = Role.DEPUTY;

        return role;
    }

    public SnakesProto.NodeRole map(Role role) {
        SnakesProto.NodeRole nodeRole;

        if (role.equals(Role.MASTER)) nodeRole = SnakesProto.NodeRole.MASTER;
        else if (role.equals(Role.VIEWER)) nodeRole = SnakesProto.NodeRole.VIEWER;
        else if (role.equals(Role.NORMAL)) nodeRole = SnakesProto.NodeRole.NORMAL;
        else nodeRole = SnakesProto.NodeRole.DEPUTY;

        return nodeRole;
    }

    public State map(SnakesProto.GameState.Snake.SnakeState state) {
        State stateDTO;

        if (state.equals(SnakesProto.GameState.Snake.SnakeState.ALIVE)) stateDTO = State.Alive;
        else stateDTO = State.Zombie;

        return stateDTO;
    }

    public SnakesProto.GameState.Snake.SnakeState map(State state) {
        SnakesProto.GameState.Snake.SnakeState snakeState;

        if (state.equals(State.Alive)) snakeState = SnakesProto.GameState.Snake.SnakeState.ALIVE;
        else snakeState = SnakesProto.GameState.Snake.SnakeState.ZOMBIE;

        return snakeState;
    }

    public Direction map(SnakesProto.Direction direction) {

        Direction directionDTO;
        if (direction.equals(SnakesProto.Direction.UP)) directionDTO = Direction.UP;
        else if (direction.equals(SnakesProto.Direction.LEFT)) directionDTO = Direction.LEFT;
        else if (direction.equals(SnakesProto.Direction.RIGHT)) directionDTO = Direction.RIGHT;
        else directionDTO = Direction.DOWN;

        return directionDTO;
    }

    public SnakesProto.Direction map(Direction directionDTO) {

        SnakesProto.Direction direction;

        if (directionDTO.equals(Direction.UP)) direction = SnakesProto.Direction.UP;
        else if (directionDTO.equals(Direction.DOWN)) direction = SnakesProto.Direction.DOWN;
        else if (directionDTO.equals(Direction.LEFT)) direction = SnakesProto.Direction.LEFT;
        else direction = SnakesProto.Direction.RIGHT;

        return direction;
    }

    public List<SnakesProto.GamePlayer> mapFromGamePlayerDTO(List<GamePlayer> gamePlayerList) {
        List<SnakesProto.GamePlayer> gamePlayersList = new ArrayList<>();

        for (GamePlayer gamePlayer : gamePlayerList) {
            SnakesProto.GamePlayer newGamePlayer = SnakesProto.GamePlayer
                    .newBuilder()
                    .setScore(gamePlayer.getScore())
                    .setRole(map(gamePlayer.getRole()))
                    .setId(gamePlayer.getId())
                    .setName(gamePlayer.getName())
                    .setPort(gamePlayer.getPort())
                    .setIpAddress(gamePlayer.getAddress())
                    .build();
            gamePlayersList.add(newGamePlayer);
        }

        return gamePlayersList;
    }

    public SnakesProto.GameConfig map(GameConfig gameConfigDTO) {
        SnakesProto.GameConfig gameConfig = SnakesProto.GameConfig
                .newBuilder()
                .setHeight(gameConfigDTO.getHeight())
                .setWidth(gameConfigDTO.getWidth())
                .setStateDelayMs(gameConfigDTO.getStateDelayMs())
                .setFoodStatic(gameConfigDTO.getFoodStatic())
                .build();

        return gameConfig;
    }

    public GameConfig map(SnakesProto.GameConfig gameConfig) {

        GameConfig gameConfigDTO = new GameConfig(
                gameConfig.getWidth(),
                gameConfig.getHeight(),
                gameConfig.getFoodStatic(),
                gameConfig.getStateDelayMs()
        );

        return gameConfigDTO;
    }

    public List<SnakesProto.GameState.Snake> map(List<Snake> snakeDTOS) {

        List<SnakesProto.GameState.Snake> snakes = new ArrayList<>();

        for (Snake snakeDTO : snakeDTOS) {
            List<SnakesProto.GameState.Coord> coordList = new ArrayList<>();

            for (Coord coord : snakeDTO.getCoordList()) {
                coordList.add(SnakesProto.GameState.Coord
                        .newBuilder()
                        .setX(coord.getX())
                        .setY(coord.getY())
                        .build());
            }
            SnakesProto.GameState.Snake snake = SnakesProto.GameState.Snake
                    .newBuilder()
                    .addAllPoints(coordList)
                    .setState(map(snakeDTO.getState()))
                    .setPlayerId(snakeDTO.getPlayerId())
                    .setHeadDirection(map(snakeDTO.getHeadDirection()))
                    .build();

            snakes.add(snake);
        }

        return snakes;
    }
}
