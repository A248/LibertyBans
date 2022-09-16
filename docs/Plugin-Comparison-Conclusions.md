
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

Little information is known about LiteBans. In fact, it may be illegal for us to disclose the results of our investigation into the workings of LiteBans.

Because it is impossible for us to audit LiteBans, we cannot make any hard conclusions regarding the LiteBans codebase. It may be wholly *incorrect* or *unreliable*. We can't say.

We will focus on the major *known* reason not to use LiteBans. There may be other reasons than this.

### The Future is Uncertain

The most compelling reason not to use LiteBans regards its future. LiteBans' future is arguably in peril. By relying on LiteBans, you place your server at risk.

The development of LiteBans depends on a single person. This has several implications, which bear on both the present and future:

* If, for any reason, the author loses interest, has a better job, or is otherwise busy with other matters, development of LiteBans will stop.
  * Unlike other proprietary plugins, LiteBans is a 1-person team. Other popular proprietary plugins are often composed of a team, so that if one developer leaves, development as a whole may continue.
  * If the development of LiteBans stops, that is the end of LiteBans. No on else has a copy of the source code. No amount of money would bring LiteBans back if the author decided to abandon it permanently.

* Only the author can debug the plugin.
  * Even if you never plan to debug any of your plugins, other users will. As a user, you benefit from *other users* who *do* debug a plugin you both use.
  * On a large network, it is imperative that you retain the ability to debug your own plugins not only *in isolation*, but also *in relation to one another*. Complex bugs can arise through interactions between plugins.
    * See also [The Bug Nobody is Allowed to Understand](https://www.gnu.org/philosophy/bug-nobody-allowed-to-understand.en.html)
    * Complex bugs arising from multiple plugins need not be LiteBans' fault, but they can still exist because of the presence of LiteBans. Sometimes, a bug is dependent on the most extraneous circumstances. Retaining the ability to debug your own proxy, with all its various intricacies, is essential.
    * The LiteBans author will not personally debug your entire proxy.

* Only the author can make a new release. LiteBans follows a fixed release model.
  * Suppose a bug is fixed. The LiteBans author tells you to wait for the next release. You *must* wait until the next release; there is no alternative.
  * If a critical security vulnerability is discovered, you will need to shut down your server until a new LiteBans release is made.
    * It is naïve to think that the LiteBans author will *always* respond in a timely manner to the latest security vulnerabilities. Sometimes, you need to patch your own software. Even if you aren't a developer, you can apply someone else's patch. With LiteBans, this is impossible.
    * Think security vulnerabilities don't happen to plugins? Think again. The H2 database, bundled by LiteBans, has [multiple security notices (CVEs)](https://www.cvedetails.com/vulnerability-list.php?vendor_id=17893&product_id=45580). It is possible to be vulnerable to problems in H2 even if you do not use H2 with LiteBans.
    * More importantly, the modern software chain depends on code from dozens of libraries. Through no fault of a plugin, a plugin can rely on a library which has a security exploit. This can, in extreme situations, lead to a system takeover.

* Have a feature request, but the author of LiteBans doesn't want to implement it? You will *never* see the request implemented.
  * No one can fork LiteBans and add the feature.
  * Even if you try to hire another developer, the other developer will not be able to modify LiteBans.
  * Because only LiteBans can implement feature requests, and no one can fork it, competition is reduced. Intentionally limiting competition is an indicator of a bad product.
