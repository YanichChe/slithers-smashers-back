package ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorMsg extends Message{
    private String errorMessage;
}
