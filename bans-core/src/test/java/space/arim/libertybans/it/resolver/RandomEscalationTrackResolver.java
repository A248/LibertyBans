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

package space.arim.libertybans.it.resolver;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.it.util.RandomUtil;

import java.util.concurrent.ThreadLocalRandom;

public class RandomEscalationTrackResolver extends TypeBasedParameterResolver<EscalationTrack> {

	@Override
	public EscalationTrack resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		if (!parameterContext.isAnnotated(NonNullTrack.class) && ThreadLocalRandom.current().nextBoolean()) {
			return null;
		}
		String namespace = RandomUtil.randomString(1, 64);
		String value = RandomUtil.randomString(1, 64);
		return EscalationTrack.create(namespace, value);
	}

}
