package com.chuanwise.xiaoming.minecraft.pack.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendTitleContent extends SendTitleToAllPlayersContent {
    Set<String> playerId;

    public SendTitleContent(Set<String> playerId, String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
        this.playerId = playerId;
    }

    public SendTitleContent(Set<String> playerId, String title, String subtitle, int fadeIn, int delay, int fadeOut) {
        super(title, subtitle, fadeIn, delay, fadeOut);
        this.playerId = playerId;
    }
}