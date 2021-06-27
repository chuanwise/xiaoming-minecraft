package com.chuanwise.xiaoming.minecraft.pack;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pack {
    PackType type = PackType.UNKNOWN;
    Object content;

    public static final Pack HEARTBEAT_REQUEST_PACK = new Pack(PackType.HEARTBEAT_REQUEST);
    public static final Pack HEARTBEAT_ALIVE_PACK = new Pack(PackType.HEARTBEAT_ALIVE);

    public Pack(PackType type) {
        this.type = type;
    }

    public String serialize() {
        return JSON.toJSONString(this);
    }

    public static Pack deserialize(String string) {
        return JSON.parseObject(string, Pack.class);
    }

    public <T> T getContent(Class<T> clazz) {
        if (Objects.isNull(content)) {
            return null;
        }
        if (clazz.isAssignableFrom(content.getClass())) {
            return ((T) content);
        } else {
            return JSON.parseObject(JSON.toJSONString(content), clazz);
        }
    }
}
