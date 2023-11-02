package ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.ccfit.nsu.chernovskaya.slitherssmashersback.SnakesProto;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class AnnouncementMsg extends Message{
    SnakesProto.GameAnnouncement gameAnnouncement;
}