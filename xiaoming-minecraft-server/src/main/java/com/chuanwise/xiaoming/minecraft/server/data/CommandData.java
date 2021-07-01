package com.chuanwise.xiaoming.minecraft.server.data;

import com.chuanwise.xiaoming.core.preserve.JsonFilePreservable;
import com.chuanwise.xiaoming.minecraft.server.command.CustomCommand;
import lombok.Getter;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Getter
public class CommandData extends JsonFilePreservable {
    Set<CustomCommand> customCommands = new HashSet<>();

    public CustomCommand forName(String name) {
        for (CustomCommand customCommand : customCommands) {
            if (Objects.equals(customCommand.getName(), name)) {
                return customCommand;
            }
        }
        return null;
    }
}
