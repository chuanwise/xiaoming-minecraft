package com.chuanwise.xiaoming.minecraft.socket;

import com.chuanwise.xiaoming.minecraft.pack.Pack;
import com.chuanwise.xiaoming.minecraft.pack.PackType;
import com.chuanwise.xiaoming.minecraft.pack.content.IdContent;
import com.chuanwise.xiaoming.minecraft.pack.content.ResultContent;
import com.chuanwise.xiaoming.minecraft.thread.StopableRunnable;
import com.chuanwise.xiaoming.minecraft.util.ByteUtils;
import com.chuanwise.xiaoming.minecraft.util.TimeUtils;
import lombok.Data;
import org.slf4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Data
public class SocketController implements StopableRunnable {
    SocketConfiguration socketConfiguration = new SocketConfiguration();

    final Socket socket;
    final OutputStream outputStream;
    final InputStream inputStream;

    final Logger logger;

    final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    Consumer<Throwable> onThrowable = Throwable::printStackTrace;
    Consumer<Pack> onUnknownPack;
    Consumer<Pack> onReceivePack;
    Consumer<Pack> onSendPack;
    Runnable onNormalExit;
    Runnable onFinally;

    final List<Pack> recentPacks = new CopyOnWriteArrayList<>();
    final Queue<Pack> sendPacks = new ConcurrentLinkedQueue<>();

    final Map<PackType, List<Consumer<Pack>>> listeners = new ConcurrentHashMap<>();

    volatile boolean running;
    volatile boolean debug = false;

    volatile Throwable throwable;

    volatile long lastReceiveTime = System.currentTimeMillis();

    final boolean shouldSendHeartbeat;

    public SocketController(Socket socket, Logger logger, boolean shouldSendHeartbeat) throws IOException {
        this.socket = socket;
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
        this.logger = logger;
        this.shouldSendHeartbeat = shouldSendHeartbeat;

        socket.setKeepAlive(true);
        socket.setOOBInline(true);

        flushAsConfiguration();
    }

    public void setSocketConfiguration(SocketConfiguration socketConfiguration) throws IOException {
        this.socketConfiguration = socketConfiguration;
        flushAsConfiguration();
    }

    protected void flushAsConfiguration() throws IOException {
        socket.setSoTimeout(((int) TimeUnit.MILLISECONDS.toSeconds(socketConfiguration.getTimeout())));
    }

    public void register(PackType type, Consumer<Pack> consumer) {
        List<Consumer<Pack>> consumers = listeners.get(type);
        if (Objects.isNull(consumers)) {
            consumers = new LinkedList<>();
            listeners.put(type, consumers);
        }

        consumers.add(consumer);
    }

    public void onPack(Pack pack) {
        final List<Consumer<Pack>> consumers = listeners.get(pack.getType());
        if (Objects.isNull(consumers) || consumers.isEmpty()) {
            runIfNonNull(onUnknownPack, pack);
        } else {
            consumers.forEach(consumer -> runIfNonNull(consumer, pack));
        }
    }

    public void send(Pack pack) throws IOException {
        synchronized (outputStream) {
            final String serializedPack = pack.serialize();
            final byte[] bytes = serializedPack.getBytes(socketConfiguration.encode);

            outputStream.write(ByteUtils.intToByteArray(bytes.length));
            outputStream.flush();

            outputStream.write(bytes);
            outputStream.flush();

            runIfNonNull(onSendPack, pack);
            logIfDebug("send << " + pack);
        }
    }

    public void send(PackType type, Object content) throws IOException {
        send(new Pack(type, content));
    }

    public void send(PackType type) throws IOException {
        send(new Pack(type));
    }

    public void sendLater(Pack pack) {
        synchronized (sendPacks) {
            sendPacks.add(pack);
        }
    }

    public void sendLater(PackType type) {
        sendLater(new Pack(type));
    }

    public void sendLater(PackType type, Object content) {
        sendLater(new Pack(type, content));
    }

    public Pack nextPack(long timeout) throws IOException {
        final long timeoutTime = System.currentTimeMillis() + socketConfiguration.timeout;
        synchronized (recentPacks) {
            try {
                recentPacks.wait(socketConfiguration.timeout);
            } catch (InterruptedException exception) {
            }
        }

        if (System.currentTimeMillis() > timeoutTime || recentPacks.isEmpty()) {
            throw new SocketTimeoutException();
        } else {
            return recentPacks.get(recentPacks.size() - 1);
        }
    }

    public Pack nextPack(PackType type) throws IOException {
        return nextPack(pack -> pack.getType() == type);
    }

    public Pack nextPack(Predicate<Pack> predicate) throws IOException {
        Pack pack = null;
        final long latestTime = System.currentTimeMillis() + socketConfiguration.timeout;
        long remainTimeout;
        do {
            remainTimeout = latestTime - System.currentTimeMillis();
            if (remainTimeout < 0) {
                throw new SocketTimeoutException();
            }
            pack = nextPack(remainTimeout);
        } while (!predicate.test(pack));
        return pack;
    }

    public IdContent nextContent(PackType type, int id) throws IOException {
        final Pack nextPack = nextPack(pack -> {
            return pack.getType() == type && pack.getContent(IdContent.class).getRequestId() == id;
        });
        return nextPack.getContent(IdContent.class);
    }

    public ResultContent nextResult(PackType type, int id) throws IOException {
        final Pack nextPack = nextPack(pack -> {
            return pack.getType() == type && pack.getContent(IdContent.class).getRequestId() == id;
        });
        return nextPack.getContent(IdContent.class).getContent(ResultContent.class);
    }

    public Pack nextPack() throws IOException {
        return nextPack(socketConfiguration.timeout);
    }

    public Pack nextPack(Collection<PackType> definations) throws IOException {
        return nextPack(pack -> definations.contains(pack.getType()));
    }

    protected Pack receivePack() throws IOException {
        synchronized (inputStream) {
            final byte[] sizeBuffer = new byte[4];
            inputStream.read(sizeBuffer);
            final int packSize = ByteUtils.byteArrayToInt(sizeBuffer);
            final byte[] bytes = new byte[packSize];

            int len = 0;
            int receivedSize = 0;
            try {
                while (inputStream.available() < packSize) {
                }
                inputStream.read(bytes);
            } catch (SocketTimeoutException exception) {
                logger.error("接受包超时，将丢弃未接收的半个包（" +
                        "包总大小：" + packSize + "，" +
                        "已接收 " + receivedSize + "，占 " + (((double) receivedSize / packSize) * 100) + "%）");
                return null;
            }

            final String serializedPack = new String(bytes, socketConfiguration.decode);

            boolean deserializeSuccessfully;
            Pack pack = null;
            try {
                pack = Pack.deserialize(serializedPack);
                deserializeSuccessfully = Objects.nonNull(pack);
            } catch (Throwable throwable) {
                deserializeSuccessfully = false;
            }

            if (deserializeSuccessfully) {
                synchronized (recentPacks) {
                    recentPacks.add(pack);
                    recentPacks.notifyAll();
                }
                runIfNonNull(onReceivePack, pack);
                logIfDebug("receive >> " + pack);
            } else {
                logger.error("解析包出现错误（收到的内容为：" + serializedPack + "），可能是通讯协议错误、编码错误或网络问题", throwable);
            }
            return pack;
        }
    }

    public boolean test() {
        try {
            send(Pack.HEARTBEAT_REQUEST_PACK);
            return true;
        } catch (IOException exception) {
            stopCausedBy(exception);
            return false;
        }
    }

    public void logIfDebug(String message) {
        if (debug) {
            logger.info(message);
        }
    }

    public void logIfDebug(String message, Throwable throwable) {
        if (debug) {
            logger.error(message, throwable);
        }
    }

    @Override
    public void run() {
        running = true;

        // 心跳发送线程
        if (shouldSendHeartbeat) {
            threadPool.execute(() -> {
                try {
                    while (running) {
                        if (System.currentTimeMillis() - lastReceiveTime > socketConfiguration.heartbeatPeriod) {
                            send(Pack.HEARTBEAT_REQUEST_PACK);
                        } else {
                            logIfDebug("心跳发送：最近收包时间距今为" + TimeUtils.toTimeString(System.currentTimeMillis() - lastReceiveTime) + "，小于要求时间，不发送心跳包");
                        }
                        Thread.sleep(socketConfiguration.heartbeatPeriod);
                    }
                } catch (Throwable throwable) {
                    logIfDebug("心跳发送出现异常", throwable);
                    stop();
                }
            });
        } else {
            threadPool.execute(() -> {
                try {
                    while (running) {
                        int tryTime = 0;
                        for (; tryTime < socketConfiguration.maxTryTimes && System.currentTimeMillis() - lastReceiveTime > socketConfiguration.maxNoResponseTime;
                             tryTime++) {
                            send(Pack.HEARTBEAT_REQUEST_PACK);
                            Thread.sleep(socketConfiguration.tryPeriod);
                        }
                        if (tryTime == socketConfiguration.maxTryTimes) {
                            logger.error("对方长时间未应答，已断开连接");
                            stop();
                            break;
                        }
                        Thread.sleep(socketConfiguration.heartbeatPeriod);
                    }
                } catch (Throwable throwable) {
                    logIfDebug("看门狗出现异常", throwable);
                    stop();
                }
            });
        }

        // 添加一个自动清除近期包缓存的线程
        threadPool.execute(() -> {
            try {
                while (running) {
                    Thread.sleep(socketConfiguration.checkRecentPackPeriod);
                    synchronized (recentPacks) {
                        while (recentPacks.size() > socketConfiguration.maxRecentPackSize) {
                            recentPacks.remove(0);
                        }
                    }
                }
            } catch (Throwable throwable) {
                logIfDebug("自动清包出现异常", throwable);
                stop();
            }
        });

        // 添加一个自动发包线程
        threadPool.execute(() -> {
            try {
                while (running) {
                    Thread.sleep(socketConfiguration.sendPackPeriod);
                    synchronized (sendPacks) {
                        while (!sendPacks.isEmpty()) {
                            send(sendPacks.poll());
                        }
                    }
                }
            } catch (Throwable throwable) {
                logIfDebug("自动发包出现异常", throwable);
                stop();
            }
        });

        // 自动收包线程
        threadPool.execute(() -> {
            try {
                while (running) {
                    Thread.sleep(socketConfiguration.receivePackPeriod);
                    while (inputStream.available() > 4) {
                        final Pack pack = receivePack();
                        if (Objects.nonNull(pack)) {
                            if (pack.getType() == PackType.HEARTBEAT_REQUEST) {
                                send(Pack.HEARTBEAT_ALIVE_PACK);
                            } else {
                                threadPool.execute(() -> onPack(pack));
                            }
                            lastReceiveTime = System.currentTimeMillis();
                        }
                    }
                }
            } catch (Throwable throwable) {
                logIfDebug("自动收包出现异常", throwable);
                stop();
            }
        });

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException exception) {
            } finally {
                logIfDebug("连接即将断开");
                if (Objects.nonNull(throwable)) {
                    runIfNonNull(onThrowable, throwable);
                } else {
                    runIfNonNull(onNormalExit);
                }

                runIfNonNull(onFinally);
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        try {
            socket.close();
        } catch (IOException exception) {
        }
        synchronized (this) {
            notifyAll();
        }
    }

    public void stopCausedBy(Throwable throwable) {
        this.throwable = throwable;
        logger.error("连接因异常断开：" + throwable);
        runIfNonNull(onThrowable, throwable);
        stop();
    }

    protected void runIfNonNull(Runnable runnable) {
        if (Objects.nonNull(runnable)) {
            runnable.run();
        }
    }

    protected <T> void runIfNonNull(Consumer<T> consumer, T argument) {
        if (Objects.nonNull(consumer)) {
            consumer.accept(argument);
        }
    }
}
