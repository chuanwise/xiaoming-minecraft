package com.chuanwise.xiaoming.minecraft.server.configuration;

import com.chuanwise.xiaoming.api.util.MD5Utils;
import com.chuanwise.xiaoming.core.preserve.JsonFilePreservable;
import com.chuanwise.xiaoming.minecraft.socket.SocketConfiguration;
import com.chuanwise.xiaoming.minecraft.util.PasswordHashUtils;
import lombok.Data;
import lombok.Getter;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Data
public class ServerConfiguration extends JsonFilePreservable {
    /**
     * 客户端的身份字符串，将加密后发送给客户端
     */
    String xiaomingIdentify = "xiaoming-minecraft-plugin-server-default-identify";

    String logGroupTag = "log";

    int port = 23333;

    boolean autoEnableServer = true;
    volatile boolean debug = false;

    boolean enableConnectLog = false;
    boolean enableDisconnectLog = false;
    boolean enableJoinLog = false;
    boolean enableExitLog = false;

    boolean alwaysLogFailConnection = false;

    Map<String, MinecraftServerDetail> serverDetails = new HashMap<>();

    String defaultTarget = "xiaoming-minecraft-plugin-client-default-identify";

    ChatSettings chatSettings = new ChatSettings();

    SocketConfiguration socketConfiguration = new SocketConfiguration();

    long applyCommandTimeout = TimeUnit.MINUTES.toMillis(3);

    long confirmTimeout = TimeUnit.MINUTES.toMillis(1);

    long executeXiaomingCommandTimeout = TimeUnit.SECONDS.toMillis(30);

    public MinecraftServerDetail forServerIdentify(String identify) {
        return serverDetails.get(identify);
    }

    public MinecraftServerDetail forServerName(String name) {
        for (MinecraftServerDetail configuration : serverDetails.values()) {
            if (Objects.equals(configuration.getName(), name)) {
                return configuration;
            }
        }
        return null;
    }

    public MinecraftServerDetail forEncryptedIdentify(String encryptedIdentify) throws NoSuchAlgorithmException, InvalidKeySpecException {
        for (MinecraftServerDetail detail : serverDetails.values()) {
            if (PasswordHashUtils.validatePassword(detail.getIdentify(), encryptedIdentify)) {
                return detail;
            }
        }
        return null;
    }

    public Set<MinecraftServerDetail> forServerTag(String tag) {
        Set<MinecraftServerDetail> result = new HashSet<>();
        serverDetails.values().forEach(detail -> {
            if (detail.hasTag(tag)) {
                result.add(detail);
            }
        });
        return result;
    }

    public MinecraftServerDetail addServer(String identify, String name) {
        final MinecraftServerDetail detail = new MinecraftServerDetail();
        detail.setIdentify(identify);
        detail.setName(name);
        detail.addTag(name);
        detail.addTag("recorded");
        serverDetails.put(name, detail);
        return detail;
    }
}