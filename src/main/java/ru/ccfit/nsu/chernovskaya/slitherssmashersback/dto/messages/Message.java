package ru.ccfit.nsu.chernovskaya.slitherssmashersback.dto.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AnnouncementMsg.class, name = "AnnouncementMsg"),
        @JsonSubTypes.Type(value = DiscoverMsg.class, name = "DiscoverMsg"),
        @JsonSubTypes.Type(value = ErrorMsg.class, name = "ErrorMsg"),
        @JsonSubTypes.Type(value = JoinMsg.class, name = "JoinMsg"),
        @JsonSubTypes.Type(value = RoleChangeMsg.class, name = "RoleChangeMsg"),
        @JsonSubTypes.Type(value = StateMsg.class, name = "StateMsg"),
        @JsonSubTypes.Type(value = SteerMsg.class, name = "SteerMsg")
})
public abstract class Message {}