### 处罚类型

目前有4种处罚类型：
封禁、禁言、踢出、与警告。

每一种处罚都可以施加到玩家或者IP地址上。基于IP地址的处罚将根据您的地址严格等级配置来执行。

### 命令

/ban、/mute、和/warn命令的基础格式是`/<命令> <目标> [时间] <原因>`。

当您封禁/禁言/警告一名玩家时，如果您不指定时间，玩家将受到永久性的处罚。
比如：
* `/ban ExamplePlayer 服从即解放` -> ExamplePlayer被永久封禁，原因是"服从即解放"
* `/ban ExamplePlayer permanently 服从即解放` -> ExamplePlayer被永久封禁，原因是"服从即解放"
* `/ban ExamplePlayer 5d 服从即解放` -> ExamplePlayer被封禁**5天**，原因是"服从即解放"

### 时间格式
时间格式参照这个简单的公式：
`<数字><时间单位>`

您可以使用以下单位：
* m = 分钟
* h = 小时
* d = 天
* mo = 月

比如：
* 3h -> 3小时
* 8d -> 8天
* 1mo -> 1个月
* 1m -> 1分钟

### 目标

您可以使用玩家的玩家名或者IP地址来执行处罚。

* 如果您指定的是玩家名，LibertyBans将封禁该玩家的UUID，这样该玩家无法通过改名绕过处罚。
* 如果您指定的是IP地址，LibertyBans将封禁该IP地址，任何使用该IP地址的玩家都会被封禁（参见[处罚的执行方式](zh-cn/Punishment-Enforcement_-Lenient,-Normal,-and-Strict-settings)）

封禁举例：
* `/ban DeviousPlayer` - 封禁DeviousPlayer。
* `/ban 192.110.162.103` - 封禁IP地址192.110.162.103。
* `/ipban 192.110.162.103` 封禁IP地址192.110.162.103。与`/ban 192.110.162.103`效果一致。
* `/ipban DeviousPlayer` - 封禁DeviousPlayer的IP地址。

解封举例：

* `/unban SaintlyPlayer` - 解封SaintlyPlayer。
* `/unban 192.222.222.222` - 解封IP地址192.110.162.103。
* `/unbanip 192.222.222.222` 解封IP地址192.110.162.103。与`/unban 192.110.162.103`效果一致。
* `/unbanip SaintlyPlayer` - 解封SaintlyPlayer的IP地址。
