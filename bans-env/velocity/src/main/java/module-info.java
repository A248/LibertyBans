/*
This descriptor is not published, but kept for compilation purposes
It may be published in the future with velocity.api rewritten to com.velocitypowered.api
*/
module space.arim.libertybans.env.velocity {
	requires jakarta.inject;
	requires space.arim.api.env;
	requires space.arim.api.env.velocity;
	requires space.arim.injector;
	requires space.arim.libertybans.core;
	requires velocity.api; // com.velocitypowered.api
	exports space.arim.libertybans.env.velocity to space.arim.injector;
	opens space.arim.libertybans.env.velocity to com.velocitypowered.api;
}