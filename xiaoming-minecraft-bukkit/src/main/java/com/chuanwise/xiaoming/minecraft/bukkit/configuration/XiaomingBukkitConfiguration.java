package com.chuanwise.xiaoming.minecraft.bukkit.configuration;

import com.chuanwise.xiaoming.minecraft.socket.SocketConfiguration;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class XiaomingBukkitConfiguration extends XiaomingFileConfiguration {
    String address = "localHost";
    int port = 23333;

    public static final String DEFAULT_SERVER_IDENTIFY = "xiaoming-minecraft-plugin-client-default-identify";
    String serverIdentify = DEFAULT_SERVER_IDENTIFY;

    public static final String DEFAULT_XIAOMING_IDENTIFY = "xiaoming-minecraft-plugin-server-default-identify";
    String xiaomingIdentify = DEFAULT_XIAOMING_IDENTIFY;

    boolean autoReconnect = true;
    long reconnectDelay = TimeUnit.SECONDS.toMillis(20);
    boolean autoConnect = true;

    SocketConfiguration socketConfiguration = new SocketConfiguration();
}
