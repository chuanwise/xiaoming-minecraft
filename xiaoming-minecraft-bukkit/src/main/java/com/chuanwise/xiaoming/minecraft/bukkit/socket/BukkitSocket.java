package com.chuanwise.xiaoming.minecraft.bukkit.socket;

import com.chuanwise.xiaoming.minecraft.bukkit.XiaomingBukkitPlugin;
import com.chuanwise.xiaoming.minecraft.bukkit.command.executor.XiaomingCommandExecutor;
import com.chuanwise.xiaoming.minecraft.bukkit.command.sender.XiaomingCommandSender;
import com.chuanwise.xiaoming.minecraft.bukkit.command.sender.XiaomingConsoleCommandSender;
import com.chuanwise.xiaoming.minecraft.bukkit.command.sender.XiaomingNamedCommandSender;
import com.chuanwise.xiaoming.minecraft.bukkit.command.sender.XiaomingPlayerCommandSender;
import com.chuanwise.xiaoming.minecraft.bukkit.configuration.XiaomingBukkitConfiguration;
import com.chuanwise.xiaoming.minecraft.bukkit.event.XiaomingExecuteCommandEvent;
import com.chuanwise.xiaoming.minecraft.util.*;
import com.chuanwise.xiaoming.minecraft.bukkit.util.PlayerUtils;
import com.chuanwise.xiaoming.minecraft.pack.content.*;
import com.chuanwise.xiaoming.minecraft.pack.Pack;
import com.chuanwise.xiaoming.minecraft.pack.PackType;
import com.chuanwise.xiaoming.minecraft.socket.SocketController;
import com.chuanwise.xiaoming.minecraft.util.Formatter;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Getter
public class BukkitSocket {
    final Logger logger;
    final XiaomingBukkitConfiguration configuration;
    final XiaomingBukkitPlugin plugin;

    @Setter
    String address;

    @Setter
    int port;

    SocketController controller;

    final Server server;

    public BukkitSocket(XiaomingBukkitPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configuration = plugin.getConfiguration();
        this.server = plugin.getServer();

        address = configuration.getAddress();
        port = configuration.getPort();
    }

    public void logIfDebug(String message) {
        if (configuration.isDebug()) {
            logger.info(message);
        }
    }

    public synchronized void connect() throws IOException {
        if (test()) {
            return;
        }

        logger.info("正在和小明建立连接");
        controller = new SocketController(new Socket(address, port),
                LoggerFactory.getLogger(logger.getName()), true);
        controller.setSocketConfiguration(configuration.getSocketConfiguration());
        controller.setOnNormalExit(() -> {
            if (Objects.nonNull(controller)) {
                try {
                    controller.send(PackType.XM_DISCONNECT);
                } catch (IOException exception) {
                    getLogger().severe("无法向小明发送断连消息");
                }
            }
        });
        controller.setOnFinally(() -> {
            getLogger().severe("断开了和小明的连接");
            disconnectOrReconnect();
        });

        controller.setDebug(configuration.isDebug());
        controller.setOnThrowable(throwable -> {
            if (configuration.isDebug()) {
                throwable.printStackTrace();
            }
        });

        controller.register(PackType.XM_ERROR, pack -> {
            final String message = pack.getContent(String.class);
            final String messageOrNull = Objects.nonNull(message) && !message.isEmpty() ? ("（" + message + "）") : "";

            logger.severe("小明发生错误" + messageOrNull);
            disconnect();
        });

        controller.register(PackType.XM_DISCONNECT, pack -> {
            logger.warning("小明断开了与 Minecraft 服务器的连接");
            disconnect();
        });

        controller.register(PackType.XM_REQUIRE_MC_ENCRYPTED_IDENTIFY, pack -> {
            try {
                logger.info("收到小明的身份验证请求，正在准备加密凭据");

                // 获得保存的服务器身份字符串，用 PBKDF2 加密后发送
                final String serverIdentify = configuration.getServerIdentify();
                final String encryptedServerIdentify = PasswordHashUtils.createHash(serverIdentify);

                if (Objects.equals(serverIdentify, XiaomingBukkitConfiguration.DEFAULT_SERVER_IDENTIFY)) {
                    logger.warning("当前使用的是默认的身份凭据，建议修改为自己的身份凭据");
                }
                final String xiaomingIdentify = configuration.getXiaomingIdentify();

                controller.sendLater(PackType.MC_ENCRYPTED_IDENTIFY, encryptedServerIdentify);
                logger.info("已发送本服凭据，等待小明验证");

                final Pack nextPack = controller.nextPack(Arrays.asList(PackType.XM_ACCEPT_ENCRYPTED_IDENTIFY, PackType.XM_REQUIRE_ORIGINAL_IDENTIFY));
                if (nextPack.getType() == PackType.XM_ACCEPT_ENCRYPTED_IDENTIFY) {
                    final String encryptedXiaomingIdentify = nextPack.getContent(String.class);
                    if (PasswordHashUtils.validatePassword(xiaomingIdentify, encryptedXiaomingIdentify)) {
                        controller.sendLater(PackType.MC_ACCEPT_ENCRYPTED_IDENTIFY);
                        logger.info("验证成功，成功和小明建立连接");
                        registerListeners();
                    } else {
                        controller.sendLater(PackType.MC_DENY_ENCRYPTED_IDENTIFY);
                        logger.info("小明验证通过了本服务器身份，但该小明未能提供正确的小明凭据，故被断连");
                        disconnect();
                    }
                } else if (nextPack.getType() == PackType.XM_REQUIRE_ORIGINAL_IDENTIFY) {
                    controller.sendLater(PackType.MC_IDENTIFY, serverIdentify);

                    final Pack verifyResult = controller.nextPack(Arrays.asList(PackType.XM_ACCEPT_IDENTIFY, PackType.XM_DENY_IDENTIFY));
                    if (verifyResult.getType() == PackType.XM_ACCEPT_IDENTIFY) {
                        logger.info("初次和小明建立了连接");
                        registerListeners();
                    } else {
                        final String content = verifyResult.getContent(String.class);
                        logger.info("小明没有批准本服务器的连接" + (StringUtils.isEmpty(content) ? "" : ("：" + content)));
                        disconnect();
                    }
                } else {
                    logger.severe("错误的小明通讯协议，请检查小明插件和 Bukkit 插件版本是否匹配");
                    disconnect();
                }
            } catch (Throwable throwable) {
                onThrowable(throwable);
            }
        });

        server.getScheduler().runTaskAsynchronously(plugin, controller);
    }

    public synchronized void disconnect() {
        if (!isConnected()) {
            logger.info("并未与小明建立连接，无需断开");
        } else {
            try {
                controller.send(PackType.MC_DISCONNECT);
            } catch (IOException exception) {
                logger.severe("无法向服务器发送断连消息");
            }
            controller.stop();
            controller = null;
        }
    }

    public boolean isConnected() {
        return Objects.nonNull(controller) && controller.isRunning();
    }

    public boolean test() {
        if (isConnected()) {
            return controller.test();
        } else {
            return false;
        }
    }

    public synchronized void reconnect(long delay) throws IOException, InterruptedException {
        disconnect();
        logger.info("将在" + TimeUtils.toTimeString(delay) + "后重连");
        Thread.sleep(delay);
        connect();
    }

    public Long getBindedQQ(String playerId) throws IOException {
        logIfDebug("正在获取玩家 " + playerId + " 绑定的 QQ");
        final int requestId = allocateRequestId();
        controller.sendLater(PackType.MC_GET_BINDED_QQ, new IdContent(requestId, playerId));
        return controller.nextContent(PackType.XM_GET_BINDED_QQ_RESULT, requestId).getContent(Long.class);
    }

    public String getAliasOrCode(String playerId) throws IOException {
        logIfDebug("正在获取玩家 " + playerId + " 绑定的 QQ 的备注");
        final int requestId = allocateRequestId();
        final IdContent content = new IdContent(requestId, playerId);
        return controller.nextContent(PackType.XM_GET_ALIAS_OR_CODE_RESULT, requestId).getContent(String.class);
    }

    public synchronized void disconnectOrReconnect() {
        try {
            if (test()) {
                disconnect();
            }
            if (configuration.isAutoReconnect()) {
                long reconnectDelay = configuration.getReconnectDelay();
                if (reconnectDelay < 0) {
                    logger.severe("自动重连周期错误：其必须是个大于 0 的值。已采用 0 值");
                    reconnectDelay = 0;
                }
                reconnect(reconnectDelay);
            }
        } catch (Throwable throwable) {
            onThrowable(throwable);
        }
    }

    public void onThrowable(Throwable throwable) {
        if (Objects.nonNull(controller) && !(throwable instanceof SocketTimeoutException)) {
            try {
                controller.send(PackType.MC_ERROR, throwable.toString());
            } catch (IOException exception) {
            } finally {
                controller.stopCausedBy(throwable);
            }
        }
    }

    public void sendLogger(String message) {
        controller.sendLater(PackType.MC_LOGGER, message);
    }

    public Set<PlayerContent> onOnlinePlayers() {
        logIfDebug("获取在线玩家列表");
        final Collection<? extends Player> onlinePlayers = server.getOnlinePlayers();
        final Set<PlayerContent> playerSummaries = new HashSet<>(onlinePlayers.size());
        onlinePlayers.forEach(player -> {
            playerSummaries.add(PlayerUtils.forPlayer(player));
        });
        controller.sendLater(PackType.MC_ONLINE_PLAYER_RESULT, new OnlinePlayerContent(playerSummaries));
        return playerSummaries;
    }

    public Set<String> onGetWorldNames() {
        logIfDebug("获取世界列表");
        final Set<String> worldNames = new HashSet<>();
        server.getWorlds().forEach(world -> worldNames.add(world.getName()));

        controller.sendLater(PackType.MC_WORLD_NAME_RESULT, worldNames);
        return worldNames;
    }

    public boolean onHasWorld(int requestId, String worldName) {
        logIfDebug("检查存在某世界 " + worldName);
        final boolean content = Objects.nonNull(server.getWorld(worldName));
        PackUtils.sendResult(controller, requestId, PackType.MC_HAS_WORLD_RESULT, new IdContent(requestId, content));
        return content;
    }

    public boolean onHasPermission(int requestId, String playerId, String permission) {
        logIfDebug("检查在线玩家 " + playerId + " 是否具备权限 " + permission);
        final Player player = server.getPlayer(playerId);
        final boolean content = Objects.nonNull(player) && Objects.equals(playerId, player.getName())  && player.hasPermission(permission);
        PackUtils.sendResult(controller, requestId, PackType.MC_HAS_PERMISSION_RESULT, content);
        return content;
    }

    public boolean onIsOnline(int requestId, String playerId) {
        logIfDebug("检查用户 " + playerId + " 是否在线");
        final Player player = server.getPlayer(playerId);
        final boolean content = Objects.nonNull(player) && Objects.equals(playerId, player.getName()) && player.isOnline();
        PackUtils.sendResult(controller, requestId, PackType.MC_CHECK_ONLINE_RESULT, content);
        return content;
    }

    public ResultContent onSendWorldMessage(int requestId, Set<String> worldNames, String message) {
        logIfDebug("在世界：" + worldNames + " 内显示消息：" + message);
        final Set<String> failWorldNames = new HashSet<>();
        if (worldNames.isEmpty()) {
            return PackUtils.sendResult(controller, requestId, PackType.MC_SEND_WORLD_MESSAGE_RESULT, false, "没有需要发送消息的世界");
        }

        boolean success = false;
        String resultMessage = null;
        for (String worldName : worldNames) {
            final World world = server.getWorld(worldName);

            if (Objects.isNull(world)) {
                success = false;
                failWorldNames.add(worldName);
            } else {
                final List<Player> players = world.getPlayers();
                if (players.isEmpty()) {
                    success = false;
                } else {
                    for (Player player : players) {
                        player.sendMessage(message);
                    }
                    success = true;
                }
            }
        }

        if (!failWorldNames.isEmpty()) {
            resultMessage = "无法找到世界：" + CollectionUtils.getSummary(failWorldNames, String::toString, "", "", "、");
        }

        return PackUtils.sendResult(controller, requestId, PackType.MC_SEND_WORLD_MESSAGE_RESULT, success, resultMessage);
    }

    public void onBroadcastMessage(String message) {
        logIfDebug("广播消息：" + message);
        server.broadcastMessage(message);
    }

    public ResultContent onExecuteAsConsole(int requestId, String command) {
        logIfDebug("以控制台身份执行命令：" + command + "，请求 ID：" + requestId);

        server.getScheduler().runTask(plugin, () -> {
            server.getPluginManager().callEvent(new XiaomingExecuteCommandEvent(requestId, null, command));
        });
        final XiaomingConsoleCommandSender fakeConsole = new XiaomingConsoleCommandSender(server.getConsoleSender());
        final ResultContent resultContent = new ResultContent();
        server.getScheduler().runTask(plugin, () -> {
            try {
                resultContent.setSuccess(server.dispatchCommand(fakeConsole, command));
                resultContent.setObject(CollectionUtils.getSummary(fakeConsole.getMessages(), String::toString, "", "指令已执行，无返回结果", "\n"));
            } catch (CommandException exception) {
                resultContent.setSuccess(false);
                sendLogger("以控制台身份执行指令 " + command + " 时出现异常：" + exception);
                logger.severe("远程执行指令时出现异常");
                exception.printStackTrace();
            }
            PackUtils.sendResult(controller, requestId, PackType.MC_COMMAND_RESULT, resultContent);
        });
        return resultContent;
    }

    public ResultContent onExecuteAsPlayer(int requestId, String playerId, String command) {
        logIfDebug("以玩家：" + playerId + " 身份执行命令：" + command + "，请求 ID：" + requestId);
        final Player realPlayer = server.getPlayer(playerId);
        final XiaomingCommandSender<?> fakePlayer;

        if (Objects.isNull(realPlayer) || !Objects.equals(playerId, realPlayer.getName())) {
            fakePlayer = new XiaomingNamedCommandSender(server.getConsoleSender(), playerId);
        } else {
            fakePlayer = new XiaomingPlayerCommandSender(realPlayer);
            realPlayer.sendMessage(Formatter.headThen(Formatter.yellow("小明将以你的身份执行指令：" + Formatter.green(command))));
        }

        server.getScheduler().runTask(plugin, () -> {
            server.getPluginManager().callEvent(new XiaomingExecuteCommandEvent(requestId, playerId, command));
        });
        final ResultContent resultContent = new ResultContent();
        server.getScheduler().runTask(plugin, () -> {
            try {
                resultContent.setSuccess(server.dispatchCommand(fakePlayer, command));
                resultContent.setObject(CollectionUtils.getSummary(fakePlayer.getMessages(), String::toString, "", "指令已执行，无返回结果", "\n"));
            } catch (CommandException exception) {
                resultContent.setSuccess(false);
                sendLogger("以玩家 " + playerId + " 身份执行指令 " + command + " 时出现异常：" + exception);
                logger.severe("远程执行指令时出现异常");
                exception.printStackTrace();
            }
            PackUtils.sendResult(controller, requestId, PackType.MC_COMMAND_RESULT, resultContent);
        });
        return resultContent;
    }

    public ResultContent onAsyncPlayerAccept(int requestId, String playerId, String message, long timeout) {
        logIfDebug("令玩家 " + playerId + " 在" + TimeUtils.toTimeString(timeout) + "内决定：" + message);
        final Player player = server.getPlayer(playerId);
        final ResultContent resultContent = new ResultContent();

        if (Objects.isNull(player) || !Objects.equals(playerId, player.getName())) {
            resultContent.setSuccess(false);
            resultContent.setObject("该玩家不在服务器");
            return PackUtils.sendResult(controller, requestId, PackType.MC_PLAYER_ACCET_RESULT, resultContent);
        } else {
            final XiaomingCommandExecutor commandExecutor = plugin.getCommandExecutor();
            if (commandExecutor.isWaitingUserAccept(playerId)) {
                resultContent.setSuccess(false);
                resultContent.setObject("用户正在等待审批另一件事务");
                return PackUtils.sendResult(controller, requestId, PackType.MC_PLAYER_ACCET_RESULT, resultContent);
            } else {
                player.sendMessage(Formatter.headThen(Formatter.yellow(message)));
                player.sendMessage("若想接受请求，输入 " + Formatter.green("/xmaccept") + "。\n" +
                        "若想拒绝请求，输入 " + Formatter.red("/xmdeny") + "。\n" +
                        "此请求将在" + Formatter.yellow(TimeUtils.toTimeString(timeout)) + "后自动取消");

                commandExecutor.asyncWaitUserAccept(playerId, timeout, () -> {
                    resultContent.setSuccess(true);
                    PackUtils.sendResult(controller, requestId, PackType.MC_PLAYER_ACCET_RESULT, resultContent);
                }, () -> {
                    resultContent.setSuccess(false);
                    resultContent.setObject("太长时间没有处理，或拒绝了该请求");
                    PackUtils.sendResult(controller, requestId, PackType.MC_PLAYER_ACCET_RESULT, resultContent);
                });
                return resultContent;
            }
        }
    }

    public ResultContent onAsyncPlayerConfirm(int requestId, String playerId, String message, long timeout) {
        logIfDebug("令玩家 " + playerId + " 在" + TimeUtils.toTimeString(timeout) + "内确定：" + message);
        final Player player = server.getPlayer(playerId);
        final ResultContent resultContent = new ResultContent();

        if (Objects.isNull(player) || !Objects.equals(playerId, player.getName())) {
            resultContent.setSuccess(false);
            resultContent.setObject("该玩家不在服务器");
            return PackUtils.sendResult(controller, requestId, PackType.MC_PLAYER_CONFIRM_RESULT, resultContent);
        } else {
            final XiaomingCommandExecutor commandExecutor = plugin.getCommandExecutor();
            if (commandExecutor.isWaitingUserConfirm(playerId)) {
                resultContent.setSuccess(false);
                resultContent.setObject("用户正在等待确认另一件事务");
                return PackUtils.sendResult(controller, requestId, PackType.MC_PLAYER_CONFIRM_RESULT, resultContent);
            } else {
                player.sendMessage(Formatter.headThen(Formatter.yellow(message)));
                player.sendMessage("若想确认请求，输入 " + Formatter.green("/xmconfirm") + "。\n" +
                        "若想取消请求，输入 " + Formatter.red("/xmcancel") + "。\n" +
                        "此请求将在" + Formatter.yellow(TimeUtils.toTimeString(timeout)) + "后自动取消");

                commandExecutor.asyncWaitUserConfirm(playerId, timeout, () -> {
                    resultContent.setSuccess(true);
                    PackUtils.sendResult(controller, requestId, PackType.MC_PLAYER_CONFIRM_RESULT, resultContent);
                }, () -> {
                    resultContent.setSuccess(false);
                    resultContent.setObject("太长时间没有处理，或取消了该请求");
                    PackUtils.sendResult(controller, requestId, PackType.MC_PLAYER_CONFIRM_RESULT, resultContent);
                });
                return resultContent;
            }
        }
    }

    public ResultContent onSendMessage(int requestId, Set<String> playerIds, String message) {
        logIfDebug("向玩家 " + playerIds + " 显示消息：" + message);
        final Set<String> failPlayerNames = new HashSet<>();

        for (String playerId : playerIds) {
            final Player player = server.getPlayer(playerId);
            if (Objects.isNull(player) || !Objects.equals(playerId, player.getName())) {
                failPlayerNames.add(playerId);
            } else {
                player.sendMessage(message);
            }
        }

        String resultMessage = null;
        if (!failPlayerNames.isEmpty()) {
            resultMessage = "无法找到玩家：" + CollectionUtils.getSummary(failPlayerNames, String::toString, "", "", "、");
        }
        return PackUtils.sendResult(controller, requestId, PackType.MC_SEND_MESSAGE_RESULT, !failPlayerNames.isEmpty(), resultMessage);
    }

    public ResultContent onSendTitle(int requestId, Set<String> playerIds, String title, String subtitle, int fadeIn, int delay, int fadeOut) {
        logIfDebug("向玩家 " + playerIds + " 显示标题：" + title + "，副标题：" + subtitle);
        final Set<String> failPlayerNames = new HashSet<>();

        for (String playerId : playerIds) {
            final Player player = server.getPlayer(playerId);
            if (Objects.isNull(player) || !Objects.equals(playerId, player.getName())) {
                failPlayerNames.add(playerId);
            } else {
                player.sendTitle(title, subtitle, fadeIn, delay, fadeOut);
            }
        }

        String resultMessage = null;
        if (!failPlayerNames.isEmpty()) {
            resultMessage = "无法找到玩家：" + CollectionUtils.getSummary(failPlayerNames, String::toString, "", "", "、");
        }

        return PackUtils.sendResult(controller, requestId, PackType.MC_SEND_TITLE_RESULT, !failPlayerNames.isEmpty(), resultMessage);
    }

    public ResultContent onSendTitleToAllPlayers(int requestId, String title, String subtitle, int fadeIn, int delay, int fadeOut) {
        logIfDebug("向所有玩家显示标题：" + title + "，副标题：" + subtitle);
        final Collection<? extends Player> onlinePlayers = server.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) {
            return PackUtils.sendResult(controller, requestId, PackType.MC_SEND_TITLE_RESULT, false, "服务器没有任何玩家");
        } else {
            onlinePlayers.forEach(player -> {
                player.sendTitle(title, subtitle, fadeIn, delay, fadeOut);
            });
            return PackUtils.sendResult(controller, requestId, PackType.MC_SEND_TITLE_RESULT, true, "成功向 " + onlinePlayers.size() + " 个玩家显示了该标题");
        }
    }

    public ResultContent onSendMessageToAllPlayers(int requestId, String message) {
        logIfDebug("向所有玩家显示消息：" + message);
        final Collection<? extends Player> onlinePlayers = server.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) {
            return PackUtils.sendResult(controller, requestId, PackType.MC_SEND_MESSAGE_RESULT, false, "服务器没有任何玩家");
        } else {
            onlinePlayers.forEach(player -> {
                player.sendMessage(message);
            });
            return PackUtils.sendResult(controller, requestId, PackType.MC_SEND_MESSAGE_RESULT, true, "成功向 " + onlinePlayers.size() + " 个玩家发送了该消息");
        }
    }

    protected void registerListeners() {
        // 获取在线玩家
        controller.register(PackType.XM_ONLINE_PLAYER, pack -> {
            onOnlinePlayers();
        });

        // 获取世界列表
        controller.register(PackType.XM_WORLD_NAME, pack -> {
            onGetWorldNames();
        });

        // 确认是否存在某世界
        controller.register(PackType.XM_HAS_WORLD, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final String content = idContent.getContent(String.class);
            onHasWorld(idContent.getRequestId(), content);
        });

        // 验证玩家是否有权限
        controller.register(PackType.XM_HAS_PERMISSION, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final HasPermissionContent content = idContent.getContent(HasPermissionContent.class);
            onHasPermission(idContent.getRequestId(), content.getPlayerId(), content.getPermission());
        });

        // 检查玩家是否在线
        controller.register(PackType.XM_CHECK_ONLINE, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final String playerName = idContent.getContent(String.class);
            onIsOnline(idContent.getRequestId(), playerName);
        });

        // 显示世界信息
        controller.register(PackType.XM_SEND_WORLD_MESSAGE, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final WorldMessageContent content = idContent.getContent(WorldMessageContent.class);
            onSendWorldMessage(idContent.getRequestId(), content.getWorldName(), content.getMessage());
        });

        // 公告信息
        controller.register(PackType.XM_BROADCAST_MESSAGE, pack -> {
            onBroadcastMessage(pack.getContent(String.class));
        });

        // 执行指令的返回信息
        controller.register(PackType.XM_COMMAND, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final ServerCommandContent content = idContent.getContent(ServerCommandContent.class);
            final String playerId = content.getPlayerId();

            if (Objects.nonNull(playerId)) {
                onExecuteAsPlayer(idContent.getRequestId(), playerId, content.getCommand());
            } else {
                onExecuteAsConsole(idContent.getRequestId(), content.getCommand());
            }
        });

        // 让确认消息
        controller.register(PackType.XM_PLAYER_CONFIRM, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final PlayerConfirmOrAcceptContent content = idContent.getContent(PlayerConfirmOrAcceptContent.class);
            onAsyncPlayerConfirm(idContent.getRequestId(), content.getPlayerId(), content.getMessage(), content.getTimeout());
        });

        // 让审批消息
        controller.register(PackType.XM_PLAYER_ACCEPT, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final PlayerConfirmOrAcceptContent content = idContent.getContent(PlayerConfirmOrAcceptContent.class);
            onAsyncPlayerAccept(idContent.getRequestId(), content.getPlayerId(), content.getMessage(), content.getTimeout());
        });

        // 显示一些信息
        controller.register(PackType.XM_SEND_MESSAGE, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final ShowMessageContent content = idContent.getContent(ShowMessageContent.class);
            onSendMessage(idContent.getRequestId(), content.getPlayerIds(), content.getMessage());
        });

        // 显示一些信息
        controller.register(PackType.XM_SEND_MESSAGE_TO_ALL_PLAYERS, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            onSendMessageToAllPlayers(idContent.getRequestId(), idContent.getContent(String.class));
        });

        // 显示大标题
        controller.register(PackType.XM_SEND_TITLE, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final SendTitleContent content = idContent.getContent(SendTitleContent.class);
            onSendTitle(idContent.getRequestId(), content.getPlayerId(), content.getTitle(), content.getSubtitle(), content.getFadeIn(), content.getDelay(), content.getFadeOut());
        });

        // 给每个成员都显示一个大标题
        controller.register(PackType.XM_SEND_TITLE_TO_ALL_PLAYERS, pack -> {
            final IdContent idContent = pack.getContent(IdContent.class);
            final SendTitleToAllPlayersContent content = idContent.getContent(SendTitleToAllPlayersContent.class);
            onSendTitleToAllPlayers(idContent.getRequestId(), content.getTitle(), content.getSubtitle(), content.getFadeIn(), content.getDelay(), content.getFadeOut());
        });
    }

    /** 请求 ID，只能在创建新的请求时修改 */
    protected final AtomicInteger requestId = new AtomicInteger(0);
    protected int allocateRequestId() {
        return requestId.incrementAndGet();
    }

    public ResultContent sendPlayerChatMessage(Player player, String message, boolean echo) throws IOException {
        logIfDebug("正在发送服务器聊天消息：" + player.getName() + "：" + message + "，回响：" + echo);
        final int requestId = allocateRequestId();
        final IdContent content = new IdContent(requestId, new PlayerChatContent(PlayerUtils.forPlayer(player), message, echo));

        controller.sendLater(PackType.MC_PLAYER_CHAT, content);
        return controller.nextResult(PackType.XM_PLAYER_CHAT_RESULT, requestId);
    }

    public ResultContent unbind(String playerId) throws IOException {
        logIfDebug("正在请求解绑：" + playerId);
        final int requestId = allocateRequestId();
        final IdContent content = new IdContent(requestId, playerId);

        controller.sendLater(PackType.MC_UNBIND, content);
        return controller.nextResult(PackType.XM_UNBIND_RESULT, requestId);
    }

    public ResultContent bind(String playerId, long code) throws IOException {
        logIfDebug("正在请求绑定：" + playerId + " => " + code);
        final int requestId = allocateRequestId();
        final IdContent content = new IdContent(requestId, new BindContent(playerId, code));

        controller.sendLater(PackType.MC_BIND, content);
        return controller.nextResult(PackType.XM_BIND_RESULT, requestId);
    }

    public ResultContent executeInGroup(long group, String playerId, String command) throws IOException {
        logIfDebug("正在请求以 " + playerId + " 身份在 QQ 群 " + group + " 中执行小明指令：" + command);

        final int requestId = allocateRequestId();
        final IdContent content = new IdContent(requestId, XiaomingCommandContent.groupCommandContent(group, playerId, command));
        controller.sendLater(PackType.MC_COMMAND, content);
        return controller.nextResult(PackType.XM_COMMAND_RESULT, requestId);
    }

    public ResultContent executeInTemp(long group, String playerId, String command) throws IOException {
        logIfDebug("正在请求以 " + playerId + " 身份在 QQ 群 " + group + " 中的临时会话内执行小明指令：" + command);

        final int requestId = allocateRequestId();
        final IdContent content = new IdContent(requestId, XiaomingCommandContent.tempCommandContent(group, playerId, command));
        controller.sendLater(PackType.MC_COMMAND, content);
        return controller.nextResult(PackType.XM_COMMAND_RESULT, requestId);
    }

    public ResultContent executeInPrivate(String playerId, String command) throws IOException {
        logIfDebug("正在请求以 " + playerId + " 身份在私聊中执行小明指令：" + command);

        final int requestId = allocateRequestId();
        final IdContent content = new IdContent(requestId, XiaomingCommandContent.privateCommandContent(playerId, command));
        controller.sendLater(PackType.MC_COMMAND, content);
        return controller.nextResult(PackType.XM_COMMAND_RESULT, requestId);
    }

    public ResultContent apply(Player player, String command) throws IOException {
        logIfDebug("正在申请使用指令：申请人：" + player.getName() + "，指令：" + command);
        final PlayerContent playerContent = PlayerUtils.forPlayer(player);
        final int requestId = allocateRequestId();
        final IdContent content = new IdContent(requestId, new ApplyCommandContent(playerContent, command));

        controller.sendLater(PackType.MC_APPLY_COMMAND, content);
        return controller.nextResult(PackType.XM_APPLY_COMMAND_RESULT, requestId);
    }

    public Set<String> listServerTags() throws IOException {
        logIfDebug("正在获取本服标记");
        controller.sendLater(PackType.MC_LIST_SERVER_TAG);
        return controller.nextPack(PackType.XM_LIST_SERVER_TAG_RESULT).getContent(Set.class);
    }

    public ResultContent addServerTag(String tag) throws IOException {
        logIfDebug("正在请求增加本服标记：" + tag);
        final int requestId = allocateRequestId();
        IdContent content = new IdContent(requestId, tag);
        controller.sendLater(PackType.MC_ADD_SERVER_TAG, content);
        return controller.nextResult(PackType.XM_ADD_SERVER_TAG_RESULT, requestId);
    }

    public ResultContent removeServerTag(String tag) throws IOException {
        logIfDebug("正在请求删除本服标记：" + tag);
        final int requestId = allocateRequestId();
        IdContent content = new IdContent(requestId, tag);
        controller.sendLater(PackType.MC_REMOVE_SERVER_TAG, content);
        return controller.nextResult(PackType.XM_REMOVE_SERVER_TAG_RESULT, requestId);
    }

    public Set<String> listWorldTags(String worldName) throws IOException {
        logIfDebug("正在获取世界 " + worldName + " 的标记");
        final int requestId = allocateRequestId();
        final IdContent content = new IdContent(requestId, worldName);
        controller.sendLater(PackType.MC_LIST_WORLD_TAG, content);
        return controller.nextContent(PackType.XM_LIST_WORLD_TAG_RESULT, requestId).getContent(Set.class);
    }

    public ResultContent addWorldTag(String worldName, String tag) throws IOException {
        logIfDebug("正在请求为世界 " + worldName + " 增加标记 " + tag);
        final int requestId = allocateRequestId();
        IdContent content = new IdContent(requestId, new WorldTagContent(worldName, tag));
        controller.sendLater(PackType.MC_ADD_WORLD_TAG, content);
        return controller.nextResult(PackType.XM_ADD_WORLD_TAG_RESULT, requestId);
    }

    public ResultContent removeWorldTag(String worldName, String tag) throws IOException {
        logIfDebug("正在请求删除世界 " + worldName + " 的标记 " + tag);
        final int requestId = allocateRequestId();
        IdContent content = new IdContent(requestId, new WorldTagContent(worldName, tag));
        controller.sendLater(PackType.MC_REMOVE_WORLD_TAG, content);
        return controller.nextResult(PackType.XM_REMOVE_WORLD_TAG_RESULT, requestId);
    }

    public Set<String> listWorldNames() throws IOException {
        logIfDebug("正在获取所有世界缓存");
        controller.sendLater(PackType.MC_LIST_WORLD);
        return controller.nextPack(PackType.XM_LIST_WORLD_RESULT).getContent(Set.class);
    }

    public void flushWorld() {
        logIfDebug("正在请求刷新世界缓存");
        controller.sendLater(PackType.MC_FLUSH_WORLD);
    }

    public void flush() {
        logIfDebug("正在请求刷新所有缓存");
        controller.sendLater(PackType.MC_FLUSH);
    }
}