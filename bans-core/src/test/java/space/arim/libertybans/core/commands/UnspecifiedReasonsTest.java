/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.core.commands;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.core.commands.extra.ArgumentParser;
import space.arim.libertybans.core.commands.extra.DurationPermission;
import space.arim.libertybans.core.commands.extra.DurationPermissionsConfig;
import space.arim.libertybans.core.commands.extra.ReasonsConfig;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.config.AdditionsSection;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(CommandSetupExtension.class)
public class UnspecifiedReasonsTest {

	private final PunishmentDrafter drafter;
	private final InternalFormatter formatter;
	private final EnvEnforcer<?> envEnforcer;
	private final TabCompletion tabCompletion;
	private final AdditionsSection.BanAddition section;
	private final ReasonsConfig reasonsConfig;
	private PunishCommands punishCommands;

	public UnspecifiedReasonsTest(@Mock PunishmentDrafter drafter, @Mock InternalFormatter formatter,
								  @Mock EnvEnforcer<?> envEnforcer, @Mock TabCompletion tabCompletion,
								  @Mock AdditionsSection.BanAddition section, @Mock ReasonsConfig reasonsConfig) {
		this.drafter = drafter;
		this.formatter = formatter;
		this.envEnforcer = envEnforcer;
		this.tabCompletion = tabCompletion;
		this.section = section;
		this.reasonsConfig = reasonsConfig;
	}

	@BeforeEach
	public void setConfigSection(AbstractSubCommandGroup.Dependencies dependencies,
			/* Mock */ Configs configs, /* Mock */ ArgumentParser argParser) {
		{
			MainConfig mainConfig = mock(MainConfig.class);
			when(configs.getMainConfig()).thenReturn(mainConfig);
			when(mainConfig.reasons()).thenReturn(reasonsConfig);
			when(mainConfig.durationPermissions()).thenReturn(new DurationPermissionsConfig() {
				@Override
				public boolean enable() { return false; }

				@Override
				public Set<DurationPermission> permissionsToCheck() { return Set.of(); }
			});
		}
		{
			MessagesConfig messagesConfig = mock(MessagesConfig.class);
			when(configs.getMessagesConfig()).thenReturn(messagesConfig);
			AdditionsSection additionsSection = mock(AdditionsSection.class);
			when(messagesConfig.additions()).thenReturn(additionsSection);
			when(additionsSection.forType(PunishmentType.BAN)).thenReturn(section);
		}
		when(argParser.parseVictimByName(any(), eq("A248"))).thenReturn(
				new IndifferentFactoryOfTheFuture().completedFuture(PlayerVictim.of(UUID.randomUUID())));
		punishCommands = new PlayerPunishCommands(dependencies, drafter, formatter, envEnforcer, tabCompletion);
	}

	private void executeBan(CmdSender sender) {
		when(sender.hasPermission(any())).thenReturn(true);
		punishCommands.execute(sender, new ArrayCommandPackage("ban", "A248"), "ban").execute();
	}

	private void executeBanSuccessful(CmdSender sender, String reason) {
		DraftPunishmentBuilder draftBuilder = Mockito.mock(DraftPunishmentBuilder.class, Mockito.RETURNS_SELF);
		when(drafter.draftBuilder()).thenReturn(draftBuilder);
		// Stop execution and make assertions once the main code proceeds to the draft builder
		RuntimeException ex = new RuntimeException();
		when(draftBuilder.build()).thenThrow(ex);

		RuntimeException thrown = assertThrows(RuntimeException.class, () -> executeBan(sender));
		assertSame(ex, thrown, "Some other, unforeseen exception was thrown");
		verify(draftBuilder).reason(reason);
	}

	@Test
	public void useEmptyReason(@Mock CmdSender sender) {
		when(reasonsConfig.effectiveUnspecifiedReasonBehavior())
				.thenReturn(ReasonsConfig.UnspecifiedReasonBehavior.USE_EMPTY_REASON);
		executeBanSuccessful(sender, "");
	}

	@Test
	public void requireReason(@Mock CmdSender sender) {
		when(reasonsConfig.effectiveUnspecifiedReasonBehavior())
				.thenReturn(ReasonsConfig.UnspecifiedReasonBehavior.REQUIRE_REASON);
		Component usage = Component.text("usage");
		when(section.usage()).thenReturn(usage);
		executeBan(sender);
		verify(sender).sendMessage(usage);
	}

	@Test
	public void substituteDefaultReason(@Mock CmdSender sender) {
		String defaultReason = "default reason";
		when(reasonsConfig.effectiveUnspecifiedReasonBehavior())
				.thenReturn(ReasonsConfig.UnspecifiedReasonBehavior.SUBSTITUTE_DEFAULT);
		when(reasonsConfig.defaultReason()).thenReturn(defaultReason);
		executeBanSuccessful(sender, defaultReason);
	}
}
