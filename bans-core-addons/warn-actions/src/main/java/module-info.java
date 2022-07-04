import space.arim.libertybans.core.addon.AddonProvider;
import space.arim.libertybans.core.addon.warnactions.WarnActionsProvider;

module space.arim.libertybans.core.addon.warnactions {
	requires jakarta.inject;
	requires org.slf4j;
	requires space.arim.api.jsonchat;
	requires space.arim.dazzleconf;
	requires space.arim.injector;
	requires space.arim.libertybans.core;
	exports space.arim.libertybans.core.addon.warnactions;
	provides AddonProvider with WarnActionsProvider;
}