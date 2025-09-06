
LibertyBans 1.1.0不会破坏您的设置，并且与1.0.x完全兼容。不过，您可能需要升级您的技术环境，或微调一些配置。

## Java版本要求

LibertyBans 1.1.0现在需要Java 17。

如果您需要升级到Java 17的帮助，我们很乐意帮忙。请尽情提问，但请保持耐心。

## 对于MariaDB用户

特别建议升级到至少MariaDB 10.6版本。尽管LibertyBans仍然可以在MariaDB 10.3-10.5运行，但是对这些旧版本已被弃用，并且可能会在未来的更新中被移除。

归根到底，是您承担着升级数据库的责任。

运行MariaDB 10.3, 10.4, 或10.5，性能会不可避免的受到轻微影响，因为LibertyBans需要重写SQL查询语句，以兼容旧版中供应商特定的SQL语法。

## 对于Floodgate用户

* LibertyBans现在能自动检测Geyser和Floodgate。在大多数情况下，您不再需要LibertyBans的config.yml中手动配置Geyser的用户名前缀。
* 不过自动检测有一个例外。如果您将LibertyBans安装到了别的位置——比如把LibertyBans装在了代理端，但是把Floodgate装在了后端服务器——您需要手动配置LibertyBans使用的前缀。该配置即一个新的配置项`force-geyser-prefix`。

`GEYSER`服务器类型已被移除。**您需要在config.yml中使用一个恰当的`server-type`：** ONLINE、OFFLINE、或MIXED。
