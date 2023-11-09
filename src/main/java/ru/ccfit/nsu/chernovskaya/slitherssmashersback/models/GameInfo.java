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
    private boolean canJoin = true;
    private String gameName;

    private List<SnakesProto.GameState.Snake> snakes = new ArrayList<>();
    private List<SnakesProto.GameState.Coord> foods = new ArrayList<>();
    private List<SnakesProto.GamePlayer> gamePlayers = new ArrayList<>();

    private int height;
    private int width;
    private String masterInetAddress;
    private int masterPort;

    private String playerName;
    private int playerId = -1;

    private long msqSeq = -1;

    private SnakesProto.NodeRole nodeRole;
    private int stateOrder = 0;

     public synchronized long getIncrementMsgSeq() {
        msqSeq++;
        return msqSeq;
    }
}
