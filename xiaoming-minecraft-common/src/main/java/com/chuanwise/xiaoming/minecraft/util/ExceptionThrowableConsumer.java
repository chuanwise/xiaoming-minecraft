package com.chuanwise.xiaoming.minecraft.util;

public interface ExceptionThrowableConsumer<T, E extends Throwable> {
    void accept(T t) throws E;
}
