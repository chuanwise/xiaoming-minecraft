package com.chuanwise.xiaoming.minecraft.bukkit.util;

import com.chuanwise.xiaoming.minecraft.pack.content.PlayerContent;
import org.bukkit.entity.Player;

public class PlayerUtils {
    public static PlayerContent forPlayer(Player player) {
        return new PlayerContent(player.getName(), player.getWorld().getName(), player.getDisplayName(), player.getCustomName(), player.getPlayerListName());
    }
}
