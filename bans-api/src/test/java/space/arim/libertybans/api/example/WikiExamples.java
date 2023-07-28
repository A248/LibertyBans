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

package space.arim.libertybans.api.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.LibertyBans;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.event.PunishEvent;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.punish.PunishmentRevoker;
import space.arim.libertybans.api.punish.RevocationOrder;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;
import space.arim.omnibus.events.EventConsumer;
import space.arim.omnibus.events.ListenerPriorities;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WikiExamples {

	private final Omnibus omnibus;
	private final LibertyBans libertyBans;

	public WikiExamples(Omnibus omnibus, LibertyBans libertyBans) {
		this.omnibus = omnibus;
		this.libertyBans = libertyBans;
	}

	public static WikiExamples create() {
		Omnibus omnibus = OmnibusProvider.getOmnibus();
		LibertyBans libertyBans = omnibus.getRegistry().getProvider(LibertyBans.class).orElseThrow();
		return new WikiExamples(omnibus, libertyBans);
	}
	
	private final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	public void banPlayerUsingLibertyBans(UUID uuidToBan) {

		PunishmentDrafter drafter = libertyBans.getDrafter();

		DraftPunishment draftBan = drafter.draftBuilder().type(PunishmentType.BAN).victim(PlayerVictim.of(uuidToBan))
				.reason("Because I said so").build();

		draftBan.enactPunishment().thenAcceptSync((optPunishment) -> {

			// In this example it is assumed you have a logger
			// You should not copy and paste examples verbatim
			if (optPunishment.isEmpty()) {
				logger.info("UUID {} is already banned", uuidToBan);
				return;
			}
			logger.info("ID of the enacted punishment is {}", optPunishment.get().getIdentifier());
		});
	}

	public ReactionStage<List<Punishment>> getMutesFrom(UUID staffMemberUuid) {
		return libertyBans.getSelector()
				.selectionBuilder()
				.type(PunishmentType.MUTE)
				.operator(PlayerOperator.of(staffMemberUuid))
				.build()
				.getAllSpecificPunishments();
	}

	public ReactionStage<Optional<Punishment>> getMutesApplyingTo(UUID playerUuid, InetAddress playerAddress) {
		return libertyBans.getSelector()
				.selectionByApplicabilityBuilder(playerUuid, playerAddress)
				.type(PunishmentType.MUTE)
				.build()
				.getFirstSpecificPunishment();
	}

	public ReactionStage<?> revokeBanFor(UUID bannedPlayer) {
		PunishmentRevoker revoker = libertyBans.getRevoker();

		// Relies on the fact a player victim can only have 1 active ban
		RevocationOrder revocationOrder = revoker.revokeByTypeAndVictim(PunishmentType.BAN, PlayerVictim.of(bannedPlayer));
		return revocationOrder.undoPunishment().thenAccept((undone) -> {
			if (undone) {
				// ban existed and was undone
			} else {
				// there was no ban
			}
		});
	}

	public void listenToPunishEvent() {
		EventConsumer<PunishEvent> listener = new EventConsumer<>() {
			@Override
			public void accept(PunishEvent event) {
				logger.info("Listening to punish event {}", event);
			}
		};
		omnibus.getEventBus().registerListener(PunishEvent.class, ListenerPriorities.NORMAL, listener);
	}

}
