package com.chuanwise.xiaoming.minecraft.pack.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerChatContent {
    PlayerContent player;
    String message;
    boolean echo;

    public PlayerChatContent(PlayerContent player, String message) {
        this.player = player;
        this.message = message;
    }
}
