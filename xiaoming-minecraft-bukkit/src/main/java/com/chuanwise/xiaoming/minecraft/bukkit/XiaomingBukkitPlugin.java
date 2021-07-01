package com.chuanwise.xiaoming.minecraft.bukkit;

import com.chuanwise.xiaoming.minecraft.bukkit.command.executor.XiaomingCommandExecutor;
import com.chuanwise.xiaoming.minecraft.bukkit.configuration.XiaomingBukkitConfiguration;
import com.chuanwise.xiaoming.minecraft.bukkit.configuration.XiaomingFileConfiguration;
import com.chuanwise.xiaoming.minecraft.util.Formatter;
import com.chuanwise.xiaoming.minecraft.bukkit.listener.XiaomingListener;
import com.chuanwise.xiaoming.minecraft.bukkit.socket.BukkitSocket;
import com.chuanwise.xiaoming.minecraft.util.ArgumentUtils;
import com.chuanwise.xiaoming.minecraft.util.TimeUtils;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Getter
public class XiaomingBukkitPlugin extends JavaPlugin {
    public static XiaomingBukkitPlugin INSTANCE;

    XiaomingBukkitConfiguration configuration;
    BukkitSocket bukkitSocket;
    XiaomingCommandExecutor commandExecutor;

    public static final String VERSION = "1.1";
    public static final String AUTHOR = "Chuanwise";
    public static final String GITHUB = "https://github.com/Chuanwise/xiaoming-bot";
    public static final String GROUP = "1028959718";
    public static String INFO = "小明插件详情\n" +
            Formatter.green("插件名") + Formatter.gray("：") + "XiaomingMinecraftBukkit\n" +
            Formatter.green("作者") + Formatter.gray("：") + AUTHOR + "\n" +
            Formatter.green("版本") + Formatter.gray("：") + VERSION + "\n" +
            Formatter.green("GITHUB") + Formatter.gray("：") + GITHUB + "\n" +
            Formatter.yellow("QQ 群") + Formatter.gray("：") + Formatter.white(GROUP);

    @Override
    public void onLoad() {
        final Logger logger = getLogger();
        getServer().getConsoleSender().sendMessage("正在载入小明\n" +
                Formatter.green("=================================================") + "\n" +
                " __   __ _                __  __  _               \n" +
                " \\ \\ / /(_)              |  \\/  |(_)              \n" +
                "  \\ V /  _   __ _   ___  | \\  / | _  _ __    __ _ \n" +
                "   > <  | | / _` | / _ \\ | |\\/| || || '_ \\  / _` |\n" +
                "  / . \\ | || (_| || (_) || |  | || || | | || (_| |\n" +
                " /_/ \\_\\|_| \\__,_| \\___/ |_|  |_||_||_| |_| \\__, |\n" +
                "                                             __/ |\n" +
                "                                            |___/ \n" +
                "                                        " + Formatter.blue("@") + Formatter.yellow(AUTHOR) + "\n" +
                INFO + "\n" +
                Formatter.green("=================================================")
        );

        INSTANCE = this;
        getDataFolder().mkdirs();

        logger.info("正在加载配置文件");
        configuration = XiaomingFileConfiguration.loadOrProduce(new File(getDataFolder(), "configurations.yml"), XiaomingBukkitConfiguration.class, XiaomingBukkitConfiguration::new);

        logger.info("正在准备服务器");
        bukkitSocket = new BukkitSocket(this);
        commandExecutor = new XiaomingCommandExecutor(this);
    }

    @Override
    public void onEnable() {
        // 连接服务器
        if (configuration.isAutoConnect()) {
            // 连接服务器线程
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    getLogger().info("正在通过地址：" + bukkitSocket.getAddress() + "，端口：" + bukkitSocket.getPort() + " 连接小明");
                    bukkitSocket.connect();
                } catch (ConnectException exception) {
                    getLogger().severe("无法连接到小明，可能是地址端口不正确，或小明尚未启动");
                } catch (IOException exception) {
                    getLogger().severe("连接小明时出现异常");
                    exception.printStackTrace();
                }
//                if (!bukkitSocket.test() && configuration.isAutoReconnect()) {
//                    getLogger().info("将在" + TimeUtils.toTimeString(configuration.getReconnectDelay()) + "后自动重连");
//                }
//                try {
//                    do {
//                        if (!bukkitSocket.isConnected()) {
//
//                        }
//                        Thread.sleep(configuration.getReconnectDelay());
//                    } while (configuration.isAutoReconnect());
//                } catch (InterruptedException exception) {
//                    getLogger().severe("连接任务被打断");
//                }
            });
        }

        getServer().getPluginManager().registerEvents(new XiaomingListener(this), this);

        getCommand("xiaoming").setExecutor(commandExecutor);

        getCommand("xiaomingconfirm").setExecutor((sender, command, s, strings) -> {
            if (strings.length != 0) {
                sender.sendMessage(Formatter.headThen(Formatter.red("确认请求不需要带任何参数")));
                return true;
            }
            if (commandExecutor.onUserConfirm(sender.getName())) {
                sender.sendMessage(Formatter.headThen(Formatter.green("成功确认了该请求")));
            } else {
                sender.sendMessage(Formatter.headThen(Formatter.red("你没有等待确认的请求")));
            }
            return true;
        });
        getCommand("xiaomingcancel").setExecutor((sender, command, s, strings) -> {
            if (strings.length != 0) {
                sender.sendMessage(Formatter.headThen(Formatter.red("取消请求不需要带任何参数")));
                return true;
            }
            if (commandExecutor.onUserCancel(sender.getName())) {
                sender.sendMessage(Formatter.headThen(Formatter.green("成功取消了该请求")));
            } else {
                sender.sendMessage(Formatter.headThen(Formatter.red("你没有等待取消的请求")));
            }
            return true;
        });

        getCommand("xiaomingaccept").setExecutor((sender, command, s, strings) -> {
            if (strings.length != 0) {
                sender.sendMessage(Formatter.headThen(Formatter.red("同意请求不需要带任何参数")));
                return true;
            }
            if (commandExecutor.onUserAccept(sender.getName())) {
                sender.sendMessage(Formatter.headThen(Formatter.green("成功同意了该请求")));
            } else {
                sender.sendMessage(Formatter.headThen(Formatter.red("你没有等待处理的请求")));
            }
            return true;
        });
        getCommand("xiaomingdeny").setExecutor((sender, command, s, strings) -> {
            if (strings.length != 0) {
                sender.sendMessage(Formatter.headThen(Formatter.red("拒绝请求不需要带任何参数")));
                return true;
            }
            if (commandExecutor.onUserDeny(sender.getName())) {
                sender.sendMessage(Formatter.headThen(Formatter.green("成功拒绝了该请求")));
            } else {
                sender.sendMessage(Formatter.headThen(Formatter.red("你没有等待处理的请求")));
            }
            return true;
        });

        getCommand("xiaomingexecute").setExecutor((sender, command, s, strings) -> {
            return getServer().dispatchCommand(sender, "xiaoming execute " + ArgumentUtils.getReaminArgs(strings, 0));
        });
        getCommand("xiaomingsend").setExecutor((sender, command, s, strings) -> {
            return getServer().dispatchCommand(sender, "xiaoming send " + ArgumentUtils.getReaminArgs(strings, 0));
        });
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        final ConsoleCommandSender consoleSender = getServer().getConsoleSender();

        consoleSender.sendMessage(
                Formatter.green("=================================================") + "\n" +
                        "正在关闭小明插件");

        try {
            consoleSender.sendMessage("正在保存配置文件");
            configuration.save();
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        bukkitSocket.disconnect();
        consoleSender.sendMessage(Formatter.green("关闭完成"));

        consoleSender.sendMessage(
                INFO + "\n" +
                        Formatter.green("=================================================")
        );
    }

    public void onReload(CommandSender sender) {
        sender.sendMessage(Formatter.headThen("正在重载小明插件"));
        try {
            onDisable();
            sender.sendMessage(Formatter.headThen("请等待 5 秒后插件启动"));
            TimeUnit.SECONDS.sleep(5);
            onLoad();
            onEnable();
        } catch (Exception exception) {
            sender.sendMessage(Formatter.headThen(Formatter.red("重载出现异常，建议重启服务器。在控制台可获取详细信息")));
            exception.printStackTrace();
        }
        sender.sendMessage(Formatter.headThen(Formatter.green("重载完成")));
    }
}
