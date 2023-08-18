/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

import space.arim.libertybans.core.addon.AddonProvider;

module space.arim.libertybans.core {
	requires com.github.benmanes.caffeine;
	requires com.zaxxer.hikari;
	requires jakarta.inject;
	requires static java.compiler;
	requires net.kyori.adventure;
	requires net.kyori.examination.api;
	requires net.kyori.adventure.text.serializer.legacy;
	requires org.flywaydb.core;
	requires static org.checkerframework.checker.qual;
	requires static org.jetbrains.annotations;
	requires org.jooq;
	requires org.slf4j;
	requires space.arim.api.env;
	requires space.arim.api.jsonchat;
	requires space.arim.api.util.dazzleconf;
	requires space.arim.api.util.web;
	requires space.arim.dazzleconf;
	requires space.arim.dazzleconf.ext.snakeyaml;
	requires space.arim.injector;
	requires transitive space.arim.libertybans.api;
	requires transitive space.arim.libertybans.bootstrap;
	exports space.arim.libertybans.core;
	exports space.arim.libertybans.core.addon;
	exports space.arim.libertybans.core.addon.exempt;
	exports space.arim.libertybans.core.addon.staffrollback.execute to space.arim.libertybans.core.addon.staffrollback;
	exports space.arim.libertybans.core.alts to space.arim.dazzleconf, space.arim.injector;
	exports space.arim.libertybans.core.commands;
	exports space.arim.libertybans.core.commands.extra;
	exports space.arim.libertybans.core.commands.usage to space.arim.injector;
	exports space.arim.libertybans.core.config;
	exports space.arim.libertybans.core.database to space.arim.dazzleconf, space.arim.injector;
	exports space.arim.libertybans.core.database.execute to space.arim.injector;
	exports space.arim.libertybans.core.database.flyway to org.flywaydb.core;
	exports space.arim.libertybans.core.env;
	exports space.arim.libertybans.core.env.message;
	exports space.arim.libertybans.core.event to space.arim.injector, space.arim.libertybans.core.addon.shortcutreasons, space.arim.libertybans.core.addon.layouts;
	exports space.arim.libertybans.core.importing;
	exports space.arim.libertybans.core.punish;
	exports space.arim.libertybans.core.punish.sync to space.arim.injector;
	exports space.arim.libertybans.core.scope;
	exports space.arim.libertybans.core.selector;
	exports space.arim.libertybans.core.selector.cache;
	exports space.arim.libertybans.core.service;
	exports space.arim.libertybans.core.uuid;
	opens space.arim.libertybans.core.alts to space.arim.dazzleconf;
	opens space.arim.libertybans.core.commands.extra to space.arim.dazzleconf;
	opens space.arim.libertybans.core.config to space.arim.dazzleconf;
	opens space.arim.libertybans.core.importing to space.arim.dazzleconf;
	opens space.arim.libertybans.core.selector to space.arim.dazzleconf;
	opens space.arim.libertybans.core.uuid to space.arim.dazzleconf;
	exports space.arim.libertybans.core.punish.permission;
	opens space.arim.libertybans.core.punish.permission to space.arim.dazzleconf;
	uses AddonProvider;
}