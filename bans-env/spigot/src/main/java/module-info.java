/*
This descriptor is not published, but kept for compilation purposes
It may be published in the future with spigot.api rewritten to org.bukkit
*/
module space.arim.libertybans.env.spigot {
	requires jakarta.inject;
	requires space.arim.api.env;
	requires space.arim.api.env.bukkit;
	requires space.arim.injector;
	requires space.arim.libertybans.core;
	requires space.arim.morepaperlib;
	requires spigot.api; // org.bukkit
	opens space.arim.libertybans.env.spigot to org.bukkit;
}