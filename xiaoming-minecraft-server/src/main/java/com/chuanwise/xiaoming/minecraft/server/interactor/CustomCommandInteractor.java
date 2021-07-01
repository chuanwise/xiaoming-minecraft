package com.chuanwise.xiaoming.minecraft.server.interactor;

import com.chuanwise.xiaoming.api.annotation.*;
import com.chuanwise.xiaoming.api.contact.message.Message;
import com.chuanwise.xiaoming.api.interactor.filter.ParameterFilterMatcher;
import com.chuanwise.xiaoming.api.user.GroupXiaomingUser;
import com.chuanwise.xiaoming.api.user.XiaomingUser;
import com.chuanwise.xiaoming.api.util.AtUtils;
import com.chuanwise.xiaoming.api.util.CollectionUtils;
import com.chuanwise.xiaoming.api.util.InteractorUtils;
import com.chuanwise.xiaoming.core.interactor.command.CommandInteractorImpl;
import com.chuanwise.xiaoming.minecraft.pack.content.ResultContent;
import com.chuanwise.xiaoming.minecraft.server.XiaomingMinecraftPlugin;
import com.chuanwise.xiaoming.minecraft.server.command.CustomCommand;
import com.chuanwise.xiaoming.minecraft.server.data.CommandData;
import com.chuanwise.xiaoming.minecraft.server.data.ServerPlayer;
import com.chuanwise.xiaoming.minecraft.server.data.ServerPlayerData;
import com.chuanwise.xiaoming.minecraft.server.server.BukkitPluginReceptionist;
import com.chuanwise.xiaoming.minecraft.server.server.XiaomingMinecraftServer;
import com.chuanwise.xiaoming.minecraft.server.util.EnvironmentUtils;
import com.chuanwise.xiaoming.minecraft.server.util.ServerWords;
import com.chuanwise.xiaoming.minecraft.server.util.TargetUtils;
import com.chuanwise.xiaoming.minecraft.util.ArgumentUtils;
import com.chuanwise.xiaoming.minecraft.util.Formatter;
import com.chuanwise.xiaoming.minecraft.util.StringUtils;
import net.mamoe.mirai.message.code.MiraiCode;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class CustomCommandInteractor extends CommandInteractorImpl {
    final XiaomingMinecraftPlugin plugin;
    final CommandData commandData;

    public CustomCommandInteractor(XiaomingMinecraftPlugin plugin) {
        this.plugin = plugin;
        this.commandData = plugin.getCommandData();
        enableUsageCommand(ServerWords.SERVER + ServerWords.COMMAND);
    }

    /** 执行自定义指令 */
    @NonNext
    @Filter(value = "", pattern = FilterPattern.STARTS_WITH, enableUsage = false)
    public boolean onCommand(XiaomingUser user, Message message) {
        final var customCommands = commandData.getCustomCommands();
        final int maxIterateTime = getXiaomingBot().getConfiguration().getMaxIterateTime();
        final XiaomingMinecraftServer minecraftServer = plugin.getMinecraftServer();
        final ServerPlayerData playerData = plugin.getPlayerData();

        boolean executed = false;
        for (var customCommand : customCommands) {
            // 查找匹配的变量列表
            Map<String, String> parameterValues = null;
            for (var filterParameter : customCommand.getParameterFilterMatchers()) {
                if (filterParameter.apply(user, message)) {
                    parameterValues = filterParameter.getArgumentValue(user.getCode());
                    break;
                }
            }
            if (Objects.isNull(parameterValues)) {
                continue;
            }

            // 验证权限
            if (!user.requirePermission("minecraft.command.execute." + customCommand.getName())) {
                continue;
            }

            // 获取绑定的 ID
            final ServerPlayer serverPlayer = playerData.forPlayer(user.getCode());
            String executorId = null;
            if (customCommand.getExecutorIdentify() == CustomCommand.ExecutorIdentify.AUTO) {
                if (Objects.nonNull(serverPlayer)) {
                    executorId = serverPlayer.getId();
                } else {
                    user.sendError("你还没有绑定服务器 ID，赶快使用「绑定  [你的 ID]」绑定一个吧！");
                    continue;
                }
            }

            // toPlayerId
            for (Map.Entry<String, String> entry : parameterValues.entrySet()) {
                final String parameterName = entry.getKey();
                final String parameterValue = entry.getValue();

                // 用 QQ 解析
                if (parameterName.contains("toPlayerId") || parameterName.contains("ToPlayerId")) {
                    // 试试用 QQ 解析
                    final long qq = AtUtils.parseQQ(parameterValue);
                    if (qq != -1) {
                        ServerPlayer qqToServerPlayer = playerData.forPlayer(qq);
                        if (Objects.nonNull(qqToServerPlayer)) {
                            parameterValues.put(parameterName, qqToServerPlayer.getId());
                        }
                    }
                }
            }

            // 设置原生变量
            parameterValues.putAll(EnvironmentUtils.forUser(user));
            if (user instanceof GroupXiaomingUser) {
                parameterValues.putAll(EnvironmentUtils.forGroup(((GroupXiaomingUser) user).getContact()));
            }

            // 获取目标服务器
            final String serverTag = customCommand.getServerTag();
            final Set<BukkitPluginReceptionist> receptionists;
            if (StringUtils.isEmpty(serverTag)) {
                final BukkitPluginReceptionist receptionist = TargetUtils.requireTarget(user);
                if (Objects.isNull(receptionist)) {
                    continue;
                } else {
                    receptionists = CollectionUtils.asSet(receptionist);
                }
            } else {
                receptionists = minecraftServer.forTag(serverTag);
            }

            if (receptionists.isEmpty()) {
                user.sendError("这个指令对应的服务器并没有连接到小明哦");
                continue;
            }

            // 翻译指令
            final var subCommands = customCommand.getSubCommands();
            final var finalParameterValues = parameterValues;
            final var commands = CollectionUtils.addTo(subCommands, new ArrayList<>(subCommands.size()), format -> {
                        return MiraiCode.deserializeMiraiCode(ArgumentUtils.replaceArguments(format, finalParameterValues, maxIterateTime)).contentToString();
                    });

            // 执行指令
            final List<String> results = new ArrayList<>();
            for (String command : commands) {
                for (BukkitPluginReceptionist receptionist : receptionists) {
                    try {
                        final ResultContent resultContent;
                        if (customCommand.getExecutorIdentify() == CustomCommand.ExecutorIdentify.CONSOLE) {
                            resultContent = receptionist.executeAsConsole(command);
                        } else {
                            resultContent = receptionist.executeAsPlayer(executorId, command);
                        }
                        results.add(resultContent.getDescription("指令执行"));
                    } catch (IOException exception) {
                        receptionist.onThrowable(exception);
                    }
                }
            }

            user.sendMessage(Formatter.clearColors(CollectionUtils.getSummary(results, String::toString, "", "没有返回任何消息", "\n")));

            if (customCommand.isNonNext()) {
                break;
            }

            executed = true;
        }
        return executed;
    }

    @Filter(ServerWords.ADD + ServerWords.SERVER + ServerWords.COMMAND + " {name}")
    @Filter(ServerWords.NEW + ServerWords.SERVER + ServerWords.COMMAND + " {name}")
    @Require("minecraft.command.add")
    public void onAddCommand(XiaomingUser user, @FilterParameter("name") String name) {
        CustomCommand customCommand = commandData.forName(name);
        if (Objects.nonNull(customCommand)) {
            user.sendError("已经存在名为「{name}」的指令了");
            return;
        }
        customCommand = new CustomCommand();
        customCommand.setName(name);

        final List<String> formats = InteractorUtils.fillCollection(user, "这个指令的格式是", "指令格式", new ArrayList<>(), string -> true, string -> {
            return MiraiCode.deserializeMiraiCode(string).serializeToMiraiCode();
        }, false);
        try {
            customCommand.setFormat(formats);
        } catch (Exception exception) {
            getLog().error(exception.getMessage(), exception);
            user.sendError("指令格式出现错误：" + exception);
            return;
        }

        user.sendMessage("执行这个指令是以什么身份？特定的 ID、执行者绑定的 ID 还是控制台？告诉我「执行者」「控制台」或一个 ID 吧");
        final String identify = user.nextInput().serialize();
        switch (identify) {
            case "执行者":
                customCommand.setExecutorIdentify(CustomCommand.ExecutorIdentify.AUTO);
                customCommand.setExecutorId(null);
                break;
            case "控制台":
                customCommand.setExecutorIdentify(CustomCommand.ExecutorIdentify.CONSOLE);
                break;
            default:
                customCommand.setExecutorIdentify(CustomCommand.ExecutorIdentify.EXPLICIT);
                customCommand.setExecutorId(identify);
        }

        customCommand.setNonNext(true);

        user.sendMessage("需要在哪些服务器执行这个指令呢？告诉小明它们的 tag 吧");
        customCommand.setServerTag(user.nextInput().serialize());

        final List<String> strings = InteractorUtils.fillStringCollection(user, "需要在这些服务器上执行哪些指令呢", "执行指令", new ArrayList<>(), false);
        customCommand.setSubCommands(strings);

        user.sendMessage("成功创建服务器指令，详情如下：\n" + getCommandSummary(customCommand));

        commandData.getCustomCommands().add(customCommand);
        getXiaomingBot().getScheduler().readySave(commandData);
    }

    @Filter(ServerWords.REMOVE + ServerWords.SERVER + ServerWords.COMMAND + " {command}")
    @Require("minecraft.command.remove")
    public void onRemoveCommand(XiaomingUser user, @FilterParameter("command") CustomCommand customCommand) {
        final Set<CustomCommand> customCommands = commandData.getCustomCommands();
        customCommands.remove(customCommand);
        user.sendMessage("成功删除指令「{command}」");
        getXiaomingBot().getScheduler().readySave(commandData);
    }

    @Filter(ServerWords.SERVER + ServerWords.COMMAND + " {command}")
    @Require("minecraft.command.look")
    public void onLookCommand(XiaomingUser user, @FilterParameter("command") CustomCommand customCommand) {
        user.sendMessage("【指令详情】\n" + getCommandSummary(customCommand));
    }

    @Filter(ServerWords.SERVER + ServerWords.COMMAND)
    @Require("minecraft.command.list")
    public void onLookCommand(XiaomingUser user) {
        final Set<CustomCommand> customCommands = commandData.getCustomCommands();
        if (customCommands.isEmpty()) {
            user.sendMessage("没有定义任何指令");
        } else {
            user.sendMessage("已定义的指令有：\n" + CollectionUtils.getIndexSummary(customCommands, CustomCommand::getName));
        }
    }

    public String getCommandSummary(CustomCommand customCommand) {
        final CustomCommand.ExecutorIdentify executorIdentify = customCommand.getExecutorIdentify();
        return "指令名：" + customCommand.getName() + "\n" +
                "指令格式：" + CollectionUtils.getIndexSummary(customCommand.getParameterFilterMatchers(), ParameterFilterMatcher::toString, "\n", "（无）", "\n") + "\n" +
                "目标服务器：" + customCommand.getServerTag() + "（" + plugin.getConfiguration().forServerTag(customCommand.getServerTag()).size() + " 个）" + "\n" +
                "执行方式：" + (executorIdentify == CustomCommand.ExecutorIdentify.AUTO ? "执行者" : (executorIdentify == CustomCommand.ExecutorIdentify.CONSOLE ? "控制台" : ("玩家：" + customCommand.getExecutorId()))) + "\n" +
                "服务器指令：" + CollectionUtils.getIndexSummary(customCommand.getSubCommands(), String::toString, "\n", "（无）", "\n") + "\n" +
                "是否阻断：" + (customCommand.isNonNext() ? "是" : "否");
    }

    @Override
    public Set<String> getUsageStrings(XiaomingUser user) {
        final Set<String> usageStrings = super.getUsageStrings(user);
        for (CustomCommand customCommand : commandData.getCustomCommands()) {
            if (user.hasPermission("minecraft.command.execute." + customCommand.getName())) {
                CollectionUtils.addTo(customCommand.getParameterFilterMatchers(), usageStrings, Objects::toString);
            }
        }
        return usageStrings;
    }

    @Override
    public <T> T onParameter(XiaomingUser user, Class<T> clazz, String parameterName, String currentValue, String defaultValue) {
        final T object = super.onParameter(user, clazz, parameterName, currentValue, defaultValue);
        if (Objects.nonNull(object)) {
            return object;
        }

        if (CustomCommand.class.isAssignableFrom(clazz) && Objects.equals("command", parameterName)) {
            final CustomCommand customCommand = commandData.forName(currentValue);
            if (Objects.isNull(customCommand)) {
                user.sendError("没有名为「{command}」的指令呢");
                return null;
            } else {
                return ((T) customCommand);
            }
        }
        return null;
    }
}