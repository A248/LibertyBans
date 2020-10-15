/* 
 * LibertyBans-integrationtest
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-integrationtest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-integrationtest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-integrationtest. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.it.test;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.it.ProfligateInstanceProvider;
import space.arim.libertybans.it.test.applicable.ApplicabilityTesting;
import space.arim.libertybans.it.test.applicable.NonapplicabilityTesting;

public class ApplicabilityIT {
	
	private static final List<Operator> operators = List.of(
			ConsoleOperator.INSTANCE,
			PlayerOperator.of(UUID.randomUUID()),
			PlayerOperator.of(UUID.randomUUID()));
	
	@ParameterizedTest
	@ArgumentsSource(ProfligateInstanceProvider.class)
	public void testApplicability(LibertyBansCore core) {
		for (PunishmentType type : MiscUtil.punishmentTypes()) {
			for (Operator operator : operators) {
				new ApplicabilityTesting(core, type, operator).doTest();
				new NonapplicabilityTesting(core, type, operator).doTest();
			}
		}
	}
	
}
