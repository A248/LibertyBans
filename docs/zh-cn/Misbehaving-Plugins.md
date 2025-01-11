Let's say you see this message in your server console:

```
10.05 14:41:56 [Server] ERROR [space.arim.libertybans.core.env.ParallelisedListener]: You likely have a misbehaving plugin installed on your server. 
This may lead to bans or mutes not being checked and enforced.

Reason: the event PlayerChatEvent<some information> was previously blocked by the server or another plugin, but since 
then, some plugin has uncancelled the blocking.
```

This is a helpful message emitted by LibertyBans warning you that you likely have a misbehaving plugin.

Other ban plugins do not issue the same warning - they don't report the problem, but it will occur silently.

### Solving the Issue

You have another plugin installed on your server which is probably misbehaving. I recommend investigating the situation by setting up a test server and seeing if you can get this message to go away by adding and removing plugins. If you remove plugin X and the message disappears, you know that plugin X is at fault.

### Technical Explanation

This means something very specific. It relates to how events are processed by different plugins. The way event systems work, first the event is passed to one plugin, then another, then the next:

```
1. Plugin A listens to the chat event
2. LibertyBans listens to the chat event
3. Plugin B listens to the chat event
```

Any of these plugins may *cancel* the event. If they do, the other plugins still see the event, but they see that it is cancelled. Plugins can also *undo* the cancellation of an event.

Normally, if a player is muted, LibertyBans will cancel the chat event for that player.

LibertyBans does its job to cancel the chat event if the event is not already cancelled. However, the following can occur:
1. Plugin A cancels the chat event
2. LibertyBans sees the cancelled event and ignores it as a result
3. Plugin B *un-cancels* the chat event

The console message is a warning that this situation has occurred. If this happens, you have a plugin like `Plugin B` which is un-cancelling chat events. Regardless of how LibertyBans is designed, a plugin un-cancelling chat events has the capability to allow muted players to talk. That plugin will have to be fixed.
