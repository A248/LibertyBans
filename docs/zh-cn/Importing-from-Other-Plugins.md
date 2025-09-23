LibertyBans支持从以下来源导入数据：

* AdvancedBan
* BanManager
* LiteBans
* 原版服务器封禁 (包括 Essentials)
  * 从原版服务器导入封禁数据需要您在导入过程中在Bukkit服务器上运行LibertyBans。导入完成之后，您就可以将LibertyBans移动到其他受支持的平台。
* LibertyBans自身，旨在进行存储后端转换。请查阅[自我导入](zh-cn/Self-Importing)页面了解更多信息。

如果您没有看到您使用的插件列在上面，请创建[新的反馈](https://github.com/A248/LibertyBans/issues)并描述您需要的功能。

# 导入步骤

1. 备份您的数据。备份数据*永远*是一个好的习惯。如果您想要有额外的保障，请确保您还能恢复您的备份数据。
2. 调整`import.yml`的配置。
3. 运行导入命令 - `/libertybans import <source>`。

# 注意事项

导入操作并非完全对等的过程，因为不同插件使用了不同的存储方式。

## AdvancedBan和原版的UUID支持

| 导入来源        | UUID支持（目标） | UUID支持（操作者） |
|---------------|----------------|-----------------|
| AdvancedBan   | ✔️            | ❌              |
| BanManager    | ✔️            | ✔️              |
| LiteBans      | ✔️            | ✔️              |
| 原版           | ❌            | ❌              |



AdvancedBans和原版会在LibertyBans需要UUID的一些地方使用名称。LibertyBans在所有地方都使用UUID识别，所以在以上情况下LibertyBans会进行UUID查询。

UUID查询的执行方式取决于`config.yml`中的`uuid-resolution`和`server-type`配置。

* 如果您运行的是一个正常的正版登录服务器，您需要添加额外的API解析网站。否则，您的服务器就会因执行过多UUID查询而触发Mojang的速率限制：
```
player-uuid-resolution:
  server-type: 'ONLINE'
  web-api-resolvers:
    - 'ASHCON' // 示例 - 先加上这个
    - 'MOJANG' 
```

* 如果您运行的是一个离线（盗版）服务器，并且使用的是`CRACKED`UUID选项 - 即所有的玩家都使用盗版UUID - 您无需进行任何操作。LibertyBans会假设所有用户都是盗版用户，并自动计算他们的盗版UUID。

* 如果您运行的是一个离线服务器，并且使用的是`MIXED`UUID选项，这种情况下部分用户使用盗版UUID，部分用户使用正版UUID。此时LibertyBans无法进行任何UUID查询，有一些处罚记录会因为LibertyBans无法决定UUID而被跳过。
  * 如果所有玩家都先加入一次，或者在LibertyBans安装期间加入过服务器，您就不会丢失处罚记录。如果玩家之前加入过服务器，LibertyBans会记住他们的名字和UUID，之后在导入时LibertyBans就能对应地匹配玩家的UUID。

## 使用H2数据库的LiteBans插件

如果您的LiteBans插件使用MariaDB、MySQL、或PostgreSQL数据库存储数据，您不需要额外操作。LibertyBans自带这些数据库的驱动。

如果您的LiteBans插件使用H2数据库存储数据，您就需要先下载安装对应的驱动：

1. 下载H2驱动jar文件：
  * [下载直链](https://repo1.maven.org/maven2/com/h2database/h2/1.4.199/h2-1.4.199.jar)
  * [Maven中心页](https://mvnrepository.com/artifact/com.h2database/h2/1.4.199)
2. 找到`LibertyBans/internal/attachments`路径。将驱动jar文件上传到这里。
3. 重启服务器。
4. 照常进行导入程序。

如需帮助，请加入Discord服务器寻求支持。
