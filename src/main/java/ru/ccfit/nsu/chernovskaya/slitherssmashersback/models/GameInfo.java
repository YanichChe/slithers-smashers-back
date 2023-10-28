package ru.ccfit.nsu.chernovskaya.slitherssmashersback.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
@Setter
public class GameInfo {
    private SnakesProto.GamePlayers gamePlayers = SnakesProto.GamePlayers.newBuilder().build();
    private SnakesProto.GameConfig gameConfig = SnakesProto.GameConfig.newBuilder().build();
    private final Object monitor = new Object();

    private boolean canJoin = true;
    private String gameName;

    private List<SnakesProto.GameState.Snake> snakes = new ArrayList<>();
    private List<SnakesProto.GameState.Coord> foods = new ArrayList<>();

    private int msqSeq = -1;

    public int getIncrementMsgSeq() {
        synchronized (monitor) {
            msqSeq++;
        }

        return msqSeq;
    }
}
