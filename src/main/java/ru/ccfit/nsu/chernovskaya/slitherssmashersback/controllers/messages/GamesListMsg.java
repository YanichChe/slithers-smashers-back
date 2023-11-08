package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class GamesListMsg {
    List<String> gamesName;
}
