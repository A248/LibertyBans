module space.arim.libertybans.api {
	requires transitive java.sql;
	requires transitive space.arim.omnibus;
	exports space.arim.libertybans.api;
	exports space.arim.libertybans.api.database;
	exports space.arim.libertybans.api.event;
	exports space.arim.libertybans.api.formatter;
	exports space.arim.libertybans.api.punish;
	exports space.arim.libertybans.api.scope;
	exports space.arim.libertybans.api.select;
	exports space.arim.libertybans.api.user;
}