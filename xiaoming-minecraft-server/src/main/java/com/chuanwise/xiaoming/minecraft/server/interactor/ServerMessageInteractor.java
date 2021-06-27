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
import com.chuanwise.xiaoming.minecraft.server.configuration.ChatSettings;
import com.chuanwise.xiaoming.minecraft.server.configuration.MinecraftChannel;
import com.chuanwise.xiaoming.minecraft.server.configuration.MinecraftServerDetail;
import com.chuanwise.xiaoming.minecraft.server.configuration.ServerConfiguration;
import com.chuanwise.xiaoming.minecraft.server.data.ServerPlayer;
import com.chuanwise.xiaoming.minecraft.server.data.ServerPlayerData;
import com.chuanwise.xiaoming.minecraft.server.server.BukkitPluginReceptionist;
import com.chuanwise.xiaoming.minecraft.server.server.XiaomingMinecraftServer;
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

        final Set<MinecraftChannel> lackPermissionChannels = new HashSet<>();
        final Set<MinecraftChannel> failChannels = new HashSet<>();

        // 检查是否有 @ 全体和个人
        boolean hasAtAll = false;
        for (SingleMessage singleMessage : messageChain) {
            if (singleMessage instanceof AtAll) {
                hasAtAll = true;
                break;
            }
        }

        // @ 个人比 @ 全体优先级高，所以先发 @ 全体再发 @ 个人
        // 如果带有 @ 全体，那就转发到服务器发送 @ 全体成员消息
        if (hasAtAll) {
            final Set<String> failServerNames = new HashSet<>();
            final List<BukkitPluginReceptionist> receptionists = server.getReceptionists();
            final String subtitle = "§f请注意§7「§a" + user.getContact().getAlias() + "§7」§f中的消息";
            final String title = Formatter.yellow("@ 全体成员");
            final String messageWithoutHead = Formatter.aroundByGrayBracket(Formatter.blue("@ 全体成员")) + " " +
                    Formatter.blue(user.getAlias()) + Formatter.red(" >> ") + Formatter.yellow(serializedMessage);

            // 找到和本群关联的服务器世界
            receptionists.forEach(receptionist -> {
                try {
                    receptionist.sendTitleToAllPlayers(title, subtitle);
                    receptionist.sendMessageToAllPlayersWithoutMessageHead(messageWithoutHead);
                } catch (IOException exception) {
                    failServerNames.add(receptionist.getDetail().getName());
                }
            });

            if (!failServerNames.isEmpty()) {
                if (failServerNames.size() == receptionists.size()) {
                    user.sendError("小明尝试帮你把 @全体成员 消息转发服务器内，但全都失败了");
                } else {
                    user.sendError("小明尝试帮你把 @全体成员 消息转发到所有服务器中，但在这些服务器里失败了：" +
                            CollectionUtils.getSummary(failServerNames, String::toString, "", "", "、"));
                }
            }
        }


        final Map<String, Object> commonEnvironment = new HashMap<>();
        commonEnvironment.put("sender.code", user.getCode());
        commonEnvironment.put("sender.name", user.getName());
        commonEnvironment.put("sender.alias", user.getAlias());

        final ServerPlayer serverPlayer = playerData.forPlayer(user.getCode());
        commonEnvironment.put("sender.id", Objects.nonNull(serverPlayer) ? serverPlayer.getId() : "null");
        commonEnvironment.put("time", TimeUtils.FORMAT.format(System.currentTimeMillis()));

        commonEnvironment.put("group.code", user.getGroupCode());
        commonEnvironment.put("group.name", user.getContact().getName());
        commonEnvironment.put("group.alias", user.getContact().getAlias());

        AtomicBoolean sent = new AtomicBoolean(false);
        for (MinecraftChannel channel : minecraftChannels) {
            if (!serializedMessage.startsWith(channel.getHead()) || serializedMessage.length() <= channel.getHead().length()) {
                continue;
            }
            final String messageWithoutHead = serializedMessage.substring(channel.getHead().length());

            // 检查权限
            if (!user.hasPermission("minecraft.chat." + channel.getName())) {
                lackPermissionChannels.add(channel);
                continue;
            }

            final Map<String, Object> channelEnvironment = new HashMap<>();
            channelEnvironment.put("channel.name", channel.getName());
            channelEnvironment.put("channel.head", channel.getHead());
            channelEnvironment.put("channel.serverTag", channel.getServerTag());
            channelEnvironment.put("channel.worldTag", channel.getWorldTag());
            channelEnvironment.put("message", messageWithoutHead);

            final String commonEnvironmentReplacedMessage = ArgumentUtils.replaceArguments(Formatter.translateColorCodes(channel.getFormat()), commonEnvironment, maxIterateTime);
            final String channelEnvironmentReplacedMessage = ArgumentUtils.replaceArguments(commonEnvironmentReplacedMessage, channelEnvironment, maxIterateTime);

            // 对每一个关联的服务器
            try {
                sent.set(true);
                final Set<MinecraftServerDetail> details = serverConfigurations.forServerTag(channel.getServerTag());
                for (MinecraftServerDetail detail : details) {
                    final BukkitPluginReceptionist receptionist = server.forReceptionist(detail.getName());
                    if (Objects.isNull(receptionist)) {
                        continue;
                    }

                    Map<String, Object> serverEnvironment = new HashMap<>();
                    serverEnvironment.put("server.name", detail.getName());

                    final String serverEnvironmentReplacedMessage = ArgumentUtils.replaceArguments(channelEnvironmentReplacedMessage, serverEnvironment, maxIterateTime);

                    final StringBuilder finalStringBuilder = new StringBuilder(serverEnvironmentReplacedMessage);
                    final Set<String> noticedOnlinePlayers = getNoticedPlayers(user.getContact(), finalStringBuilder);
                    final String finalString = finalStringBuilder.toString();

                    // 对于每一个需要发送该消息的世界，都发消息
                    final Set<String> worldNames = detail.forTaggedWorldNames(channel.getWorldTag());
                    if (!worldNames.isEmpty()) {
                        receptionist.sendWorldMessageWithoutMessageHead(worldNames, finalString);
                    }

                    // 对于每一个 @ 到的人，都发消息
                    if (!noticedOnlinePlayers.isEmpty()) {
                        receptionist.sendTitle(noticedOnlinePlayers, Formatter.yellow("有人 @ 你"),
                                Formatter.green(user.getAlias()) + " 在 " +
                                        Formatter.gray("「") + Formatter.blue(channel.getName()) + Formatter.gray("」") + " 频道 " + Formatter.yellow("@") + " 你了");
                    }
                }
            } catch (IOException exception) {
                failChannels.add(channel);
            }
        }

        // 如果不是发送到频道上的，但仍然 @ 了人
        if (!sent.get()) {
            final StringBuilder stringBuilder = new StringBuilder(serializedMessage);
            final Set<String> noticedPlayers = getNoticedPlayers(user.getContact(), stringBuilder);
            final String finalMessage = Formatter.translateColorCodes(stringBuilder.toString());

            if (!noticedPlayers.isEmpty()) {
                for (BukkitPluginReceptionist receptionist : server.getReceptionists()) {
                    try {
                        receptionist.sendTitle(noticedPlayers, Formatter.yellow("有人 @ 你"), Formatter.yellow(user.getAlias()) +
                                " 在" + Formatter.gray("「") + Formatter.blue(user.getContact().getAlias()) + Formatter.gray("」") + "里 " +
                                Formatter.yellow("@") + " 你了");
                        receptionist.sendMessageWithoutMessageHead(noticedPlayers, Formatter.aroundByGrayBracket(Formatter.blue("有人 @ 你")) + " " +
                                Formatter.yellow(user.getAlias()) + Formatter.blue(" >> ") + Formatter.green(finalMessage));
                        sent.set(true);
                    } catch (IOException exception) {
                        receptionist.onThrowable(exception);
                    }
                }
            }
        }

        if (!lackPermissionChannels.isEmpty()) {
            user.sendError("因为你缺少相关权限，小明没有帮你把消息发送到下面的频道：\n" +
                    CollectionUtils.getIndexSummary(lackPermissionChannels, MinecraftChannel::getName));
            return true;
        }
        return sent.get();
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
