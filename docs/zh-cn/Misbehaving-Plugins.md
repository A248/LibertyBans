假设您在服务器后台看到了这样的信息：

```
10.05 14:41:56 [Server] ERROR [space.arim.libertybans.core.env.ParallelisedListener]: You likely have a misbehaving plugin installed on your server. 
This may lead to bans or mutes not being checked and enforced.

Reason: the event PlayerChatEvent<some information> was previously blocked by the server or another plugin, but since 
then, some plugin has uncancelled the blocking.
```

这是一条来自LibertyBans的实用信息，它提醒您有很可能安装了一个行为异常的插件。

其他封禁插件不会发出类似的警告 - 它们不会反馈这个问题，但问题确实会悄悄发生。

### 解决问题

您的服务器上很可能装有一个行为异常的插件。我建议您设置一个测试服务器，然后看看您能否通过增减插件使得该提示消失。如果您移除了一个插件，然后提示消失了，那么您就知道这个插件导致了问题。

### 技术型解释

这个提示指向了一个很具体的事情，这与事件在不同插件之间的处理有关。事件系统是这样运行的：一个事件会传递给一个插件，然后是下一个，依此类推，比如下面这样：

```
1. 插件A监听了聊天事件
2. LibertyBans监听了聊天事件
3. 插件B监听了聊天事件
```

任意插件都可能会*取消*这个事件。如果事件被取消，其他插件仍然能够监听到事件，但是它们也会知道事件已经被取消。插件也可以*撤销*事件的取消状态，即恢复事件。

正常来讲，如果一个玩家被禁言，LibertyBans会取消这个玩家的聊天事件。

如果聊天事件没有被取消，LibertyBans就会取消这个聊天事件。但是，以下情况也有可能发生：
1. 插件A取消了聊天事件
2. LibertyBans监听到了已经被取消的事件，因此忽略了它
3. 插件B*恢复*了被取消的聊天事件

如果上述情况出现，LibertyBans就会在控制台发出警告。这种情况下，您的服务器上就有一个会恢复聊天事件的"插件B"。无论LibertyBans如何设计，一个能取消聊天事件的插件将会导致被禁言的玩家仍能正常发言。这种插件就需要让他们的开发者修复。
