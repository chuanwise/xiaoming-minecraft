package com.chuanwise.xiaoming.minecraft.server.util;

import com.chuanwise.xiaoming.api.contact.contact.GroupContact;
import com.chuanwise.xiaoming.api.user.GroupXiaomingUser;
import com.chuanwise.xiaoming.api.user.XiaomingUser;
import com.chuanwise.xiaoming.api.util.CollectionUtils;
import com.chuanwise.xiaoming.api.util.StaticUtils;
import com.chuanwise.xiaoming.api.util.UnstaticUtils;
import com.chuanwise.xiaoming.minecraft.pack.content.PlayerContent;
import com.chuanwise.xiaoming.minecraft.server.XiaomingMinecraftPlugin;
import com.chuanwise.xiaoming.minecraft.server.configuration.GroupChannel;
import com.chuanwise.xiaoming.minecraft.server.configuration.MinecraftChannel;
import com.chuanwise.xiaoming.minecraft.server.configuration.MinecraftServerDetail;
import com.chuanwise.xiaoming.minecraft.server.data.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EnvironmentUtils extends UnstaticUtils {
    static Map<String, Map<String, String>> minecraftChannelEnvironments = new HashMap<>();
    static Map<String, Map<String, String>> groupChannelEnvironments = new HashMap<>();
    static Map<String, Map<String, String>> serverEnvironments = new HashMap<>();
    static Map<String, Map<String, String>> playerEnvironments = new HashMap<>();
    static Map<Long, Map<String, String>> userEnvironments = new HashMap<>();
    static  Map<Long, Map<String, String>> groupEnvironments = new HashMap<>();

    public static Map<String, String> forMinecraftChannel(MinecraftChannel channel) {
        return CollectionUtils.getOrPut(minecraftChannelEnvironments, channel.getName(), () -> {
            Map<String, String> result = new HashMap<>();
            result.put("channel.name", channel.getName());
            result.put("channel.head", channel.getHead());
            result.put("channel.serverTag", channel.getServerTag());
            result.put("channel.worldTag", channel.getWorldTag());
            result.put("channel.format", channel.getFormat());
            result.put("channel.groupTag", channel.getGroupTag());
            return result;
        });
    }

    public static Map<String, String> forGroupChannel(GroupChannel channel) {
        return CollectionUtils.getOrPut(groupChannelEnvironments, channel.getName(), () -> {
            Map<String, String> result = new HashMap<>();
            result.put("channel.name", channel.getName());
            result.put("channel.head", channel.getHead());
            result.put("channel.serverTag", channel.getServerTag());
            result.put("channel.worldTag", channel.getWorldTag());
            result.put("channel.groupTag", channel.getGroupTag());
            result.put("channel.format", channel.getFormat());
            return result;
        });
    }

    public static Map<String, String> forUser(XiaomingUser user) {
        final Map<String, String> map = CollectionUtils.getOrPut(userEnvironments, user.getCode(), () -> {
            Map<String, String> result = new HashMap<>();
            result.put("sender.name", user.getName());
            result.put("sender.alias", user.getAlias());
            result.put("sender.code", user.getCodeString());
            return result;
        });
        final ServerPlayer serverPlayer = XiaomingMinecraftPlugin.INSTANCE.getPlayerData().forPlayer(user.getCode());
        if (Objects.nonNull(serverPlayer)) {
            map.put("sender.id", serverPlayer.getId());
        }
        return map;
    }

    public static Map<String, String> forGroup(GroupContact groupContact) {
        return CollectionUtils.getOrPut(groupEnvironments, groupContact.getCode(), () -> {
            Map<String, String> result = new HashMap<>();
            result.put("group.name", groupContact.getName());
            result.put("group.alias", groupContact.getAlias());
            result.put("group.code", groupContact.getCodeString());
            return result;
        });
    }

    public static Map<String, String> forServer(MinecraftServerDetail detail) {
        return CollectionUtils.getOrPut(serverEnvironments, detail.getName(), () -> {
            Map<String, String> result = new HashMap<>();
            result.put("server.name", detail.getName());
            return result;
        });
    }

    public static Map<String, String> forPlayer(PlayerContent playerContent) {
        final Map<String, String> map = new HashMap<>();
        map.put("player.id", playerContent.getPlayerId());
        map.put("player.customName", playerContent.getCustomName());
        map.put("player.listName", playerContent.getPlayerListName());
        map.put("player.displayName", playerContent.getDisplayName());
        playerEnvironments.put(playerContent.getPlayerId(), map);
        return map;
    }
}
