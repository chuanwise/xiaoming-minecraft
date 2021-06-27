package com.chuanwise.xiaoming.minecraft.server.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerPlayer {
    long code;
    String id;
}
