package ru.ccfit.nsu.chernovskaya.slitherssmashersback.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Component
public class GamesInfo {
    List<SnakesProto.GameAnnouncement> gameAnnouncementList;

    public SnakesProto.GameAnnouncement getAnnouncementByName(String gameName) {
        for (SnakesProto.GameAnnouncement gameAnnouncement: gameAnnouncementList) {
            if (gameAnnouncement.getGameName().equals(gameName))
                return gameAnnouncement;
        }

        return null;
    }
}
