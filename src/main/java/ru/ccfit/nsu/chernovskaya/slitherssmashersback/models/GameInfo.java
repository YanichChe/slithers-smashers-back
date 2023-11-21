package ru.ccfit.nsu.chernovskaya.slitherssmashersback.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.Coord;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.GameConfig;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.GamePlayer;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game.Snake;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
@Setter
public class GameInfo {

    private GameConfig gameConfig;
    private boolean canJoin = true;
    private String gameName;

    private List<Snake> snakes = new ArrayList<>();
    private List<Coord> foods = new ArrayList<>();
    private List<GamePlayer> gamePlayers = new ArrayList<>();

    private int height;
    private int width;
    private String masterInetAddress;
    private int masterPort;

    private String playerName;
    private int playerId = -1;
    private boolean increase;
    private boolean isAlive;
    private int score = 0;

    private long msqSeq = -1;

    private SnakesProto.NodeRole nodeRole;
    private int stateOrder = 0;

    public synchronized long getIncrementMsgSeq() {
        msqSeq++;
        return msqSeq;
    }
}
