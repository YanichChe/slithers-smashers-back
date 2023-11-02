package ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto.messages;

import lombok.Getter;
@Getter
public class JoinMsg extends Message {
    String playerName;
    String gameName;
}
