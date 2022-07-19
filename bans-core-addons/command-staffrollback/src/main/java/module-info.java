import space.arim.libertybans.core.addon.AddonProvider;
import space.arim.libertybans.core.addon.staffrollback.StaffRollbackProvider;

module space.arim.libertybans.core.addon.staffrollback {
	requires com.github.benmanes.caffeine;
	requires jakarta.inject;
	requires net.kyori.adventure;
	requires net.kyori.examination.api;
	requires static org.checkerframework.checker.qual;
	requires static org.jetbrains.annotations;
	requires org.jooq;
	requires space.arim.api.jsonchat;
	requires space.arim.dazzleconf;
	requires space.arim.injector;
	requires space.arim.libertybans.core;
	exports space.arim.libertybans.core.addon.staffrollback;
	provides AddonProvider with StaffRollbackProvider;
}