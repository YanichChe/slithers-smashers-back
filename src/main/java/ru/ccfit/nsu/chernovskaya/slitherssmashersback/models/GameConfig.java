package ru.ccfit.nsu.chernovskaya.slitherssmashersback.models;

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
}
