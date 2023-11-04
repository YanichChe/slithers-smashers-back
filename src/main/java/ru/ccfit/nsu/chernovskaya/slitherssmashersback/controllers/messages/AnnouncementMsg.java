package ru.ccfit.nsu.chernovskaya.slitherssmashersback.controllers.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class AnnouncementMsg {
    SnakesProto.GameAnnouncement gameAnnouncement;
}
