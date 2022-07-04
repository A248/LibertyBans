import space.arim.libertybans.core.addon.AddonProvider;
import space.arim.libertybans.core.addon.checkpunish.CheckPunishProvider;

module space.arim.libertybans.core.addon.checkpunish {
	requires jakarta.inject;
	requires net.kyori.adventure;
	requires net.kyori.examination.api;
	requires static org.jetbrains.annotations;
	requires space.arim.api.jsonchat;
	requires space.arim.dazzleconf;
	requires space.arim.injector;
	requires space.arim.libertybans.core;
	exports space.arim.libertybans.core.addon.checkpunish;
	provides AddonProvider with CheckPunishProvider;
}