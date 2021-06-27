package com.chuanwise.xiaoming.minecraft.pack.content;

import com.alibaba.fastjson.JSON;
import lombok.*;

import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdContent {
    int requestId;
    Object content;

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

    public void asResultOf(int requestId) {
        this.requestId = requestId;
    }
}
