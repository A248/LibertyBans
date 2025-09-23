7本页面是一个在LibertyBans，AdvancedBan，BanManager，和LiteBans之间进行对比的速查表格。

**特别注意：*本页面上的图标不能展示全貌！*** 如果您想要了解细节，请阅读详细的解释和比较文本。您可以在侧边栏找到它们。对于这里所示的每一个插件，都有一份与LibertyBans的详细对比文档。

## 总表

<!-- Platform logos -->

[Bukkit]:https://media.forgecdn.net/avatars/97/684/636293448268093543.png
[Sponge]:https://www.spongepowered.org/favicon.ico

<!-- License logos -->

[AGPL]:https://www.gnu.org/graphics/agplv3-155x51.png
[GPL]:https://www.gnu.org/graphics/gplv3-127x51.png
[CC-BY-NC]:http://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-nc.png

| 插件      | 支持平台                                                                                                                                                                                                                                                                                                                                                      | Java版本要求 | 免费 | 开源               | 支持数据库                        | 线程安全设计 | 稳定的API | 支持Geyser | 支持多实例 | 使用连接池 | 豁免功能 | 服务器范围功能 | 预设/模板功能 | 使用UUID | 数据库完备性 | 可切换存储方式 | 支持的导入源                                                                                                                                        |
|-------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|---------------|----------------------------|--------------------------------------------|--------------------|------------|----------------|------------------------|-----------------|-----------|---------------|---------------------|------------|------------------|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| LibertyBans | ![Bukkit]Bukkit<br/> <img src="https://avatars.githubusercontent.com/u/1007849?v=4" width=16>Bungee<br/> <img src="https://www.spongepowered.org/assets/img/icons/spongie-mark.svg" width=16>Sponge <img src="https://raw.githubusercontent.com/PaperMC/velocitypowered.com/5878ae0941e3adff3a238fe9020250c5f01c2899/src/assets/img/velocity-blue.png" width=16>Velocity | 17+       | ✔️            | ✔️ ![AGPL]                 | HSQLDB (本地), MariaDB, MySQL, PostgreSQL | ✔️                 | ✔️         | ✔️             | ✔️                     | ✔️              | ✔️        | ✔️            | ✔️                  | ✔️         | ✔️               | ✔️                      | AdvancedBan<br/>BanManager<br/>LiteBans<br/>原版                                                                                                |
| AdvancedBan | ![Bukkit]Bukkit<br/> <img src="https://avatars.githubusercontent.com/u/1007849?v=4" width=16>Bungee                                                                                                                                                                                                                                                                      | 8+        | ✔️            | ✔️ ![GPL]                  | HSQLDB (本地), MariaDB, MySQL             | ❌️                 | ❌️         | ❓              | ❌️                     | ❌️              | ✔️        | ❌️            | ✔️                  | ➖️         | ❌️               | ❌️                      |                                                                                                                                                    |
| BanManager  | ![Bukkit]Bukkit<br/> <img src="https://avatars.githubusercontent.com/u/1007849?v=4" width=16>Bungee<br/> <img src="https://www.spongepowered.org/assets/img/icons/spongie-mark.svg" width=16>Sponge                                                                                                                                                                      | 8+        | ✔️            | ➖️️ ![CC-BY-NC]            | H2 (本地), MariaDB, MySQL                 | ❌️️                | ❌️         | ➖️️            | ✔️                     | ✔️              | ❌️        | ➖️            | ➖️                  | ✔️         | ✔️               | ➖️                      | AdvancedBan<br/>原版                                                                                                                            |
| LiteBans    | ![Bukkit]Bukkit<br/> <img src="https://avatars.githubusercontent.com/u/1007849?v=4" width=16>Bungee<br/> <img src="https://raw.githubusercontent.com/PaperMC/velocitypowered.com/5878ae0941e3adff3a238fe9020250c5f01c2899/src/assets/img/velocity-blue.png" width=16>Velocity                                                                                            | 8+        | ❌️            | ❌️ 闭源、代码混淆 | H2 (本地), MariaDB, MySQL, PostgreSQL     | ❓                  | ➖️         | ➖️️            | ✔️                     | ❓               | ➖️        | ➖️            | ✔️                  | ✔️         | ❌️               | ✔️                      | AdvancedBan<br/>BanManager<br/>LibertyBans➖️<br/>原版<br/>*+4 弃用的来源*<img src="https://clipart-library.com/data_images/508362.png" width=20> |
| 原版     | ![Bukkit]Bukkit<br/> <img src="https://www.spongepowered.org/assets/img/icons/spongie-mark.svg" width=16>Sponge                                                                                                                                                                                                                                                          | NA        | ✔️            | ❌️ 闭源             | 纯文本文件                                   | ✔️                 | ✔️         | ✔️             | ❌️                     | NA              | ❌️        | ❌️            | ❌️️                 | ✔️         | ❌️               | NA                      |                                                                                                                                                    |

图例：

✔️ – 是

➖️ – 部分

❌️ – 否

❓ - 未知（插件闭源、或功能尚未测试）

NA - 不适用

### 为什么LibertyBans的所有单元格都打了勾？

过去不是这样。这个表格是在LibertyBans还没有多实例支持、豁免功能、服务器范围配置、模板预设、或切换存储后端等功能的时候创建的。在这些功能尚不存在的时候，所有的功能都被列在了表格里面，但它们还没有被实现，也没人提出相关请求——也就是还没计划加入这些功能！那时，LibertyBans还有很多❌或➖️的标记。

时过境迁，这些功能陆续得到需求并被实现，因此LibertyBans现在满足这些类别的要求，并得到了✔️标记。同类插件里还有很多LibertyBans里面不存在的功能，但是就处罚玩家这个目的而言，它们的功能不算很重要或者很核心，因此没有在表格上出现。

我们是如何知道表格中列出的功能对于处罚插件是核心且重要的？在大多数情况下，我们会*预测*哪些功能最重要，并把它们列入表格中。这些预测最后被证实是准确的，因为表格中的这些类别也是功能请求中热度最高的几项。

## 类别

由于有些类别的含义不太明确，它们需要进一步的解释。以下是详细描述。

有些类别在这里描述起来太过复杂。您需要去读一下具体的插件对插件的对比页面来了解这些类别的细节。您可以在侧边栏里面找到这些页面。每一个插件都有一份与LibertyBans的详尽对比说明。

### 支持平台

即插件的稳定发布版所支持的软件平台。

### Java版本要求

即软件运行所要求的最低Java版本。

### 开源

即插件是否以自由软件许可证发布。有以下两点要求：
1. 源代码自由：软件公开了源代码
2. 使用自由：用户可以将软件用于任何目的

BanManager的许可证禁止将其用于商业用途。因此将其用于赚钱是**非法**的。尽管如此，它的源代码仍然是公开的，因此它得到了“部分”的标记。

LiteBans的代码经过了混淆，这意味着它的jar文件被刻意地进行了混淆处理，防止任何人研究其运作方式、调试问题、或进行任何修改。

免责声明：本页面不构成法律建议，其作者也不是律师。

### 线程安全设计

如果插件做不到线程安全，它的行为就会不稳定、很容易出现问题。不过，有些不稳定的行为只会在罕见的意外情况中出现，这使得开发者很难调试、分析这些问题。

请查阅详细的插件对比页面了解详情。

### 稳定的API

即插件的API是否遵循语义化版本控制，因此其他插件是否可以稳定地依赖它的API。

请参阅[semver.org](https://semver.org/lang/zh-CN/)和详细的插件对比页面。

LiteBans的“部分”标记：LiteBans遵循语义化版本控制中对“主版本号”仅限于破坏性修改的要求，但是并没有遵循对新API的语义化版本要求，这理应在次版本号中予以指出。

### 支持Geyser

即插件是否与Geyser、Floodgate兼容。这是通过修改用户名来使基岩版玩家得以进入服务器或跨服端的插件。

LiteBans的*部分*标记：LiteBans宣称与Geyser兼容，但根据其文档所述，默认的句点（.）字符是唯一支持的基岩版用户名前缀。

### 支持多实例

该功能使得同一插件的多个实例之间能互相同步处罚记录。这与群组服密切相关。

该功能一般用于多代理端的配置，但也可以用于把插件安装在在后端服务器的情形。

### 使用连接池

即插件是否使用连接池*并*利用其优势。

AdvancedBan采用了连接池，但实际上它只能同时建立1个连接，这和不使用连接池是等效的。

### 豁免功能

即插件能否通过豁免等级系统阻止低级管理员封禁高级管理员。

BanManager的“否”标记：尽管BanManager具备一个不带等级的豁免功能，但是这个功能需要将被豁免的玩家手动写入BanManager的配置文件里面。由于每次调整管理员等级都需要重新配置并加载BanManager，在多数情况下这都实在是不够方便。

LiteBans的“部分”标记：插件不支持通过数字配置豁免等级。

### 服务器范围功能

该功能与群组服相关。此项表示插件能否定义服务器范围，并执行针对特定范围的处罚。该功能使得管理员可以执行针对特定服务器或一组服务器的处罚。

BanManager和LiteBans的“部分”标记：BanManager可以执行一个“本地”处罚，即只对一个后端服务器生效的处罚。不过，它无法让一个处罚在一组后端服务器生效；同时管理员也不能执行针对其他后端服务器的处罚。LiteBans具有单服务器范围功能，但同样，它不能执行对一组服务器生效的处罚。

### 预设/模板功能

即插件能否自动补全处罚的详细信息，如原因和时间。这也包括根据目标玩家的既往处罚历史自动升级处罚的功能。

BanManager的“部分”标记：插件提供了简写原因的功能，但是基于过往记录计算处罚时间的功能还没有实现。

### 使用UUID

即插件是否存储处罚目标和执行者的UUID，而不是他们的玩家名。玩家名可以被修改，因此不能用于身份判别。

AdvancedBan的“部分”标记：AdvancedBan会使用UUID记录处罚目标。但是对于执行者，它只记录了玩家名称。

### 数据库完备性

即插件为数据库定义的数据种类是否具有合适的约束和准确的类型。

* 如果插件的数据库完备，插件本身的漏洞就*不会*损坏用户的数据。
* 否则，插件的漏洞就有可能破坏用户数据。

一旦数据损坏，就很难恢复，并且需要运维手动操作数据库。

### 可切换存储方式

即用户能否在支持的存储数据库之间切换数据存储方式。该功能也称作“自我导入”。

BanManager的“部分”标记：BanManager只支持由H2数据库到MySQL/MariaDB的转换，但不能将其迁回H2。

### 支持的导入源

即插件支持的数据导入来源。

LiteBans对LibertyBans的“部分”标记：LiteBans不能导入服务器范围信息，但是服务器范围是二者共有的功能。

此外，LiteBans还能从以下4个被弃用的插件导入数据：BanHammer，BungeeAdminTools，MaxBans，和UltraBans。以上插件均已至少3年没有更新过代码。

--------------------------------------------------------------------------

最后说明一下，以上信息均在合理的时间范围内尽量保持更新。我们欢迎来自其他插件的相关人士提出必需的信息更新PR。
