/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

package space.arim.libertybans.core.punish;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import space.arim.api.jsonchat.adventure.util.Adventure5Compat;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.EnforcementOptions.Broadcasting;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.core.config.AdditionsSection;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.selector.EnforcementConfig;
import space.arim.libertybans.core.config.PunishmentAdditionSection;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.libertybans.core.punish.permission.PunishmentPermission;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StandardLocalEnforcerTest {

	static Stream<Arguments> notifications() {
		return Arrays.stream(PunishmentType.values()).flatMap(type ->
				Arrays.stream(Broadcasting.values()).map(broadcasting -> Arguments.of(type, broadcasting)));
	}

	@ParameterizedTest
	@MethodSource("notifications")
	void issueNotificationUsesSilentStatus(PunishmentType type, Broadcasting broadcasting) {
		FactoryOfTheFuture futures = new IndifferentFactoryOfTheFuture();
		Configs configs = mock(Configs.class);
		MessagesConfig messages = mock(MessagesConfig.class);
		AdditionsSection additions = mock(AdditionsSection.class);
		PunishmentAdditionSection.WithLayout section = mock(PunishmentAdditionSection.WithLayout.class);
		InternalFormatter formatter = mock(InternalFormatter.class);
		EnvEnforcer<Object> envEnforcer = mock(EnvEnforcer.class);
		Punishment punishment = mock(Punishment.class);
		UUID victimId = UUID.randomUUID();
		when(punishment.getType()).thenReturn(type);
		when(punishment.getVictim()).thenReturn(PlayerVictim.of(victimId));
		MainConfig mainConfig = mock(MainConfig.class);
		EnforcementConfig enforcementConfig = mock(EnforcementConfig.class);
		when(configs.getMainConfig()).thenReturn(mainConfig);
		when(mainConfig.enforcement()).thenReturn(enforcementConfig);
		when(enforcementConfig.addressStrictness()).thenReturn(AddressStrictness.NORMAL);
		when(configs.getMessagesConfig()).thenReturn(messages);
		when(messages.additions()).thenReturn(additions);
		when(additions.forType(type)).thenReturn(section);
		when(formatter.getPunishmentMessage(punishment)).thenReturn(futures.completedFuture(Component.empty()));
		// An offline victim: no enforcement callback runs, but staff notifications still must be formatted.
		when(envEnforcer.doForPlayerIfOnline(eq(victimId), any())).thenReturn(futures.completedFuture(null));

		ComponentText template = ComponentText.create(Component.text("%TARGET%: %SILENCE%"), Adventure5Compat.DEFAULT);
		Component notification = Component.text("formatted notification");
		boolean silent = broadcasting == Broadcasting.SILENT;
		if (broadcasting != Broadcasting.NONE) {
			when(section.successNotification()).thenReturn(template);
			when(formatter.formatNotificationIssue(any(), eq(punishment), eq(silent)))
					.thenReturn(futures.completedFuture(notification));
			when(envEnforcer.sendToThoseWithPermission(anyString(), eq(notification)))
					.thenReturn(futures.completedFuture(null));
		}
		StandardLocalEnforcer<Object> enforcer = new StandardLocalEnforcer<>(
				InstanceType.PROXY, configs, futures, null, mock(InternalScopeManager.class),
				null, null, formatter, envEnforcer, null);
		EnforcementOpts options = EnforcementOpts.builder().broadcasting(broadcasting).targetArgument("target").build();
		enforcer.enforceWithoutSynchronization(punishment, options).join();

		verify(formatter, never()).formatWithPunishment(any(), any());
		if (broadcasting == Broadcasting.NONE) {
			verify(formatter, never()).formatNotificationIssue(any(), any(), anyBoolean());
			verify(envEnforcer, never()).sendToThoseWithPermission(anyString(), any());
		} else {
			verify(formatter).formatNotificationIssue(template.replaceText("%TARGET%", "target"), punishment, silent);
			verify(envEnforcer).sendToThoseWithPermission(
					new PunishmentPermission(type, Mode.DO).notifyPermission(silent), notification);
		}
	}
}
