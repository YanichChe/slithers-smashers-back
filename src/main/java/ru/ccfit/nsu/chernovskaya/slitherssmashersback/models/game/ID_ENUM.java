package ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.game;

import lombok.Getter;

@Getter
public enum ID_ENUM {
    UNDEFINED(-2), DELETED(-1);

    int value;
    ID_ENUM(int i) {
        this.value = i;
    }
}
