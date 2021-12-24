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

import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.EnforcementOptionsFactory;
import space.arim.libertybans.core.punish.sync.SynchronizationProtocol;

import java.util.Objects;
import java.util.Optional;

/**
 * New options added to this class, whether standardized in the API or added solely
 * here, MUST be serialized and deserialized in {@link SynchronizationProtocol}
 * in order for enforcement to work consistently across multiple instances
 *
 */
public final class EnforcementOpts implements EnforcementOptions {

	private final Enforcement enforcement;
	private final Broadcasting broadcasting;
	private final String targetArgument;
	private final Operator unOperator;

	private EnforcementOpts(Enforcement enforcement, Broadcasting broadcasting, String targetArgument, Operator unOperator) {
		this.enforcement = Objects.requireNonNull(enforcement, "enforcement");
		this.broadcasting = Objects.requireNonNull(broadcasting, "broadcast");
		this.targetArgument = targetArgument;
		this.unOperator = unOperator;
	}

	@Override
	public Enforcement enforcement() {
		return enforcement;
	}

	@Override
	public Broadcasting broadcasting() {
		return broadcasting;
	}

	/**
	 * Non-API extension
	 *
	 * @param notification the text to replace
	 * @return the replaced text
	 */
	public ComponentText replaceTargetArgument(ComponentText notification) {
		if (targetArgument == null) {
			return notification;
		}
		return notification.replaceText("%TARGET%", targetArgument);
	}

	/**
	 * Non-API extension
	 *
	 * @return the target argument
	 */
	public Optional<String> targetArgument() {
		return Optional.ofNullable(targetArgument);
	}

	/**
	 * Non-API extension
	 *
	 * @return the undoing operator
	 */
	public Optional<Operator> unOperator() {
		return Optional.ofNullable(unOperator);
	}

	interface Factory extends EnforcementOptionsFactory {

		@Override
		default EnforcementOptions.Builder enforcementOptionsBuilder() {
			return builder();
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder implements EnforcementOptions.Builder {

		private Enforcement enforcement = Enforcement.GLOBAL;
		private Broadcasting broadcasting = Broadcasting.NONE;
		private String targetArgument;
		private Operator unOperator;

		private Builder() {}

		@Override
		public Builder enforcement(Enforcement enforcement) {
			this.enforcement = Objects.requireNonNull(enforcement, "enforcement");
			return this;
		}

		@Override
		public Builder broadcasting(Broadcasting broadcasting) {
			this.broadcasting = Objects.requireNonNull(broadcasting, "broadcasting");
			return this;
		}

		/**
		 * Non-API extension
		 *
		 * @param targetArgument the target argument. May be null for none
		 * @return this builder
		 */
		public Builder targetArgument(String targetArgument) {
			this.targetArgument = targetArgument;
			return this;
		}

		/**
		 * Non-API extension
		 *
		 * @param unOperator the undoing operator. May be null for none
		 * @return this builder
		 */
		public Builder unOperator(Operator unOperator) {
			this.unOperator = unOperator;
			return this;
		}

		@Override
		public EnforcementOpts build() {
			return new EnforcementOpts(enforcement, broadcasting, targetArgument, unOperator);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Builder builder = (Builder) o;
			return enforcement == builder.enforcement && broadcasting == builder.broadcasting;
		}

		@Override
		public int hashCode() {
			int result = enforcement.hashCode();
			result = 31 * result + broadcasting.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return "EnforcementOpts.Builder{" +
					"enforcement=" + enforcement +
					", broadcasting=" + broadcasting +
					'}';
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EnforcementOpts that = (EnforcementOpts) o;
		return enforcement == that.enforcement && broadcasting == that.broadcasting;
	}

	@Override
	public int hashCode() {
		int result = enforcement.hashCode();
		result = 31 * result + broadcasting.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "EnforcementOpts{" +
				"enforcement=" + enforcement +
				", broadcasting=" + broadcasting +
				'}';
	}
}
