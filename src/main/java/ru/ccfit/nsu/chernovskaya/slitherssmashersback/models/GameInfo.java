package ru.ccfit.nsu.chernovskaya.slitherssmashersback.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
@Setter
public class GameInfo {

    private SnakesProto.GameConfig gameConfig;

    private final Object monitor = new Object();

    private boolean canJoin = true;

    private String gameName;

    private List<SnakesProto.GameState.Snake> snakes = new ArrayList<>();
    private List<SnakesProto.GameState.Coord> foods = new ArrayList<>();
    private List<SnakesProto.GamePlayer> gamePlayers = new ArrayList<>();

    private int msqSeq = -1;

    private int height;

    private int width;

    private SnakesProto.NodeRole nodeRole;

     public synchronized int getIncrementMsgSeq() {
        msqSeq++;
        return msqSeq;
    }
}
