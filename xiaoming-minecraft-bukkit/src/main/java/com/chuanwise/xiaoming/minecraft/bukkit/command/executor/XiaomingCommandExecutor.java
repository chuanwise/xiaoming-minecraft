package com.chuanwise.xiaoming.minecraft.bukkit.command.executor;

import com.chuanwise.xiaoming.minecraft.bukkit.XiaomingBukkitPlugin;
import com.chuanwise.xiaoming.minecraft.bukkit.configuration.XiaomingBukkitConfiguration;
import com.chuanwise.xiaoming.minecraft.socket.SocketConfiguration;
import com.chuanwise.xiaoming.minecraft.util.*;
import com.chuanwise.xiaoming.minecraft.bukkit.socket.BukkitSocket;
import com.chuanwise.xiaoming.minecraft.pack.PackType;
import com.chuanwise.xiaoming.minecraft.pack.content.*;
import com.chuanwise.xiaoming.minecraft.socket.SocketController;
import lombok.AllArgsConstructor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 通用小明指令 {@code xm/xiaoming <content>}
 */
@AllArgsConstructor
public class XiaomingCommandExecutor implements CommandExecutor {
    public static String HELP = "目前支持的小明指令\n" +
            Formatter.green("/xm info") + Formatter.gray("：") + "查看插件信息\n" +
            Formatter.green("/xm bind [QQ]") + Formatter.gray("：") + "绑定到 QQ\n" +
            Formatter.green("/xm unbind") + Formatter.gray("：") + "解除和之前绑定的 QQ 之间的绑定\n" +
            Formatter.green("/xm apply [command]") + Formatter.gray("：") + "向管理员申请使用某指令\n" +
            Formatter.green("/xm help") + Formatter.gray("：") + "显示帮助文档\n" +
            Formatter.green("/xmsend [一些消息]") + Formatter.gray("：") + "向群内发送一些消息\n" +
            Formatter.green("/xmexe group [群号] [小明指令]") + Formatter.gray("：") + "在某群中执行小明指令\n" +
            Formatter.green("/xmexe temp [群号] [小明指令]") + Formatter.gray("：") + "在某群的临时会话中执行小明指令\n" +
            Formatter.green("/xmexe private [小明指令]") + Formatter.gray("：") + "在和小明的私聊中执行小明指令\n" +
            Formatter.yellow("/xm test") + Formatter.gray("：") + "发送一个心跳包检测服务器是否在线\n" +
            Formatter.yellow("/xm tag") + Formatter.gray("：") + "获取服务器所有的标记\n" +
            Formatter.yellow("/xm tag [add|remove] [tag]") + Formatter.gray("：") + "增加或删除本服标记\n" +
            Formatter.yellow("/xm world") + Formatter.gray("：") + "拉取世界缓存列表\n" +
            Formatter.yellow("/xm world [worldName|~]") + Formatter.gray("：") + "查看某世界（~ 时为当前所在世界）的标记\n" +
            Formatter.yellow("/xm world [worldName|~] [add|remove] [tag]") + Formatter.gray("：") + "为某世界增加或删除标记\n" +
            Formatter.yellow("/xm flush") + Formatter.gray("：") + "手动刷新小明有关本服的缓存\n" +
            Formatter.yellow("/xm flush world") + Formatter.gray("：") + "手动刷新小明的世界缓存\n" +
            Formatter.blue("/xm link") + Formatter.gray("：") + "查看连接状态\n" +
            Formatter.blue("/xm link test") + Formatter.gray("：") + "测试连接状态\n" +
            Formatter.blue("/xm link connect") + Formatter.gray("：") + "通过设置的地址和端口连接小明\n" +
            Formatter.blue("/xm link connect [address] [port]") + Formatter.gray("：") + "通过指定的地址和端口连接到服务器\n" +
            Formatter.blue("/xm link disconnect") + Formatter.gray("：") + "断开和服务器的连接\n" +
            Formatter.blue("/xm link reconnect") + Formatter.gray("：") + "通过设置的地址和端口重新连接到小明\n" +
            Formatter.blue("/xm link reconnect [address] [port]") + Formatter.gray("：") + "通过指定的地址和端口重新连接到小明\n" +
            Formatter.red("/xm reload") + Formatter.gray("：") + "重新加载插件\n" +
            Formatter.red("/xm debug") + Formatter.gray("：") + "启动或关闭调试模式\n" +
            Formatter.red("/xm identify") + Formatter.gray("：") + "查看相关凭据\n" +
            Formatter.red("/xm identify xiaoming") + Formatter.gray("：") + "查看合法小明凭据\n" +
            Formatter.red("/xm identify xiaoming [setTo]") + Formatter.gray("：") + "修改小明凭据\n" +
            Formatter.red("/xm identify server") + Formatter.gray("：") + "查看服务器凭据\n" +
            Formatter.red("/xm identify server [setTo]") + Formatter.gray("：") + "修改服务器凭据";
    final XiaomingBukkitPlugin plugin;
    final XiaomingBukkitConfiguration configuration;
    final BukkitSocket bukkitSocket;

    public XiaomingCommandExecutor(XiaomingBukkitPlugin plugin) {
        this.plugin = plugin;
        this.configuration = plugin.getConfiguration();
        this.bukkitSocket = plugin.getBukkitSocket();
    }

    final Map<String, AtomicBoolean> confirmResult = new HashMap<>();
    final Map<String, AtomicBoolean> acceptResult = new HashMap<>();

    public boolean requirePermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(Formatter.headThen(Formatter.red("小明不能帮你做这件事呢，因为你缺少权限：" + permission)));
            return false;
        } else {
            return true;
        }
    }

    public AtomicBoolean asyncWaitUserConfirm(String name, long timeout, Runnable onSuccess, Runnable onFail) {
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        confirmResult.put(name, atomicBoolean);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            final long latestTime = System.currentTimeMillis() + timeout;
            synchronized (atomicBoolean) {
                try {
                    atomicBoolean.wait(timeout);
                } catch (InterruptedException exception) {
                    onFail.run();
                    return;
                }
            }
            if (System.currentTimeMillis() >= latestTime || !atomicBoolean.get()) {
                onFail.run();
            } else {
                onSuccess.run();
            }
        });

        return atomicBoolean;
    }

    public AtomicBoolean asyncWaitUserAccept(String name, long timeout, Runnable onSuccess, Runnable onFail) {
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        acceptResult.put(name, atomicBoolean);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            final long latestTime = System.currentTimeMillis() + timeout;
            synchronized (atomicBoolean) {
                try {
                    atomicBoolean.wait(timeout);
                } catch (InterruptedException exception) {
                    onFail.run();
                    return;
                }
            }
            if (System.currentTimeMillis() >= latestTime || !atomicBoolean.get()) {
                onFail.run();
            } else {
                onSuccess.run();
            }
        });

        return atomicBoolean;
    }

    public boolean isWaitingUserConfirm(String name) {
        return confirmResult.containsKey(name);
    }

    public boolean isWaitingUserAccept(String name) {
        return acceptResult.containsKey(name);
    }

    public boolean onUserConfirm(String name) {
        synchronized (confirmResult) {
            final AtomicBoolean atomicBoolean = confirmResult.get(name);
            if (Objects.isNull(atomicBoolean)) {
                return false;
            } else {
                atomicBoolean.set(true);
                synchronized (atomicBoolean) {
                    atomicBoolean.notifyAll();
                }
                confirmResult.remove(name);
                return true;
            }
        }
    }

    public boolean onUserAccept(String name) {
        synchronized (acceptResult) {
            final AtomicBoolean atomicBoolean = acceptResult.get(name);
            if (Objects.isNull(atomicBoolean)) {
                return false;
            } else {
                atomicBoolean.set(true);
                synchronized (atomicBoolean) {
                    atomicBoolean.notifyAll();
                }
                acceptResult.remove(name);
                return true;
            }
        }
    }

    public boolean onUserDeny(String name) {
        final AtomicBoolean atomicBoolean = acceptResult.get(name);
        if (Objects.isNull(atomicBoolean)) {
            return false;
        } else {
            atomicBoolean.set(false);
            synchronized (atomicBoolean) {
                atomicBoolean.notifyAll();
            }
            acceptResult.remove(name);
            return true;
        }
    }

    public boolean onUserCancel(String name) {
        final AtomicBoolean atomicBoolean = confirmResult.get(name);
        if (Objects.isNull(atomicBoolean)) {
            return false;
        } else {
            atomicBoolean.set(false);
            synchronized (atomicBoolean) {
                atomicBoolean.notifyAll();
            }
            confirmResult.remove(name);
            return true;
        }
    }

    public boolean requireConnected(CommandSender sender) {
        if (!bukkitSocket.test()) {
            sender.sendMessage(Formatter.headThen(Formatter.yellow("小明好像还不在线，让管理重连一下吧")));
            return false;
        } else {
            return true;
        }
    }

    public void onIOException(CommandSender sender, IOException exception) {
        sender.sendMessage(Formatter.headThen(Formatter.red("小明的网络出现了一些问题，过一会儿再试吧")));
        bukkitSocket.onThrowable(exception);
    }

    public void onSocketTimeout(CommandSender sender) {
        sender.sendMessage(Formatter.headThen(Formatter.yellow("小明的网络不太好呢，等了好久都没得到那边的回应，过一会儿再试吧")));
    }

    protected void runOrCatch(CommandSender sender, ExceptionThrowableRunnable<IOException> callable) {
        try {
            callable.run();
        } catch (SocketTimeoutException exception) {
            onSocketTimeout(sender);
        } catch (IOException exception) {
            onIOException(sender, exception);
        } catch (Exception exception) {
            sender.sendMessage(Formatter.headThen(Formatter.red("小明遇到了一些问题，这个问题已经上报啦，一起期待更好的小明吧")));
            exception.printStackTrace();
            boolean errorSended = false;
            if (bukkitSocket.isConnected()) {
                try {
                    bukkitSocket.getController().send(PackType.MC_LOGGER, "与用户「" + sender.getName() + "」交互时出现异常：" + exception);
                    errorSended = true;
                } catch (IOException e) {
                }
            }

            if (!errorSended) {
                plugin.getLogger().severe("无法向日志群发送该错误报告");
            }
        }
    }

    protected String requireWorldName(CommandSender sender, String worldName) {
        if (Objects.equals(worldName, "~")) {
            if (sender instanceof Player) {
                return ((Player) sender).getWorld().getName();
            } else {
                sender.sendMessage(Formatter.headThen(Formatter.red("世界名自动填充写法「~」仅适用于真正的玩家")));
                return null;
            }
        } else {
            if (Objects.isNull(plugin.getServer().getWorld(worldName))) {
                sender.sendMessage(Formatter.headThen(Formatter.red("本服务器不存在世界「" + worldName + "」")));
                return null;
            } else {
                return worldName;
            }
        }
    }

    protected void readTheFuckManual(CommandSender sender) {
        sender.sendMessage(Formatter.headThen(Formatter.red("小明不理解你的意思，试试用 ") + Formatter.yellow("/xm help") + Formatter.red(" 查看帮助吧")));
    }

    protected <T> T require(CommandSender sender, String what, String input, Predicate<String> predicate, Function<String, T> translator) {
        if (predicate.test(input)) {
            return translator.apply(input);
        } else {
            sender.sendMessage(Formatter.headThen(Formatter.gray("「") + Formatter.yellow(input) + Formatter.gray("」") +
                    Formatter.red("似乎并不是一个合理的") + Formatter.gray("「") + Formatter.blue(what) + Formatter.gray("」") +Formatter.red("呢")));
            return null;
        }
    }

    protected Long requireLong(CommandSender sender, String what, String input) {
        return require(sender, what, input, string -> string.matches("\\d+"), string -> Long.parseLong(string));
    }

    protected Integer requireInt(CommandSender sender, String what, String input) {
        return require(sender, what, input, string -> string.matches("\\d+"), string -> Integer.parseInt(string));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command originalCommand, String label, String[] strings) {
        if (strings.length == 0) {
            return false;
        }

        final SocketController controller = bukkitSocket.getController();
        final String firstArgument = strings[0];
        final Server server = plugin.getServer();
        final BukkitScheduler scheduler = server.getScheduler();

        switch (firstArgument) {
            // link connect
            // link connect [ADDRESS] [PORT]
            // link reconnect
            // link reconnect [ADDRESS] [PORT]
            // link disconnect
            case "link": {
                if (strings.length == 1) {
                    if (!requirePermission(sender, "minecraft.link.look")) {
                        return true;
                    }

                    final XiaomingBukkitConfiguration configuration = bukkitSocket.getConfiguration();
                    final SocketConfiguration socketConfiguration = configuration.getSocketConfiguration();
                    sender.sendMessage(Formatter.headThen("服务器连接信息") + "\n" +
                            Formatter.blue("连接状态") + Formatter.gray("：") + (bukkitSocket.test() ? Formatter.green("已连接") : Formatter.red("未链接")) + "\n" +
                            Formatter.blue("地址") + Formatter.gray("：") + Formatter.white(bukkitSocket.getAddress()) + "\n" +
                            Formatter.blue("端口") + Formatter.gray("：") + Formatter.white(String.valueOf(bukkitSocket.getPort())) + "\n" +
                            Formatter.red("服务器凭据") + Formatter.gray("：") + Formatter.white(configuration.getServerIdentify()) + "\n" +
                            Formatter.red("小明凭据") + Formatter.gray("：") + Formatter.white(configuration.getXiaomingIdentify()) + "\n" +
                            Formatter.green("重连时长") + Formatter.gray("：") + Formatter.white(String.valueOf(configuration.getReconnectDelay())) + "\n" +
                            Formatter.green("发包周期") + Formatter.gray("：") + Formatter.white(TimeUtils.toTimeString(socketConfiguration.getSendPackPeriod())) + "\n" +
                            Formatter.green("收包周期") + Formatter.gray("：") + Formatter.white(TimeUtils.toTimeString(socketConfiguration.getReceivePackPeriod())) + "\n" +
                            Formatter.green("心跳周期") + Formatter.gray("：") + Formatter.white(TimeUtils.toTimeString(socketConfiguration.getHeartbeatPeriod())) + "\n" +
                            Formatter.green("清包周期") + Formatter.gray("：") + Formatter.white(TimeUtils.toTimeString(socketConfiguration.getCheckRecentPackPeriod())) + "\n" +
                            Formatter.green("最大缓存包数") + Formatter.gray("：") + Formatter.white(String.valueOf(socketConfiguration.getMaxRecentPackSize())) + "\n" +
                            Formatter.green("最大无响应时长") + Formatter.gray("：") + Formatter.white(TimeUtils.toTimeString(socketConfiguration.getMaxNoResponseTime())) + "\n" +
                            Formatter.green("无响应时最大尝试次数") + Formatter.gray("：") + Formatter.white(String.valueOf(socketConfiguration.getMaxTryTimes())) + "\n" +
                            Formatter.green("尝试测试连接周期") + Formatter.gray("：") + Formatter.white(TimeUtils.toTimeString(socketConfiguration.getTryPeriod())));
                    return true;
                }
                switch (strings[1]) {
                    case "reconnect": {
                        if (!requirePermission(sender, "xiaoming.link.reconnect")) {
                            return true;
                        }

                        String address = bukkitSocket.getAddress();
                        int port = bukkitSocket.getPort();

                        // link connect <Address> <Port>
                        // link connect
                        if (strings.length == 4) {
                            address = strings[2];

                            final Integer portObject = requireInt(sender, "端口", strings[3]);
                            if (Objects.isNull(portObject)) {
                                return true;
                            }
                            port = portObject;

                            configuration.setAddress(address);
                            configuration.setPort(port);
                        } else if (strings.length != 2) {
                            readTheFuckManual(sender);
                            return true;
                        }

                        final String finalAddress = address;
                        final int finalPort = port;
                        scheduler.runTaskAsynchronously(plugin, () -> {
                            bukkitSocket.setAddress(finalAddress);
                            bukkitSocket.setPort(finalPort);

                            if (bukkitSocket.test()) {
                                sender.sendMessage(Formatter.headThen(Formatter.gray("已断开服务器连接，一段时间后会自动重新连接")));
                                bukkitSocket.disconnectOrReconnect();
                            } else {
                                sender.sendMessage(Formatter.headThen(Formatter.gray("一段时间后会自动连接")));
                                bukkitSocket.disconnectOrReconnect();
                            }
                        });
                        break;
                    }
                    case "connect": {
                        if (!requirePermission(sender, "xiaoming.link.connect")) {
                            return true;
                        }

                        String address = bukkitSocket.getAddress();
                        int port = bukkitSocket.getPort();

                        // connect <Address> <Port>
                        // connect
                        if (strings.length == 4) {
                            address = strings[2];

                            final Integer portObject = requireInt(sender, "端口", strings[3]);
                            if (Objects.isNull(portObject)) {
                                return true;
                            }
                            port = portObject;

                            configuration.setAddress(address);
                            configuration.setPort(port);
                        } else if (strings.length != 2) {
                            readTheFuckManual(sender);
                            return false;
                        }

                        if (bukkitSocket.test()) {
                            sender.sendMessage(Formatter.headThen("服务器已经连接到小明了，无需重新连接"));
                        } else {
                            final String finalAddress = address;
                            final int finalPort = port;
                            scheduler.runTaskAsynchronously(plugin, () -> {
                                boolean success;
                                try {
                                    bukkitSocket.setAddress(finalAddress);
                                    bukkitSocket.setPort(finalPort);
                                    sender.sendMessage(Formatter.headThen("正在连接小明"));
                                    bukkitSocket.connect();
                                    success = true;
                                } catch (Exception exception) {
                                    success = false;
                                }
                                if (success) {
                                    sender.sendMessage(Formatter.headThen(Formatter.green("成功连接到小明")));
                                } else {
                                    sender.sendMessage(Formatter.headThen(Formatter.red("无法连接到小明，可能是地址端口不正确，或小明尚未启动")));
                                }
                            });
                        }
                        break;
                    }
                    case "disconnect": {
                        if (strings.length != 2) {
                            readTheFuckManual(sender);
                            return true;
                        }
                        if (!requirePermission(sender, "xiaoming.link.disconnect")) {
                            return true;
                        }

                        if (bukkitSocket.test()) {
                            bukkitSocket.disconnect();
                            sender.sendMessage(Formatter.headThen(Formatter.green("成功断开与小明的连接")));
                        } else {
                            sender.sendMessage(Formatter.headThen(Formatter.yellow("服务器并未连接到小明")));
                        }
                        break;
                    }
                    case "test": {
                        if (strings.length != 2) {
                            readTheFuckManual(sender);
                            return true;
                        }
                        if (!requirePermission(sender, "xiaoming.link.test")) {
                            return true;
                        }

                        if (bukkitSocket.test()) {
                            sender.sendMessage(Formatter.headThen(Formatter.green("小明和服务器连接正常")));
                        } else {
                            sender.sendMessage(Formatter.headThen(Formatter.red("小明和服务器连接错误")));
                        }
                        break;
                    }
                    default:
                        readTheFuckManual(sender);
                        return true;
                }
                break;
            }

            // send <HEAD> <MESSAGE>
            case "send": {
                if (strings.length == 1) {
                    sender.sendMessage(Formatter.headThen(Formatter.red("发送的消息不能为空")));
                    return true;
                }
                if (!requireConnected(sender)) {
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Formatter.headThen(Formatter.red("只有真正的玩家才能发送信息")));
                    return true;
                }

                scheduler.runTaskAsynchronously(plugin, () -> {
                    runOrCatch(sender, () -> {
                        sender.sendMessage(Formatter.headThen(Formatter.gray("正在发送聊天消息")));
                        final ResultContent resultContent = bukkitSocket.sendPlayerChatMessage((Player) sender, ArgumentUtils.getReaminArgs(strings, 1), true);
                        if (resultContent.isSuccess() || !StringUtils.isEmpty(resultContent.getString())) {
                            sender.sendMessage(Formatter.headThen(Formatter.gray(resultContent.getDescription("消息发送"))));
                        } else {
                            sender.sendMessage(Formatter.headThen(Formatter.red("该消息似乎不属于任何频道")));
                        }
                    });
                });
                break;
            }

            // unbind
            case "unbind": {
                if (strings.length != 1) {
                    return false;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Formatter.headThen(Formatter.red("你搁这搁这呢")));
                    return true;
                }

                if (!requirePermission(sender, "xiaoming.user.unbind")) {
                    return true;
                }
                if (!requireConnected(sender)) {
                    return true;
                }

                scheduler.runTaskAsynchronously(plugin, () -> {
                    runOrCatch(sender, () -> {
                        sender.sendMessage(Formatter.headThen(bukkitSocket.unbind(sender.getName()).getDescription("解绑")));
                    });
                });
                break;
            }

            // bind QQ
            case "bind": {
                if (strings.length != 2) {
                    return false;
                }
                final String qqString = strings[1];

                if (!(sender instanceof Player)) {
                    sender.sendMessage(Formatter.headThen(Formatter.red("你搁这搁这呢")));
                    return true;
                }
                if (!qqString.matches("\\d+")) {
                    sender.sendMessage(Formatter.headThen(Formatter.red("「" + qqString + "」并不是一个 QQ 号")));
                    break;
                }

                if (!requirePermission(sender, "xiaoming.user.bind")) {
                    return true;
                }
                if (!requireConnected(sender)) {
                    return true;
                }

                final long code = Long.parseLong(qqString);
                scheduler.runTaskAsynchronously(plugin, () -> {
                    runOrCatch(sender, () -> {
                        final ResultContent resultContent = bukkitSocket.bind(sender.getName(), code);
                        sender.sendMessage(Formatter.headThen(resultContent.getDescription("绑定")));
                    });
                });
                break;
            }

            // reload
            case "reload": {
                if (strings.length != 1) {
                    return true;
                }
                // 因为插件关闭时会 cancel tasks，所以暂时用裸线程整
                // 太草了
                new Thread(() -> plugin.onReload(sender)).start();
                break;
            }

            // apply [command]
            case "apply": {
                if (strings.length <= 1) {
                    sender.sendMessage(Formatter.headThen(Formatter.red("申请执行的指令不能为空")));
                    return true;
                }
                if (!requirePermission(sender, "xiaoming.user.apply")) {
                    return true;
                }
                if (!requireConnected(sender)) {
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(Formatter.headThen(Formatter.gray("只有真正的玩家才能申请执行指令")));
                    return true;
                }

                final String command = ArgumentUtils.getReaminArgs(strings, 1);
                scheduler.runTaskAsynchronously(plugin, () -> {
                    runOrCatch(sender, () -> {
                        sender.sendMessage(Formatter.headThen(Formatter.gray("正在申请指令使用权限，请稍等")));
                        final ResultContent resultContent = bukkitSocket.apply(((Player) sender), command);
                        sender.sendMessage(Formatter.headThen(resultContent.getDescription("指令申请")));

                        if (resultContent.isSuccess()) {
                            scheduler.runTask(plugin, () -> {
                                final boolean isOp = sender.isOp();
                                try {
                                    sender.setOp(true);
                                    sender.recalculatePermissions();
                                    server.dispatchCommand(sender, command);
                                } catch (Exception exception) {
                                    sender.sendMessage(Formatter.headThen(Formatter.red("这个指令好像有一些问题呢")));
                                    controller.sendLater(PackType.MC_LOGGER, "为用户「" + sender.getName() + "」" +
                                            "执行申请的指令「" + command + "」时出现异常：" + exception);
                                } finally {
                                    sender.setOp(isOp);
                                    sender.recalculatePermissions();
                                }
                            });
                        }
                    });
                });
                break;
            }

            // help
            // info
            case "help": {
                if (strings.length != 1) {
                    return false;
                }
                if (!requirePermission(sender, "xiaoming.user.help")) {
                    return true;
                }

                sender.sendMessage(Formatter.headThen(HELP));
                break;
            }
            case "info": {
                if (strings.length != 1) {
                    return false;
                }
                sender.sendMessage(Formatter.headThen(XiaomingBukkitPlugin.INFO));
                break;
            }

            // tag
            // tag <add|remove> <TAG>
            case "tag": {
                if (!requireConnected(sender)) {
                    return true;
                }
                if (strings.length == 1) {
                    if (!requirePermission(sender, "minecraft.server.tag.list")) {
                        return false;
                    }

                    scheduler.runTaskAsynchronously(plugin, () -> {
                        runOrCatch(sender, () -> {
                            sender.sendMessage(Formatter.headThen(Formatter.gray("正在拉取服务器标记")));
                            final Set<String> serverTags = bukkitSocket.listServerTags();
                            if (serverTags.isEmpty()) {
                                sender.sendMessage(Formatter.headThen("本服务器没有任何 tag"));
                            } else {
                                sender.sendMessage(Formatter.headThen("本服务器的标记有：" +
                                        CollectionUtils.getSummary(serverTags, Formatter::green, "", "", Formatter.gray("、"))));
                            }
                        });
                    });
                    return true;
                } else {
                    final String operator = strings[1];
                    switch (operator) {
                        // tag add <TAG>
                        case "add": {
                            if (strings.length != 3) {
                                sender.sendMessage(Formatter.headThen(Formatter.red("添加本服务器的标记的正确格式为：/xm tag add <TAG>，且标记不能包含空格")));
                                return true;
                            }
                            if (!requirePermission(sender, "minecraft.server.tag.add")) {
                                return false;
                            }

                            final String tag = strings[2];
                            scheduler.runTaskAsynchronously(plugin, () -> {
                                runOrCatch(sender, () -> {
                                    sender.sendMessage(Formatter.headThen(Formatter.gray("正在请求增加服务器标记")));
                                    final ResultContent content = bukkitSocket.addServerTag(tag);
                                    sender.sendMessage(Formatter.headThen(content.getDescription("添加服务器标记")));
                                });
                            });
                            break;
                        }
                        // tag remove <TAG>
                        case "remove": {
                            if (strings.length != 3) {
                                sender.sendMessage(Formatter.headThen(Formatter.red("删除本服务器的标记的正确格式为：/xm tag remove <TAG>，且标记不能包含空格")));
                                return true;
                            }
                            if (!requirePermission(sender, "minecraft.server.tag.remove")) {
                                return false;
                            }

                            final String tag = strings[2];
                            scheduler.runTaskAsynchronously(plugin, () -> {
                                runOrCatch(sender, () -> {
                                    sender.sendMessage(Formatter.headThen(Formatter.gray("正在请求删除服务器标记")));
                                    final ResultContent content = bukkitSocket.removeServerTag(tag);
                                    sender.sendMessage(Formatter.headThen(content.getDescription("删除服务器标记")));
                                });
                            });
                            break;
                        }
                        default:
                            sender.sendMessage(Formatter.headThen(Formatter.red("行为参数 " + operator + " 错误，只能是 add 或 remove")));
                            return true;
                    }
                }
                break;
            }

            // world
            // world <NAME>
            // world <NAME> <add|remove> <TAG>
            // world ~ <add|remove> <TAG>
            case "world": {
                if (!requireConnected(sender)) {
                    return true;
                }
                switch (strings.length) {
                    case 1: {
                        if (!requirePermission(sender, "minecraft.world.list")) {
                            return true;
                        }

                        scheduler.runTaskAsynchronously(plugin, () -> {
                            runOrCatch(sender, () -> {
                                final Set<String> worldNames = bukkitSocket.listWorldNames();
                                if (CollectionUtils.isEmpty(worldNames)) {
                                    sender.sendMessage(Formatter.headThen("小明没有缓存任何世界名，赶快用 /xm flush world 刷新缓存吧"));
                                } else {
                                    sender.sendMessage(Formatter.headThen("小明记录的本服务器的世界有：" +
                                            CollectionUtils.getSummary(worldNames, Formatter::green, "", "", Formatter.gray("、"))));
                                }
                            });
                        });
                        break;
                    }
                    case 2: {
                        if (!requirePermission(sender, "minecraft.world.look")) {
                            return true;
                        }
                        final String worldName = requireWorldName(sender, strings[1]);
                        if (Objects.isNull(worldName)) {
                            return true;
                        }

                        scheduler.runTaskAsynchronously(plugin, () -> {
                            runOrCatch(sender, () -> {
                                final Set<String> tags = bukkitSocket.listWorldTags(worldName);
                                if (Objects.isNull(tags)) {
                                    sender.sendMessage(Formatter.headThen("小明处还没有缓存这个世界。" +
                                            "如果你坚信存在这个世界，请尝试使用 " + Formatter.yellow("/xm flush world") + " 刷新小明的相关记录"));
                                } else {
                                    if (tags.isEmpty()) {
                                        sender.sendMessage(Formatter.headThen("世界「" + worldName + "」没有任何标记哦"));
                                    } else {
                                        sender.sendMessage(Formatter.headThen("该世界的标记有：" + CollectionUtils.getSummary(tags, Formatter::green, "", "", Formatter.gray("、"))));
                                    }
                                }
                            });
                        });
                        break;
                    }
                    case 3: {
                        sender.sendMessage(Formatter.headThen(Formatter.red("缺少必要的行为参数")));
                        break;
                    }
                    case 4: {
                        final String worldName = requireWorldName(sender, strings[1]);
                        if (Objects.isNull(worldName)) {
                            return true;
                        }
                        final String tag = strings[3];

                        final String operator = strings[2];
                        switch (operator) {
                            case "add": {
                                if (!requirePermission(sender, "minecraft.world.add")) {
                                    return true;
                                }
                                scheduler.runTaskAsynchronously(plugin, () -> {
                                    runOrCatch(sender, () -> {
                                        sender.sendMessage(Formatter.headThen(bukkitSocket.addWorldTag(worldName, tag).getDescription("世界标记增加")));
                                    });
                                });
                                break;
                            }
                            case "remove": {
                                if (!requirePermission(sender, "minecraft.world.remove")) {
                                    return true;
                                }
                                scheduler.runTaskAsynchronously(plugin, () -> {
                                    runOrCatch(sender, () -> {
                                        sender.sendMessage(Formatter.headThen(bukkitSocket.removeWorldTag(worldName, tag).getDescription("世界标记删除")));
                                    });
                                });
                                break;
                            }
                            default:
                                sender.sendMessage(Formatter.headThen(Formatter.red("参数错误：对 tag 的行为操作只能为 add 或 remove")));
                        }
                        break;
                    }
                    default:
                        return false;
                }
                break;
            }

            // flush
            // flush world
            case "flush": {
                if (!requireConnected(sender)) {
                    return true;
                }
                if (strings.length == 1) {
                    if (!requirePermission(sender, "minecraft.flush")) {
                        return true;
                    }
                    bukkitSocket.flush();
                    sender.sendMessage(Formatter.headThen("所有缓存刷新成功"));
                } else if (strings.length == 2) {
                    final String target = strings[1];
                    switch (target) {
                        case "world": {
                            if (!requirePermission(sender, "minecraft.flush.world")) {
                                return true;
                            }
                            bukkitSocket.flushWorld();
                            sender.sendMessage(Formatter.headThen("世界缓存刷新成功"));
                            break;
                        }
                        default:
                            sender.sendMessage(Formatter.headThen(Formatter.red("刷新对象只能为 world")));
                            return true;
                    }
                } else {
                    return false;
                }
                break;
            }

            // identify server
            // identify xiaoming
            // identify server [SETTO]
            // identify xiaoming [SETTO]
            case "identify": {
                if (!requirePermission(sender, "xiaoming.identify")) {
                    return true;
                }
                if (strings.length == 1) {
                    sender.sendMessage(Formatter.headThen(Formatter.aroundByGrayBracket(Formatter.red("凭据信息")) + "\n" +
                            Formatter.red("本服原始凭据") + Formatter.gray("：") + Formatter.yellow(configuration.getServerIdentify())) + "\n" +
                            Formatter.red("小明原始凭据") + Formatter.gray("：") + Formatter.red(configuration.getXiaomingIdentify()));
                    return true;
                }

                switch (strings[1]) {
                    case "server": {
                        switch (strings.length) {
                            case 2: {
                                final String serverIdentify = configuration.getServerIdentify();
                                sender.sendMessage(Formatter.headThen("本服的原始凭据为：" + Formatter.yellow(serverIdentify)));
                                break;
                            }
                            case 3: {
                                configuration.setServerIdentify(strings[2]);
                                sender.sendMessage(Formatter.headThen("原始凭据已被修改，将在下一次连接小明时生效"));
                                break;
                            }
                            default:
                                return false;
                        }
                        break;
                    }
                    case "xiaoming": {
                        switch (strings.length) {
                            case 2: {
                                final String xiaomingIdentify = configuration.getXiaomingIdentify();
                                sender.sendMessage(Formatter.headThen("本服记录的小明原始凭据为：" + Formatter.yellow(xiaomingIdentify)));
                                break;
                            }
                            case 3: {
                                configuration.setXiaomingIdentify(strings[2]);
                                sender.sendMessage(Formatter.headThen("小明原始凭据已被修改，将在下一次连接小明时生效"));
                                break;
                            }
                            default:
                                return false;
                        }
                        break;
                    }
                    default:
                        sender.sendMessage(Formatter.headThen(Formatter.red("目标错误，只能为 xiaoming 或 server")));
                        return true;
                }
                break;
            }

            // execute group <GROUP> <COMMAND>
            // execute temp <TEMP> <COMMAND>
            // execute private <COMMAND>
            case "execute": {
                if (strings.length == 1) {
                    readTheFuckManual(sender);
                    return true;
                }
                if (!requireConnected(sender)) {
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(Formatter.headThen(Formatter.gray("只有真正的玩家才能执行小明指令")));
                    return true;
                }

                final String position = strings[1];
                switch (position) {
                    case "group": {
                        if (!requirePermission(sender, "xiaoming.user.execute.group")) {
                            return true;
                        }

                        final String command = ArgumentUtils.getReaminArgs(strings, 3);
                        if (StringUtils.isEmpty(command)) {
                            sender.sendMessage(Formatter.headThen(Formatter.red("需要执行的指令内容不能为空")));
                            return true;
                        }

                        final long group;
                        final Long groupObject = requireLong(sender, "群号", strings[2]);
                        if (Objects.isNull(groupObject)) {
                            return true;
                        }
                        group = groupObject;

                        scheduler.runTaskAsynchronously(plugin, () -> {
                            runOrCatch(sender, () -> {
                                sender.sendMessage(Formatter.headThen(Formatter.gray("正在群聊中执行小明指令")));
                                sender.sendMessage(Formatter.headThen(bukkitSocket.executeInGroup(group, sender.getName(), command).getDescription(" 小明指令执行")));
                            });
                        });
                        break;
                    }
                    case "temp": {
                        if (!requirePermission(sender, "xiaoming.user.execute.temp")) {
                            return true;
                        }

                        final String command = ArgumentUtils.getReaminArgs(strings, 3);
                        if (StringUtils.isEmpty(command)) {
                            sender.sendMessage(Formatter.headThen(Formatter.red("需要执行的指令内容不能为空")));
                            return true;
                        }

                        final long group;
                        final Long groupObject = requireLong(sender, "群号", strings[2]);
                        if (Objects.isNull(groupObject)) {
                            return true;
                        }
                        group = groupObject;

                        scheduler.runTaskAsynchronously(plugin, () -> {
                            runOrCatch(sender, () -> {
                                sender.sendMessage(Formatter.headThen(Formatter.gray("正在群聊的临时会话中执行小明指令")));
                                sender.sendMessage(Formatter.headThen(bukkitSocket.executeInTemp(group, sender.getName(), command).getDescription("小明指令执行")));
                            });
                        });
                        break;
                    }
                    case "private": {
                        if (!requirePermission(sender, "xiaoming.user.execute.private")) {
                            return true;
                        }

                        final String command = ArgumentUtils.getReaminArgs(strings, 2);
                        if (StringUtils.isEmpty(command)) {
                            sender.sendMessage(Formatter.headThen(Formatter.red("需要执行的指令内容不能为空")));
                            return true;
                        }

                        scheduler.runTaskAsynchronously(plugin, () -> {
                            runOrCatch(sender, () -> {
                                sender.sendMessage(Formatter.headThen(Formatter.gray("正在私聊中执行小明指令")));
                                sender.sendMessage(Formatter.headThen(bukkitSocket.executeInPrivate(sender.getName(), command).getDescription("小明指令执行")));
                            });
                        });
                        break;
                    }
                    default:
                        sender.sendMessage(Formatter.headThen(Formatter.red("执行指令的场合只能是 group [群号]、temp [群号] 或 private")));
                        return true;
                }
                break;
            }

            // debug
            case "debug": {
                if (strings.length != 1) {
                    readTheFuckManual(sender);
                    return true;
                }
                if (!requirePermission(sender, "xiaoming.debug")) {
                    return true;
                }

                configuration.setDebug(!configuration.isDebug());
                if (bukkitSocket.isConnected()) {
                    bukkitSocket.getController().setDebug(configuration.isDebug());
                }
                if (configuration.isDebug()) {
                    sender.sendMessage(Formatter.headThen(Formatter.green("成功启动调试模式")));
                } else {
                    sender.sendMessage(Formatter.headThen(Formatter.yellow("成功关闭调试模式")));
                }
                break;
            }
            default:
                readTheFuckManual(sender);
                return true;
        }
        return true;
    }
}