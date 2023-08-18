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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.api.jsonchat.adventure.util.TextGoal;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.event.PostPunishEvent;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.omnibus.events.AsynchronousEventConsumer;
import space.arim.omnibus.events.EventFireController;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

@Singleton
public final class WarnActionsListener implements AsynchronousEventConsumer<PostPunishEvent> {

	private final FactoryOfTheFuture futuresFactory;
	private final PunishmentDrafter drafter;
	private final ScopeManager scopeManager;
	private final PunishmentSelector selector;
	private final InternalFormatter formatter;
	private final EnvEnforcer<?> envEnforcer;
	private final WarnActionsAddon addon;

	@Inject
	public WarnActionsListener(FactoryOfTheFuture futuresFactory, PunishmentDrafter drafter, ScopeManager scopeManager,
							   PunishmentSelector selector, InternalFormatter formatter,
							   EnvEnforcer<?> envEnforcer, WarnActionsAddon addon) {
		this.futuresFactory = futuresFactory;
		this.drafter = drafter;
		this.scopeManager = scopeManager;
		this.selector = selector;
		this.formatter = formatter;
		this.envEnforcer = envEnforcer;
		this.addon = addon;
	}

	@Override
	public void acceptAndContinue(PostPunishEvent event, EventFireController controller) {
		Punishment punishment = event.getPunishment();
		if (punishment.getType() != PunishmentType.WARN) {
			controller.continueFire();
			return;
		}
		selector.selectionBuilder()
				.victim(punishment.getVictim())
				.seekBefore(punishment.getStartDate(), punishment.getIdentifier())
				.selectActiveOnly()
				.build()
				.countNumberOfPunishments()
				.thenCompose((numberOfWarns) -> {
					return new Handler(punishment, numberOfWarns).handleActions();
				})
				.whenComplete((ignore, ex) -> {
					if (ex != null) {
						Logger logger = LoggerFactory.getLogger(WarnActionsListener.class);
						logger.warn("Error while trying to administer warn actions", ex);
					}
					controller.continueFire();
				});
	}

	private final class Handler {

		private final Punishment warn;
		private final int numberOfWarns;
		private final WarnActionsConfig config = addon.config();

		private Handler(Punishment warn, int numberOfWarns) {
			this.warn = warn;
			this.numberOfWarns = numberOfWarns;
		}

		private ReactionStage<?> handleActions() {
			return futuresFactory.allOf(handleAutoCommand(), handleAutoPunish());
		}

		private CentralisedFuture<?> handleAutoCommand() {
			String command = config.autoCommands().get(numberOfWarns);
			if (command == null) {
				return futuresFactory.completedFuture(null);
			}
			// Turn command into ComponentText for purposes of variable replacement
			ComponentText commandText = ComponentText.create(
					Component.text(command), TextGoal.SIMPLE_TEXT
			);
			return formatter.formatWithPunishment(commandText, warn).thenCompose((formattedCommand) -> {
				String commandToExecute = ((TextComponent) formattedCommand).content();
				return envEnforcer.executeConsoleCommand(commandToExecute);
			});
		}

		private CentralisedFuture<?> handleAutoPunish() {
			WarnActionsConfig.WarnActionPunishment additionalPunishment = config.autoPunishments().get(numberOfWarns);
			if (additionalPunishment == null) {
				return futuresFactory.completedFuture(null);
			}
			DraftPunishment draftPunishment = drafter.draftBuilder()
					.type(additionalPunishment.type())
					.victim(warn.getVictim())
					.operator(ConsoleOperator.INSTANCE)
					.reason(additionalPunishment.reason())
					.duration(additionalPunishment.duration().duration())
					.scope(additionalPunishment.scope().actualize(scopeManager))
					.build();
			EnforcementOptions enforcementOptions = draftPunishment
					.enforcementOptionsBuilder()
					.enforcement(EnforcementOptions.Enforcement.GLOBAL)
					.broadcasting(additionalPunishment.broadcastNotification() ?
							EnforcementOptions.Broadcasting.NORMAL : EnforcementOptions.Broadcasting.NONE)
					.build();
			return draftPunishment.enactPunishment(enforcementOptions).toCompletableFuture();
		}
	}
}
