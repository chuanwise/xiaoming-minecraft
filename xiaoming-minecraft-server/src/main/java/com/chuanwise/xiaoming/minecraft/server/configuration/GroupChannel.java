package com.chuanwise.xiaoming.minecraft.server.configuration;

import com.chuanwise.xiaoming.minecraft.util.ArgumentUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupChannel {
    String head;
    String groupTag;
    String name;
    String format = "{sender.alias}：{message}";
    String serverTag = "recorded";
    String worldTag = "recorded";
}
