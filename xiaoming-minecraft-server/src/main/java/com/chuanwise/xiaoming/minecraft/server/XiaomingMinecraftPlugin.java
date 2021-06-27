package com.chuanwise.xiaoming.minecraft.server;

import com.chuanwise.xiaoming.core.plugin.XiaomingPluginImpl;
import com.chuanwise.xiaoming.minecraft.server.configuration.ConnectHistory;
import com.chuanwise.xiaoming.minecraft.server.configuration.ServerConfiguration;
import com.chuanwise.xiaoming.minecraft.server.data.ServerPlayerData;
import com.chuanwise.xiaoming.minecraft.server.interactor.ServerCommandInteractor;
import com.chuanwise.xiaoming.minecraft.server.interactor.ServerMessageInteractor;
import com.chuanwise.xiaoming.minecraft.server.server.XiaomingMinecraftServer;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.Random;

@Getter
public class XiaomingMinecraftPlugin extends XiaomingPluginImpl {
    public static XiaomingMinecraftPlugin INSTANCE;

    ServerConfiguration configuration;
    XiaomingMinecraftServer minecraftServer;
    ServerPlayerData playerData;

    ServerMessageInteractor serverMessageInteractor;
    ConnectHistory connectHistory;

    @Override
    public void onLoad() {
        INSTANCE = this;
        getDataFolder().mkdirs();
    }

    @Override
    public void onEnable() {
        configuration = loadConfigurationOrProduce(ServerConfiguration.class, ServerConfiguration::new);
        minecraftServer = new XiaomingMinecraftServer(this);
        playerData = loadFileOrProduce(ServerPlayerData.class, new File(getDataFolder(), "players.json"), ServerPlayerData::new);
        serverMessageInteractor = new ServerMessageInteractor(this, configuration, playerData, minecraftServer);
        connectHistory = loadFileOrProduce(ConnectHistory.class, new File(getDataFolder(), "histories.json"), ConnectHistory::new);

        // 启动服务器
        if (configuration.isAutoEnableServer()) {
            try {
                minecraftServer.start();
                getLog().info("成功在端口 " + configuration.getPort() + " 上启动服务器");
            } catch (IOException exception) {
                getLog().error("启动服务器失败", exception);
                sendMessageToLog("无法在端口 " + configuration.getPort() + " 上启动服务器以连接 Minecraft 服务器");
            }
        }

        getXiaomingBot().getInteractorManager().register(new ServerCommandInteractor(this), this);
        getXiaomingBot().getInteractorManager().register(serverMessageInteractor, this);
//        getXiaomingBot().getInteractorManager().getInteractors(this).remove(serverMessageInteractor);
    }

    public void sendMessageToLog(String message) {
        getXiaomingBot().getResponseGroupManager().sendMessageToTaggedGroup(configuration.getLogGroupTag(), message);
    }
}