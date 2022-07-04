module space.arim.libertybans.env.velocity {
	requires com.velocitypowered.api;
	requires jakarta.inject;
	requires org.slf4j;
	requires space.arim.api.env;
	requires space.arim.api.env.velocity;
	requires space.arim.injector;
	requires space.arim.libertybans.core;
	exports space.arim.libertybans.env.velocity to space.arim.injector;
	opens space.arim.libertybans.env.velocity to com.velocitypowered.api;
}