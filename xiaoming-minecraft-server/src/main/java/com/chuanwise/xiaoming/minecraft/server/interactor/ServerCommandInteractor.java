package com.chuanwise.xiaoming.minecraft.server.interactor;

import com.chuanwise.xiaoming.api.account.Account;
import com.chuanwise.xiaoming.api.annotation.Filter;
import com.chuanwise.xiaoming.api.annotation.FilterParameter;
import com.chuanwise.xiaoming.api.annotation.Require;
import com.chuanwise.xiaoming.api.user.PrivateXiaomingUser;
import com.chuanwise.xiaoming.api.user.XiaomingUser;
import com.chuanwise.xiaoming.api.util.*;
import com.chuanwise.xiaoming.core.interactor.command.CommandInteractorImpl;
import com.chuanwise.xiaoming.minecraft.server.XiaomingMinecraftPlugin;
import com.chuanwise.xiaoming.minecraft.server.util.ServerWords;
import com.chuanwise.xiaoming.minecraft.server.util.TargetUtils;
import com.chuanwise.xiaoming.minecraft.util.Formatter;
import com.chuanwise.xiaoming.minecraft.pack.content.*;
import com.chuanwise.xiaoming.minecraft.server.configuration.*;
import com.chuanwise.xiaoming.minecraft.server.data.ServerPlayer;
import com.chuanwise.xiaoming.minecraft.server.data.ServerPlayerData;
import com.chuanwise.xiaoming.minecraft.server.server.BukkitPluginReceptionist;
import com.chuanwise.xiaoming.minecraft.server.server.XiaomingMinecraftServer;
import com.chuanwise.xiaoming.minecraft.util.ExceptionThrowableRunnable;
import com.chuanwise.xiaoming.minecraft.util.StringUtils;
import net.mamoe.mirai.message.code.MiraiCode;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;

public class ServerCommandInteractor extends CommandInteractorImpl {
    final XiaomingMinecraftServer server;
    final ServerConfiguration configuration;
    final ServerPlayerData playerData;
    final XiaomingMinecraftPlugin plugin;

    public ServerCommandInteractor(XiaomingMinecraftPlugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getMinecraftServer();
        this.configuration = plugin.getConfiguration();
        this.playerData = plugin.getPlayerData();

        enableUsageCommand(ServerWords.SERVER);
    }

    @Filter(ServerWords.SET + ServerWords.XIAOMING + ServerWords.IDENTIFY + " {remain}")
    @Filter(ServerWords.EDIT + ServerWords.XIAOMING + ServerWords.IDENTIFY + " {remain}")
    @Require("minecraft.identify.set")
    public void onSetXiaomingIdentify(XiaomingUser user,
                                      @FilterParameter("remain") String identify) {
        configuration.setXiaomingIdentify(identify);
        user.sendMessage("成功修改小明凭据");
        getXiaomingBot().getScheduler().readySave(configuration);
    }

    @Filter(ServerWords.XIAOMING + ServerWords.IDENTIFY)
    @Require("minecraft.identify.look")
    public void onLookXiaomingIdentify(XiaomingUser user) {
        user.sendMessage("当前的小明凭据为：" + configuration.getXiaomingIdentify());
    }

    @Filter(ServerWords.DEBUG + ServerWords.SERVER)
    @Require("minecraft.debug")
    public void onDebug(XiaomingUser user) {
        configuration.setDebug(!configuration.isDebug());
        getXiaomingBot().getScheduler().readySave(configuration);
        if (configuration.isDebug()) {
            user.sendMessage("已启动调试模式");
        } else {
            user.sendMessage("已关闭调试模式");
        }
    }

    @Filter(ServerWords.TEST + ServerWords.SERVER + " {server}")
    @Require("minecraft.test")
    public void onTestServer(XiaomingUser user, @FilterParameter("server") BukkitPluginReceptionist receptionist) {
        if (receptionist.test()) {
            user.sendMessage("小明和该服务器连接正常");
        } else {
            user.sendError("无法连接到该服务器");
        }
    }

    @Filter(ServerWords.CONNECTION + ServerWords.HISTORY)
    @Filter(ServerWords.SERVER + ServerWords.CONNECTION + ServerWords.HISTORY)
    @Require("minecraft.history.list")
    public void onListConnectionHistory(XiaomingUser user) {
        final ConnectHistory connectHistory = plugin.getConnectHistory();
        final Map<String, List<Long>> histories = connectHistory.getHistories();
        if (histories.isEmpty()) {
            user.sendMessage("没有任何连接记录");
        } else {
            user.sendMessage("服务器连接记录：\n" +
                    CollectionUtils.getSummary(histories.entrySet(), entry -> {
                        return entry.getKey() + "：" + entry.getValue().size() + " 条";
                    }));
        }
    }

    @Filter(ServerWords.CONNECTION + ServerWords.HISTORY + " {identifyOrName}")
    @Filter(ServerWords.SERVER + ServerWords.CONNECTION + ServerWords.HISTORY + " {identifyOrName}")
    @Require("minecraft.history.look")
    public void onLookConnectionHistory(XiaomingUser user, @FilterParameter("identifyOrName") String identifyOrName) {
        final ConnectHistory connectHistory = plugin.getConnectHistory();
        List<Long> times = connectHistory.forIdentify(identifyOrName);

        if (CollectionUtils.isEmpty(times)) {
            final MinecraftServerDetail serverDetail = configuration.forServerName(identifyOrName);
            if (Objects.nonNull(serverDetail)) {
                times = connectHistory.forIdentify(serverDetail.getIdentify());
            }
        }

        if (CollectionUtils.isEmpty(times)) {
            user.sendError("该服务器从未连接过小明");
        } else {
            user.sendMessage("该服务器的连接记录共有 " + times.size() + " 条：\n" +
                    CollectionUtils.getIndexSummary(times, com.chuanwise.xiaoming.minecraft.util.TimeUtils.FORMAT::format));
        }
    }

    @Filter(ServerWords.REMOVE + ServerWords.CONNECTION + ServerWords.HISTORY + " {identifyOrName}")
    @Filter(ServerWords.REMOVE + ServerWords.SERVER + ServerWords.CONNECTION + ServerWords.HISTORY + " {identifyOrName}")
    @Require("minecraft.history.remove")
    public void onRemoveConnectionHistory(XiaomingUser user, @FilterParameter("identifyOrName") String identifyOrName) {
        final ConnectHistory connectHistory = plugin.getConnectHistory();
        List<Long> times = connectHistory.forIdentify(identifyOrName);
        String identify = identifyOrName;

        if (CollectionUtils.isEmpty(times)) {
            final MinecraftServerDetail serverDetail = configuration.forServerName(identifyOrName);
            if (Objects.nonNull(serverDetail)) {
                identify = serverDetail.getIdentify();
                times = connectHistory.forIdentify(serverDetail.getIdentify());
            }
        }

        if (CollectionUtils.isEmpty(times)) {
            user.sendError("该服务器从未连接过小明");
            return;
        }

        long before = System.currentTimeMillis() - TimeUtils.parseTime(InteractorUtils.waitNextLegalInput(user, string -> {
            return com.chuanwise.xiaoming.minecraft.util.TimeUtils.parseTime(string) != -1;
        }, "「{last}」并不是一个合理的时间段哦").serialize());
        connectHistory.removeBefore(identify, before);

        getXiaomingBot().getScheduler().readySave(connectHistory);
        user.sendMessage("成功删除该服务器在 " + com.chuanwise.xiaoming.minecraft.util.TimeUtils.FORMAT.format(before) + " 之前的连接记录");
    }

    @Filter(ServerWords.REMOVE + ServerWords.CONNECTION + ServerWords.HISTORY)
    @Filter(ServerWords.REMOVE + ServerWords.SERVER + ServerWords.CONNECTION + ServerWords.HISTORY)
    @Require("minecraft.history.remove")
    public void onRemoveConnectionHistory(XiaomingUser user) {
        final ConnectHistory connectHistory = plugin.getConnectHistory();
        user.sendMessage("希望删除多久之前的连接记录？");
        long before = System.currentTimeMillis() - TimeUtils.parseTime(InteractorUtils.waitNextLegalInput(user, string -> {
            return com.chuanwise.xiaoming.minecraft.util.TimeUtils.parseTime(string) != -1;
        }, "「{last}」并不是一个合理的时间段哦").serialize());
        connectHistory.removeBefore(before);

        getXiaomingBot().getScheduler().readySave(connectHistory);
        user.sendMessage("成功删除所有服务器在 " + com.chuanwise.xiaoming.minecraft.util.TimeUtils.FORMAT.format(before) + " 之前的连接记录");
    }

    @Filter(ServerWords.REMOVE + ServerWords.SERVER + " {server}")
    @Require("minecraft.server.remove")
    public void onRemoveServer(XiaomingUser user, @FilterParameter("server") MinecraftServerDetail detail) {
        configuration.getServerDetails().remove(detail.getName());
        final BukkitPluginReceptionist receptionist = server.forName(detail.getName());
        if (Objects.nonNull(receptionist)) {
            user.sendWarning("成功删除该服务器，并断开与之的连接");
            receptionist.stop();
        } else {
            user.sendWarning("成功删除该服务器");
        }
        getXiaomingBot().getScheduler().readySave(configuration);
    }

    @Filter(ServerWords.DISCONNECT + ServerWords.SERVER + " {server}")
    @Require("minecraft.link.disconnect")
    public void onDisconnectServer(XiaomingUser user, @FilterParameter("server") BukkitPluginReceptionist receptionist) {
        receptionist.stop();
        user.sendMessage("已断开与该服的连接");
    }

    @Filter(ServerWords.TAG + ServerWords.SERVER + ServerWords.WORLD + " {server} {world} {tag}")
    @Require("minecraft.server.world.tag.add")
    public void onAddWorldTag(XiaomingUser user,
                              @FilterParameter("server") MinecraftServerDetail detail,
                              @FilterParameter("world") String worldName,
                              @FilterParameter("tag") String tag) {
        if (detail.worldHasTag(worldName, tag)) {
            user.sendWarning("该世界已经拥有这个标记了");
        } else {
            detail.addWorldTag(worldName, tag);
            getXiaomingBot().getScheduler().readySave(configuration);

            user.sendMessage("成功为「{server}」的世界「{world}」增加了标记「{tag}」");
        }
    }

    @Filter(ServerWords.BIND + ServerWords.MESSAGE + " {qq}")
    @Require("minecraft.admin.bind.look")
    public void onLookBindMessage(XiaomingUser user, @FilterParameter("qq") ServerPlayer player) {
        user.sendMessage("该用户绑定的服务器 ID 为：" + player.getId());
    }

    @Filter(ServerWords.REMOVE + ServerWords.TAG + ServerWords.WORLD + " {server} {world} {tag}")
    @Require("minecraft.server.world.tag.remove")
    public void onRemoveWorldTag(XiaomingUser user,
                                 @FilterParameter("server") MinecraftServerDetail detail,
                                 @FilterParameter("world") String worldName,
                                 @FilterParameter("tag") String tag) {
        if (Objects.equals(tag, "recorded") || Objects.equals(worldName, tag)) {
            user.sendMessage("「{tag}」是原生标记，不能删除");
            return;
        }
        if (detail.worldHasTag(worldName, tag)) {
            detail.removeWorldTag(worldName, tag);
            getXiaomingBot().getScheduler().readySave(configuration);

            user.sendMessage("成功删除「{server}」的世界「{world}」的标记「{tag}」");
        } else {
            user.sendWarning("该世界并没有这个标记");
        }
    }

    @Filter(ServerWords.WORLD + ServerWords.TAG + " {server} {world}")
    @Require("minecraft.server.world.tag.look")
    public void onLookWorldTags(XiaomingUser user,
                                @FilterParameter("server") MinecraftServerDetail detail,
                                @FilterParameter("world") String worldName) {
        final Set<String> strings = detail.getWorldTags().get(worldName);
        if (CollectionUtils.isEmpty(strings)) {
            final BukkitPluginReceptionist receptionist = server.forName(detail.getName());

            if (Objects.nonNull(receptionist)) {
                user.sendError("该服务器上并没有这个世界");
            } else {
                user.sendError("该服务器上并没有这个世界。如果你坚信存在这个世界，请让该服务器连接小明，小明会刷新数据");
            }
        } else {
            user.sendMessage("该世界具有的标记有：" + CollectionUtils.getSummary(strings, String::toString, "", "", "、"));
        }
    }

    @Filter(ServerWords.SERVER + ServerWords.PORT)
    @Filter(ServerWords.PORT)
    @Require("minecraft.port.look")
    public void onLookWorldTags(XiaomingUser user) {
        user.sendMessage("当前服务器端口：" + server.getPort());
    }

    @Filter(ServerWords.SET + ServerWords.PORT + " {port}")
    @Filter(ServerWords.EDIT + ServerWords.PORT + " {port}")
    @Filter(ServerWords.SET + ServerWords.SERVER + ServerWords.PORT + " {port}")
    @Filter(ServerWords.EDIT + ServerWords.SERVER + ServerWords.PORT + " {port}")
    @Require("minecraft.port.set")
    public void onLookWorldTags(XiaomingUser user, @FilterParameter("port") String portString) {
        final int port;
        if (portString.matches("\\d+")) {
            port = Integer.parseInt(portString);
        } else {
            user.sendError("「{port}」并不是一个合理的端口呢");
            return;
        }

        server.setPort(port);
        configuration.setPort(port);
        getXiaomingBot().getScheduler().readySave(configuration);

        if (server.isRunning()) {
            user.sendMessage("成功修改服务器端口为：" + server.getPort() + "，它将在下一次启动服务器时生效");
        } else {
            user.sendMessage("成功修改服务器端口为：" + server.getPort() + "，它将在下一次启动服务器时生效");
        }
    }

    @Filter(ServerWords.WORLD + ServerWords.TAG + " {server}")
    @Require("minecraft.server.world.tag.look")
    public void onLookWorldTags(XiaomingUser user,
                                @FilterParameter("server") MinecraftServerDetail detail) {
        final Map<String, Set<String>> worldTags = detail.getWorldTags();
        if (CollectionUtils.isEmpty(worldTags.entrySet())) {
            user.sendMessage("该服务器上没有任何世界");
        } else {
            user.sendMessage("该服务器的世界的标记有：\n" + CollectionUtils.getIndexSummary(worldTags.entrySet(), entry -> {
                return entry.getKey() + "：" + CollectionUtils.getSummary(entry.getValue(), String::toString, "", "（无标记）", "、");
            }));
        }
    }

    @Filter(ServerWords.ONLINE + ServerWords.SERVER)
    @Require("minecraft.list")
    public void onListOnlineServer(XiaomingUser user) {
        final List<BukkitPluginReceptionist> receptionists = server.getReceptionists();
        if (receptionists.isEmpty()) {
            user.sendMessage("没有任何 Minecraft 服务器成功连接至小明");
        } else {
            user.sendMessage("当前在线的服务器：\n" +
                    CollectionUtils.getIndexSummary(receptionists, receptionist -> {
                        return receptionist.getDetail().getName() +
                                "（已连接：" + TimeUtils.toTimeString(System.currentTimeMillis() - receptionist.getConnectTime()) + "）";
                    }));
        }
    }

    @Filter(ServerWords.SERVER)
    @Require("minecraft.list")
    public void onListServer(XiaomingUser user) {
        final Map<String, MinecraftServerDetail> serverDetails = configuration.getServerDetails();
        if (serverDetails.isEmpty()) {
            user.sendMessage("小明没有记录任何 Minecraft 服务器");
        } else {
            user.sendMessage("服务器列表：\n" + CollectionUtils.getIndexSummary(serverDetails.values(), this::getServerDetailString));
        }
    }

    @Filter(ServerWords.SERVER + " {server}")
    @Require("minecraft.look")
    public void onLookServer(XiaomingUser user, @FilterParameter("server") MinecraftServerDetail detail) {
        if (user instanceof PrivateXiaomingUser) {
            user.sendMessage("服务器信息：\n" + getServerCompleteDetailString(detail));
        } else {
            user.sendMessage("服务器信息：\n" + getServerDetailString(detail) + "\n（私聊可查看原始凭据）");
        }
    }

    @Filter(ServerWords.ADD + ServerWords.SERVER + " {name}")
    @Filter(ServerWords.NEW + ServerWords.SERVER + " {name}")
    @Filter(ServerWords.ADD + ServerWords.SERVER + " {name} {identify}")
    @Filter(ServerWords.NEW + ServerWords.SERVER + " {name} {identify}")
    @Require("minecraft.server.add")
    public void onAddServer(XiaomingUser user, @FilterParameter("identify") String identify, @FilterParameter("name") String name) {
        final Map<String, MinecraftServerDetail> serverDetails = configuration.getServerDetails();
        final MinecraftServerDetail sameIdentifyConfiguration = serverDetails.get(identify);

        if (Objects.nonNull(sameIdentifyConfiguration)) {
            user.sendError("已经有服务器的名字为「{identify}」了，换一个标识吧");
        } else {
            if (StringUtils.isEmpty(identify)) {
                user.sendMessage("这个服务器的原始凭据是什么？");
                identify = user.nextInput().serialize();
                user.setProperty("identify", identify);
            }
            final MinecraftServerDetail detail = configuration.addServer(identify, name);
            getXiaomingBot().getScheduler().readySave(configuration);

            user.sendMessage("成功添加新的服务器，详细信息如下：\n" + getServerDetailString(detail));
        }
    }

    @Filter(ServerWords.ADD + ServerWords.GROUP + ServerWords.CHANNEL + " {channel}")
    @Filter(ServerWords.NEW + ServerWords.GROUP + ServerWords.CHANNEL + " {channel}")
    @Require("minecraft.channel.group.add")
    public void onAddGroupChannel(XiaomingUser user, @FilterParameter("channel") String name) {
        final ChatSettings chatSettings = configuration.getChatSettings();
        if (Objects.nonNull(chatSettings.forGroupChannel(name))) {
            user.sendError("已经存在名为「" + name + "」的群聊频道了，换个频道名吧");
            return;
        }

        final GroupChannel channel = new GroupChannel();
        channel.setName(name);

        user.sendMessage("该频道和哪些群相关呢？给出群 tag 吧");
        channel.setGroupTag(user.nextInput().serialize());

        user.sendMessage("以什么开头的消息是这个频道内的？");
        channel.setHead(user.nextInput().serialize());

        chatSettings.getGroupChannels().add(channel);
        getXiaomingBot().getScheduler().readySave(configuration);

        user.sendMessage("成功创建群聊频道「" + channel.getName() + "」，详情如下：\n" + getChannelSummary(channel));
    }

    @Filter(ServerWords.SET + ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.FORMAT + " {channel} {remain}")
    @Filter(ServerWords.EDIT + ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.FORMAT + " {channel} {remain}")
    @Filter(ServerWords.SET + ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.MESSAGE + ServerWords.FORMAT + " {channel} {remain}")
    @Filter(ServerWords.EDIT + ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.MESSAGE + ServerWords.FORMAT + " {channel} {remain}")
    @Require("minecraft.channel.group.format.set")
    public void onSetGroupChannelFormat(XiaomingUser user, @FilterParameter("channel") GroupChannel channel, @FilterParameter("remain") String format) {
        channel.setFormat(format);
        getXiaomingBot().getScheduler().readySave(configuration);
        user.sendMessage("成功设置群聊频道消息格式");
    }

    @Filter(ServerWords.SET + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.FORMAT + " {channel} {remain}")
    @Filter(ServerWords.EDIT + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.FORMAT + " {channel} {remain}")
    @Filter(ServerWords.SET + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.MESSAGE + ServerWords.FORMAT + " {channel} {remain}")
    @Filter(ServerWords.EDIT + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.MESSAGE + ServerWords.FORMAT + " {channel} {remain}")
    @Require("minecraft.channel.server.format.set")
    public void onSetServerChannelFormat(XiaomingUser user, @FilterParameter("channel") MinecraftChannel channel, @FilterParameter("remain") String format) {
        channel.setFormat(format);
        getXiaomingBot().getScheduler().readySave(configuration);
        user.sendMessage("成功设置服务器频道消息格式");
    }

    @Filter(ServerWords.SET + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.HEAD + " {channel} {head}")
    @Filter(ServerWords.EDIT + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.HEAD + " {channel} {head}")
    @Filter(ServerWords.SET + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.MESSAGE + ServerWords.HEAD + " {channel} {head}")
    @Filter(ServerWords.EDIT + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.MESSAGE + ServerWords.HEAD + " {channel} {head}")
    @Require("minecraft.channel.server.head.set")
    public void onSetServerChannelHead(XiaomingUser user, @FilterParameter("channel") MinecraftChannel channel, @FilterParameter("head") String head) {
        if (StringUtils.isEmpty(head)) {
            user.sendError("消息格式不能为空");
        } else {
            channel.setHead(head);
            getXiaomingBot().getScheduler().readySave(configuration);
            user.sendMessage("成功设置服务器频道消息标志");
        }
    }

    @Filter(ServerWords.SET + ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.TAG + " {channel} {tag}")
    @Filter(ServerWords.EDIT + ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.TAG + " {channel} {tag}")
    @Require("minecraft.channel.group.groupTag.set")
    public void onSetGroupChannelGroupTag(XiaomingUser user, @FilterParameter("channel") GroupChannel channel, @FilterParameter("tag") String tag) {
        channel.setGroupTag(tag);
        getXiaomingBot().getScheduler().readySave(configuration);
        user.sendMessage("成功设置群聊频道「{channel}」的关联群标记为「{tag}」");
    }

    @Filter(ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.TAG + " {channel}")
    @Require("minecraft.channel.group.groupTag.look")
    public void onLookGroupChannelGroupTag(XiaomingUser user, @FilterParameter("channel") GroupChannel channel) {
        user.sendMessage("群聊频道「{channel}」的关联群标记为「" + channel.getGroupTag() + "」");
    }

    @Filter(ServerWords.SET + ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.HEAD + " {channel} {head}")
    @Filter(ServerWords.EDIT + ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.HEAD + " {channel} {head}")
    @Require("minecraft.channel.group.head.set")
    public void onSetGroupChannelHead(XiaomingUser user, @FilterParameter("channel") GroupChannel channel, @FilterParameter("head") String head) {
        channel.setHead(head);
        getXiaomingBot().getScheduler().readySave(configuration);
        user.sendMessage("成功设置群聊频道「{channel}」的消息头为「{tag}」");
    }

    @Filter(ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.HEAD + " {channel}")
    @Require("minecraft.channel.group.groupTag.look")
    public void onLookGroupChannelHead(XiaomingUser user, @FilterParameter("channel") GroupChannel channel) {
        user.sendMessage("群聊频道「{channel}」的消息头为「" + channel.getHead() + "」");
    }

    @Filter(ServerWords.SET + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.WORLD + ServerWords.TAG + " {channel} {tag}")
    @Filter(ServerWords.EDIT + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.WORLD + ServerWords.TAG + " {channel} {tag}")
    @Require("minecraft.channel.server.worldTag.set")
    public void onSetServerChannelWorldTag(XiaomingUser user, @FilterParameter("channel") MinecraftChannel channel, @FilterParameter("tag") String tag) {
        channel.setWorldTag(tag);
        getXiaomingBot().getScheduler().readySave(configuration);
        user.sendMessage("成功设置服务器频道「{channel}」的世界标记为「{tag}」");
    }

    @Filter(ServerWords.SET + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.SERVER + ServerWords.TAG + " {channel} {tag}")
    @Filter(ServerWords.EDIT + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.SERVER + ServerWords.TAG + " {channel} {tag}")
    @Require("minecraft.channel.server.serverTag.set")
    public void onSetServerChannelServerTag(XiaomingUser user, @FilterParameter("channel") MinecraftChannel channel, @FilterParameter("tag") String tag) {
        channel.setServerTag(tag);
        getXiaomingBot().getScheduler().readySave(configuration);
        user.sendMessage("成功设置服务器频道「{channel}」的服务器标记为「{tag}」");
    }

    @Filter(ServerWords.SET + ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.SERVER + ServerWords.TAG + " {channel} {tag}")
    @Filter(ServerWords.EDIT + ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.SERVER + ServerWords.TAG + " {channel} {tag}")
    @Require("minecraft.channel.group.serverTag.set")
    public void onSetGroupChannelServerTag(XiaomingUser user, @FilterParameter("channel") GroupChannel channel, @FilterParameter("tag") String tag) {
        channel.setServerTag(tag);
        getXiaomingBot().getScheduler().readySave(configuration);
        user.sendMessage("成功设置群聊频道「{channel}」的服务器标记为「{tag}」");
    }

    @Filter(ServerWords.SET + ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.WORLD + ServerWords.TAG + " {channel} {tag}")
    @Filter(ServerWords.EDIT + ServerWords.GROUP + ServerWords.CHANNEL + ServerWords.WORLD + ServerWords.TAG + " {channel} {tag}")
    @Require("minecraft.channel.group.worldTag.set")
    public void onSetGroupChannelWorldTag(XiaomingUser user, @FilterParameter("channel") GroupChannel channel, @FilterParameter("tag") String tag) {
        channel.setWorldTag(tag);
        getXiaomingBot().getScheduler().readySave(configuration);
        user.sendMessage("成功设置群聊频道「{channel}」的世界标记为「{tag}」");
    }

    @Filter(ServerWords.SET + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.GROUP + ServerWords.TAG + " {channel} {tag}")
    @Filter(ServerWords.EDIT + ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.GROUP + ServerWords.TAG + " {channel} {tag}")
    @Require("minecraft.channel.server.groupTag.set")
    public void onSetServerChannelGroupTag(XiaomingUser user, @FilterParameter("channel") MinecraftChannel channel, @FilterParameter("tag") String tag) {
        channel.setGroupTag(tag);
        getXiaomingBot().getScheduler().readySave(configuration);
        user.sendMessage("成功设置服务器频道「{channel}」的群聊标记为「{tag}」");
    }

    @Filter(ServerWords.SERVER + ServerWords.CHANNEL + ServerWords.SERVER + ServerWords.TAG + " {channel}")
    @Require("minecraft.channel.server.serverTag.look")
    public void onLookServerChannelServerTag(XiaomingUser user, @FilterParameter("channel") MinecraftChannel channel) {
        user.sendMessage("服务器频道「{channel}」的服务器标记为「" + channel.getServerTag() + "」");
    }

    @Filter(ServerWords.ADD + ServerWords.SERVER + ServerWords.CHANNEL + " {channel}")
    @Filter(ServerWords.NEW + ServerWords.SERVER + ServerWords.CHANNEL + " {channel}")
    @Require("minecraft.channel.server.add")
    public void onAddMinecraftChannel(XiaomingUser user, @FilterParameter("channel") String name) {
        final ChatSettings chatSettings = configuration.getChatSettings();
        if (Objects.nonNull(chatSettings.forMinecraftChannel(name))) {
            user.sendError("已经存在名为「" + name + "」的服务器频道了，换个频道名吧");
            return;
        }

        final MinecraftChannel channel = new MinecraftChannel();
        channel.setName(name);

        user.sendMessage("该频道和哪些服务器有关呢？给出相关服务器共有的 tag 吧");
        channel.setServerTag(user.nextInput().serialize());

        user.sendMessage("该频道和这些服务器的哪些世界有关呢？给出这些世界的 tag 吧");
        channel.setWorldTag(user.nextInput().serialize());

        user.sendMessage("以什么开头的消息是这个频道内的？");
        channel.setHead(user.nextInput().serialize());

        chatSettings.getMinecraftChannels().add(channel);
        getXiaomingBot().getScheduler().readySave(configuration);

        user.sendMessage("成功创建服务器频道「" + channel.getName() + "」，详情如下：\n" + getChannelSummary(channel));
    }

    @Filter(ServerWords.REMOVE + ServerWords.GROUP + ServerWords.CHANNEL + " {channel}")
    @Require("minecraft.channel.group.remove")
    public void onRemoveGroupChannel(XiaomingUser user, @FilterParameter("channel") GroupChannel channel) {
        final ChatSettings chatSettings = configuration.getChatSettings();
        chatSettings.getGroupChannels().remove(channel);
        getXiaomingBot().getScheduler().readySave(configuration);

        user.sendMessage("成功删除群聊频道「{channel}」");
    }

    @Filter(ServerWords.REMOVE + ServerWords.SERVER + ServerWords.CHANNEL + " {channel}")
    @Require("minecraft.channel.server.remove")
    public void onRemoveMinecraftChannel(XiaomingUser user, @FilterParameter("channel") MinecraftChannel channel) {
        final ChatSettings chatSettings = configuration.getChatSettings();
        chatSettings.getMinecraftChannels().remove(channel);
        getXiaomingBot().getScheduler().readySave(configuration);

        user.sendMessage("成功删除服务器频道「{channel}」");
    }

    @Filter(ServerWords.SERVER + ServerWords.CHANNEL)
    @Require("minecraft.channel.server.list")
    public void onListServerChannel(XiaomingUser user) {
        final ChatSettings chatSettings = configuration.getChatSettings();
        final Set<MinecraftChannel> minecraftChannels = chatSettings.getMinecraftChannels();

        if (minecraftChannels.isEmpty()) {
            user.sendMessage("当前没有任何服务器频道");
        } else {
            user.sendMessage("服务器频道：\n" + CollectionUtils.getIndexSummary(minecraftChannels, MinecraftChannel::getName));
        }
    }

    @Filter(ServerWords.SERVER + ServerWords.CHANNEL + " {channel}")
    @Require("minecraft.channel.server.look")
    public void onLookServerChannel(XiaomingUser user, @FilterParameter("channel") MinecraftChannel channel) {
        user.sendMessage("服务器频道详情：\n" + getChannelSummary(channel));
    }

    @Filter(ServerWords.GROUP + ServerWords.CHANNEL + " {channel}")
    @Require("minecraft.channel.group.look")
    public void onLookGroupChannel(XiaomingUser user, @FilterParameter("channel") GroupChannel channel) {
        user.sendMessage("群聊频道详情：\n" + getChannelSummary(channel));
    }

    @Filter(ServerWords.GROUP + ServerWords.CHANNEL)
    @Require("minecraft.channel.group.list")
    public void onListGroupChannel(XiaomingUser user) {
        final ChatSettings chatSettings = configuration.getChatSettings();
        final Set<GroupChannel> groupChannels = chatSettings.getGroupChannels();

        if (groupChannels.isEmpty()) {
            user.sendMessage("当前没有任何群聊频道");
        } else {
            user.sendMessage("群聊频道：\n" + CollectionUtils.getIndexSummary(groupChannels, channel -> {
                return channel.getName();
            }));
        }
    }

    public String getChannelSummary(MinecraftChannel channel) {
        return "频道名：" + channel.getName() + "\n" +
                "消息标志：" + channel.getHead() + "\n" +
                "消息格式：" + channel.getFormat() + "\n" +
                "关联群：" + channel.getGroupTag() + "\n" +
                "关联服务器：" + channel.getServerTag() + ("（" + configuration.forServerTag(channel.getServerTag()).size() + " 个）") + "\n" +
                "关联世界：" + channel.getWorldTag();
    }

    public String getChannelSummary(GroupChannel channel) {
        return "频道名：" + channel.getName() + "\n" +
                "消息标志：" + channel.getHead() + "\n" +
                "消息格式：" + channel.getFormat() + "\n" +
                "相关群：" + channel.getGroupTag() + ("（" + getXiaomingBot().getResponseGroupManager().forTag(channel.getGroupTag()).size() + " 个）") + "\n" +
                "相关服务器：" + channel.getServerTag() + "\n" +
                "相关世界：" + channel.getServerTag();
    }

    @Filter(ServerWords.ONLINE + ServerWords.PLAYERS)
    @Filter(ServerWords.ONLINE + ServerWords.POPULATION)
    @Require("minecraft.user.onlinePlayers")
    public void onOnlinePlayers(XiaomingUser user) {
        final BukkitPluginReceptionist target = TargetUtils.requireTarget(user);

        getXiaomingBot().getScheduler().run(() -> {
            runOrCatch(user, target, () -> {
                final Set<PlayerContent> players = target.listOnlinePlayers();
                if (players.isEmpty()) {
                    user.sendWarning("服务器一个人都没有");
                } else {
                    user.sendMessage("「" + target.getDetail().getName() + "」有 " + players.size() + " 人在线：\n" +
                            Formatter.clearColors(CollectionUtils.getIndexSummary(players, PlayerContent::getDisplayName)));
                }
            });
        });
    }

    @Filter(ServerWords.ALL + ServerWords.SERVER + ServerWords.ONLINE + ServerWords.PLAYERS)
    @Filter(ServerWords.ALL + ServerWords.SERVER + ServerWords.ONLINE + ServerWords.POPULATION)
    @Require("minecraft.user.onlinePlayers")
    public void onAllOnlinePlayers(XiaomingUser user) {
        List<String> onlinePlayers = new ArrayList<>();
        for (BukkitPluginReceptionist receptionist : server.getReceptionists()) {
            runOrCatch(user, receptionist, () -> {
                final Set<PlayerContent> playerContents = receptionist.listOnlinePlayers();
                CollectionUtils.addTo(playerContents, onlinePlayers, PlayerContent::getPlayerListName);
            });
        }
        if (onlinePlayers.isEmpty()) {
            user.sendWarning("服务器一个人都没有");
        } else {
            Collections.sort(onlinePlayers);
            user.sendMessage("所有服务器有 " + onlinePlayers.size() + " 人在线：\n" +
                    Formatter.clearColors(CollectionUtils.getIndexSummary(onlinePlayers)));
        }
    }

    @Filter(ServerWords.WORLD)
    @Require("minecraft.worldNames")
    public void onWorldNames(XiaomingUser user) {
        final BukkitPluginReceptionist target = TargetUtils.requireTarget(user);
        getXiaomingBot().getScheduler().run(() -> {
            runOrCatch(user, target, () -> {
                final Set<String> worldsNames = target.listWorldsNames();
                if (worldsNames.isEmpty()) {
                    user.sendMessage("服务器里没有任何世界呢");
                } else {
                    user.sendMessage("服务器的世界有：\n" + CollectionUtils.getSummary(worldsNames, String::toString, "", "", "、"));
                }
            });
        });
    }

    @Filter(ServerWords.ENABLE + ServerWords.SERVER)
    @Require("minecraft.enable")
    public void onEnableServer(XiaomingUser user) {
        if (server.isRunning()) {
            user.sendError("服务器已经启动了");
        } else {
            try {
                server.start();
                user.sendMessage("成功启动服务器");
            } catch (IOException exception) {
                user.sendError("启动服务器时出现异常：" + exception);
            }
        }
    }

    @Filter("(重新|重|re)" + ServerWords.ENABLE + ServerWords.SERVER)
    @Require("minecraft.reenable")
    public void onReenableServer(XiaomingUser user) {
        if (!server.isRunning()) {
            user.sendError("服务器并没有启动");
        } else {
            try {
                server.restart(0);
                user.sendMessage("重启启动服务器");
            } catch (InterruptedException ignored) {
            } catch (IOException exception) {
                user.sendError("重启服务器时出现异常：" + exception);
            }
        }
    }

    @Filter(ServerWords.DISABLE + ServerWords.SERVER)
    @Require("minecraft.disable")
    public void onDisableServer(XiaomingUser user) {
        if (!server.isRunning()) {
            user.sendError("服务器并没有启动");
        } else {
            try {
                server.shutdown();
                user.sendMessage("成功关闭服务器");
            } catch (IOException exception) {
                user.sendError("关闭服务器时出现异常：" + exception);
            }
        }
    }

    @Filter(ServerWords.CONSOLE + ServerWords.EXECUTE + " {remain}")
    @Require("minecraft.admin.execute")
    public void onExecuteAsConsole(XiaomingUser user, @FilterParameter("remain") String command) {
        final BukkitPluginReceptionist target = TargetUtils.requireTarget(user);
        if (StringUtils.isEmpty(command)) {
            user.sendError("指令内容不能为空");
            return;
        }

        getXiaomingBot().getScheduler().run(() -> {
            runOrCatch(user, target, () -> {
                final String discription = Formatter.clearColors(target.executeAsConsole(MiraiCode.deserializeMiraiCode(command).contentToString()).getDescription("指令执行"));
                user.sendMessage(discription);
            });
        });
    }

    @Filter(ServerWords.EXECUTE + " {remain}")
    @Require("minecraft.admin.execute")
    public void onExecuteAsPlayer(XiaomingUser user, @FilterParameter("remain") String command) {
        final BukkitPluginReceptionist target = TargetUtils.requireTarget(user);

        final ServerPlayer serverPlayer = playerData.forPlayer(user.getCode());
        if (Objects.isNull(serverPlayer)) {
            user.sendError("你还没有绑定服务器账号，赶快用「绑定 [你的服务器ID]」绑定一个吧");
            return;
        }
        if (StringUtils.isEmpty(command)) {
            user.sendError("指令内容不能为空");
            return;
        }

        getXiaomingBot().getScheduler().run(() -> {
            runOrCatch(user, target, () -> {
                final String discription = Formatter.clearColors(target.executeAsPlayer(serverPlayer.getId(), MiraiCode.deserializeMiraiCode(command).contentToString()).getDescription("指令执行"));
                user.sendMessage(discription);
            });
        });
    }

    @Filter(ServerWords.FORCE + ServerWords.EXECUTE + " {qq} {remain}")
    @Require("minecraft.admin.execute")
    public void onExecuteAsOthers(XiaomingUser user, @FilterParameter("qq") ServerPlayer player, @FilterParameter("remain") String command) {
        final BukkitPluginReceptionist target = TargetUtils.requireTarget(user);
        if (StringUtils.isEmpty(command)) {
            user.sendError("指令内容不能为空");
            return;
        }

        getXiaomingBot().getScheduler().run(() -> {
            runOrCatch(user, target, () -> {
                final String discription = Formatter.clearColors(target.executeAsPlayer(player.getId(), MiraiCode.deserializeMiraiCode(command).contentToString()).getDescription("强制执行指令"));
                user.sendMessage(discription);
            });
        });
    }

    @Filter(ServerWords.UNBIND)
    @Require("minecraft.user.unbind")
    public void onUnbind(XiaomingUser user) {
        final ServerPlayer serverPlayer = playerData.forPlayer(user.getCode());
        if (Objects.isNull(serverPlayer)) {
            user.sendError("你并没有绑定在任何服务器 ID 上呢");
        } else {
            playerData.unbind(user.getCode());
            user.sendMessage("成功解除与 ID「" + serverPlayer.getId() + "」的绑定");
            getXiaomingBot().getScheduler().readySave(playerData);
        }
    }

    @Filter(ServerWords.UNBIND + " {qq}")
    @Require("minecraft.admin.unbind")
    public void onUnbind(XiaomingUser user, @FilterParameter("qq") ServerPlayer player) {
        playerData.unbind(player.getCode());
        getXiaomingBot().getScheduler().readySave(playerData);
        user.sendMessage("成功解除「" + getXiaomingBot().getAccountManager().getAliasAndCode(player.getCode()) + "」" + "和「" + player.getId() + "」之间的绑定");
    }

    @Filter(ServerWords.BIND + " {player}")
    @Require("minecraft.user.bind")
    public void onBind(XiaomingUser user, @FilterParameter("player") String playerId) {
        final ServerPlayer elderBind = playerData.forPlayer(user.getCode());
        if (Objects.nonNull(elderBind)) {
            user.sendError("你的 QQ 已经被绑定到 ID：" + elderBind.getId() + " 了");
            return;
        }

        final ServerPlayer sameNameServerPlayer = playerData.forPlayer(playerId);
        if (Objects.nonNull(sameNameServerPlayer)) {
            final Account account = getXiaomingBot().getAccountManager().getAccount(sameNameServerPlayer.getCode());
            final String name;
            if (Objects.nonNull(account)) {
                name = account.getCompleteName();
            } else {
                name = String.valueOf(sameNameServerPlayer.getId());
            }
            user.sendMessage("这个 ID 已经被绑定到「" + name + "」上了");
        } else {
            final List<BukkitPluginReceptionist> receptionists = server.getReceptionists();
            if (receptionists.isEmpty()) {
                user.sendError("现在没有任何一个服务器连接到小明，暂时不能绑定哦");
                return;
            }

            // 绑定线程
            getXiaomingBot().getScheduler().run(() -> {
                // 找到一个玩家在线的服务器
                BukkitPluginReceptionist target = null;
                for (BukkitPluginReceptionist receptionist : receptionists) {
                    try {
                        if (receptionist.isOnline(playerId)) {
                            target = receptionist;
                        }
                    } catch (IOException exception) {
                        onIOException(user, receptionist, exception);
                        return;
                    }
                }

                if (Objects.isNull(target)) {
                    user.sendMessage("这个账户没有登录任何小明所在的服务器，登陆后再进行绑定操作吧");
                    return;
                }

                final BukkitPluginReceptionist finalTarget = target;
                runOrCatch(user, target, () -> {
                    user.sendMessage("小明发现这个账户目前在「" + finalTarget.getDetail().getName() + "」在线，留意一下这个服务器里的消息吧");

                    final ResultContent resultContent = finalTarget.playerAccept(playerId, user.getAliasAndCode() + "是你的 QQ 账号吗？这个账号请求将你的账号绑定到他的 QQ 上");
                    user.sendMessage(resultContent.getDescription("绑定"));

                    // 如果绑定成功就保存绑定信息
                    if (resultContent.isSuccess()) {
                        playerData.bind(user.getCode(), playerId);
                        getXiaomingBot().getScheduler().readySave(playerData);
                    }
                });
            });
        }
    }

    @Filter(ServerWords.TAG + ServerWords.SERVER + " {server} {tag}")
    @Require("minecraft.server.tag.add")
    public void onAddServerTag(XiaomingUser user, @FilterParameter("server") MinecraftServerDetail detail, @FilterParameter("tag") String tag) {
        if (detail.hasTag(tag)) {
            user.sendError("该服务器已经具有此 tag 了");
        } else {
            detail.addTag(tag);
            getXiaomingBot().getScheduler().readySave(configuration);
            user.sendMessage("成功为服务器增加标记「{tag}」");
        }
    }

    @Filter(ServerWords.REMOVE + ServerWords.SERVER + ServerWords.TAG + " {server} {tag}")
    @Require("minecraft.server.tag.remove")
    public void onRemoveServerTag(XiaomingUser user, @FilterParameter("server") MinecraftServerDetail detail, @FilterParameter("tag") String tag) {
        if (Arrays.asList(detail.getName(), "recorded").contains(tag)) {
            user.sendError("「{tag}」是该服务器的原生标记，不可以删除");
            return;
        }
        if (detail.hasTag(tag)) {
            detail.getTags().remove(tag);
            getXiaomingBot().getScheduler().readySave(configuration);
            user.sendMessage("成功删除该服务器的标记「{tag}」");
        } else {
            user.sendError("该服务器并没有此 tag");
        }
    }

    @Filter(ServerWords.BIND + " {qq} {player}")
    @Require("minecraft.admin.bind")
    public void onBind(XiaomingUser user, @FilterParameter("qq") long qq, @FilterParameter("player") String newId) {
        final ServerPlayer sameNamePlayer = playerData.forPlayer(newId);
        if (Objects.nonNull(sameNamePlayer)) {
            if (sameNamePlayer.getCode() == qq) {
                user.sendError("禁止原地 TP");
            } else {
                user.sendMessage("这个 ID 已经被绑定到账号「" + getXiaomingBot().getAccountManager().getAliasOrCode(sameNamePlayer.getCode()) + "」了");
            }
            return;
        }

        playerData.getPlayers().put(qq, new ServerPlayer(qq, newId));
        getXiaomingBot().getScheduler().readySave(playerData);

        user.sendMessage("成功将该用户的 ID 绑定到「{player}」");
    }

    @Filter(ServerWords.DEFAULT + ServerWords.TARGET + ServerWords.SERVER)
    @Require("minecraft.user.target.look")
    public void onDefaultTarget(XiaomingUser user) {
        user.sendMessage("默认目标服务器为：" + configuration.getDefaultTarget());
    }

    @Filter(ServerWords.PERSONAL + ServerWords.TARGET + ServerWords.SERVER)
    @Require("minecraft.user.target.look")
    public void onMyTarget(XiaomingUser user) {
        final String userTarget = user.getPropertyOrDefault(TargetUtils.SERVER_TARGET_TAG, configuration.getDefaultTarget());
        user.sendMessage("你的目标服务器为：" + userTarget);
    }

    @Filter(ServerWords.SET + ServerWords.TARGET + ServerWords.SERVER + " {server}")
    @Require("minecraft.user.target.set")
    public void onSetTarget(XiaomingUser user, @FilterParameter("server") BukkitPluginReceptionist receptionist) {
        final String name = receptionist.getDetail().getName();
        user.setProperty(TargetUtils.SERVER_TARGET_TAG, name);
        user.sendMessage("成功设置你的目标服务器为「" + name + "」");
    }

    @Filter(ServerWords.SET + ServerWords.DEFAULT + ServerWords.TARGET + ServerWords.SERVER + " {server}")
    @Require("minecraft.user.target.set")
    public void onSetDefaultTarget(XiaomingUser user, @FilterParameter("server") BukkitPluginReceptionist receptionist) {
        final String name = receptionist.getDetail().getName();
        configuration.setDefaultTarget(name);
        user.sendMessage("成功设置所有人的目标服务器为「" + name + "」");
    }

    public String getServerDetailString(MinecraftServerDetail detail) {
        return "服务器名：" + detail.getName() + "\n" +
                "标记：" + CollectionUtils.getSummary(detail.getTags(), String::toString, "", "（无）", "、");
    }

    public String getServerCompleteDetailString(MinecraftServerDetail detail) {
        return getServerDetailString(detail) + "\n" +
                "原始凭据：" + detail.getIdentify();
    }

    public void runOrCatch(XiaomingUser user, BukkitPluginReceptionist receptionist, ExceptionThrowableRunnable<IOException> callable) {
        try {
            callable.run();
        } catch (IOException exception) {
            onIOException(user, receptionist, exception);
        }
    }

    public void onIOException(XiaomingUser user, BukkitPluginReceptionist receptionist, IOException exception) {
        if (exception instanceof SocketTimeoutException) {
            getLog().warn("服务器「" + receptionist.getDetail().getName() + "」和用户 " + user.getCompleteName() + " 交互时超时");
        } else {
            receptionist.getController().stopCausedBy(exception);
        }
        user.sendError("小明的网络好像不太好，过一会儿再试吧");
    }

    @Override
    public <T> T onParameter(XiaomingUser user, Class<T> clazz, String parameterName, String currentValue, String defaultValue) {
        Object result = super.onParameter(user, clazz, parameterName, currentValue, defaultValue);
        if (Objects.nonNull(result)) {
            return ((T) result);
        }

        if (clazz.isAssignableFrom(MinecraftServerDetail.class) && Objects.equals(parameterName, "server")) {
            final MinecraftServerDetail minecraftServerConfiguration = configuration.forServerName(currentValue);
            if (Objects.nonNull(minecraftServerConfiguration)) {
                result = minecraftServerConfiguration;
            } else {
                user.sendError("没有叫做「{server}」的服务器哦");
                result = null;
            }
        } else if (clazz.isAssignableFrom(BukkitPluginReceptionist.class) && Objects.equals(parameterName, "server")) {
            final BukkitPluginReceptionist bukkitPluginReceptionist = server.forName(currentValue);
            if (Objects.isNull(bukkitPluginReceptionist)) {
                user.sendError("该服务器并没有连接到小明哦");
                result = null;
            } else {
                result = bukkitPluginReceptionist;
            }
        } else if (clazz.isAssignableFrom(ServerPlayer.class) && Objects.equals(parameterName, "player")) {
            final ServerPlayer serverPlayer = playerData.forPlayer(currentValue);
            if (Objects.nonNull(serverPlayer)) {
                result = serverPlayer;
            } else {
                user.sendError("该用户并没有账户绑定到 ID「{player}」上哦");
                result = null;
            }
        } else if (clazz.isAssignableFrom(ServerPlayer.class) && Objects.equals(parameterName, "qq")) {
            final long qq = AtUtils.parseQQ(currentValue);
            if (qq == -1) {
                user.sendError("「{qq}」并不是一个合理的 QQ 哦");
            } else {
                final ServerPlayer serverPlayer = playerData.forPlayer(qq);
                if (Objects.nonNull(serverPlayer)) {
                    result = serverPlayer;
                } else {
                    user.sendError("该用户没有绑定 ID 呢");
                    result = null;
                }
            }
        } else if (clazz.isAssignableFrom(MinecraftChannel.class) && Objects.equals(parameterName, "channel")) {
            final ChatSettings chatSettings = configuration.getChatSettings();
            final MinecraftChannel minecraftChannel = chatSettings.forMinecraftChannel(currentValue);
            if (Objects.nonNull(minecraftChannel)) {
                result = minecraftChannel;
            } else {
                user.sendError("找不到名为「{channel}」的服务器频道");
                result = null;
            }
        } else if (clazz.isAssignableFrom(GroupChannel.class) && Objects.equals(parameterName, "channel")) {
            final ChatSettings chatSettings = configuration.getChatSettings();
            final GroupChannel groupChannel = chatSettings.forGroupChannel(currentValue);
            if (Objects.nonNull(groupChannel)) {
                result = groupChannel;
            } else {
                user.sendError("找不到名为「{channel}」的群聊频道");
                result = null;
            }
        }

        return ((T) result);
    }
}