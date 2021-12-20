As a modern program, LibertyBans takes advantage of the newer APIs introduced in Java 11. It will not run on Java 8; it *cannot* run on Java 8. The many new tools available to developers in newer Java versions allow creating faster, higher-quality programs with less code. There are also known bugs in Java 8 itself!

The latest java version is not 11. As of the time of writing this, it is 16.

## Enhancements

A lot has changed since Java 8. Here is just a snapshot of the changes from 8 to 11:

* Jigsaw modularization
* CompletableFuture enhancements
* StackWalker API
* Low-level concurrency APIs as safe and supported alternatives to sun.misc.Unsafe
* Ahead of time compilation
* Prebuilt minimized runtime images
* Compact strings
* IO-related constructors accept Charset instead of a String.
* try-with-resources improvements
* File IO additions
* var keyword
* HttpClient API
* Immutable collection factory methods
* Additions to streams
* Experimental JIT compiler from GraalVM
* Improvements to the G1 garbage collector, as well as more modern, state-of-the-art garbage collectors (ZGC and Shenandoah)
* TLS 1.3 support

Of the many performance improvements, hastening of reflection, more compact Strings, and improved hash structure performance are especially relevant for Minecraft servers.

Of these changes listed here, LibertyBans heavily relies on CompletableFuture improvements, makes frequent use of the StackWalker API, uses the more organized IO-related constructors and try-with-resources declarations, often depends on the immutable collection factory methods, takes advantage of the HttpClient API, and uses the var keyword to further simplify programming.


