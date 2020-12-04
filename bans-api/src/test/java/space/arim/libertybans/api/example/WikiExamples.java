/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.api.example;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.ReactionStage;

import space.arim.libertybans.api.LibertyBans;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.revoke.PunishmentRevoker;
import space.arim.libertybans.api.revoke.RevocationOrder;
import space.arim.libertybans.api.select.PunishmentSelector;

public class WikiExamples {

	private final LibertyBans libertyBans = findLibertyBansInstance();
	
	private static LibertyBans findLibertyBansInstance() {
		 Omnibus omnibus = OmnibusProvider.getOmnibus();
		 LibertyBans instance = omnibus.getRegistry().getProvider(LibertyBans.class);
		 if (instance == null) {
			 throw new IllegalStateException("LibertyBans not found");
		 }
		 return instance;
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
			logger.info("ID of the enacted punishment is {}", optPunishment.get().getID());
		});
	}
	
	public ReactionStage<List<Punishment>> getMutesFrom(UUID staffMemberUuid) {
		PunishmentSelector selector = libertyBans.getSelector();

		return selector.selectionBuilder().operator(PlayerOperator.of(staffMemberUuid)).type(PunishmentType.MUTE)
				.build().getAllSpecificPunishments();
	}

	public ReactionStage<?> revokeBanFor(UUID bannedPlayer) {
		PunishmentRevoker revoker = libertyBans.getRevoker();

		// Relies on the fact a single victim can only have 1 active ban
		RevocationOrder revocationOrder = revoker.revokeByTypeAndVictim(PunishmentType.BAN, PlayerVictim.of(bannedPlayer));
		return revocationOrder.undoPunishment().thenAccept((undone) -> {
			if (undone) {
				// ban existed and was undone
			} else {
				// there was no ban
			}
		});
	}

}
