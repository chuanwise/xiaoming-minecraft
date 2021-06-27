package com.chuanwise.xiaoming.minecraft.bukkit.command.sender;

import lombok.Data;
import lombok.Getter;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public abstract class XiaomingCommandSender<C extends CommandSender> implements CommandSender {
    final List<String> messages = new CopyOnWriteArrayList<>();
    final C commandSender;

    public XiaomingCommandSender(C commandSender) {
        this.commandSender = commandSender;
    }

    @Override
    public void sendMessage(String s) {
        messages.add(s);
        commandSender.sendMessage(s);
    }

    @Override
    public void sendMessage(String[] strings) {
        messages.addAll(Arrays.asList(strings));
        commandSender.sendMessage(strings);
    }

    @Override
    public Server getServer() {
        return commandSender.getServer();
    }

    @Override
    public String getName() {
        return commandSender.getName();
    }

    @Override
    public boolean isPermissionSet(String s) {
        return commandSender.isPermissionSet(s);
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        return commandSender.isPermissionSet(permission);
    }

    @Override
    public boolean hasPermission(String s) {
        return commandSender.hasPermission(s);
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return commandSender.hasPermission(permission);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b) {
        return commandSender.addAttachment(plugin, s, b);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return commandSender.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b, int i) {
        return commandSender.addAttachment(plugin, s, b, i);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int i) {
        return commandSender.addAttachment(plugin, i);
    }

    @Override
    public void removeAttachment(PermissionAttachment permissionAttachment) {
        commandSender.removeAttachment(permissionAttachment);
    }

    @Override
    public void recalculatePermissions() {
        commandSender.recalculatePermissions();
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return commandSender.getEffectivePermissions();
    }

    @Override
    public boolean isOp() {
        return commandSender.isOp();
    }

    @Override
    public void setOp(boolean b) {
        commandSender.setOp(b);
    }
}
