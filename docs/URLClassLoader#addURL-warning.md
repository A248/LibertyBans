# Fixed in 0.7.4 release

You may see a warning in your console about illegal reflective access:

> WARNING: Illegal reflective access by space.arim.libertybans.bootstrap.LibertyBansLauncher (file:plugins/LibertyBans_0.2.0-SNAPSHOT.jar) to method java.net.URLClassLoader.addURL(java.net.URL)

Don't worry, *in this case* it is harmless. The important thing to note is `method java.net.URLClassLoader.addURL(java.net.URL)`. That's a protected method which is not going anywhere.

Sometimes illegal reflective access is bad. If you find a similar warning about a different method or field, e.g. `field sun.nio.ch.FileChannelImpl.fd` - that may be a problem you should investigate.

### Why does this occur?

Your platform (Bukkit or Bungee) does not support adding to the classpath at runtime, so LibertyBans needs to *kind of* hack this in.

On Velocity, this problem does not happen because Velocity has proper support.