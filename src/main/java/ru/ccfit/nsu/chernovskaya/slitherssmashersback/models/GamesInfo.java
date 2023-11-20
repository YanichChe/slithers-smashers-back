package ru.ccfit.nsu.chernovskaya.slitherssmashersback.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto.GameAnnouncementDTO;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Component
public class GamesInfo {
    List<GameAnnouncementDTO> gameAnnouncementList = new ArrayList<>();
}
