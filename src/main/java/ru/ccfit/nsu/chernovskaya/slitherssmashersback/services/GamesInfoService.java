package ru.ccfit.nsu.chernovskaya.slitherssmashersback.services;

import org.springframework.stereotype.Service;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto.GameAnnouncementDTO;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.models.GamesInfo;

@Service
public class GamesInfoService {

    private final GamesInfo gamesInfo;

    public GamesInfoService(GamesInfo gamesInfo) {
        this.gamesInfo = gamesInfo;
    }


    public GameAnnouncementDTO getAnnouncementDTOByName(String gameName) {
        for (GameAnnouncementDTO gameAnnouncement: gamesInfo.getGameAnnouncementList()) {
            if (gameAnnouncement.getGameName().equals(gameName))
                return gameAnnouncement;
        }

        return null;
    }
}
