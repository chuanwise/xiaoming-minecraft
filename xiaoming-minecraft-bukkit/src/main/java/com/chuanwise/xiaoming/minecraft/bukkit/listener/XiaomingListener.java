package com.chuanwise.xiaoming.minecraft.bukkit.listener;

import com.chuanwise.xiaoming.minecraft.bukkit.XiaomingBukkitPlugin;
import com.chuanwise.xiaoming.minecraft.util.Formatter;
import com.chuanwise.xiaoming.minecraft.bukkit.socket.BukkitSocket;
import com.chuanwise.xiaoming.minecraft.pack.content.*;
import com.chuanwise.xiaoming.minecraft.util.StringUtils;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Objects;

@AllArgsConstructor
public class XiaomingListener implements Listener {
    final XiaomingBukkitPlugin plugin;
    final BukkitSocket bukkitSocket;

    public XiaomingListener(XiaomingBukkitPlugin plugin) {
        this.plugin = plugin;
        this.bukkitSocket = plugin.getBukkitSocket();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final String message = event.getMessage();

        if (bukkitSocket.test()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    final ResultContent resultContent = bukkitSocket.sendPlayerChatMessage(player, message, false);
                    if (resultContent.isSuccess() || !StringUtils.isEmpty(resultContent.getString())) {
                        player.sendMessage(Formatter.headThen(Formatter.gray(resultContent.getDescription("消息发送"))));
                    }
                } catch (SocketTimeoutException exception) {
                } catch (IOException exception) {
                    bukkitSocket.onThrowable(exception);
                }
            });
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (bukkitSocket.test()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                final Player player = event.getPlayer();
                try {
                    final String playerId = player.getName();
                    if (Objects.isNull(bukkitSocket.getBindedQQ(playerId))) {
                        player.sendMessage(Formatter.headThen("你还没有绑定 QQ，很多小明的功能都不能使用。赶快使用 " + Formatter.green("/xm bind <QQ>") + " 绑定一个吧"));
                    }
                } catch (IOException exception) {
                    bukkitSocket.onThrowable(exception);
                }
            });
        }
    }
}
