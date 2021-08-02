module space.arim.libertybans.core.it.jpmscompat {
	requires jakarta.inject;
	requires transitive space.arim.api.env;
	requires space.arim.injector;
	requires transitive space.arim.libertybans.core;
	exports space.arim.libertybans.core.it.jpmscompat;
}