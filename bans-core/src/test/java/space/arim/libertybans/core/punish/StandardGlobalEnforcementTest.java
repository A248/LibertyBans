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

package space.arim.libertybans.core.punish;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.SqlConfig;
import space.arim.libertybans.core.punish.sync.MessageReceiver;
import space.arim.libertybans.core.punish.sync.SynchronizationMessenger;
import space.arim.libertybans.core.punish.sync.SynchronizationProtocol;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.util.concurrent.ThreadLocalRandom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StandardGlobalEnforcementTest {

	private final SqlConfig.Synchronization conf;
	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
	private final LocalEnforcer enforcer;
	private final SynchronizationProtocol synchronizationProtocol = new SynchronizationProtocol(futuresFactory);
	private final SynchronizationMessenger synchronizationMessenger;

	private StandardGlobalEnforcement globalEnforcement;

	public StandardGlobalEnforcementTest(@Mock SqlConfig.Synchronization conf,
										 @Mock LocalEnforcer enforcer,
										 @Mock SynchronizationMessenger synchronizationMessenger) {
		this.conf = conf;
		this.enforcer = enforcer;
		this.synchronizationMessenger = synchronizationMessenger;
	}

	@BeforeEach
	public void setGlobalEnforcement(@Mock Configs configs) {
		lenient().when(enforcer.enforceWithoutSynchronization(any(), any())).thenReturn(futuresFactory.completedFuture(null));
		lenient().when(enforcer.unenforceWithoutSynchronization(any(), any())).thenReturn(futuresFactory.completedFuture(null));
		lenient().when(enforcer.unenforceWithoutSynchronization(anyLong(), any(), any())).thenReturn(futuresFactory.completedFuture(null));

		lenient().when(synchronizationMessenger.dispatch(any())).thenReturn(futuresFactory.completedFuture(null));

		SqlConfig sqlConfig = mock(SqlConfig.class);
		lenient().when(configs.getSqlConfig()).thenReturn(sqlConfig);
		lenient().when(sqlConfig.synchronization()).thenReturn(conf);

		globalEnforcement = new StandardGlobalEnforcement(
				configs, futuresFactory, enforcer,
				synchronizationProtocol, () -> synchronizationMessenger, mock(MessageReceiver.class));
	}

	private Punishment punishmentWithType(PunishmentType type) {
		long id = ThreadLocalRandom.current().nextLong();
		Punishment punishment = mock(Punishment.class);
		lenient().when(punishment.getIdentifier()).thenReturn(id);
		lenient().when(punishment.getType()).thenReturn(type);
		return punishment;
	}

	private void enableSync() {
		when(conf.enabled()).thenReturn(true);
	}

	// No enforcement

	@Test
	public void noEnforcement() {
		PunishmentType type = PunishmentType.BAN;
		Punishment punishment = punishmentWithType(type);

		EnforcementOpts enforcementOpts = EnforcementOpts
				.builder()
				.enforcement(EnforcementOptions.Enforcement.NONE)
				.build();
		globalEnforcement.enforce(punishment, enforcementOpts).join();
		globalEnforcement.unenforce(punishment, enforcementOpts).join();
		globalEnforcement.unenforce(punishment.getIdentifier(), type, enforcementOpts).join();
		verifyNoMoreInteractions(synchronizationMessenger, enforcer);
	}

	// Single server only

	@Test
	public void enforceSingleServerOnly() {
		Punishment punishment = punishmentWithType(PunishmentType.WARN);

		EnforcementOpts enforcementOpts = EnforcementOpts
				.builder()
				.enforcement(EnforcementOptions.Enforcement.SINGLE_SERVER_ONLY)
				.build();
		globalEnforcement.enforce(punishment, enforcementOpts).join();
		verify(enforcer).enforceWithoutSynchronization(punishment, enforcementOpts);
		verifyNoMoreInteractions(synchronizationMessenger);
	}

	@Test
	public void unenforceSingleServerOnly() {
		Punishment punishment = punishmentWithType(PunishmentType.MUTE);

		EnforcementOpts enforcementOpts = EnforcementOpts
				.builder()
				.enforcement(EnforcementOptions.Enforcement.SINGLE_SERVER_ONLY)
				.build();
		globalEnforcement.unenforce(punishment, enforcementOpts).join();
		verify(enforcer).unenforceWithoutSynchronization(punishment, enforcementOpts);
		verifyNoMoreInteractions(synchronizationMessenger);
	}

	@ParameterizedTest
	@EnumSource(PunishmentType.class)
	public void unenforceByIdAndTypeSingleServerOnly(PunishmentType type) {
		Punishment punishment = punishmentWithType(type);

		EnforcementOpts enforcementOpts = EnforcementOpts
				.builder()
				.enforcement(EnforcementOptions.Enforcement.SINGLE_SERVER_ONLY)
				.build();
		globalEnforcement.unenforce(punishment.getIdentifier(), type, enforcementOpts).join();
		verify(enforcer).unenforceWithoutSynchronization(punishment.getIdentifier(), type, enforcementOpts);
		verifyNoMoreInteractions(synchronizationMessenger);
	}

	// Global but sync disabled

	@Test
	public void enforceGlobalButSyncDisabled() {
		Punishment punishment = punishmentWithType(PunishmentType.WARN);

		EnforcementOpts enforcementOpts = EnforcementOpts
				.builder()
				.enforcement(EnforcementOptions.Enforcement.GLOBAL)
				.build();
		globalEnforcement.enforce(punishment, enforcementOpts).join();
		verify(enforcer).enforceWithoutSynchronization(punishment, enforcementOpts);
		verifyNoMoreInteractions(synchronizationMessenger);
	}

	@Test
	public void unenforceGlobalButSyncDisabled() {
		Punishment punishment = punishmentWithType(PunishmentType.MUTE);

		EnforcementOpts enforcementOpts = EnforcementOpts
				.builder()
				.enforcement(EnforcementOptions.Enforcement.GLOBAL)
				.build();
		globalEnforcement.unenforce(punishment, enforcementOpts).join();
		verify(enforcer).unenforceWithoutSynchronization(punishment, enforcementOpts);
		verifyNoMoreInteractions(synchronizationMessenger);
	}

	@ParameterizedTest
	@EnumSource(PunishmentType.class)
	public void unenforceByIdAndTypeGlobalButSyncDisabled(PunishmentType type) {
		Punishment punishment = punishmentWithType(type);

		EnforcementOpts enforcementOpts = EnforcementOpts
				.builder()
				.enforcement(EnforcementOptions.Enforcement.GLOBAL)
				.build();
		globalEnforcement.unenforce(punishment.getIdentifier(), type, enforcementOpts).join();
		verify(enforcer).unenforceWithoutSynchronization(punishment.getIdentifier(), type, enforcementOpts);
		verifyNoMoreInteractions(synchronizationMessenger);
	}

	// Global and sync enabled

	@Test
	public void enforceGlobal() {
		enableSync();

		Punishment punishment = punishmentWithType(PunishmentType.WARN);

		EnforcementOpts enforcementOpts = EnforcementOpts
				.builder()
				.enforcement(EnforcementOptions.Enforcement.GLOBAL)
				.build();
		globalEnforcement.enforce(punishment, enforcementOpts).join();
		verify(enforcer).enforceWithoutSynchronization(punishment, enforcementOpts);
		verify(synchronizationMessenger).dispatch(notNull());
	}

	@Test
	public void unenforceGlobal() {
		enableSync();

		Punishment punishment = punishmentWithType(PunishmentType.MUTE);

		EnforcementOpts enforcementOpts = EnforcementOpts
				.builder()
				.enforcement(EnforcementOptions.Enforcement.GLOBAL)
				.build();
		globalEnforcement.unenforce(punishment, enforcementOpts).join();
		verify(enforcer).unenforceWithoutSynchronization(punishment, enforcementOpts);
		verify(synchronizationMessenger).dispatch(notNull());
	}

	@ParameterizedTest
	@EnumSource(PunishmentType.class)
	public void unenforceByIdAndTypeGlobal(PunishmentType type) {
		enableSync();

		Punishment punishment = punishmentWithType(type);

		EnforcementOpts enforcementOpts = EnforcementOpts
				.builder()
				.enforcement(EnforcementOptions.Enforcement.GLOBAL)
				.build();
		globalEnforcement.unenforce(punishment.getIdentifier(), type, enforcementOpts).join();
		verify(enforcer).unenforceWithoutSynchronization(punishment.getIdentifier(), type, enforcementOpts);
		verify(synchronizationMessenger).dispatch(notNull());
	}
}
