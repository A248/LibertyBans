/*
 * LibertyBans
 * Copyright © 2020 Anand Beh
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

package space.arim.libertybans.core.importing;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.IntegerRange;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.core.database.DatabaseSettingsConfig;

@ConfHeader({
		"Settings for importing from other plugins",
		"Most users should not have to touch this.",
		"",
		"In order to perform an import, run /libertybans import <source>.",
		"Available sources are 'advancedban', 'litebans', 'vanilla', and 'self'.",
		"",
		"Importing from vanilla is only possible on Bukkit.",
		"Essentials users: Note that Essentials uses the vanilla ban system.",
		"",
		"During the import, it is not needed to enable the plugin you are importing from.",
		"",
		"--- NOTICE ---",
		"You MUST backup your data before performing the import.",
		"LibertyBans will never delete your data, but taking a backup is the best practice",
		"whenever you are performing any large transfer of data.",
		"",
		"--- WARNING when importing from AdvancedBan and Vanilla ---",
		"AdvancedBan/Vanilla does not store the uuid of the operator who made a punishment.",
		"To work around this, LibertyBans will attempt to lookup the operator uuid. By default",
		"this uses the Mojang API. However, if your server sends the Mojang API too many requests,",
		"your server might be rate limited (which you do NOT want to happen).",
		"To prevent this, add another service to your web-api-resolvers, in the config.yml",
		"before importing.",
		""})
public interface ImportConfig {

	@ConfKey("retrieval-size")
	@ConfComments({
			"How many punishments to retrieve at once from the import source.",
			"You may be surprised to find out your server is capable of retrieving hundreds,",
			"even thousands of punishments, into memory without much trouble. However,",
			"this is set to 250 as a reasonable conservative estimate."})
	@ConfDefault.DefaultInteger(250)
	@IntegerRange(min = 1)
	int retrievalSize();

	@ConfKey("advancedban")
	@SubSection
	AdvancedBanSettings advancedBan();

	@ConfHeader({
			"",
			"",
			"",
			"-- Importing from AdvancedBan --",
			"",
			"The defaults here match the default settings in AdvancedBan.",
			"If you use AdvancedBan's default storage, you do not need to change anything here.",
			"",
			"--- Using AdvancedBan's MySQL/MariaDB Storage ---",
			"However, if you use MySQL with AdvancedBan, you will need to change a few things.",
			"The jdbc-url should be set to 'jdbc:mariadb://<host>:<port>/<database>' with <host>,",
			"<port>, and <database> replaced with the correct values. The username and password",
			"ought to be the same username and password you used with AdvancedBan.",
			"",
			"For example: You used AdvancedBan with MySQL. Your host is localhost, your port",
			"is 3306, and your database name is 'bans'. You use the username 'advancedban' and",
			"password 'password'. You should set the jdbc-url option to",
			"'jdbc:mysql://localhost:3306/bans', the username to 'advancedban', and the password to 'password'"})
	interface AdvancedBanSettings {

		@ConfKey("jdbc-url")
		@ConfDefault.DefaultString("jdbc:hsqldb:file:plugins/AdvancedBan/data/storage;hsqldb.lock_file=false")
		String jdbcUrl();

		@ConfDefault.DefaultString("SA")
		String username();

		@ConfDefault.DefaultString("")
		String password();

		default ConnectionSource toConnectionSource() {
			return new ConnectionSource.InformativeErrorMessages(
					new JdbcDetails(jdbcUrl(), username(), password()),
					"AdvancedBan"
			);
		}
	}

	@ConfKey("litebans")
	@SubSection
	LiteBansSettings litebans();

	@ConfHeader({
			"",
			"",
			"",
			"-- Importing from LiteBans --",
			"",
			"This is no easy task. The main problem is that LiteBans is a closed-source black box,",
			"such that no one except its author knows how the plugin works internally.",
			"",
			"However, it is still possible to import from LiteBans to LibertyBans, and this is achieved",
			"primarily from inspecting the generated SQL schema.",
			"",
			"--- Config Options Explained ---",
			"The jdbc-url depends on the storage mode you are using LiteBans with.",
			"- Using H2, it should be 'jdbc:h2:./plugins/LiteBans/litebans'",
			"- Using MySQL or MariaDB, it should be 'jdbc:mariadb://<host>:<port>/<database>' with <host>,",
			"<port>, and <database> replaced with the correct values.",
			"- Using PostgreSQL, it should be 'jdbc:postgresql://<host>:<port>/<database>' with <host>,",
			"<port>, and <database> replaced with the correct values.",
			"",
			"If you configured a username and password for LiteBans, you should enter the same",
			"username and password in this section.",
			"",
			"--- JDBC Driver Availability: If you use H2 ---",
			"LiteBans uses the H2 database by default. If importing from H2, you will need to install the H2 driver first.",
			"",
			"The driver is installed by adding it to the classpath. This is done by downloading the driver jar, and starting",
			"your server with the -cp option. The wiki has a tutorial on how to do this."})
	interface LiteBansSettings {

		@ConfKey("jdbc-url")
		@ConfComments("The default value here is set for H2.")
		@ConfDefault.DefaultString("jdbc:h2:./plugins/LiteBans/litebans")
		String jdbcUrl();

		@ConfDefault.DefaultString("")
		String username();

		@ConfDefault.DefaultString("")
		String password();

		@ConfKey("table-prefix")
		@ConfComments("The same table prefix you used with LiteBans")
		@ConfDefault.DefaultString("litebans_")
		String tablePrefix();

		default ConnectionSource toConnectionSource() {
			return new ConnectionSource.InformativeErrorMessages(
					new JdbcDetails(jdbcUrl(), username(), password()),
					"LiteBans"
			);
		}
	}

	@ConfKey("self")
	@SubSection
	SelfSettings self();

	@ConfHeader({
			"",
			"",
			"",
			"-- Importing from another LibertyBans database --",
			"",
			"NOTICE: The current database MUST be empty when the import is performed.",
			"",
			"The self-import process is different from the import process for other plugins.",
			"It is a direct transfer, which is why the current database must be empty. Also, punishment IDs are preserved.",
			"",
			"Self-importing may be used to switch between various database backends supported by LibertyBans.",
			"For example, you could transfer your data from HSQLDB to MariaDB.",
			"",
			"The settings here are used exactly as in sql.yml - in fact, they are the same settings."})
	interface SelfSettings extends DatabaseSettingsConfig {

		@ConfKey("reverse-direction")
		@ConfComments({
				"Whether to reverse the direction of the self-import. If enabled, this will import TO the database",
				"configured here, rather than FROM it.",
				"",
				"Normally, LibertyBans will import from this database, to the current database defined in sql.yml.",
				"If this option is enabled, LibertyBans will import to the current database in sql.yml, from this database."
		})
		@ConfDefault.DefaultBoolean(false)
		boolean reverseDirection();

	}

	@ConfKey("ban-manager")
	@SubSection
	BanManagerSettings banManager();

	@ConfHeader({
			"",
			"",
			"",
			"-- Importing from BanManager (v7) --",
			"",
			"Bans, IP bans, mutes, IP mutes, warns, and kicks are supported.",
			"",
			"BanManager's IP range bans, name bans, notes, and reports are not supported. LibertyBans has no equivalent features.",
			"",
			"--- Config Options Explained ---",
			"The jdbc-url depends on the storage mode you are using BanManager with.",
			"- Using H2, it should be 'jdbc:h2:./plugins/BanManager/local-bans'",
			"- Using MySQL or MariaDB, it should be 'jdbc:mariadb://<host>:<port>/<database>' with <host>,",
			"<port>, and <database> replaced with the correct values.",
			"",
			"If you configured a username and password for BanManager, you should enter the same",
			"username and password in this section.",
	})
	interface BanManagerSettings {

		@ConfKey("jdbc-url")
		@ConfComments("The default value here is set for H2.")
		@ConfDefault.DefaultString("jdbc:h2:./plugins/BanManager/local-bans")
		String jdbcUrl();

		@ConfDefault.DefaultString("")
		String username();

		@ConfDefault.DefaultString("")
		String password();

		@ConfKey("table-prefix")
		@ConfComments("The same table prefix you used with BanManager")
		@ConfDefault.DefaultString("bm_")
		String tablePrefix();

		default ConnectionSource toConnectionSource() {
			return new ConnectionSource.InformativeErrorMessages(
					new JdbcDetails(jdbcUrl(), username(), password()),
					"BanManager"
			);
		}
	}
}
