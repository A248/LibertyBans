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

package space.arim.libertybans.core.addon.exempt;

import jakarta.inject.Inject;
import space.arim.injector.MultiBinding;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.Set;
import java.util.concurrent.CompletionStage;

public final class Exemption {

	private final FactoryOfTheFuture futuresFactory;
	private final Set<ExemptProvider> providers;

	@Inject
	public Exemption(FactoryOfTheFuture futuresFactory, @MultiBinding Set<ExemptProvider> providers) {
		this.futuresFactory = futuresFactory;
		this.providers = providers;
	}

	public CompletionStage<Boolean> isVictimExempt(CmdSender sender, String category, Victim target) {
		CompletionStage<Boolean> isExemptFuture = null;
		for (ExemptProvider provider : providers) {
			var thisFuture = provider.isExempted(sender, category, target);
			if (isExemptFuture == null) {
				isExemptFuture = thisFuture;
			} else {
				isExemptFuture = isExemptFuture.thenCombine(thisFuture, (isExempt1, isExempt2) -> isExempt1 || isExempt2);
			}
		}
		return (isExemptFuture == null) ? futuresFactory.completedFuture(false) : isExemptFuture;
	}

}
