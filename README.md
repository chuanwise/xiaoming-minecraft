### xiaoming-minecraft
# 小明-MC-QQ 互通插件
该插件为 `Bukkit`、`Spigot`、`Paper` 等 `Minecraft` 服务器提供关联 QQ 的支持，使其能在 QQ 上进行包括但不限于执行控制台指令、分频道的互相聊天、申请执行指令、跨区域 @ 用户等操作。

**小明及相关插件的技术交流 / 用户 QQ 群**：`1028959718`

## 使用方式
### 启动机器人的 QQ
#### 准备文件夹
1. 准备一个 QQ 账号作为机器人。
1. 在[这里](https://github.com/Chuanwise/xiaoming-bot/releases/)或上述群文件内下载最新的 `xiaoming-host-xxxx`（机器人的启动器）。
1. 在你的服务器上创建一个文件夹作为小明的根目录。名字随便起，例如 `xiaoming-bot`。将刚才下载好的机器人启动器放在这个目录下。

#### 准备登录机器人
在小明的根目录下创建一个记事本文件并把名字改为 `xxxx.bat` 的形式，例如 `start.bat`。它是机器人的启动脚本。内容为：
```bash
java -jar xiaoming-host-xxxx.jar
```
那个 `xxxx` 就是具体的 `xiaoming-host-xxxx.jar` 的名字。

双击该脚本初次启动。启动器会生成 `launcher` 文件夹用以存放和启动器相关的设置。请找到该文件夹内的 `launcher.json` 文件，在其中写入 QQ 账号和密码。例如：
```json
{
    "account": {
        "qq": 123456789,
        "md5": null,
        "password": "password"
    },
    "protocol": "ANDROID_PAD",
    "autoReconnectOnForceOffline": false
}
```
`account.qq` 处填写 QQ 号，`account.password` 处填写密码。

`protocol` 是登陆方式，可以选用下表中的一种：
|登录方式|说明|
|---|---|
|`ANDROID_PHONE`|安卓手机|
|`ANDROID_PAD`|安卓平板|
|`ANDROID_WATCH`|安卓手表|

保存后双击启动脚本再次启动机器人。

若是初次在当前设备上登录，会涉及滑块验证等问题。请添加启动参数 `Dmirai.slider.captcha.supported`，也就是把机器人的启动脚本改成类似这样：
```bash
java -Dmirai.slider.captcha.supported -jar xiaoming-host-xxxx.jar
```
之后重新启动该脚本。在群内，或[这里](https://github.com/mzdluo123/TxCaptchaHelper)下载滑块验证助手，将启动机器人时显示的弹框内容复制到滑块验证助手中，再将获得的一串文字复制回弹框后关闭弹框即可。

如果上述方式仍无法正确通过滑块验证，请查阅最新的 `Mirai` [滑块验证文档](https://github.com/project-mirai/mirai-login-solver-selenium)，或在群内联系插件作者。

### 和小明的初次邂逅
#### 给自己授权
在打开机器人后的窗口输入 `grant <你的QQ> *`，例如 `grant 1437100907 *` 可以给你所有权限，方便后面继续配置。

#### 让它注意一些群内的消息
机器人默认不会理会任何群聊消息。把机器人拉到需要响应的群后，在群内发送 `本群启动小明`，机器人就会开始注意本群的消息。启动了小明的群，都叫**响应群**。

#### 找一个群存放机器人的日志
机器人遇到问题需要反馈到日志群聊中。找一个群作为日志群，在群内启动小明后，发送`标记本群  log`，将本群设置为日志群。这样未来小明遇到问题就会反馈到这个群里。

### 让小明连接 MC 服务器
1. 请在机器人目录下的 `plugins` 文件夹中放入 `xiaoming-minecraft-server-xxx.jar`，然后重新启动小明。也可以在启动了小明的群（或私聊）中依次发送 `刷新插件` 和 `加载插件  xiaoming-minecraft-bukkit`（下文称之为`执行小明指令`）。

1. 在你的 `MC` 服务器 `plugins` 目录下放入 `xiaoming-minecraft-bukkit-xxx.jar`，然后重新启动 `MC` 服务器加载这个插件。

1. 小明和服务器只会连接它们认识的 `QQ` 和服务器，所以一开始它们并不会连接。

1. 在 `QQ` 上存在小明的群，或者和小明的私聊发送指令 `添加服务器   <给你的服务器起一个名字>`。例如 `添加服务器  RPG服务器`，随后根据小明的提示，设置一个服务器的凭据（相当于密码，例如 `MyMinecraftServerIdentify`）。

1. 在 `MC` 服务器上执行 `/xm identify server <刚才设置的凭据>`（例如 `/xm identify server MyMinecraftServerIdentify`）设置服务器的凭据。之后使用 `/xm link reconnect` 重新让服务器连接到 `QQ`，此时一般都能正确连接（控制台会输出有关插件连接的内容，当你看到 `成功连接到小明` 意味着服务器连接成功）。

1. 如果执行小明指令 `在线服务器` 时看到了自己的服务器，那么恭喜你，你的 `QQ` 从此和服务器互通了！执行小明指令 `指令格式` 查看相关小明指令的格式。可以尝试一下执行「在线人数」查看是否连接正确。

### 设置相关频道
现在机器人已经连接了服务器，可以执行一些基本的操作，例如绑定 ID、执行服务器指令等。但还没有互通聊天的设置。

群内往服务器发消息的频道，叫「服务器频道」，反过来叫「群聊频道」。它们的设置方式非常类似，这里以设置群聊频道（服务器往群聊发消息的频道）为例。

服务器往群聊发消息，需要解决「在哪些服务器的哪些世界能往这个频道发」「消息以什么格式该发到哪些群」这两个问题。如何方便地指定若干群、服务器和世界？小明给出的解决方案是「打标记」。

群聊、服务器和世界都可以打标记，如同贴纸条。例如给服务器的世界 `game` 和 `hall` 都打上 `public` 标记，我们就可以用 `public` 标记指代这两个世界了。类似地，我们可以给服务器、世界、群聊打标记，然后将频道关联到标记上，这样就可以方便地指定频道的相关范围。

所有的群、服务器和世界都自动带有 `recorded` 标记。此外，群自动带有群号标记，服务器自动带有服务器名标记，世界还带有世界名这个标记。这样默认自带的标记被称为`原生标记`，不可以被删除。我们还可以根据需要添加新的标记。所以显然世界标记`recorded`指代的是所有世界，服务器标记`recorded`指代所有服务器等等。

接下来假设我们希望创建一个在全服任何世界都可以发送，消息发往服务器 `QQ` 群的频道。我们应该执行小明指令 `添加群聊频道  服务器群` 随后根据小明的提示，依次输入 `（服务器群的群号）` 和 `#`。这样，在服务器任何世界发送 `#` 开头的消息，都会被送入这个频道，也就是转发到服务器群中。

上述例子利用了群聊的群号是原生标记这一特点。如果希望小明将消息发送到两个群中，则应该在这两个群中执行「标记本群  <起一个名字>」，然后在小明询问 `该频道和哪些群相关呢？给出群 tag 吧` 时发送刚才的那个名字就可以了。

添加服务器频道的方式是输入指令 `添加服务器频道  <频道名>`，随后根据提示设置。

## 高级内容
### 消息格式
默认的群聊频道的消息格式是 `{sender.alias}：{message}`，服务器频道的格式是 `§7[§e{channel.name}§7] §a{sender.alias} §b>§1> §r{message}`。可以通过小明指令 `设置服务器频道消息格式  {format}` 或 `设置群聊频道消息格式  {format}` 修改。

其中可用的变量有：
|变量名|含义|
|---|---|
|`channel.name`|频道名|
|`channel.head`|频道消息标志|
|`channel.serverTag`|频道关联服务器标记|
|`channel.worldTag`|频道关联世界标记|
|`channel.groupTag`|频道关联群聊记|
|`sender.code`|发送方QQ|
|`sender.name`|发送方QQ名|
|`sender.alias`|发送方备注或QQ名|
|`sender.id`|发送方绑定的 ID，可能为 `null`|
|`group.code`|发送者所在群号|
|`group.name`|发送者所在群名|
|`group.alias`|发送者所在群备注或群名|
|`message`|消息|
|`time`|当前时间|
|`server.name`|服务器名|

### 权限控制
小明内置一套权限管理系统。你可以通过 `授权权限组  default  minecraft.user.*` 免去精细设计的麻烦。如果你有意自定义，请阅读接下来的内容。

通过小明指令 `权限组帮助` 可以获得和权限相关指令的格式。下面是本插件相关的权限：

权限节点|作用
---|---
`minecraft.admin.bind`|强制绑定一个 QQ 和 ID 的权限
`minecraft.admin.bind.look`|查看一个 QQ 绑定的 ID 的权限
`minecraft.admin.execute`|以控制台身份、他人身份或自己绑定 ID 身份执行指令的权限
`minecraft.admin.unbind`|强制解绑一个 QQ 和 ID 的权限
`minecraft.channel.group.add`|添加群聊频道的权限
`minecraft.channel.group.format.set`|设置群聊频道消息显示格式的权限
`minecraft.channel.group.groupTag.look`|查看群聊频道关联的群 tag 的权限
`minecraft.channel.group.groupTag.set`|设置群聊频道关联的群 tag 的权限
`minecraft.channel.group.head.set`|设置群聊频道消息头的权限
`minecraft.channel.group.list`|查看所有群聊频道的权限
`minecraft.channel.group.look`|查看某个群聊频道详情的权限
`minecraft.channel.group.remove`|删除一个群聊频道的权限
`minecraft.channel.group.serverTag.set`|设置群聊频道关联的服务器 tag 的权限
`minecraft.channel.group.worldTag.set`|设置群聊频道关联的世界 tag 的权限
`minecraft.channel.server.add`|添加服务器频道的权限
`minecraft.channel.server.format.set`|设置服务器频道消息显示格式的权限
`minecraft.channel.server.groupTag.set`|查看服务器频道关联的群 tag 的权限
`minecraft.channel.server.head.set`|设置服务器频道消息头的权限
`minecraft.channel.server.list`|查看所有服务器频道的权限
`minecraft.channel.server.look`|查看某个服务器频道详情的权限
`minecraft.channel.server.remove`|删除一个服务器频道的权限
`minecraft.channel.server.serverTag.look`|查看某个群聊频道关联的服务器 tag 的权限
`minecraft.channel.server.serverTag.set`|设置某个群聊频道关联的服务器 tag 的权限
`minecraft.channel.server.worldTag.set`|设置某个群聊频道关联的世界 tag 的权限
`minecraft.command.add`|添加自定义指令的权限
`minecraft.command.list`|查看所有自定义指令的权限
`minecraft.command.look`|查看一个自定义指令详情的权限
`minecraft.command.remove`|删除一个自定义指令的权限
`minecraft.debug`|开关调试模式的权限
`minecraft.disable`|关闭本插件的服务器的权限
`minecraft.enable`|启动本插件的服务器的权限
`minecraft.history.list`|查看所有服务器连接记录的权限
`minecraft.history.look`|查看特定服务器连接记录的权限
`minecraft.history.remove`|删除服务器连接记录的权限
`minecraft.identify.look`|查看小明凭据的权限
`minecraft.identify.set`|设置小明凭据的权限
`minecraft.link.disconnect`|断开和某个服务服务器连接
`minecraft.list`|查看所有服务器的权限
`minecraft.look`|查看某个服务器详情的权限
`minecraft.port.look`|查看本插件的服务器的端口的权限
`minecraft.port.set`|设置本插件的服务器的端口的权限
`minecraft.reenable`|重新启动本插件的服务器的权限
`minecraft.server.add`|添加服务器的权限
`minecraft.server.remove`|删除服务器的权限
`minecraft.server.tag.add`|为某个服务器增加标记的权限
`minecraft.server.tag.remove`|删除某个服务器标记的权限
`minecraft.server.world.tag.add`|增加某个服务器的世界的标记的权限
`minecraft.server.world.tag.look`|查看某个服务器的世界的标记的权限
`minecraft.server.world.tag.remove`|删除某个服务器的世界的标记的权限
`minecraft.test`|测试连接是否正常的权限
`minecraft.user.bind`|在 QQ 群绑定 ID 的权限
`minecraft.user.onlinePlayers`|查看在线人数的权
`minecraft.user.target.look`|查看当前目标服务器的权限
`minecraft.user.target.set`|设置当前目标服务器的权限
`minecraft.user.unbind`|在 QQ 解绑 ID 的权限
`minecraft.chat.server.<channel>`|在名为`channel`的服务器频道上聊天的权限
`minecraft.chat.group.<channel>`|在名为`channel`的群聊频道上聊天的权限

### 设定关键字回复
关键字回复有助于玩家快速查询相关帮助。例如，可以规定玩家在群内发送「服务器IP」回复一段文字。

这个功能由小明的**词库插件**提供，请在[这里](https://github.com/Chuanwise/xiaoming-lexicons)阅读相关教程。

### 自定义指令
自定义指令有助于你在 QQ 上优雅地执行一些指令。例如，可以让玩家在 QQ 上发送 `查钱` 后返回该玩家在服务器的余额。

如果我们希望创建 `查钱` 指令，可以输入小明指令 `创建服务器指令  查钱指令`，随后根据提示输入接下来的内容。

在小明要求输入格式的时候，格式中可以带参数。例如 `授予{toPlayerId}在{worldName}中的建筑权限`。**参数只能由大小写英文和数字组成**。在接下来小明询问「需要在这些服务器上执行哪些指令呢？逐一告诉小明吧，使用『结束』结束」时，输入的内容可以包含刚才定义过的变量，例如 `pex user {toPlayerId} group add p-world-builder {worldName}`。在执行时它们的值会被自动替换。

请注意这些参数会被特殊处理：
|参数|说明|
|---|---|
|名字包含 `toPlayerId` 或 `ToPlayerId` 的参数|此处的值如果是 QQ 号或者 @ 人，且这个用户绑定了服务器 ID，则会被替换为该用户绑定的服务器 ID。
|`remain`|将会被替换为该处及以后的所有内容。例如，格式为 `公告 {remain}`，输入为 `公告 伙计们 这是一个带空格的公告` 时，`{remain}` 的值是 `伙计们 这是一个带空格的公告`。如果输入为 `公告`，则小明不会理会。
|`nullableRemain`|和 `remain` 的区别是如果输入为 `公告 `，小明会理会，且则该变量的值为空的一串字符。

在需要在服务器上执行的指令中，除了可以使用在指令格式中出现的参数，还可以添加这些参数：
|参数|说明|
|---|---|
|`sender.id`|指令发送者绑定的 ID。如果他没有绑定，则指令执行失败|
|`sender.code`|发送方QQ|
|`sender.name`|发送方QQ名|
|`sender.alias`|发送方备注或QQ名|

例如，添加一个指令 `自我介绍`，在服务器内执行 `bc 大家停一下，我说个事` 和 `bc 我是 {该玩家绑定的ID}，我是伞兵一号`，则指令格式为 `自我介绍`，在服务器内应该执行 `bc 大家停一下，我说个事` 和 `bc 我是 {sender.id}，我是伞兵一号`。

执行一个自定义指令的权限是 `minecraft.command.execute.<指令名>`，通过 `授权权限组  default  minecraft.command.execute.<指令名>` 可以授权。

### 添加扩展功能
小明除了可以和 MC 服务器互通，还可以有更多的功能。例如关键词回复等等。欢迎来到[小明的插件中心](https://github.com/Chuanwise/xiaoming-bot/blob/main/docs/PluginCenter.md)。


### 其他设置
请参阅[小明用户手册](https://github.com/Chuanwise/xiaoming-bot/blob/main/docs/Manual.md) 以查看更多信息。

> **更新日志**
> 
> |本文作者|最后更新时间|对应版本号|说明|
> |---|---|---|---|
> |`Chuanwise`|`2021年7月1日`|`1.0`|初次提交