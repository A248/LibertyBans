
Clearly, the advice here will tell you to use LibertyBans.

Nevertheless, it's useful to know why. This page derives conclusions from the comparisons to each individual plugin:
* [Comparison to AdvancedBan](Comparison-to-AdvancedBan)
* [Comparison to BanManager](Comparison-to-BanManager)
* [Comparison to LiteBans](Comparison-to-LiteBans)

### Terminology

We will repeatedly refer to *correctness* and *reliability*.

*Correctness* means a plugin implements the correct intended behavior. It doesn't mean the plugin is free of bugs; rather, that the plugin is designed to work correctly. If a plugin is not designed to work correctly, that is an intentional failing of the plugin.

*Reliability* refers to whether the plugin is known to fail in specific scenarios. If a plugin acts unpredictably, it is unreliable. If the plugin works most of the time, but has bugs which occur very rarely, it is unreliable.

### Disclaimer

Please note that no harm is meant to subjects of criticism. If the writing sounds harsh, we apologize; please let us know and we will make the language less harsh.

## Why not AdvancedBan

If you read the comparison to AdvancedBan, then I believe the reasons why not to use AdvancedBan should be sufficiently clear.

AdvancedBan suffers from design and implementation issues which cannot be fixed without dramatically rewriting most of AdvancedBan.  At that point, you might as well use a different plugin than wait for a rewrite of AdvancedBan. A rewrite would effectively create a new plugin anyway.

Reasons to use AdvancedBan over LibertyBans<sup>*</sup>
* You absolutely need to use Java 8 and cannot upgrade
* You absolutely need to use MariaDB or MySQL, and you absolutely need to use an old database version.

<sup>* Consider using BanManager before AdvancedBan; see below</sup>

The problems with AdvancedBan regard its *correctness* and *reliability*. Correctness and reliability are more important than features, we do not recommend using AdvancedBan in any other case.

## Why not BanManager

Firstly, if you plan to run a commercial server or network, it is **illegal** for you to use BanManager. Otherwise, keep reading. <sub>Disclaimer: This not legal advice and the writer is not a lawyer</sub>

Unlike AdvancedBan, BanManager does not suffer from problems of correctness and reliability. BanManager's schema is sufficiently robust so that, even if there are bugs in BanManager, such bugs will not corrupt the database data.

However, BanManager still has its own structural issues. These issues cannot be solved without drastic changes to BanManager. As such, we recommend LibertyBans.
* If you are a developer, you may find working with BanManager more difficult, because the API is not clearly defined and is statically accessed.
* With respect to codebases, we find that LibertyBans has better code quality, which suggests BanManager will experience more bugs as updates are released.

Moreover, we judge that LibertyBans prioritizes bugs to a greater extent than in BanManager. BanManager is more likely to work on new features than fix existing bugs:
* As of this writing, there are multiple open issues with the "bug" label on the BanManager issue tracker; however, BanManager is working on significant new features rather than solving these bugs (such as Velocity support and moving to Gradle as the build system).
* In contrast, it is a stated policy of LibertyBans that solution of bugs will be prioritized over new features.

A good reason to use BanManager would be if you absolutely needed to use Java 8, since LibertyBans does not support Java 8. Other good reasons to use BanManager would be that you require specific features not found in LibertyBans.

## Why not LiteBans

Most importantly, LiteBans is proprietary and closed-source. The developer team consists of a single individual, and the plugin jar is obfuscated. If LiteBans were ever to halt development, or the author to vanish, the whole plugin would have to be rewritten from scratch, because the source code could not be recovered.

Due to LiteBans' proprietary nature, even simple updates and bug fixes must wait for the lone author. Contrast this to LibertyBans, which has two official maintainers, A248 and Simon. If one of them is absent, LibertyBans can still receive critical bug fixes such as updates to newer Minecraft versions, which has happened before.

As a further consequence, it is impossible for us to fully audit LiteBans. We cannot make any hard conclusions regarding the LiteBans codebase. It may be wholly *incorrect* or *unreliable*. We can't say. LiteBans might break down in specific situations, or fail silently in some cases. Or it might run fine all the time. LiteBans might have security vulnerabilities from using H2, a database which has [a history of vulnerabilities](https://www.cvedetails.com/vulnerability-list.php?vendor_id=17893&product_id=45580). We'll never know.

There are other reasons not to use LiteBans. Evidence suggests LiteBans does not employ automated testing much, increasing the prevalence of bugs in releases. Moreover, its database schema does not utilize integrity constraints, which can lead to data corruption. Yet because accessing the database is recommended over API use, LiteBans has to keep backwards compatibility with its backwards database, hindering development.

Features that languish on the LiteBans issue tracker will stay incomplete until the author decides to add them. No one else can contribute them or prioritize them. Even if a feature is heavily demanded, only the LiteBans author decides. As of 11 September 2023, the following LiteBans feature requests are fully implemented by LibertyBans:
* [Define scopes in punishment templates](https://gitlab.com/ruany/LiteBans/-/issues/502)
* [Extend punishment duration](https://gitlab.com/ruany/LiteBans/-/issues/494)
* [Purge a punishment completely](https://gitlab.com/ruany/LiteBans/-/issues/494) (same link)
* [Server scope groups](https://gitlab.com/ruany/LiteBans/-/issues/452)
* [Default reason for kicking](https://gitlab.com/ruany/LiteBans/-/issues/406)
* [Tab complete offline player names](https://gitlab.com/ruany/LiteBans/-/issues/349)
* [Add /ipkick command](https://gitlab.com/ruany/LiteBans/-/issues/301)
* [Confirmation for /staffrollback](https://gitlab.com/ruany/LiteBans/-/issues/185)
* [Notification permissions per punishment type](https://gitlab.com/ruany/LiteBans/-/issues/130)
* [Support Sponge platform](https://gitlab.com/ruany/LiteBans/-/issues/41)
* [Import vanilla IP bans](https://gitlab.com/ruany/LiteBans/-/issues/22)

For developers, the LiteBans API is poorly defined and commonly cited as a pain to work with. This makes it harder to integrate other plugins with LiteBans, unless the developer resorts to brittle command execution, forfeiting API guarantees. Not to mention that the inability to look at method implementations shackles debugging attempts. The API does not fully follow semantic versioning, either.

Ultimately, little information is known about LiteBans. In fact, it may even be illegal for us to disclose the results of our investigation into the workings of LiteBans.

Imagine one day that you find a very complex bug and track it down to LiteBans. Complex bugs arising from multiple plugins need not even be LiteBans' fault, but they can still exist because of the presence of LiteBans. Sometimes, a bug is dependent on the most extraneous circumstances. However, with LiteBans, we'd have a case of [The Bug Nobody is Allowed to Understand](https://www.gnu.org/philosophy/bug-nobody-allowed-to-understand.en.html).
