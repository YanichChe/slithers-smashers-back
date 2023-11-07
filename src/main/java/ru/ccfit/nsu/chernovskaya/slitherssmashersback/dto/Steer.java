package ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;

@NoArgsConstructor
@AllArgsConstructor
@Setter
public class Steer {

    private int direction;

    public SnakesProto.Direction getheadDirection() {
        return switch (direction) {
            case 0 -> SnakesProto.Direction.UP;
            case 1 -> SnakesProto.Direction.LEFT;
            case 2 -> SnakesProto.Direction.RIGHT;
            case 3 -> SnakesProto.Direction.DOWN;
            default -> null;
        };
    }
}
