package com.chuanwise.xiaoming.minecraft.util;

public interface ExceptionThrowableRunnable<E extends Throwable> {
    void run() throws E;
}
