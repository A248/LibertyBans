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
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.revoke.PunishmentRevoker;
import space.arim.libertybans.core.commands.extra.ArgumentParser;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.config.RemovalsSection;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.punish.EmptyRevocationOrder;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(CommandSetupExtension.class)
@ExtendWith(MockitoExtension.class)
public class PlayerUnpunishCommandsTest {

	private PlayerUnpunishCommands commands;
	private final PunishmentRevoker revoker;
	private final InternalFormatter formatter;
	private final EnvEnforcer<?> envEnforcer;
	private final TabCompletion tabCompletion;
	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();

	public PlayerUnpunishCommandsTest(@Mock PunishmentRevoker revoker,
									  @Mock InternalFormatter formatter,
									  @Mock EnvEnforcer<?> envEnforcer,
									  @Mock TabCompletion tabCompletion) {
		this.revoker = revoker;
		this.formatter = formatter;
		this.envEnforcer = envEnforcer;
		this.tabCompletion = tabCompletion;
	}

	@BeforeEach
	public void setPlayerUnpunishCommands(AbstractSubCommandGroup.Dependencies dependencies) {
		commands = new PlayerUnpunishCommands(dependencies, revoker, formatter, envEnforcer, tabCompletion);
	}

	@Test
	public void unbanExplicitAddress(@Mock CmdSender sender,
			/* Mock */ ArgumentParser argParser, /* Mock */ Configs configs,
									 @Mock MessagesConfig messagesConfig)
			throws UnknownHostException {
		String address = "167.65.44.109";
		Victim victim = AddressVictim.of(NetworkAddress.of(InetAddress.getByName(address)));

		when(sender.getOperator()).thenReturn(ConsoleOperator.INSTANCE);
		when(sender.hasPermission(any())).thenReturn(true);
		when(argParser.parseVictimByName(sender, address)).thenReturn(futuresFactory.completedFuture(victim));
		when(configs.getMessagesConfig()).thenReturn(messagesConfig);
		Component notFoundMsg = Component.text("Not found");
		{
			RemovalsSection.PunishmentRemoval punishmentRemoval = mock(RemovalsSection.PunishmentRemoval.class);
			when(punishmentRemoval.notFound()).thenReturn(ComponentText.create(notFoundMsg));
			RemovalsSection removalsSection = mock(RemovalsSection.class);
			when(removalsSection.forType(PunishmentType.BAN)).thenReturn(punishmentRemoval);
			when(messagesConfig.removals()).thenReturn(removalsSection);
		}

		when(revoker.revokeByTypeAndVictim(any(), any())).thenReturn(new EmptyRevocationOrder(futuresFactory));

		commands.execute(sender, new ArrayCommandPackage("libertybans", address), "unban").execute();
		verify(revoker).revokeByTypeAndVictim(PunishmentType.BAN, victim);
		verify(sender).sendMessage(argThat(new ComponentMatcher<>(notFoundMsg)));
	}
}
