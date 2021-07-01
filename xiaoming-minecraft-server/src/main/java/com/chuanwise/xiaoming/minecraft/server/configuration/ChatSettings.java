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

    boolean enableAtNotice = true;
    NoticeType atNoticeType = NoticeType.MESSAGE;
    String atTitleFormat = "§e有人 §e§l@ §e你";
    String atSubtitleFormat = "§a{sender.alias} §f在§7「§b{group.alias}§7」§f里 §e@ §f你了";
    String atMessageFormat = "§7[§6有人 §e@ §6你§7] §7[§a{group.alias}§7] §b{sender.alias} §e§l>§6§l>§e§l> §e{message}";

    boolean enableAtAllNotice = true;
    NoticeType atAllNoticeType = NoticeType.TITLE_AND_MESSAGE;
    String atAllTitleFormat = "§e§l@ §e全体成员";
    String atAllSubtitleFormat = "请注意 §a{sender.alias} §f在§7「§b{group.alias}§7」§f发布的消息";
    String atAllMessageFormat = "§7[§c§l@ §c全体成员§7] §7[§6{group.alias}§7] §e{sender.alias} §c§l>§4§l>§c§l> §c§l{message}";

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
