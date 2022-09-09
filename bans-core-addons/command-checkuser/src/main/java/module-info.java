import space.arim.libertybans.core.addon.AddonProvider;
import space.arim.libertybans.core.addon.checkuser.CheckUserProvider;

module space.arim.libertybans.core.addon.checkuser {
	requires jakarta.inject;
	requires net.kyori.adventure;
	requires net.kyori.examination.api;
	requires static org.jetbrains.annotations;
	requires space.arim.api.jsonchat;
	requires space.arim.dazzleconf;
	requires space.arim.injector;
	requires space.arim.libertybans.core;
	exports space.arim.libertybans.core.addon.checkuser;
	provides AddonProvider with CheckUserProvider;
}