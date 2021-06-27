package com.chuanwise.xiaoming.minecraft.server.configuration;

import com.chuanwise.xiaoming.minecraft.util.ArgumentUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinecraftChannel {
    String head;
    String worldTag;
    String serverTag;
    String name;

    String format = "§7[§e{channel.name}§7] §a{sender.alias} §b>§1> §r{message}";
}
