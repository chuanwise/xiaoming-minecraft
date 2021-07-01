package com.chuanwise.xiaoming.minecraft.bukkit.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
@AllArgsConstructor
public class XiaomingExecuteCommandEvent extends Event {
    protected final HandlerList handlers = new HandlerList();

    int requestId;
    String playerId;
    String command;
}
