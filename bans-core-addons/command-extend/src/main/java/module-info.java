import space.arim.libertybans.core.addon.AddonProvider;
import space.arim.libertybans.core.addon.extend.ExtendProvider;

module space.arim.libertybans.core.addon.extend {
	requires jakarta.inject;
	requires net.kyori.adventure;
	requires net.kyori.examination.api;
	requires static org.checkerframework.checker.qual;
	requires static org.jetbrains.annotations;
	requires space.arim.api.jsonchat;
	requires space.arim.dazzleconf;
	requires space.arim.injector;
	requires space.arim.libertybans.core;
	exports space.arim.libertybans.core.addon.extend;
	provides AddonProvider with ExtendProvider;
}
