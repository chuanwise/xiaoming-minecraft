package com.chuanwise.xiaoming.minecraft.pack.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnlinePlayerContent {
    Set<PlayerContent> playerContents;
}
