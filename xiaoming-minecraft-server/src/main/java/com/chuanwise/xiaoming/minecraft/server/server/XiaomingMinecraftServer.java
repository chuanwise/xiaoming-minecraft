package com.chuanwise.xiaoming.minecraft.server.server;

import com.chuanwise.xiaoming.minecraft.server.XiaomingMinecraftPlugin;
import com.chuanwise.xiaoming.minecraft.server.configuration.ServerConfiguration;
import com.chuanwise.xiaoming.minecraft.thread.StopableRunnable;
import io.ktor.util.collections.ConcurrentList;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class XiaomingMinecraftServer {
    volatile ServerSocket serverSocket;
    @Setter
    int port;

    final Logger logger;
    final ExecutorService threadPool = Executors.newCachedThreadPool();

    final List<BukkitPluginReceptionist> receptionists = new ConcurrentList<>();
    final XiaomingMinecraftPlugin plugin;
    final ServerConfiguration configuration;

    volatile boolean running = false;

    public XiaomingMinecraftServer(XiaomingMinecraftPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLog();
        this.configuration = plugin.getConfiguration();
        this.port = configuration.getPort();
    }

    protected Socket requireConfiguredSocket(Socket socket) throws IOException {
        if (!socket.getKeepAlive()) {
            socket.setKeepAlive(true);
        }
        if (!socket.getOOBInline()) {
            socket.setOOBInline(true);
        }
        return socket;
    }

    public BukkitPluginReceptionist forName(String name) {
        for (BukkitPluginReceptionist receptionist : receptionists) {
            if (Objects.equals(receptionist.getDetail().getName(), name)) {
                return receptionist;
            }
        }
        return null;
    }

    public Set<BukkitPluginReceptionist> forTag(String tag) {
        final Set<BukkitPluginReceptionist> result = new HashSet<>();
        for (BukkitPluginReceptionist receptionist : receptionists) {
            if (receptionist.getDetail().hasTag(tag)) {
                result.add(receptionist);
            }
        }
        return result;
    }

    public void restart(long delay) throws IOException, InterruptedException {
        shutdown();
        Thread.sleep(delay);
        start();
    }

    public void shutdown() throws IOException {
        running = false;
        serverSocket.close();
        serverSocket = null;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);

        threadPool.execute(() -> {
            running = true;
            while (running) {
                try {
                    final Socket socket = requireConfiguredSocket(serverSocket.accept());
                    logger.info("接入新的服务器，正在准备");
                    threadPool.execute(new BukkitPluginReceptionist(plugin, socket));
                } catch (IOException exception) {
                    logger.error("与新的 Minecraft 服务器插件客户端连接时出现异常", exception);
                }
            }
            running = false;
        });
    }
}
