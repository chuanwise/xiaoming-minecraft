package com.chuanwise.xiaoming.minecraft.socket;

import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class SocketConfiguration {
    int maxRecentPackSize = 10;
    long checkRecentPackPeriod = TimeUnit.MINUTES.toMillis(5);
    long sendPackPeriod = TimeUnit.MILLISECONDS.toMillis(250);
    long receivePackPeriod = TimeUnit.MILLISECONDS.toMillis(250);
    long heartbeatPeriod = TimeUnit.MINUTES.toMillis(10);
    long maxNoResponseTime = TimeUnit.MINUTES.toMillis(10);
    long tryPeriod = TimeUnit.SECONDS.toMillis(10);

    long timeout = TimeUnit.SECONDS.toMillis(30);
    int maxTryTimes = 30;

    String encode = "UTF-8";
    String decode = encode;
}