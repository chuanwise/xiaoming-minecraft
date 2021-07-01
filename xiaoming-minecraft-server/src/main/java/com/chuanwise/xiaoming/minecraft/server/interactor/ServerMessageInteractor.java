package com.chuanwise.xiaoming.minecraft.server.interactor;

import com.chuanwise.xiaoming.api.annotation.Filter;
import com.chuanwise.xiaoming.api.annotation.FilterPattern;
import com.chuanwise.xiaoming.api.annotation.WhenQuiet;
import com.chuanwise.xiaoming.api.contact.contact.GroupContact;
import com.chuanwise.xiaoming.api.contact.contact.MemberContact;
import com.chuanwise.xiaoming.api.contact.message.Message;
import com.chuanwise.xiaoming.api.user.GroupXiaomingUser;
import com.chuanwise.xiaoming.api.util.ArgumentUtils;
import com.chuanwise.xiaoming.api.util.CollectionUtils;
import com.chuanwise.xiaoming.core.interactor.message.MessageInteractorImpl;
import com.chuanwise.xiaoming.minecraft.server.XiaomingMinecraftPlugin;
import com.chuanwise.xiaoming.minecraft.server.configuration.*;
import com.chuanwise.xiaoming.minecraft.server.data.ServerPlayer;
import com.chuanwise.xiaoming.minecraft.server.data.ServerPlayerData;
import com.chuanwise.xiaoming.minecraft.server.server.BukkitPluginReceptionist;
import com.chuanwise.xiaoming.minecraft.server.server.XiaomingMinecraftServer;
import com.chuanwise.xiaoming.minecraft.server.util.EnvironmentUtils;
import com.chuanwise.xiaoming.minecraft.util.Formatter;
import com.chuanwise.xiaoming.minecraft.util.StringUtils;
import com.chuanwise.xiaoming.minecraft.util.TimeUtils;
import lombok.AllArgsConstructor;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.AtAll;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.SingleMessage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
public class ServerMessageInteractor extends MessageInteractorImpl {
    final XiaomingMinecraftPlugin plugin;
    final ServerConfiguration serverConfigurations;
    final ServerPlayerData playerData;
    final XiaomingMinecraftServer server;

    @WhenQuiet
    @Filter(value = "", pattern = FilterPattern.STARTS_WITH)
    public boolean onGroupChat(GroupXiaomingUser user, Message message) {
        final MessageChain messageChain = message.getMessageChain();
        final String serializedMessage = messageChain.contentToString();
        final ChatSettings chatSettings = serverConfigurations.getChatSettings();
        final int maxIterateTime = getXiaomingBot().getConfiguration().getMaxIterateTime();
        final Set<MinecraftChannel> minecraftChannels = chatSettings.getMinecraftChannels();

        final Set<MinecraftChannel> failChannels = new HashSet<>();

        // 检查是否有 @ 全体和个人
        boolean hasAtAll = false;
        for (SingleMessage singleMessage : messageChain) {
            if (singleMessage instanceof AtAll) {
                hasAtAll = true;
                break;
            }
        }

        final Map<String, String> commonEnvironment = EnvironmentUtils.forUser(user);
        commonEnvironment.putAll(EnvironmentUtils.forGroup(user.getContact()));
        commonEnvironment.put("time", TimeUtils.FORMAT.format(System.currentTimeMillis()));

        // @ 个人比 @ 全体优先级高，所以先发 @ 全体再发 @ 个人
        // 如果带有 @ 全体，那就转发到服务器发送 @ 全体成员消息
        if (hasAtAll && chatSettings.isEnableAtAllNotice()) {
            final List<BukkitPluginReceptionist> receptionists = server.getReceptionists();
            commonEnvironment.put("message", serializedMessage);

            final String subtitle = ArgumentUtils.replaceArguments(chatSettings.getAtAllSubtitleFormat(), commonEnvironment, maxIterateTime);
            final String title = ArgumentUtils.replaceArguments(chatSettings.getAtAllTitleFormat(), commonEnvironment, maxIterateTime);
            final String messageWithoutHead = ArgumentUtils.replaceArguments(chatSettings.getAtAllMessageFormat(), commonEnvironment, maxIterateTime);

            final NoticeType atAllNoticeType = chatSettings.getAtAllNoticeType();

            // 找到和本群关联的服务器世界
            receptionists.forEach(receptionist -> {
                try {
                    switch (atAllNoticeType) {
                        case MESSAGE:
                            receptionist.sendMessageToAllPlayersWithoutMessageHead(messageWithoutHead);
                            break;
                        case TITLE:
                            receptionist.sendTitleToAllPlayers(title, subtitle);
                            break;
                        case TITLE_AND_MESSAGE:
                            receptionist.sendMessageToAllPlayersWithoutMessageHead(messageWithoutHead);
                            receptionist.sendTitleToAllPlayers(title, subtitle);
                            break;
                        default:
                    }
                } catch (IOException exception) {
                    receptionist.onThrowable(exception);
                }
            });
        }

        String messageWithoutHead = null;
        for (MinecraftChannel channel : minecraftChannels) {
            if (!serializedMessage.startsWith(channel.getHead()) || serializedMessage.length() <= channel.getHead().length()) {
                continue;
            }
            messageWithoutHead = serializedMessage.substring(channel.getHead().length());

            // 检查权限
            if (!user.hasPermission("minecraft.chat.server." + channel.getName())) {
                failChannels.add(channel);
                continue;
            }

            // 检查本群是否能发送消息
            if (!getXiaomingBot().getResponseGroupManager().hasTag(user.getGroupCode(), channel.getGroupTag())) {
                failChannels.add(channel);
                continue;
            }

            final Map<String, String> channelEnvironment = EnvironmentUtils.forMinecraftChannel(channel);
            channelEnvironment.put("message", messageWithoutHead);

            final String commonEnvironmentReplacedMessage = ArgumentUtils.replaceArguments(Formatter.translateColorCodes(channel.getFormat()), commonEnvironment, maxIterateTime);
            final String channelEnvironmentReplacedMessage = ArgumentUtils.replaceArguments(commonEnvironmentReplacedMessage, channelEnvironment, maxIterateTime);

            // 对每一个关联的服务器
            try {
                for (BukkitPluginReceptionist receptionist : server.forTag(channel.getServerTag())) {
                    final MinecraftServerDetail detail = receptionist.getDetail();

                    // 检查服务器是否有该 tag
                    if (!detail.hasTag(channel.getServerTag())) {
                        continue;
                    }

                    // 构造服务器环境
                    final Map<String, String> serverEnvironment = EnvironmentUtils.forServer(detail);
                    final String serverEnvironmentReplacedMessage = ArgumentUtils.replaceArguments(channelEnvironmentReplacedMessage, serverEnvironment, maxIterateTime);

                    final StringBuilder finalStringBuilder = new StringBuilder(serverEnvironmentReplacedMessage);
                    final Set<String> noticedOnlinePlayers = getNoticedPlayers(user.getContact(), finalStringBuilder);
                    final String finalString = finalStringBuilder.toString();

                    // 对于每一个需要发送该消息的世界，都发消息
                    final Set<String> worldNames = detail.forTaggedWorldNames(channel.getWorldTag());
                    if (!worldNames.isEmpty()) {
                        receptionist.sendWorldMessageWithoutMessageHead(worldNames, finalString);
                    }
                }
            } catch (IOException exception) {
                failChannels.add(channel);
            }
        }

        // 如果不是发送到频道上的，但仍然 @ 了人
        boolean sent = false;
        if (chatSettings.isEnableAtNotice()) {
            final StringBuilder stringBuilder = new StringBuilder(serializedMessage);
            final Set<String> noticedPlayers = getNoticedPlayers(user.getContact(), stringBuilder);
            final String finalMessage = Formatter.translateColorCodes(stringBuilder.toString());

            commonEnvironment.put("message", finalMessage);
            final String title = ArgumentUtils.replaceArguments(chatSettings.getAtTitleFormat(), commonEnvironment, maxIterateTime);
            final String subtitle = ArgumentUtils.replaceArguments(chatSettings.getAtSubtitleFormat(), commonEnvironment, maxIterateTime);
            messageWithoutHead = ArgumentUtils.replaceArguments(chatSettings.getAtMessageFormat(), commonEnvironment, maxIterateTime);

            if (!noticedPlayers.isEmpty()) {
                final NoticeType atNoticeType = chatSettings.getAtNoticeType();
                for (BukkitPluginReceptionist receptionist : server.getReceptionists()) {
                    try {
                        switch (atNoticeType) {
                            case TITLE:
                                receptionist.sendTitle(noticedPlayers, title, subtitle);
                                break;
                            case MESSAGE:
                                receptionist.sendMessageWithoutMessageHead(noticedPlayers, messageWithoutHead);
                                break;
                            case TITLE_AND_MESSAGE:
                                receptionist.sendTitle(noticedPlayers, title, subtitle);
                                receptionist.sendMessageWithoutMessageHead(noticedPlayers, messageWithoutHead);
                                break;
                            default:
                        }
                        sent = true;
                    } catch (IOException exception) {
                        receptionist.onThrowable(exception);
                    }
                }
            }
        }

        if (!failChannels.isEmpty()) {
            user.sendError("因为因为一些原因，小明没有帮你把消息发送到下面的频道：\n" +
                    CollectionUtils.getIndexSummary(failChannels, MinecraftChannel::getName));
            return true;
        }
        return Objects.nonNull(messageWithoutHead) || sent;
    }

    /**
     * 将消息中所有的真正的 @QQ、@PlayerId、@Alias 都换为带颜色的 @PlayerId，且收集这些 PlayerId
     * @param contact 群会话
     * @param stringBuilder 当前消息
     * @return 收集到的 PlayerId
     * @throws IOException
     */
    protected Set<String> getNoticedPlayers(GroupContact contact, StringBuilder stringBuilder) {
        final Set<String> result = new HashSet<>();
        boolean hasAt = stringBuilder.indexOf("@") != -1;
        if (!hasAt) {
            return result;
        }

        // 对于每一个在群里的成员，都检查是否有 @ 他
        for (MemberContact member : contact.getMembers()) {
            final String serializedAtMessage = new At(member.getCode()).serializeToMiraiCode();

            // 替换所有的真正的 @QQ 为 @alias 或 @Alias
            final ServerPlayer serverPlayer = playerData.forPlayer(member.getCode());
            final String atContent = Formatter.yellow('@' + member.getAlias() + ' ');

            final String atCodeString = '@' + member.getCodeString();

            // @QQ
            if (stringBuilder.indexOf(atCodeString) != -1) {
                if (Objects.nonNull(serverPlayer)) {
                    result.add(serverPlayer.getId());
                }
                StringUtils.replaceAll(stringBuilder, atCodeString, atContent);
            }

            // 真正的 @QQ
            if (stringBuilder.indexOf(serializedAtMessage) != -1) {
                if (Objects.nonNull(serverPlayer)) {
                    result.add(serverPlayer.getId());
                }
                StringUtils.replaceAll(stringBuilder, serializedAtMessage, atContent);
            }

            // @Alias
            final String aliasAt = '@' + member.getAlias();
            if (stringBuilder.indexOf(aliasAt) != -1) {
                if (Objects.nonNull(serverPlayer)) {
                    result.add(serverPlayer.getId());
                }
                StringUtils.replaceAll(stringBuilder, aliasAt, atContent);
            }

            // 没 PlayerId 就跳过了
            if (Objects.isNull(serverPlayer)) {
                continue;
            }

            // 检查有无 @PlayerId
            final String atPlayerId = '@' + serverPlayer.getId();
            if (stringBuilder.indexOf(atPlayerId) != -1) {
                StringUtils.replaceAll(stringBuilder, atPlayerId, atContent);
                result.add(serverPlayer.getId());
            }

            hasAt = stringBuilder.indexOf("@") != -1;
            if (!hasAt) {
                break;
            }
        }
        return result;
    }
}
