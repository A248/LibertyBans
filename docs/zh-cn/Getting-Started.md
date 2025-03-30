### 惩罚类型

有四种类型:
封禁(ban)、禁言、踢出和警告.

每种惩罚可以是一个玩家或其 IP 地址。取决于配置文件中的 address-strictness(地址严格性) 选项。

### 命令

一个基本的格式例如 /ban, /mute 和 warn 他们的用法都是 `/<命令> <对象> [时间] <原因>`

禁言/禁制/警告玩家时，如果没有指定时间参数，玩家将被永久禁言。
例如:
* `/ban FakeOwner Obedience is liberating` -> FakeOwner 将会以 “Obedience is liberating” 的理由被永久封禁
* `/ban FakeOwner permanently Obedience is liberating` -> FakeOwner 将会以 “Obedience is liberating” 的理由被永久封禁
* `/ban FakeOwner 5d Obedience is liberating` -> FakeOwner 将会以 “Obedience is liberating” 的理由被**封禁5天**
> Obedience is liberating 指 服从是一种解放

### 时间格式
时间格式参照这个简单的公式：
`<数字><时间单位>`

可用的时间公式:
* m = 分钟
* h = 小时
* d = 天
* mo = 月

例如:
* 3h -> 3 小时
* 8d -> 8 天
* 1mo -> 1 月
* 1m -> 1 分钟

### 对象

可以使用玩家的姓名或 IP 地址封禁玩家。

* 如果您指定了一个名字，LibertyBans 会根据 UUID 进行封禁，即使玩家更改名称也无法逃离 LibertyBans 的手掌心。
* 如果您指定一个 IP 地址，LibertyBans 将按 IP 地址进行封禁，也就是封禁该 IP 地址的所有玩家。(另请参阅 [惩罚类型](Punishment-Enforcement_-Lenient,-Normal,-and-Strict-settings)).

封禁实例:

* `/ban DeviousPlayer` - 封禁 DeviousPlayer。
* `/ban 192.110.162.103` - 封禁 IP 地址 192.110.162.103。
* `/ipban 192.110.162.103` - 封禁 IP 地址 192.110.162.103。 相当于 `/ban 192.110.162.103`
* `/ipban DeviousPlayer` - 封禁 DeviousPlayer 的 IP 地址。

解封示例:

* `/unban SaintlyPlayer` - 解除封禁 SaintlyPlayer.
* `/unban 192.222.222.222` - 解除封禁 IP 地址 192.110.162.103。
* `/unbanip 192.222.222.222` - 解除封禁 IP 地址 192.110.162.103。 相当于 `/ban 192.110.162.103`
* `/unbanip SaintlyPlayer` - 解除封禁 DeviousPlayer 的 IP 地址。

