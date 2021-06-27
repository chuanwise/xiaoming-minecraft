package com.chuanwise.xiaoming.minecraft.pack.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class XiaomingCommandContent {
    public static enum Position {
        PRIVATE,
        GROUP,
        TEMP,
    }
    Position position;
    long code;
    String playerId;
    String command;

    public static XiaomingCommandContent groupCommandContent(long group, String playerId, String command) {
        return new XiaomingCommandContent(Position.GROUP, group, playerId, command);
    }

    public static XiaomingCommandContent privateCommandContent(String playerId, String command) {
        return new XiaomingCommandContent(Position.PRIVATE, 0, playerId, command);
    }

    public static XiaomingCommandContent tempCommandContent(long group, String playerId, String command) {
        return new XiaomingCommandContent(Position.TEMP, group, playerId, command);
    }
}
