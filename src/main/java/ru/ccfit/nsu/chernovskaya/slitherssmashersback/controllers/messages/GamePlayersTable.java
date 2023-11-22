package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class GamePlayersTable {
    Map<String, Integer> gamePlayerTable = new HashMap<>();
}
