package com.chuanwise.xiaoming.minecraft.server.server;

import com.chuanwise.xiaoming.api.bot.XiaomingBot;
import com.chuanwise.xiaoming.api.contact.contact.GroupContact;
import com.chuanwise.xiaoming.api.contact.contact.MemberContact;
import com.chuanwise.xiaoming.api.contact.contact.PrivateContact;
import com.chuanwise.xiaoming.api.contact.message.GroupMessage;
import com.chuanwise.xiaoming.api.contact.message.PrivateMessage;
import com.chuanwise.xiaoming.api.exception.InteractorTimeoutException;
import com.chuanwise.xiaoming.api.recept.Receptionist;
import com.chuanwise.xiaoming.api.response.ResponseGroup;
import com.chuanwise.xiaoming.api.schedule.task.ScheduableTask;
import com.chuanwise.xiaoming.api.user.GroupXiaomingUser;
import com.chuanwise.xiaoming.api.user.MemberXiaomingUser;
import com.chuanwise.xiaoming.api.user.PrivateXiaomingUser;
import com.chuanwise.xiaoming.api.util.StringUtils;
import com.chuanwise.xiaoming.api.util.TimeUtils;
import com.chuanwise.xiaoming.core.contact.message.GroupMessageImpl;
import com.chuanwise.xiaoming.core.contact.message.MemberMessageImpl;
import com.chuanwise.xiaoming.core.contact.message.PrivateMessageImpl;
import com.chuanwise.xiaoming.minecraft.pack.Pack;
import com.chuanwise.xiaoming.minecraft.pack.PackType;
import com.chuanwise.xiaoming.minecraft.pack.content.*;
import com.chuanwise.xiaoming.minecraft.server.XiaomingMinecraftPlugin;
import com.chuanwise.xiaoming.minecraft.server.configuration.*;
import com.chuanwise.xiaoming.minecraft.server.data.ServerPlayer;
import com.chuanwise.xiaoming.minecraft.server.data.ServerPlayerData;
import com.chuanwise.xiaoming.minecraft.socket.SocketController;
import com.chuanwise.xiaoming.minecraft.thread.StopableRunnable;
import com.chuanwise.xiaoming.minecraft.util.*;
import com.chuanwise.xiaoming.minecraft.util.Formatter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.message.code.MiraiCode;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.PlainText;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
public class BukkitPluginReceptionist implements StopableRunnable {
    final XiaomingMinecraftServer server;
    final SocketController controller;
    final ExecutorService threadPool = Executors.newCachedThreadPool();
    final XiaomingMinecraftPlugin plugin;
    final ServerPlayerData playerData;
    final ServerConfiguration configuration;
    final ChatSettings chatSettings;
    final XiaomingBot xiaomingBot;
    final int maxIterateTime;

    MinecraftServerDetail detail;
    long connectTime;

    public BukkitPluginReceptionist(XiaomingMinecraftPlugin plugin, Socket socket) throws IOException {
        this.plugin = plugin;
        this.controller = new SocketController(socket, log, false);
        this.server = plugin.getMinecraftServer();
        this.playerData = plugin.getPlayerData();
        this.configuration = plugin.getConfiguration();
        this.chatSettings = configuration.getChatSettings();
        this.xiaomingBot = plugin.getXiaomingBot();
        this.maxIterateTime = xiaomingBot.getConfiguration().getMaxIterateTime();
    }

    @Override
    public void run() {
        final XiaomingMinecraftPlugin plugin = XiaomingMinecraftPlugin.INSTANCE;
        final ServerConfiguration configuration = plugin.getConfiguration();

        try {
            controller.setSocketConfiguration(configuration.getSocketConfiguration());
            controller.setOnNormalExit(() -> {
                try {
                    controller.send(PackType.XM_DISCONNECT);
                } catch (IOException exception) {
                    log.error("无法向 Minecraft 服务器发送断连消息");
                }
            });
            controller.setOnFinally(() -> {
                log.error("断开了和服务器的连接");
                stop();
            });

            controller.register(PackType.MC_DISCONNECT, pack -> {
                if (Objects.nonNull(detail) && !StringUtils.isEmpty(detail.getName()) && configuration.isEnableDisconnectLog()) {
                    final String name = detail.getName();
                    plugin.sendMessageToLog("「" + name + "」断开了和小明的连接");
                }
                stop();
            });
            controller.register(PackType.MC_ERROR, pack -> {
                if (Objects.nonNull(detail)) {
                    final String name = detail.getName();
                    if (!StringUtils.isEmpty(name)) {
                        final String content = pack.getContent(String.class);
                        plugin.sendMessageToLog("「" + name + "」出现问题" + (StringUtils.isEmpty(content) ? "" : ("（" + content + "）")) + "，已切断连接");
                    }
                }
                stop();
            });

            threadPool.execute(controller);

            // 验明真身
            threadPool.execute(() -> {
                try {
                    // 小明发起身份验证请求
                    controller.sendLater(new Pack(PackType.XM_REQUIRE_MC_ENCRYPTED_IDENTIFY));

                    final String serverEncryptedIdentify = controller.nextPack(PackType.MC_ENCRYPTED_IDENTIFY).getContent(String.class);
                    final MinecraftServerDetail serverDetail = configuration.forEncryptedIdentify(serverEncryptedIdentify);

                    final ConnectHistory connectHistory = plugin.getConnectHistory();
                    if (Objects.nonNull(serverDetail)) {
                        this.detail = serverDetail;

                        // 检查是否重复连接
                        final BukkitPluginReceptionist sameIdentifyReceptionist = server.forReceptionist(detail.getName());
                        if (Objects.nonNull(sameIdentifyReceptionist) && sameIdentifyReceptionist.getController().test()) {
                            controller.sendLater(PackType.XM_DENY_IDENTIFY, "同身份服务器已经连接");
                            stop();
                            return;
                        }

                        // 注册自己
                        connectTime = System.currentTimeMillis();

                        // 发送自己的身份
                        controller.sendLater(PackType.XM_ACCEPT_ENCRYPTED_IDENTIFY, PasswordHashUtils.createHash(configuration.getXiaomingIdentify()));
                        server.getReceptionists().add(this);

                        final Pack identifyVerifyResultPack = controller.nextPack(Arrays.asList(PackType.MC_ACCEPT_ENCRYPTED_IDENTIFY, PackType.MC_DENY_ENCRYPTED_IDENTIFY));
                        if (identifyVerifyResultPack.getType() == PackType.MC_ACCEPT_ENCRYPTED_IDENTIFY) {
                            if (configuration.isEnableConnectLog()) {
                                plugin.sendMessageToLog("「" + serverDetail.getName() + "」成功连接到小明");
                            }
                            registerListeners();
                        } else {
                            // 如果是新出现的请求
                            if (configuration.isAlwaysLogFailConnection() || !connectHistory.isConnected(serverDetail.getIdentify())) {
                                plugin.sendMessageToLog("「" + serverDetail.getName() + "」尝试连接小明，但因小明身份无效，该服务器拒绝了小明的请求");
                            }
                            stop();
                        }
                        connectHistory.addHistory(serverDetail.getIdentify());
                    } else {
                        controller.sendLater(PackType.XM_REQUIRE_ORIGINAL_IDENTIFY);
                        final Pack identifyPack = controller.nextPack(PackType.MC_IDENTIFY);
                        final String identify = identifyPack.getContent(String.class);

                        // 确定一下确实没这个服务器
                        if (Objects.nonNull(configuration.forServerIdentify(identify))) {
                            sendError("无法通过加密凭据找到记录，却通过 IDENTIFY 找到了");
                            stop();
                            return;
                        }

                        if (configuration.isAlwaysLogFailConnection() || !connectHistory.isConnected(identify)) {
                            plugin.sendMessageToLog("新的 Minecraft 服务器连接请求：\n" +
                                    "原始凭据：" + identify + "\n" +
                                    "时间：" + TimeUtils.FORMAT.format(System.currentTimeMillis()));
                        }

                        connectHistory.addHistory(identify);
                        controller.send(PackType.XM_DENY_IDENTIFY, "请在 QQ 上添加对本服的身份记录");
                        stop();
                    }
                    getXiaomingBot().getScheduler().readySave(connectHistory);
                } catch (Throwable throwable) {
                    controller.stopCausedBy(throwable);
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    /**
     * 将一个消息中所有的真正的 @、假的 @（@PlayerId）都换为对应的 @PlayerId。
     * 如果对应用户没有绑定 PlayerId，则换为 @QQ
     */
    protected static final Pattern QQ_AT_PATTERN = Pattern.compile("\\[mirai:at:(?<qq>\\d+)\\]");
    protected String replaceAtToPlayerId(String message) {
        final StringBuffer stringBuffer = new StringBuffer(message.length() + 32);
        final Matcher matcher = QQ_AT_PATTERN.matcher(message);

        while (matcher.find()) {
            final String qqString = matcher.group("qq");
            final long code = Long.parseLong(qqString);
            final ServerPlayer serverPlayer = playerData.forPlayer(code);

            final String afterAt = Objects.nonNull(serverPlayer) ? serverPlayer.getId() : plugin.getXiaomingBot().getAccountManager().getAliasOrCode(code);
            matcher.appendReplacement(stringBuffer, "@" + afterAt + " ");
        }
        return matcher.appendTail(stringBuffer).toString();
    }

    /**
     * 将 @QQ，@PlayerId，@Alias 换为真正的 @
     * @param message 替换前的字符串
     * @param contact 群会话
     * @return 替换后的字符串
     */
    protected String replacePlayerIdToAt(String message, GroupContact contact) {
        boolean hasAt = message.contains("@");
        if (!hasAt) {
            return message;
        }

        // 对于每一个在群里的成员，都检查是否有 @ 他
        StringBuilder stringBuilder = new StringBuilder(message);
        for (MemberContact member : contact.getMembers()) {
            final String serializedAtMessage = ' ' + new At(member.getCode()).serializeToMiraiCode() + ' ';

            // 替换所有的 @QQ
            final String atCodeString = '@' + member.getCodeString();
            StringUtils.replaceAll(stringBuilder, atCodeString, serializedAtMessage);

            // 检查有无 @Alias
            final String atAlias = '@' + member.getAlias();
            StringUtils.replaceAll(stringBuilder, atAlias, serializedAtMessage);

            final ServerPlayer serverPlayer = playerData.forPlayer(member.getCode());
            if (Objects.isNull(serverPlayer)) {
                continue;
            }
            final String playerId = serverPlayer.getId();

            // 检查有无 @PlayerId
            final String atPlayerId = '@' + playerId;
            StringUtils.replaceAll(stringBuilder, atPlayerId, serializedAtMessage);

            hasAt = message.contains("@");
            if (!hasAt) {
                break;
            }
        }
        return stringBuilder.toString();
    }

    /** 请求 ID，只能在创建新的请求时修改 */
    protected final AtomicInteger requestId = new AtomicInteger(0);
    protected int allocateRequestId() {
        return requestId.incrementAndGet();
    }

    public Set<PlayerContent> listOnlinePlayers() throws IOException {
        controller.sendLater(PackType.XM_ONLINE_PLAYER);
        return controller.nextPack(PackType.MC_ONLINE_PLAYER_RESULT).getContent(OnlinePlayerContent.class).getPlayerContents();
    }

    public void logInfo(String message) {
        log.info("[" + detail.getName() + "] " + message);
    }

    public boolean isOnline(String playerId) throws IOException {
        logInfo("请求检查玩家是否在线：" + playerId);
        final int requestId = allocateRequestId();
        controller.sendLater(PackType.XM_CHECK_ONLINE, new IdContent(requestId, playerId));
        return controller.nextContent(PackType.MC_CHECK_ONLINE_RESULT, requestId).getContent(Boolean.class);
    }

    public ResultContent sendTitle(String playerId, String title, String subtitle) throws IOException {
        return sendTitle(com.chuanwise.xiaoming.api.util.CollectionUtils.asSet(playerId), title, subtitle, 10, 100, 20);
    }

    public ResultContent sendTitle(Set<String> playerIds, String title, String subtitle) throws IOException {
        return sendTitle(playerIds, title, subtitle, 10, 100, 20);
    }

    public ResultContent sendTitle(String playerId, String title, String subtitle, int fadeIn, int delay, int fadeOut) throws IOException {
        return sendTitle(com.chuanwise.xiaoming.api.util.CollectionUtils.asSet(playerId), title, subtitle, fadeIn, delay, fadeOut);
    }

    public ResultContent sendTitle(Set<String> playerIds, String title, String subtitle, int fadeIn, int delay, int fadeOut) throws IOException {
        logInfo("向玩家：" + playerIds + " 显示主标题：" + title + "，副标题：" + subtitle);
        final int requestId = allocateRequestId();
        PackUtils.sendResult(controller, requestId, PackType.XM_SEND_TITLE, new SendTitleContent(playerIds, title, subtitle, fadeIn, delay, fadeOut));
        return controller.nextResult(PackType.MC_SEND_TITLE_RESULT, requestId);
    }

    public ResultContent sendTitleToAllPlayers(String title, String subtitle, int fadeIn, int delay, int fadeOut) throws IOException {
        logInfo("向所有玩家显示主标题：" + title + "，副标题：" + subtitle);
        final int requestId = allocateRequestId();
        PackUtils.sendResult(controller, requestId, PackType.XM_SEND_TITLE_TO_ALL_PLAYERS, new SendTitleToAllPlayersContent(title, subtitle, fadeIn, delay, fadeOut));
        return controller.nextResult(PackType.MC_SEND_TITLE_RESULT, requestId);
    }

    public ResultContent sendTitleToAllPlayers(String title, String subtitle) throws IOException {
        logInfo("向所有玩家显示主标题：" + title + "，副标题：" + subtitle);
        return sendTitleToAllPlayers(title, subtitle, 10, 100, 20);
    }

    public ResultContent sendMessage(String playerId, String message) throws IOException {
        return sendMessageWithoutMessageHead(com.chuanwise.xiaoming.api.util.CollectionUtils.asSet(playerId), Formatter.headThen(message));
    }

    public ResultContent sendMessage(Set<String> playerIds, String message) throws IOException {
        return sendMessageWithoutMessageHead(playerIds, Formatter.headThen(message));
    }

    public ResultContent sendMessageWithoutMessageHead(String playerId, String message) throws IOException {
        return sendMessageWithoutMessageHead(com.chuanwise.xiaoming.api.util.CollectionUtils.asSet(playerId), message);
    }

    public ResultContent sendMessageWithoutMessageHead(Set<String> playerIds, String message) throws IOException {
        logInfo("向玩家：" + playerIds + " 发送消息：" + message);
        final int requestId = allocateRequestId();
        controller.sendLater(PackType.XM_SEND_MESSAGE, new IdContent(requestId, new ShowMessageContent(playerIds, message)));
        return controller.nextResult(PackType.MC_SEND_MESSAGE_RESULT, requestId);
    }

    public ResultContent sendMessageToAllPlayersWithoutMessageHead(String message) throws IOException {
        logInfo("向所有玩家发送消息：" + message);
        final int requestId = allocateRequestId();
        controller.sendLater(PackType.XM_SEND_MESSAGE_TO_ALL_PLAYERS, new IdContent(requestId, message));
        return controller.nextResult(PackType.MC_SEND_MESSAGE_RESULT, requestId);
    }

    public ResultContent sendMessageToAllPlayers(String message) throws IOException {
        return sendMessageToAllPlayersWithoutMessageHead(Formatter.headThen(message));
    }

    public boolean hasWorld(String worldName) throws IOException {
        logInfo("判断是否存在世界：" + worldName);
        final int requestId = allocateRequestId();
        controller.sendLater(PackType.XM_HAS_WORLD, new IdContent(requestId, worldName));
        return controller.nextContent(PackType.MC_HAS_WORLD_RESULT, requestId).getContent(Boolean.class);
    }

    public boolean hasPermission(String playerId, String permission) throws IOException {
        logInfo("检查玩家 " + playerId + " 是否具有权限：" + permission);
        final int requestId = allocateRequestId();
        controller.sendLater(PackType.XM_HAS_PERMISSION, new IdContent(requestId, new HasPermissionContent(playerId, permission)));
        return controller.nextContent(PackType.MC_HAS_PERMISSION_RESULT, requestId).getContent(Boolean.class);
    }

    public void sendError(String message) throws IOException {
        controller.send(PackType.XM_ERROR, message);
    }

    public Set<String> listWorldsNames() throws IOException {
        logInfo("正在获取世界名");
        controller.sendLater(PackType.XM_WORLD_NAME);
        return controller.nextPack(PackType.MC_WORLD_NAME_RESULT).getContent(Set.class);
    }

    public ResultContent executeAsConsole(String command) throws IOException {
        return executeAsPlayer(null, command);
    }

    public ResultContent executeAsPlayer(String playerId, String command) throws IOException {
        if (Objects.nonNull(playerId)) {
            logInfo("正在申请以玩家：" + playerId + " 身份执行：" + command);
        } else {
            logInfo("正在申请以控制台身份执行：" + command);
        }
        final int requestId = allocateRequestId();
        final IdContent content = new IdContent(requestId, new ServerCommandContent(playerId, command));

        controller.sendLater(PackType.XM_COMMAND, content);
        return controller.nextResult(PackType.MC_COMMAND_RESULT, requestId);
    }

    public IdContent onGetBindedQQ(int requestId, String playerId) throws IOException {
        logInfo("正在获取玩家 " + playerId + " 绑定的 QQ");
        final ServerPlayer serverPlayer = playerData.forPlayer(playerId);
        return PackUtils.sendResult(controller, requestId, PackType.XM_GET_BINDED_QQ_RESULT,
                Objects.nonNull(serverPlayer) ? serverPlayer.getCode() : null);
    }

    public IdContent onGetAliasOrCode(int requestId, String playerId) throws IOException {
        logInfo("正在获取玩家 " + playerId + " 绑定的 QQ 的备注");
        final ServerPlayer serverPlayer = playerData.forPlayer(playerId);
        return PackUtils.sendResult(controller, requestId, PackType.XM_GET_ALIAS_OR_CODE_RESULT,
                Objects.nonNull(serverPlayer) ? getXiaomingBot().getAccountManager().getAliasOrCode(serverPlayer.getCode()) : null);
    }

    public ResultContent onExecuteInGroup(int requestId, long group, String playerId, String command) throws IOException {
        logInfo("正在以玩家 " + playerId + " 绑定的 QQ 的身份在群 " + group + " 中执行小明指令：" + command);
        final ServerPlayer serverPlayer = playerData.forPlayer(playerId);
        if (Objects.isNull(serverPlayer)) {
            return PackUtils.sendResult(controller, requestId, PackType.XM_COMMAND_RESULT, false, "你还没有绑定 QQ，不能执行小明指令。" +
                    "赶快用 " + Formatter.yellow("/xm bind <你的QQ>") + " 绑定一下吧！");
        }

        final Receptionist receptionist = getXiaomingBot().getReceptionistManager().getOrPutReceptionist(serverPlayer.getCode());
        final GroupContact groupContact = getXiaomingBot().getContactManager().getGroupContact(group);

        if (Objects.isNull(groupContact)) {
            return PackUtils.sendResult(controller, requestId, PackType.XM_COMMAND_RESULT, false, "小明还不在这个群里呢");
        }
        final MemberContact memberContact = groupContact.getMember(serverPlayer.getCode());
        if (Objects.isNull(memberContact)) {
            return PackUtils.sendResult(controller, requestId, PackType.XM_COMMAND_RESULT, false, "你还不在这个群里，不能在这里执行小明指令");
        }

        final GroupXiaomingUser groupXiaomingUser = receptionist.getOrPutGroupXiaomingUser(groupContact, memberContact);
        final long latestTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);

        final ResultContent resultContent = new ResultContent();
        groupXiaomingUser.enablePrintWriter();

        final ScheduableTask<Boolean> receptionistTask = getXiaomingBot().getScheduler().run(() -> {
            return getXiaomingBot().getInteractorManager().onInput(groupXiaomingUser, new GroupMessageImpl(groupXiaomingUser, new PlainText(command).plus("")));
        });
        receptionistTask.setDescription("服务器指令接待线程");

        Boolean result = null;
        try {
            result = receptionistTask.get(configuration.getExecuteXiaomingCommandTimeout());
        } catch (InterruptedException exception) {
            resultContent.setObject("指令执行过程被打断");
            receptionistTask.cancel();
        }
        resultContent.setSuccess(Objects.nonNull(result) && result);
        resultContent.setObject(MiraiCode.deserializeMiraiCode(groupXiaomingUser.getBufferAndClose()).contentToString());

        return PackUtils.sendResult(controller, requestId, PackType.XM_COMMAND_RESULT, resultContent);
    }

    public ResultContent onExecuteInTemp(int requestId, long group, String playerId, String command) throws IOException {
        logInfo("正在以玩家 " + playerId + " 绑定的 QQ 的身份在群 " + group + " 的临时会话中执行小明指令：" + command);
        final ServerPlayer serverPlayer = playerData.forPlayer(playerId);
        if (Objects.isNull(serverPlayer)) {
            return PackUtils.sendResult(controller, requestId, PackType.XM_COMMAND_RESULT, false, "你还没有绑定 QQ，不能执行小明指令。" +
                    "赶快用 " + Formatter.yellow("/xm bind <你的QQ>") + " 绑定一下吧！");
        }

        final Receptionist receptionist = getXiaomingBot().getReceptionistManager().getOrPutReceptionist(serverPlayer.getCode());
        final GroupContact groupContact = getXiaomingBot().getContactManager().getGroupContact(group);

        if (Objects.isNull(groupContact)) {
            return PackUtils.sendResult(controller, requestId, PackType.XM_COMMAND_RESULT, false, "小明还不在这个群里呢");
        }
        final MemberContact memberContact = groupContact.getMember(serverPlayer.getCode());
        if (Objects.isNull(memberContact)) {
            return PackUtils.sendResult(controller, requestId, PackType.XM_COMMAND_RESULT, false, "你还不在这个群里，不能在这里执行小明指令");
        }

        final MemberXiaomingUser memberXiaomingUser = receptionist.getOrPutMemberXiaomingUser(memberContact);
        final ResultContent resultContent = new ResultContent();
        memberXiaomingUser.enablePrintWriter();

        final ScheduableTask<Boolean> receptionistTask = getXiaomingBot().getScheduler().run(() -> {
            return getXiaomingBot().getInteractorManager().onInput(memberXiaomingUser, new MemberMessageImpl(memberXiaomingUser, new PlainText(command).plus("")));
        });
        receptionistTask.setDescription("服务器指令接待线程");

        Boolean result = null;
        try {
            result = receptionistTask.get(configuration.getExecuteXiaomingCommandTimeout());
        } catch (InterruptedException exception) {
            resultContent.setObject("指令执行过程被打断");
            receptionistTask.cancel();
        }
        resultContent.setSuccess(Objects.nonNull(result) && result);
        resultContent.setObject(MiraiCode.deserializeMiraiCode(memberXiaomingUser.getBufferAndClose()).contentToString());

        return PackUtils.sendResult(controller, requestId, PackType.XM_COMMAND_RESULT, resultContent);
    }

    public ResultContent onExecuteInPrivate(int requestId, String playerId, String command) throws IOException {
        logInfo("正在以玩家 " + playerId + " 绑定的 QQ 的身份在私聊中执行小明指令：" + command);
        final ServerPlayer serverPlayer = playerData.forPlayer(playerId);
        if (Objects.isNull(serverPlayer)) {
            return PackUtils.sendResult(controller, requestId, PackType.XM_COMMAND_RESULT, false, "你还没有绑定 QQ，不能执行小明指令。" +
                    "赶快用 " + Formatter.yellow("/xm bind <你的QQ>") + " 绑定一下吧！");
        }

        final PrivateContact privateContact = getXiaomingBot().getContactManager().getPrivateContact(serverPlayer.getCode());
        final Receptionist receptionist = getXiaomingBot().getReceptionistManager().getOrPutReceptionist(serverPlayer.getCode());

        final PrivateXiaomingUser privateXiaomingUser = receptionist.getOrPutPrivateXiaomingUser(privateContact);
        final ResultContent resultContent = new ResultContent();
        privateXiaomingUser.enablePrintWriter();

        final ScheduableTask<Boolean> receptionistTask = getXiaomingBot().getScheduler().run(() -> {
            return getXiaomingBot().getInteractorManager().onInput(privateXiaomingUser, new PrivateMessageImpl(privateXiaomingUser, new PlainText(command).plus("")));
        });
        receptionistTask.setDescription("服务器指令接待线程");

        Boolean result = null;
        try {
            result = receptionistTask.get(configuration.getExecuteXiaomingCommandTimeout());
        } catch (InterruptedException exception) {
            resultContent.setObject("指令执行过程被打断");
            receptionistTask.cancel();
        }
        resultContent.setSuccess(Objects.nonNull(result) && result);
        resultContent.setObject(MiraiCode.deserializeMiraiCode(privateXiaomingUser.getBufferAndClose()).contentToString());

        return PackUtils.sendResult(controller, requestId, PackType.XM_COMMAND_RESULT, resultContent);
    }

    public ResultContent playerConfirm(String playerId, String what, long timeout) throws IOException {
        logInfo("正在请求玩家：" + playerId + " 在" + TimeUtils.toTimeString(timeout) + "之内确认：" + what);
        final int requestId = allocateRequestId();
        final IdContent content = new IdContent(requestId, new PlayerConfirmOrAcceptContent(playerId, what, timeout));
        controller.sendLater(PackType.XM_PLAYER_CONFIRM, content);
        return controller.nextResult(PackType.MC_PLAYER_CONFIRM_RESULT, requestId);
    }

    public ResultContent playerAccept(String playerId, String what, long timeout) throws IOException {
        logInfo("正在请求玩家：" + playerId + " 在" + TimeUtils.toTimeString(timeout) + "之内审批：" + what);
        final int requestId = allocateRequestId();
        final IdContent content = new IdContent(requestId, new PlayerConfirmOrAcceptContent(playerId, what, timeout));
        controller.sendLater(PackType.XM_PLAYER_ACCEPT, content);
        return controller.nextResult(PackType.MC_PLAYER_ACCET_RESULT, requestId);
    }

    public ResultContent playerConfirm(String playerId, String what) throws IOException {
        return playerConfirm(playerId, what, server.getConfiguration().getConfirmTimeout());
    }

    public ResultContent playerAccept(String playerId, String what) throws IOException {
        return playerAccept(playerId, what, server.getConfiguration().getConfirmTimeout());
    }

    public ResultContent sendWorldMessage(String worldName, String message) throws IOException {
        return sendWorldMessage(com.chuanwise.xiaoming.api.util.CollectionUtils.asSet(worldName), message);
    }

    public ResultContent sendWorldMessage(Set<String> worldNames, String message) throws IOException {
        return sendWorldMessageWithoutMessageHead(worldNames, Formatter.headThen(message));
    }

    public ResultContent sendWorldMessageWithoutMessageHead(Set<String> worldNames, String message) throws IOException {
        logInfo("正在向世界：" + worldNames + " 内的玩家显示消息：" + message);
        final int requestId = allocateRequestId();
        final IdContent content = new IdContent(requestId, new WorldMessageContent(worldNames, message));
        controller.sendLater(PackType.XM_SEND_WORLD_MESSAGE, content);
        return controller.nextResult(PackType.MC_SEND_WORLD_MESSAGE_RESULT, requestId);
    }

    public ResultContent sendWorldMessageWithoutMessageHead(String worldName, String message) throws IOException {
        final Set<String> worldNames = new HashSet<>(1);
        worldNames.add(worldName);
        return sendWorldMessageWithoutMessageHead(worldNames, message);
    }

    public void broadcastMessage(String message) {
        broadcastMessageWithoutMessageHead(Formatter.headThen(Formatter.green(message)));
    }

    public void broadcastMessageWithoutMessageHead(String message) {
        logInfo("正在发送全服消息：" + message);
        controller.sendLater(PackType.XM_BROADCAST_MESSAGE, message);
    }

    public ResultContent onBind(int requestId, String playerId, long code) throws IOException {
        logInfo("正在受理 " + playerId + " => " + code + " 的绑定请求");

        // 找到绑定目标之前的目标
        final ServerPlayer sameQQPlayer = playerData.forPlayer(code);
        final ServerPlayer sameIdPlayer = playerData.forPlayer(playerId);

        final boolean qqBinded = Objects.nonNull(sameQQPlayer);
        final boolean idBinded = Objects.nonNull(sameIdPlayer);

        boolean success = false;
        String message = null;

        if (idBinded) {
            if (code == sameIdPlayer.getCode()) {
                message = "你已经绑定到这个 QQ 上了哦";
            } else {
                message = "你已经绑定到 " + Formatter.yellow(String.valueOf(sameIdPlayer.getCode())) + " 这个 QQ 上了哦";
            }
        } else {
            if (qqBinded) {
                if (Objects.equals(sameQQPlayer.getId(), playerId)) {
                    message = "你已经绑定到这个 QQ 上了哦";
                } else {
                    message = "这个 QQ 已经被绑定了";
                }
            } else {
                final PrivateContact privateContact = xiaomingBot.getContactManager().getPrivateContact(code);
                if (Objects.isNull(privateContact)) {
                    message = "你还没有添加小明为好友，添加之后再绑定吧";
                } else {
                    try {
                        sendMessage(playerId, "请注意 QQ 私聊，小明需要你的确认");
                        privateContact.send("你的 Minecraft 服务器 ID 是「" + playerId + "」吗？" +
                                "该玩家现在希望绑定你的 QQ。如果允许，请在一分钟之内告诉我「绑定」。若超时或回复其他消息，小明会拒绝该请求。");

                        final PrivateMessage privateMessage = privateContact.nextMessage(TimeUnit.MINUTES.toMillis(1));
                        if (Objects.equals(privateMessage.serialize(), "绑定")) {
                            success = true;
                            privateContact.send("成功绑定到 ID「" + playerId + "」");

                            playerData.bind(code, playerId);
                            xiaomingBot.getScheduler().readySave(playerData);
                        } else {
                            privateContact.send("已拒绝该绑定请求");
                            message = Formatter.red("该 QQ 用户拒绝了绑定请求");
                        }
                    } catch (InteractorTimeoutException exception) {
                        success = false;
                        message = Formatter.yellow("该用户没有及时在 QQ 上处理绑定请求");
                    }
                }
            }
        }
        return PackUtils.sendResult(controller, requestId, PackType.XM_BIND_RESULT, success, message);
    }

    public void onLog(String message) {
        logInfo(message);
        plugin.sendMessageToLog("「" + detail.getName() + "」的日志：" + message);
    }

    public ResultContent onPlayerChat(int requestId, PlayerContent player, String message, boolean echo) {
        logInfo("正在处理 " + player + " 的聊天信息：" + message + "，回响：" + echo);
        final ServerPlayer serverPlayer = playerData.forPlayer(player.getPlayerId());

        // 构造共有变量表
        Map<String, Object> commonEnvironment = new HashMap<>();
        commonEnvironment.put("server.name", detail.getName());
        commonEnvironment.put("sender.id", player.getPlayerId());
        commonEnvironment.put("sender.listName", player.getPlayerListName());
        commonEnvironment.put("sender.customName", player.getCustomName());
        commonEnvironment.put("sender.worldName", player.getWorldName());
        commonEnvironment.put("sender.displayName", player.getDisplayName());
        commonEnvironment.put("time", com.chuanwise.xiaoming.minecraft.util.TimeUtils.FORMAT.format(System.currentTimeMillis()));
        if (Objects.nonNull(serverPlayer)) {
            final long senderCode = serverPlayer.getCode();
            commonEnvironment.put("sender.code", senderCode);
            commonEnvironment.put("sender.alias", plugin.getXiaomingBot().getAccountManager().getAliasOrCode(senderCode));
        }

        final List<GroupChannel> successGroupChannels = new ArrayList<>();
        final List<GroupChannel> failChannels = new ArrayList<>();

        // 已经发送过的群集合
        // 避免在一个群内重复发送东西，所以维护一个发送过的群的表
        final Set<ResponseGroup> sentGroups = new HashSet<>();
        for (GroupChannel channel : chatSettings.getGroupChannels()) {
            final String head = channel.getHead();

            if (message.startsWith(head) && message.length() > head.length()) {
                if (Objects.isNull(serverPlayer)) {
                    return PackUtils.sendResult(controller, requestId, PackType.XM_PLAYER_CHAT_RESULT, false, "你的消息本应被被发送到「" + channel.getName() + "」等频道，" +
                            "但你还没有绑定 QQ，不能发送消息。" +
                            "赶快用 " + Formatter.yellow("/xm bind <你的QQ>") + " 来绑定吧");
                }

                final String messageWithoutHead = message.substring(head.length());

                // 构造频道变量表
                Map<String, Object> channelEnvironment = new HashMap<>();
                channelEnvironment.put("channel.name", channel.getName());
                channelEnvironment.put("channel.head", channel.getHead());
                channelEnvironment.put("channel.groupTag", channel.getGroupTag());
                commonEnvironment.put("message", messageWithoutHead);

                // 换变量，去掉颜色字符等东西
                final String commonEnvoronmentReplacedMessage = ArgumentUtils.replaceArguments(channel.getFormat(), commonEnvironment, maxIterateTime);
                final String channelEnvoronmentReplacedMessage = ArgumentUtils.replaceArguments(commonEnvoronmentReplacedMessage, channelEnvironment, maxIterateTime);

                boolean currentChannelSuccess = true;
                try {
                    // 在每个响应群内发消息
                    AtomicBoolean echoed = new AtomicBoolean(false);
                    final Set<ResponseGroup> responseGroups = xiaomingBot.getResponseGroupManager().forTag(channel.getGroupTag());
                    for (ResponseGroup group : responseGroups) {
                        // 避免往一个群内发送太多次相同的信息
                        if (sentGroups.contains(group)) {
                            continue;
                        } else {
                            sentGroups.add(group);
                        }

                        // 打开群会话。如果没有这个群就失败
                        final GroupContact groupContact = group.getContact();
                        if (Objects.isNull(groupContact)) {
                            currentChannelSuccess = false;
                            break;
                        }
                        final MemberContact senderMember = groupContact.getMember(serverPlayer.getCode());
                        if (Objects.isNull(senderMember)) {
                            currentChannelSuccess = false;
                            break;
                        }

                        // 构造群内的变量环境
                        final Map<String, Object> groupEnvironment = new HashMap<>();
                        groupEnvironment.put("group.code", groupContact.getCode());
                        groupEnvironment.put("group.name", groupContact.getName());
                        groupEnvironment.put("group.alias", groupContact.getAlias());

                        final String groupEnvironmentReplacedMessage = ArgumentUtils.replaceArguments(channelEnvoronmentReplacedMessage, groupEnvironment, maxIterateTime);
                        final String finalMessage = Formatter.clearColors(groupEnvironmentReplacedMessage);

                        groupContact.send(replacePlayerIdToAt(finalMessage, groupContact));

                        // 启动回响操作
                        if (echo && !echoed.get()) {
                            getXiaomingBot().getScheduler().run(() -> {
                                final MinecraftChannel echoChannel = chatSettings.forMinecraftChannel(channel.getName());
                                if (Objects.nonNull(echoChannel)) {
                                    logInfo("找到同名服务器频道：" + echoChannel.getName() + "，启动回响");
                                    final GroupXiaomingUser groupXiaomingUser = getXiaomingBot().getReceptionistManager().getReceptionist(serverPlayer.getCode()).getOrPutGroupXiaomingUser(groupContact, senderMember);
                                    final GroupMessageImpl groupMessage = new GroupMessageImpl(groupXiaomingUser,
                                            new PlainText(echoChannel.getHead()).plus(message));
                                    plugin.getServerMessageInteractor().onGroupChat(groupXiaomingUser, groupMessage);
                                    echoed.set(true);
                                }
                            });
                        }
                    }

                    if (currentChannelSuccess) {
                        successGroupChannels.add(channel);
                    } else {
                        failChannels.add(channel);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    failChannels.add(channel);
                }
            }
        }

        if (successGroupChannels.isEmpty() && failChannels.isEmpty()) {
            return PackUtils.sendResult(controller, requestId, PackType.XM_PLAYER_CHAT_RESULT, false, null);
        } else {
            String successMessage = "";
            String failMessage = "";
            if (!successGroupChannels.isEmpty()) {
                successMessage = "消息被成功发送至频道：" + CollectionUtils.getSummary(successGroupChannels, GroupChannel::getName, "", "", "、");
            }
            if (!failChannels.isEmpty()) {
                failMessage = "消息没有在这些频道成功发送：" + CollectionUtils.getSummary(failChannels, GroupChannel::getName, "", "", "、");
            }

            return PackUtils.sendResult(controller, requestId, PackType.XM_PLAYER_CHAT_RESULT, !failChannels.isEmpty(),
                    successMessage + (!StringUtils.isEmpty(successMessage) && !StringUtils.isEmpty(failMessage) ? "、" : "") + failMessage);
        }
    }

    public ResultContent onUnbind(int requestId, String playerId) {
        logInfo("正在受理 " + playerId + " 解绑请求");
        final ServerPlayer serverPlayer = playerData.forPlayer(playerId);

        final boolean success;
        final String message;
        if (Objects.nonNull(serverPlayer)) {
            success = true;
            playerData.unbind(serverPlayer.getCode());
            xiaomingBot.getScheduler().readySave(playerData);

            message = "成功和 QQ「" + serverPlayer.getCode() + "」解绑";
        } else {
            success = false;
            message = "你还没有绑定 QQ，不能解绑哦";
        }

        return PackUtils.sendResult(controller, requestId, PackType.XM_UNBIND_RESULT, success, message);
    }

    public ResultContent onApply(int requestId, PlayerContent player, String command) {
        logInfo("正在处理指令申请请求：" + player + "，指令：" + command);
        final long applyCommandTimeout = configuration.getApplyCommandTimeout();

        plugin.sendMessageToLog("「" + detail.getName() + "」中的玩家「" + player.getPlayerId() + "」" +
                "申请在世界「" + player.getWorldName() + "」执行指令「" + command + "」。" +
                "如果批准该请求，请在" + TimeUtils.toTimeString(applyCommandTimeout) + "之内在本群发送「批准」，" +
                "超时或其他回答将会令小明拒绝该申请");

        boolean success = false;
        String message = null;

        try {
            final GroupMessage groupMessage = xiaomingBot.getContactManager().nextGroupMessage(configuration.getLogGroupTag(), applyCommandTimeout);
            success = Objects.equals(groupMessage.serialize(), "批准");
            if (success) {
                groupMessage.getSender().sendMessage("成功同意该用户的申请");
                message = "管理员批准了你的请求";
            } else {
                groupMessage.getSender().sendMessage("已拒绝该用户的申请");
                message = "管理员拒绝了你的请求";
            }
        } catch (InteractorTimeoutException exception) {
            message = "管理员没有处理你的请求";
        }

        return PackUtils.sendResult(controller, requestId, PackType.XM_APPLY_COMMAND_RESULT, success, message);
    }

    public ResultContent onAddServerTag(int requestId, String tag) {
        logInfo("正在处理增加服务器 tag：" + tag + " 请求");
        if (detail.hasTag(tag)) {
            return PackUtils.sendResult(controller, requestId, PackType.XM_ADD_SERVER_TAG_RESULT, false, "服务器已经具备标记「" + tag + "」了");
        } else {
            detail.addTag(tag);
            getXiaomingBot().getScheduler().readySave(configuration);
            return PackUtils.sendResult(controller, requestId, PackType.XM_ADD_SERVER_TAG_RESULT, true, "成功为服务器增加了标记「" + tag + "」");
        }
    }

    public ResultContent onRemoveServerTag(int requestId, String tag) {
        logInfo("正在处理删除服务器 tag：" + tag + " 请求");
        if (Arrays.asList(detail.getName(), "recorded").contains(tag)) {
            return PackUtils.sendResult(controller, requestId, PackType.XM_REMOVE_SERVER_TAG_RESULT, false, "「" + tag + "」是原生标记，不可以删除");
        }
        if (detail.hasTag(tag)) {
            detail.removeTag(tag);
            getXiaomingBot().getScheduler().readySave(configuration);
            return PackUtils.sendResult(controller, requestId, PackType.XM_REMOVE_SERVER_TAG_RESULT, true, "成功删除了本服的标记「" + tag + "」");
        } else {
            return PackUtils.sendResult(controller, requestId, PackType.XM_REMOVE_SERVER_TAG_RESULT, false, "服务器没有标记「" + tag + "」");
        }
    }

    public Set<String> onListServerTags() {
        logInfo("正在处理获取服务器 tag 请求");
        final Set<String> tags = detail.getTags();
        controller.sendLater(PackType.XM_LIST_SERVER_TAG_RESULT, tags);
        return tags;
    }

    public void onFlushWorlds() throws IOException {
        logInfo("正在刷新世界缓存");
        final Set<String> worldsNames = listWorldsNames();
        final Map<String, Set<String>> elderWorldTags = detail.getWorldTags();
        final Map<String, Set<String>> worldTags = new HashMap<>(worldsNames.size());
        detail.setWorldTags(worldTags);

        // 将之前的记录项复制过来
        worldsNames.forEach(world -> {
            Set<String> tags = elderWorldTags.get(world);
            if (CollectionUtils.isEmpty(tags)) {
                tags = com.chuanwise.xiaoming.api.util.CollectionUtils.asSet(world, "recorded");
            }
            worldTags.put(world, tags);
        });
    }

    public void onFlush() throws IOException {
        logInfo("正在处理刷新所有数据请求");
        onFlushWorlds();
    }

    protected void registerListeners() throws IOException {
        final XiaomingBot xiaomingBot = plugin.getXiaomingBot();
        final int maxIterateTime = xiaomingBot.getConfiguration().getMaxIterateTime();

        // 拉取最新信息
        onFlush();

        // 向日志群发送日志
        controller.register(PackType.MC_LOGGER, pack -> {
            onLog(pack.getContent(String.class));
        });

        // MC 玩家聊天
        controller.register(PackType.MC_PLAYER_CHAT, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final PlayerChatContent content = idContent.getContent(PlayerChatContent.class);
            onPlayerChat(idContent.getRequestId(), content.getPlayer(), content.getMessage(), content.isEcho());
        });

        // MC 绑定 QQ 请求
        controller.register(PackType.MC_BIND, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final BindContent content = idContent.getContent(BindContent.class);
            try {
                onBind(idContent.getRequestId(), content.getPlayerId(), content.getTarget());
            } catch (IOException exception) {
                onThrowable(exception);
            }
        });

        // MC 解绑请求
        controller.register(PackType.MC_UNBIND, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            onUnbind(idContent.getRequestId(), idContent.getContent(String.class));
        });

        // MC 指令申请请求
        controller.register(PackType.MC_APPLY_COMMAND, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final ApplyCommandContent content = idContent.getContent(ApplyCommandContent.class);
            onApply(idContent.getRequestId(), content.getPlayer(), content.getCommand());
        });

        controller.register(PackType.MC_ADD_SERVER_TAG, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final String tag = idContent.getContent(String.class);
            onAddServerTag(idContent.getRequestId(), tag);
        });

        controller.register(PackType.MC_REMOVE_SERVER_TAG, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final String tag = idContent.getContent(String.class);
            onRemoveServerTag(idContent.getRequestId(), tag);
        });

        controller.register(PackType.MC_LIST_SERVER_TAG, pack -> {
            onListServerTags();
        });

        controller.register(PackType.MC_LIST_WORLD_TAG, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final String worldName = idContent.getContent(String.class);
            onListWorldTags(idContent.getRequestId(), worldName);
        });

        controller.register(PackType.MC_ADD_WORLD_TAG, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final WorldTagContent content = idContent.getContent(WorldTagContent.class);
            onAddWorldTags(idContent.getRequestId(), content.getWorldName(), content.getTag());
        });

        controller.register(PackType.MC_REMOVE_WORLD_TAG, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final WorldTagContent content = idContent.getContent(WorldTagContent.class);
            onRemoveWorldTags(idContent.getRequestId(), content.getWorldName(), content.getTag());
        });

        controller.register(PackType.MC_LIST_WORLD, pack -> {
            onListWorldNames();
        });

        controller.register(PackType.MC_COMMAND, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final XiaomingCommandContent content = idContent.getContent(XiaomingCommandContent.class);
            try {
                switch (content.getPosition()) {
                    case TEMP:
                        onExecuteInTemp(idContent.getRequestId(), content.getCode(), content.getPlayerId(), content.getCommand());
                        break;
                    case GROUP:
                        onExecuteInGroup(idContent.getRequestId(), content.getCode(), content.getPlayerId(), content.getCommand());
                        break;
                    case PRIVATE:
                        onExecuteInPrivate(idContent.getRequestId(), content.getPlayerId(), content.getCommand());
                        break;
                    default:
                        sendError("错误的小明通讯协议，请检查小明插件和 Bukkit 插件版本是否匹配");
                }
            } catch (IOException exception) {
                onThrowable(exception);
            }
        });

        controller.register(PackType.MC_FLUSH, pack -> {
            try {
                onFlush();
            } catch (IOException exception) {
                onThrowable(exception);
            }
        });

        controller.register(PackType.MC_GET_ALIAS_OR_CODE, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            try {
                onGetAliasOrCode(idContent.getRequestId(), idContent.getContent(String.class));
            } catch (IOException exception) {
                onThrowable(exception);
            }
        });


        controller.register(PackType.MC_GET_BINDED_QQ, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            try {
                onGetBindedQQ(idContent.getRequestId(), idContent.getContent(String.class));
            } catch (IOException exception) {
                onThrowable(exception);
            }
        });

        controller.register(PackType.MC_FLUSH_WORLD, pack -> {
            try {
                onFlushWorlds();
            } catch (IOException exception) {
                onThrowable(exception);
            }
        });
    }

    public Set<String> onListWorldNames() {
        logInfo("正在处理拉取世界缓存名请求");
        final Set<String> content = detail.getWorldTags().keySet();
        controller.sendLater(PackType.XM_LIST_WORLD_RESULT, content);
        return content;
    }

    public Set<String> onListWorldTags(int requestId, String worldName) {
        logInfo("正在处理拉取世界 tag 请求");
        final Set<String> tags = detail.forWorldTags(worldName);
        PackUtils.sendResult(controller, requestId, PackType.XM_LIST_WORLD_TAG_RESULT, tags);
        return tags;
    }

    public ResultContent onRemoveWorldTags(int requestId, String worldName, String tag) {
        logInfo("正在处理删除世界 " + worldName + " 的 tag：" + tag + " 请求");
        if (Arrays.asList(worldName, "recorded").contains(tag)) {
            return PackUtils.sendResult(controller, requestId, PackType.XM_REMOVE_WORLD_TAG_RESULT, false, "「" + tag + "」是原生标记，不可以删除");
        }
        if (detail.worldHasTag(worldName, tag)) {
            detail.removeWorldTag(worldName, tag);
            getXiaomingBot().getScheduler().readySave(configuration);
            return PackUtils.sendResult(controller, requestId, PackType.XM_REMOVE_WORLD_TAG_RESULT, true, "成功删除世界「" + worldName + "」的标记「" + tag + "」");
        } else {
            return PackUtils.sendResult(controller, requestId, PackType.XM_REMOVE_WORLD_TAG_RESULT, false, "世界「" + worldName + "」并没有标记「" + tag + "」");
        }
    }

    public ResultContent onAddWorldTags(int requestId, String worldName, String tag) {
        logInfo("正在处理增加世界 " + worldName + " 的 tag：" + tag + " 请求");
        if (detail.worldHasTag(worldName, tag)) {
            return PackUtils.sendResult(controller, requestId, PackType.XM_ADD_WORLD_TAG_RESULT, false, "世界「" + worldName + "」已经有标记「" + tag + "」了");
        } else {
            detail.addWorldTag(worldName, tag);
            getXiaomingBot().getScheduler().readySave(configuration);
            return PackUtils.sendResult(controller, requestId, PackType.XM_ADD_WORLD_TAG_RESULT, true, "成功为世界「" + worldName + "」增加了标记「" + tag + "」");
        }
    }

    @Override
    public void stop() {
        final List<BukkitPluginReceptionist> receptionists = server.getReceptionists();
        synchronized (receptionists) {
            receptionists.remove(this);
        }
        controller.stop();
        if (!threadPool.isShutdown()) {
            threadPool.shutdown();
        }
    }

    public void onThrowable(IOException exception) {
        controller.stopCausedBy(exception);
        stop();
    }
}
