module space.arim.libertybans.env.standalone {
	requires jakarta.inject;
	requires net.kyori.adventure.text.serializer.plain;
	requires org.checkerframework.checker.qual;
	requires org.slf4j;
	requires space.arim.api.env;
	requires space.arim.api.jsonchat;
	requires transitive space.arim.injector;
	requires transitive space.arim.libertybans.bootstrap;
	requires space.arim.libertybans.core;
	exports space.arim.libertybans.env.standalone;
	opens space.arim.libertybans.env.standalone to space.arim.injector;
}