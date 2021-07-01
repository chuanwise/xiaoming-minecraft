package com.chuanwise.xiaoming.minecraft.server.util;

import com.chuanwise.xiaoming.api.exception.ReceptCancelledException;
import com.chuanwise.xiaoming.api.user.XiaomingUser;
import com.chuanwise.xiaoming.api.util.InteractorUtils;
import com.chuanwise.xiaoming.api.util.StaticUtils;
import com.chuanwise.xiaoming.minecraft.server.XiaomingMinecraftPlugin;
import com.chuanwise.xiaoming.minecraft.server.configuration.ServerConfiguration;
import com.chuanwise.xiaoming.minecraft.server.server.BukkitPluginReceptionist;
import com.chuanwise.xiaoming.minecraft.server.server.XiaomingMinecraftServer;

import java.util.List;
import java.util.Objects;

public class TargetUtils extends StaticUtils {
    public static final String SERVER_TARGET_TAG = "minecraft-target";

    public static BukkitPluginReceptionist requireTarget(XiaomingUser user) {
        final XiaomingMinecraftPlugin plugin = XiaomingMinecraftPlugin.INSTANCE;
        final XiaomingMinecraftServer server = plugin.getMinecraftServer();
        final ServerConfiguration configuration = plugin.getConfiguration();
        final List<BukkitPluginReceptionist> receptionists = server.getReceptionists();

        if (receptionists.isEmpty()) {
            user.sendError("没有任何服务器连接到小明，操作失败");
            throw new ReceptCancelledException();
        } else {
            // 读取用户属性
            String targetName;
            Object targetObject = user.getProperty(SERVER_TARGET_TAG);
            if (targetObject instanceof String) {
                targetName = ((String) targetObject);
            } else {
                targetName = configuration.getDefaultTarget();
            }

            BukkitPluginReceptionist target = server.forName(targetName);
            if (Objects.isNull(target)) {
                user.sendMessage("目标服务器（" + targetName + "）不在线，告诉小明新的目标服务器名吧，或用「退出」取消操作");
                targetName = InteractorUtils.waitNextLegalInput(user, string -> {
                    return Objects.nonNull(server.forName(string));
                }, "该服务器不在线").serialize();
                user.setProperty(SERVER_TARGET_TAG, targetName);

                target = server.forName(targetName);
            }

            return target;
        }
    }
}
