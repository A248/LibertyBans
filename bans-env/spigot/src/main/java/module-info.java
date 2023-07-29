/*
This descriptor is not published, but kept for compilation purposes
It may be published in the future with spigot.api rewritten to org.bukkit
*/
module space.arim.libertybans.env.spigot {
	requires jakarta.inject;
	requires net.kyori.adventure.text.serializer.legacy;
	requires org.slf4j;
	requires space.arim.api.env;
	requires space.arim.api.env.bukkit;
	requires space.arim.injector;
	requires space.arim.libertybans.core;
	requires space.arim.morepaperlib;
	requires space.arim.omnibus;
	requires spigot.api; // org.bukkit
	exports space.arim.libertybans.env.spigot to space.arim.injector;
	opens space.arim.libertybans.env.spigot to org.bukkit;
}