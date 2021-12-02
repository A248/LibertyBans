module space.arim.libertybans.env.velocity {
	requires jakarta.inject;
	requires space.arim.api.env;
	requires space.arim.api.env.velocity;
	requires space.arim.injector;
	requires space.arim.libertybans.core;
	requires com.velocitypowered.api;
	exports space.arim.libertybans.env.velocity to space.arim.injector;
	opens space.arim.libertybans.env.velocity to com.velocitypowered.api;
}