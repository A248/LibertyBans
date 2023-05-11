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

package space.arim.libertybans.core.importing;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.punish.Enaction;
import space.arim.libertybans.core.service.Time;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.omnibus.util.ThisClass;

import java.util.Optional;
import java.util.UUID;

public class ImportFunction {

	private final UUIDManager uuidManager;
	private final Time time;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public ImportFunction(UUIDManager uuidManager, Time time) {
		this.uuidManager = uuidManager;
		this.time = time;
	}

	Optional<Enaction.OrderDetails> createOrder(PortablePunishment punishment, ImportSink importSink) {
		Victim victim = toVictim(punishment.victimInfo(), importSink);
		if (victim == null) {
			return Optional.empty();
		}
		Operator operator = toOperator(punishment.operatorInfo(), importSink);
		if (operator == null) {
			return Optional.empty();
		}
		PortablePunishment.KnownDetails knownDetails = punishment.knownDetails();
		return Optional.of(new Enaction.OrderDetails(
				knownDetails.type(), victim, operator,
				knownDetails.reason(), knownDetails.scope(),
				knownDetails.start(), knownDetails.end(), null
		));
	}

	private Victim toVictim(PortablePunishment.VictimInfo victimInfo, ImportSink importSink) {
		Optional<Victim> overrideVictim = victimInfo.overrideVictim();
		if (overrideVictim.isPresent()) {
			return overrideVictim.get();
		}
		Optional<UUID> uuid = victimInfo.uuid();
		if (uuid.isPresent()) {
			return PlayerVictim.of(uuid.get());
		}
		Optional<NetworkAddress> address = victimInfo.address();
		if (address.isPresent()) {
			return AddressVictim.of(address.get());
		}
		String name = victimInfo.name()
				.orElseThrow(() -> new ImportException("Victim name must be present if uuid is not"));
		UUID foundUUID = uuidManager.lookupUUIDFromExactName(name).join().orElse(null);
		if (foundUUID == null) {
			logger.warn("Skipping punishment because victim uuid could not be found for name {}", victimInfo.name());
			return null;
		}
		importSink.addNameAddressRecord(new NameAddressRecord(foundUUID, name, null, time.currentTimestamp()));
		return PlayerVictim.of(foundUUID);
	}

	private Operator toOperator(PortablePunishment.OperatorInfo operatorInfo, ImportSink importSink) {
		if (operatorInfo.console()) {
			return ConsoleOperator.INSTANCE;
		}
		Optional<UUID> uuid = operatorInfo.uuid();
		if (uuid.isPresent()) {
			return PlayerOperator.of(uuid.get());
		}
		String name = operatorInfo.name()
				.orElseThrow(() -> new ImportException("Operator name must be present if uuid is not"));
		UUID foundUUID = uuidManager.lookupUUIDFromExactName(name).join().orElse(null);
		if (foundUUID == null) {
			logger.warn("Skipping punishment because operator uuid could not be found for name {}", name);
			return null;
		}
		importSink.addNameAddressRecord(new NameAddressRecord(foundUUID, name, null, time.currentTimestamp()));
		return PlayerOperator.of(foundUUID);
	}
}
