package com.chuanwise.xiaoming.minecraft.bukkit.command.sender;

import org.bukkit.command.ConsoleCommandSender;

public class XiaomingNamedCommandSender extends XiaomingCommandSender<ConsoleCommandSender> {
    final String name;

    public XiaomingNamedCommandSender(ConsoleCommandSender commandSender, String name) {
        super(commandSender);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
