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

package space.arim.libertybans.core.commands.extra;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.formatter.PunishmentFormatter;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.commands.ComponentMatcher;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.config.ScopeConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.scope.ScopeParsing;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.libertybans.it.util.RandomUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StandardArgumentParserTest {

	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
	private final Configs configs;
	private final InternalScopeManager scopeManager;
	private final UUIDManager uuidManager;
	private final CmdSender sender;

	private final ArgumentParser argumentParser;

	private final MessagesConfig messagesConfig;
	private final MessagesConfig.All allConfig;
	private final MessagesConfig.All.NotFound notFoundConfig;

	private final Component notFoundMessage = Component.text("Not found", NamedTextColor.RED);

	public StandardArgumentParserTest(
			@Mock Configs configs, @Mock InternalScopeManager scopeManager, @Mock PunishmentFormatter formatter,
			@Mock UUIDManager uuidManager, @Mock CmdSender sender, @Mock MessagesConfig messagesConfig,
			@Mock MessagesConfig.All allConfig, @Mock MessagesConfig.All.NotFound notFoundConfig) {
		this.configs = configs;
		this.scopeManager = scopeManager;
		this.uuidManager = uuidManager;
		this.sender = sender;
		this.messagesConfig = messagesConfig;
		this.allConfig = allConfig;
		this.notFoundConfig = notFoundConfig;

		argumentParser = new StandardArgumentParser(futuresFactory, configs, scopeManager, formatter, uuidManager);
	}

	@BeforeEach
	public void setupMocks() {
		lenient().when(configs.getMessagesConfig()).thenReturn(messagesConfig).getMock();
		lenient().when(messagesConfig.all()).thenReturn(allConfig);
		lenient().when(allConfig.notFound()).thenReturn(notFoundConfig);
	}

	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}

	@Nested
	public class ParsePlayerVictim {

		private Victim parseVictim(String targetArg) {
			return argumentParser.parseVictim(
					sender, targetArg, ParseVictim.ofPreferredType(Victim.VictimType.PLAYER)
			).join();
		}

		@Test
		public void explicitPlayerUuid() {
			UUID uuid = UUID.randomUUID();

			assertEquals(PlayerVictim.of(uuid), parseVictim(uuid.toString()));
			assertEquals(PlayerVictim.of(uuid), parseVictim(uuid.toString().replace("-", "")));

			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void explicitIpv4Address() {
			NetworkAddress address = NetworkAddress.of(new byte[] {
					(byte) 127, (byte) 0, (byte) 255, (byte) 38});
			assertEquals(AddressVictim.of(address), parseVictim("127.0.255.38"));
			verify(uuidManager, never()).lookupAddress(any());
			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void lookupPlayerVictim() {
			UUID uuid = UUID.randomUUID();
			String name = "ObsidianWolf_";
			when(uuidManager.lookupUUID(name)).thenReturn(completedFuture(Optional.of(uuid)));

			assertEquals(PlayerVictim.of(uuid), parseVictim(name));

			verify(uuidManager).lookupUUID(name);
			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void unknownPlayerVictimForName() {
			String name = "A248";
			when(uuidManager.lookupUUID(name)).thenReturn(completedFuture(Optional.empty()));
			when(notFoundConfig.player()).thenReturn(ComponentText.create(notFoundMessage));

			assertNull(parseVictim(name));

			verify(uuidManager).lookupUUID(name);
			verify(sender).sendMessage(argThat(new ComponentMatcher<>(notFoundMessage)));
		}

	}

	@Nested
	public class ParseCompositeVictim {

		private Victim parseVictim(String targetArg) {
			return argumentParser.parseVictim(
					sender, targetArg, ParseVictim.ofPreferredType(Victim.VictimType.COMPOSITE)
			).join();
		}

		@Test
		public void lookupPlayerVictim() {
			UUID uuid = UUID.randomUUID();
			String name = "ObsidianWolf_";
			NetworkAddress address = RandomUtil.randomAddress();
			when(uuidManager.lookupPlayer(name)).thenReturn(completedFuture(Optional.of(new UUIDAndAddress(uuid, address))));

			assertEquals(CompositeVictim.of(uuid, address), parseVictim(name));

			verify(uuidManager).lookupPlayer(name);
			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void unknownPlayerVictimForName() {
			String name = "A248";
			when(uuidManager.lookupPlayer(name)).thenReturn(completedFuture(Optional.empty()));
			when(notFoundConfig.player()).thenReturn(ComponentText.create(notFoundMessage));

			assertNull(parseVictim(name));

			verify(uuidManager).lookupPlayer(name);
			verify(sender).sendMessage(argThat(new ComponentMatcher<>(notFoundMessage)));
		}

	}

	@Nested
	public class ParseOperator {

		private Operator parseOperator(String targetArg) {
			return argumentParser.parseOperator(sender, targetArg).join();
		}

		private void mockConsoleArguments(Set<String> consoleArguments) {
			MessagesConfig.Formatting formatting = mock(MessagesConfig.Formatting.class);
			when(messagesConfig.formatting()).thenReturn(formatting);
			when(formatting.consoleArguments()).thenReturn(consoleArguments);
		}

		@Test
		public void explicitPlayerUuid() {
			UUID uuid = UUID.randomUUID();
			mockConsoleArguments(Set.of());

			assertEquals(PlayerOperator.of(uuid), parseOperator(uuid.toString()));
			assertEquals(PlayerOperator.of(uuid), parseOperator(uuid.toString().replace("-", "")));

			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void lookupPlayerOperator() {
			UUID uuid = UUID.randomUUID();
			String name = "ObsidianWolf_";
			mockConsoleArguments(Set.of());
			when(uuidManager.lookupUUID(name)).thenReturn(completedFuture(Optional.of(uuid)));

			assertEquals(PlayerOperator.of(uuid), parseOperator(name));

			verify(uuidManager).lookupUUID(name);
			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void unknownPlayerOperatorForName() {
			String name = "A248";
			mockConsoleArguments(Set.of());
			when(uuidManager.lookupUUID(name)).thenReturn(completedFuture(Optional.empty()));
			when(notFoundConfig.player()).thenReturn(ComponentText.create(notFoundMessage));

			assertNull(parseOperator(name));

			verify(uuidManager).lookupUUID(name);
			verify(sender).sendMessage(argThat(new ComponentMatcher<>(notFoundMessage)));
		}

		@Test
		public void explicitConsoleOperator() {
			mockConsoleArguments(Set.of("console"));

			assertEquals(ConsoleOperator.INSTANCE, parseOperator("console"));
		}

	}

	@Nested
	public class ParseAddressVictim {

		private Victim parseVictim(String targetArg) {
			return argumentParser.parseVictim(
					sender, targetArg, ParseVictim.ofPreferredType(Victim.VictimType.ADDRESS)
			).join();
		}

		@Test
		public void parseIpv4() {
			NetworkAddress address = NetworkAddress.of(new byte[] {
					(byte) 127, (byte) 0, (byte) 255, (byte) 38});
			assertEquals(AddressVictim.of(address), parseVictim("127.0.255.38"));
			verify(uuidManager, never()).lookupAddress(any());
			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void lookupAddressVictim() {
			String name = "A248";
			NetworkAddress address = AddressParserTest.randomIpv4();
			when(uuidManager.lookupAddress(name)).thenReturn(completedFuture(address));

			assertEquals(AddressVictim.of(address), parseVictim(name));

			verify(uuidManager).lookupAddress(name);
			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void unknownAddressVictimForName() {
			String name = "A248";
			when(uuidManager.lookupAddress(name)).thenReturn(completedFuture(null));
			when(notFoundConfig.playerOrAddress()).thenReturn(ComponentText.create(notFoundMessage));

			assertNull(parseVictim(name));

			verify(uuidManager).lookupAddress(name);
			verify(sender).sendMessage(argThat(new ComponentMatcher<>(notFoundMessage)));
		}

	}

	@Nested
	public class ParseScopeFromCommand {

		@BeforeEach
		public void setup(@Mock ScopeConfig scopeConfig) {
			lenient().when(scopeManager.parseFrom(any())).thenAnswer((invocation) -> {
				return new ScopeParsing() {
					@Override
					public ServerScope specificScope(String server) {
						return scopeManager.specificScope(server);
					}

					@Override
					public ServerScope category(String category) {
						return scopeManager.category(category);
					}

					@Override
					public ServerScope globalScope() {
						return scopeManager.globalScope();
					}
				}.parseInputOptionally(invocation.getArgument(0));
			});
			when(configs.getScopeConfig()).thenReturn(scopeConfig);
			when(scopeConfig.requirePermissions()).thenReturn(false);
		}

		@Test
		public void noneSpecified(@Mock CommandPackage command,
								  @Mock ServerScope defaultScope) {
			when(scopeManager.defaultPunishingScope()).thenReturn(defaultScope);
			when(command.findHiddenArgumentSpecifiedValue(any())).thenReturn(null);
			assertEquals(defaultScope, argumentParser.parseScope(sender, command, ParseScope.fallbackToDefaultPunishingScope()));
			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void serverScope(@Mock CommandPackage command, @Mock ServerScope scope) {
			when(scopeManager.specificScope("lobby")).thenReturn(scope);
			when(command.findHiddenArgumentSpecifiedValue("server")).thenReturn("lobby");
			assertEquals(scope, argumentParser.parseScope(sender, command, ParseScope.fallbackToDefaultPunishingScope()));
			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void serverScopeLiteral(@Mock CommandPackage command, @Mock ServerScope scope) {
			when(scopeManager.specificScope("lobby")).thenReturn(scope);
			when(command.findHiddenArgumentSpecifiedValue("scope")).thenReturn("server:lobby");
			when(command.findHiddenArgumentSpecifiedValue(not(eq("scope")))).thenReturn(null);
			assertEquals(scope, argumentParser.parseScope(sender, command, ParseScope.fallbackToDefaultPunishingScope()));
			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void categoryScope(@Mock CommandPackage command, @Mock ServerScope scope) {
			when(scopeManager.category("pvp")).thenReturn(scope);
			when(command.findHiddenArgumentSpecifiedValue("category")).thenReturn("pvp");
			when(command.findHiddenArgumentSpecifiedValue(not(eq("category")))).thenReturn(null);
			assertEquals(scope, argumentParser.parseScope(sender, command, ParseScope.fallbackToDefaultPunishingScope()));
			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void categoryScopeLiteral(@Mock CommandPackage command, @Mock ServerScope scope) {
			when(scopeManager.category("pvp")).thenReturn(scope);
			when(command.findHiddenArgumentSpecifiedValue("scope")).thenReturn("category:pvp");
			when(command.findHiddenArgumentSpecifiedValue(not(eq("scope")))).thenReturn(null);
			assertEquals(scope, argumentParser.parseScope(sender, command, ParseScope.fallbackToDefaultPunishingScope()));
			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void globalLiteral(@Mock CommandPackage command, @Mock ServerScope globalScope) {
			when(scopeManager.globalScope()).thenReturn(globalScope);
			when(command.findHiddenArgumentSpecifiedValue("scope")).thenReturn(ScopeParsing.GLOBAL_SCOPE_USER_INPUT);
			when(command.findHiddenArgumentSpecifiedValue(not(eq("scope")))).thenReturn(null);
			assertEquals(globalScope, argumentParser.parseScope(sender, command, ParseScope.fallbackToDefaultPunishingScope()));
			verify(sender, never()).sendMessage(any());
		}

		@Test
		public void invalidScope(@Mock CommandPackage command, @Mock MessagesConfig.All.Scopes section) {
			ComponentText errorMsg = ComponentText.create(Component.text("Not a valid scope"));
			when(allConfig.scopes()).thenReturn(section);
			when(section.invalid()).thenReturn(errorMsg);

			when(command.findHiddenArgumentSpecifiedValue("scope")).thenReturn("gibberish");
			when(command.findHiddenArgumentSpecifiedValue(not(eq("scope")))).thenReturn(null);
			assertNull(argumentParser.parseScope(sender, command, ParseScope.fallbackToDefaultPunishingScope()));
			verify(sender).sendMessage(errorMsg);
		}
	}

}
