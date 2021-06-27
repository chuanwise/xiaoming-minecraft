package com.chuanwise.xiaoming.minecraft.server.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatSettings {
    Set<GroupChannel> groupChannels = new HashSet<>();
    Set<MinecraftChannel> minecraftChannels = new HashSet<>();

    public GroupChannel forGroupChannel(String name) {
        for (GroupChannel channel : groupChannels) {
            if (Objects.equals(channel.getName(), name)) {
                return channel;
            }
        }
        return null;
    }

    public MinecraftChannel forMinecraftChannel(String name) {
        for (MinecraftChannel channel : minecraftChannels) {
            if (Objects.equals(channel.getName(), name)) {
                return channel;
            }
        }
        return null;
    }
}
