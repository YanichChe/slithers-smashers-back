package ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class GameConfig {
    int width = 40;
    int height = 30;
    int foodStatic = 1;
    int stateDelayMs = 1000;

    @Override
    public String toString() {
        return "GameConfig{" +
                "width=" + width +
                ", height=" + height +
                ", foodStatic=" + foodStatic +
                ", stateDelayMs=" + stateDelayMs +
                '}';
    }
}
