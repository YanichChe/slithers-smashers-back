package ru.ccfit.nsu.chernovskaya.slitherssmashersback.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Component
public class GamesInfo {
    List<GameAnnouncement> gameAnnouncementList = new ArrayList<>();
}
