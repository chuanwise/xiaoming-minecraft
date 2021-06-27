package com.chuanwise.xiaoming.minecraft.pack.content;

import com.alibaba.fastjson.JSON;
import com.chuanwise.xiaoming.minecraft.util.StringUtils;
import lombok.*;

import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultContent {
    boolean success;
    Object object;

    public ResultContent(boolean success) {
        this.success = success;
    }

    public String getDescription(String what) {
        final String string;
        if (object instanceof String) {
            string = ((String) object);
        } else {
            string = Objects.nonNull(object) ? object.toString() : null;
        }

        if (StringUtils.isEmpty(string)) {
            return what + (success ? "成功" : "失败");
        } else {
            return string;
        }
    }

    public <T> T getAs(Class<T> clazz) {
        if (Objects.isNull(object)) {
            return null;
        }
        if (clazz.isAssignableFrom(object.getClass())) {
            return ((T) object);
        } else {
            return JSON.parseObject(JSON.toJSONString(object), clazz);
        }
    }

    public String getString() {
        return getAs(String.class);
    }
}