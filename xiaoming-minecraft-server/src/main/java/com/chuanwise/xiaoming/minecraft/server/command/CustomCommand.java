package com.chuanwise.xiaoming.minecraft.server.command;

import com.chuanwise.xiaoming.api.interactor.filter.ParameterFilterMatcher;
import com.chuanwise.xiaoming.api.util.CollectionUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 执行这个指令所需的权限是 minecraft.command.execute.{name}
 * @author Chuanwise
 */
@Data
@NoArgsConstructor
public class CustomCommand {
    public enum ExecutorIdentify {
        CONSOLE,
        AUTO,
        EXPLICIT,
    }

    String name;
    List<String> format = new ArrayList<>();
    List<String> subCommands = new ArrayList<>();
    ExecutorIdentify executorIdentify = ExecutorIdentify.AUTO;
    String executorId = "";
    String serverTag = "";
    boolean nonNext = false;

    transient List<ParameterFilterMatcher> parameterFilterMatchers;

    public void setFormat(List<String> format) {
        this.format = format;
        this.parameterFilterMatchers = CollectionUtils.addTo(format, new ArrayList<>(format.size()), ParameterFilterMatcher::new);
    }
}