package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services.info;

import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GameAnnouncement;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GamesInfo;

@Service
public class GamesInfoService {

    private final GamesInfo gamesInfo;

    public GamesInfoService(GamesInfo gamesInfo) {
        this.gamesInfo = gamesInfo;
    }


    public GameAnnouncement getAnnouncementDTOByName(String gameName) {
        for (GameAnnouncement gameAnnouncement: gamesInfo.getGameAnnouncementList()) {
            if (gameAnnouncement.getGameName().equals(gameName))
                return gameAnnouncement;
        }

        return null;
    }
}
