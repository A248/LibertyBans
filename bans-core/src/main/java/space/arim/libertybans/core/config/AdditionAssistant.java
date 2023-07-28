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

package space.arim.libertybans.core.config;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.event.BasePunishEvent;
import space.arim.libertybans.api.punish.DraftSanction;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.addon.exempt.Exemption;
import space.arim.libertybans.core.commands.AbstractCommandExecution;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.commands.extra.NotificationMessage;
import space.arim.libertybans.core.punish.permission.PermissionBase;
import space.arim.libertybans.core.punish.permission.VictimPermissionCheck;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.event.FireEventWithTimeout;
import space.arim.libertybans.core.event.PostPunishEventImpl;
import space.arim.libertybans.core.punish.EnforcementOpts;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.concurrent.CompletionStage;

public final class AdditionAssistant {

	private final FactoryOfTheFuture futuresFactory;
	private final FireEventWithTimeout fireEventWithTimeout;
	private final InternalFormatter formatter;
	private final Exemption exemption;

	@Inject
	public AdditionAssistant(FactoryOfTheFuture futuresFactory, FireEventWithTimeout fireEventWithTimeout,
							 InternalFormatter formatter, Exemption exemption) {
		this.futuresFactory = futuresFactory;
		this.fireEventWithTimeout = fireEventWithTimeout;
		this.formatter = formatter;
		this.exemption = exemption;
	}

	public interface Client<S extends DraftSanction, I> {

		ReactionStage<@Nullable Victim> parseVictim(String targetArg);

		@Nullable I parseImplement();

		PermissionBase createPermission(I implement);

		VictimPermissionCheck createPermissionCheck(I implement);

		String exemptionCategory();

		@Nullable S buildDraftSanction(Victim victim, I implement, String targetArg);

		BasePunishEvent<S> constructEvent(S draftSanction);

	}

	public final class Execution<S extends DraftSanction, I> extends AbstractCommandExecution {

		private final PunishmentAdditionSection section;
		private final Client<S, I> client;

		private String targetArg;

		public Execution(CmdSender sender, CommandPackage command,
						 PunishmentAdditionSection section, Client<S, I> client) {
			super(sender, command);
			this.section = section;
			this.client = client;
		}

		@Override
		public @Nullable ReactionStage<Void> execute() {
			if (targetArg != null) {
				throw new IllegalStateException("Cannot re-use instances");
			}
			if (!command().hasNext()) {
				sender().sendMessage(section.usage());
				return null;
			}
			targetArg = command().next();
			// Parse victim
			return client.parseVictim(targetArg).thenCompose((victim) -> {
				if (victim == null) {
					return futuresFactory.completedFuture(null);
				}
				// Parse implement
				I implement = client.parseImplement();
				if (implement == null) {
					return futuresFactory.completedFuture(null);
				}
				// Check permission
				var permissionCheck = client.createPermissionCheck(implement);
				if (!permissionCheck.checkPermission(victim, section)) {
					return futuresFactory.completedFuture(null);
				}
				// Check exemption
				return exemption.isVictimExempt(sender(), client.exemptionCategory(), victim).thenCompose((isExempt) -> {
					if (isExempt) {
						sender().sendMessage(section.exempted().replaceText("%TARGET%", targetArg));
						return futuresFactory.completedFuture(null);
					}
					// Punish
					return punishByImplement(
							victim, implement,
							new NotificationMessage(
									sender(), client.createPermission(implement)
							)
					);
				});
			});
		}

		private CompletionStage<Void> punishByImplement(Victim victim, I implement, NotificationMessage notificationMessage) {
			// Evaluate silence
			notificationMessage.evaluate(command());
			// Create draft sanction
			S draftSanction = client.buildDraftSanction(victim, implement, targetArg);
			if (draftSanction == null) {
				return futuresFactory.completedFuture(null);
			}
			// Fire event, enact, enforce
			return fireEventWithTimeout
					.fire(client.constructEvent(draftSanction))
					.thenCompose((event) -> {
						if (event.isCancelled()) {
							return futuresFactory.completedFuture(null);
						}
						return handleEnact(event);
					})
					.thenCompose((punishment) -> {
						if (punishment == null) {
							return futuresFactory.completedFuture(null);
						}
						return enforceAndSendSuccess(punishment, targetArg, notificationMessage);
					});
		}

		private CompletionStage<Punishment> handleEnact(BasePunishEvent<S> event) {
			S draftSanction = event.getDraftSanction();
			// Enforce the punishment later, after we are sure it is valid
			EnforcementOptions enforcementOptions = draftSanction
					.enforcementOptionsBuilder()
					.enforcement(EnforcementOptions.Enforcement.NONE)
					.broadcasting(EnforcementOptions.Broadcasting.NONE)
					.build();
			return draftSanction.enactPunishment(enforcementOptions).thenApply((optPunishment) -> {
				if (optPunishment.isEmpty()) {
					sender().sendMessage(
							section.conflicting().replaceText("%TARGET%", targetArg)
					);
					return null;
				}
				return optPunishment.get();
			});
		}

		private CentralisedFuture<Void> enforceAndSendSuccess(Punishment punishment, String targetArg,
															  NotificationMessage notificationMessage) {

			EnforcementOptions enforcementOptions = EnforcementOpts
					.builder()
					.enforcement(EnforcementOptions.Enforcement.GLOBAL)
					.broadcasting(notificationMessage.isSilent() ?
							EnforcementOptions.Broadcasting.SILENT : EnforcementOptions.Broadcasting.NORMAL
					)
					.targetArgument(targetArg)
					.build();
			CentralisedFuture<?> enforcement = punishment
					.enforcePunishment(enforcementOptions)
					.toCompletableFuture();
			CentralisedFuture<Component> futureMessage = formatter.formatWithPunishment(
					section.successMessage().replaceText("%TARGET%", targetArg), punishment);

			return futuresFactory.allOf(enforcement, futureMessage).thenCompose((ignore) -> {
				return fireEventWithTimeout.fire(new PostPunishEventImpl(punishment, targetArg));
			}).thenRun(() -> {
				sender().sendMessage(futureMessage.join());
			});
		}
	}

}
