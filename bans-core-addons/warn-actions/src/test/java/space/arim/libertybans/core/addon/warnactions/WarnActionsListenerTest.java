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

package space.arim.libertybans.core.addon.warnactions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.event.PostPunishEvent;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.api.select.SelectionOrder;
import space.arim.libertybans.api.select.SelectionOrderBuilder;
import space.arim.libertybans.core.addon.AddonCenter;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.ParsedDuration;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.scope.ConfiguredScope;
import space.arim.omnibus.DefaultOmnibus;
import space.arim.omnibus.events.EventFireController;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WarnActionsListenerTest {

	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();

	private final PunishmentDrafter drafter;
	private final PunishmentSelector selector;
	private final InternalFormatter formatter;
	private final AddonCenter addonCenter;
	private final EnvEnforcer<?> envEnforcer;

	private final Punishment punishment;
	private final EventFireController controller;

	private WarnActionsListener warnActionsListener;

	public WarnActionsListenerTest(@Mock PunishmentDrafter drafter, @Mock PunishmentSelector selector,
								   @Mock InternalFormatter formatter, @Mock AddonCenter addonCenter,
								   @Mock EnvEnforcer<?> envEnforcer,
								   @Mock Punishment punishment, @Mock EventFireController controller) {
		this.drafter = drafter;
		this.selector = selector;
		this.formatter = formatter;
		this.addonCenter = addonCenter;
		this.envEnforcer = envEnforcer;
		this.punishment = punishment;
		this.controller = controller;
	}

	@BeforeEach
	public void setWarnActionsListener(@Mock ScopeManager scopeManager) {
		WarnActionsAddon addon = new WarnActionsAddon(addonCenter, new DefaultOmnibus(), () -> warnActionsListener);
		warnActionsListener = new WarnActionsListener(
				futuresFactory, drafter, scopeManager, selector, formatter, envEnforcer, addon
		);
	}

	private void fireEvent() {
		PostPunishEvent event = mock(PostPunishEvent.class);
		when(event.getPunishment()).thenReturn(punishment);
		warnActionsListener.acceptAndContinue(event, controller);
	}

	@Test
	public void skipNonWarn() {
		when(punishment.getType()).thenReturn(PunishmentType.BAN);
		fireEvent();
		verifyNoInteractions(selector, envEnforcer, drafter);
	}

	private void enableConfig(Map<Integer, String> autoCommands,
							  Map<Integer, WarnActionsConfig.WarnActionPunishment> autoPunishments) {
		when(punishment.getType()).thenReturn(PunishmentType.WARN);
		WarnActionsConfig config = mock(WarnActionsConfig.class);
		when(config.autoCommands()).thenReturn(autoCommands);
		when(config.autoPunishments()).thenReturn(autoPunishments);
		when(addonCenter.configurationFor(any())).thenAnswer((invocation) -> config);
	}

	@Test
	public void twoWarnsReachedImpliesNoActions(@Mock SelectionOrderBuilder selectionBuilder,
											   @Mock SelectionOrder selectionOrder) {
		enableConfig(
				Map.of(1, "too low command", 3, "too high command"),
				Map.of(1, mock(WarnActionsConfig.WarnActionPunishment.class), 3, mock(WarnActionsConfig.WarnActionPunishment.class))
		);
		when(selector.selectionBuilder()).thenReturn(selectionBuilder);
		when(selectionBuilder.victim(any())).thenReturn(selectionBuilder);
		when(selectionBuilder.seekBefore(any(), anyLong())).thenReturn(selectionBuilder);
		when(selectionBuilder.selectActiveOnly()).thenReturn(selectionBuilder);
		when(selectionBuilder.build()).thenReturn(selectionOrder);
		when(selectionOrder.countNumberOfPunishments()).thenReturn(futuresFactory.completedStage(2));
		fireEvent();
		verifyNoInteractions(envEnforcer, drafter);
	}

	@Test
	public void theThirdWarnIsReached(@Mock SelectionOrderBuilder selectionBuilder,
									  @Mock SelectionOrder selectionOrder,
									  @Mock WarnActionsConfig.WarnActionPunishment autoPunishment,
									  @Mock DraftPunishmentBuilder draftBuilder,
									  @Mock ServerScope autoPunishmentScope,
									  @Mock DraftPunishment draftPunishment,
									  @Mock EnforcementOptions.Builder enforcementOptionsBuilder,
									  @Mock EnforcementOptions enforcementOptions) {
		enableConfig(
				Map.of(1, "too low command", 3, "just right", 4, "too high again"),
				Map.of(1, mock(WarnActionsConfig.WarnActionPunishment.class), 3, autoPunishment, 4, mock(WarnActionsConfig.WarnActionPunishment.class))
		);
		when(selector.selectionBuilder()).thenReturn(selectionBuilder);
		when(selectionBuilder.victim(any())).thenReturn(selectionBuilder);
		when(selectionBuilder.seekBefore(any(), anyLong())).thenReturn(selectionBuilder);
		when(selectionBuilder.selectActiveOnly()).thenReturn(selectionBuilder);
		when(selectionBuilder.build()).thenReturn(selectionOrder);
		when(selectionOrder.countNumberOfPunishments()).thenReturn(futuresFactory.completedStage(3));

		when(formatter.formatWithPunishment(any(), eq(punishment))).thenAnswer((invocation) -> {
			return futuresFactory.completedFuture(invocation.getArgument(0, ComponentText.class).asComponent());
		});
		when(envEnforcer.executeConsoleCommand(any())).thenReturn(futuresFactory.completedFuture(null));
		when(drafter.draftBuilder()).thenReturn(draftBuilder);
		when(autoPunishment.type()).thenReturn(PunishmentType.MUTE);
		when(draftBuilder.type(PunishmentType.MUTE)).thenReturn(draftBuilder);
		when(punishment.getVictim()).thenReturn(PlayerVictim.of(UUID.randomUUID()));
		when(draftBuilder.victim(punishment.getVictim())).thenReturn(draftBuilder);
		when(draftBuilder.operator(ConsoleOperator.INSTANCE)).thenReturn(draftBuilder);
		when(autoPunishment.reason()).thenReturn("no reason at all");
		when(draftBuilder.reason("no reason at all")).thenReturn(draftBuilder);
		when(autoPunishment.duration()).thenReturn(new ParsedDuration("3h", Duration.ofHours(3L)));
		when(draftBuilder.duration(Duration.ofHours(3L))).thenReturn(draftBuilder);
		when(autoPunishment.scope()).thenReturn(ConfiguredScope.create(autoPunishmentScope));
		when(draftBuilder.scope(autoPunishmentScope)).thenReturn(draftBuilder);
		when(draftBuilder.build()).thenReturn(draftPunishment);
		when(draftPunishment.enforcementOptionsBuilder()).thenReturn(enforcementOptionsBuilder);
		when(enforcementOptionsBuilder.enforcement(any())).thenReturn(enforcementOptionsBuilder);
		when(enforcementOptionsBuilder.broadcasting(any())).thenReturn(enforcementOptionsBuilder);
		when(enforcementOptionsBuilder.build()).thenReturn(enforcementOptions);
		when(draftPunishment.enactPunishment(enforcementOptions)).thenReturn(
				futuresFactory.completedFuture(Optional.of(mock(Punishment.class)))
		);
		fireEvent();
		verify(envEnforcer).executeConsoleCommand("just right");
		verify(draftPunishment).enactPunishment(enforcementOptions);
	}

	@AfterEach
	public void verifyController() {
		verify(controller).continueFire();
		verifyNoMoreInteractions(controller);
	}

}
