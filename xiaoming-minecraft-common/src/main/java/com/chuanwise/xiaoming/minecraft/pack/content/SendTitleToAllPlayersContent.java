package com.chuanwise.xiaoming.minecraft.pack.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendTitleToAllPlayersContent {
    String title;
    String subtitle;
    int fadeIn = 10, delay = 70, fadeOut = 10;

    public SendTitleToAllPlayersContent(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }
}
