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

package space.arim.libertybans.api.punish;

import java.util.Objects;

/**
 * Identifies the associated escalation track for a punishment. <br>
 * <br>
 * For example, a certain rulebreaking infraction such as spamming the chat may be associated to progressively
 * stricter warnings and/or mutes. The escalation track here might be identified as "chat_spamming". <br>
 * <br>
 * Note that the use of colons in namespaces and values is not permitted, as the colon is a special character
 * reserved for the implementation. Moreover, it is highly recommended to use namespaces and values without any
 * whitespace, for convenience in executing user commands.
 *
 */
public final class EscalationTrack {

	/**
	 * The default namespace for escalation tracks. This is typically used by the main implementation
	 * of LibertyBans
	 *
	 */
	public static final String DEFAULT_NAMESPACE = "default";

	private final String namespace;
	private final String value;

	private EscalationTrack(String namespace, String value) {
		if (namespace.contains(":") || value.contains(":")) {
			throw new IllegalArgumentException("Neither namespace nor value may contain colons");
		}
		this.namespace = Objects.requireNonNull(namespace, "namespace");
		this.value = Objects.requireNonNull(value, "value");
	}

	/**
	 * Creates a track with a given namespace and value
	 *
	 * @param namespace the namespace
	 * @param value the value
	 * @return the escalation track
	 */
	public static EscalationTrack create(String namespace, String value) {
		return new EscalationTrack(namespace, value);
	}

	/**
	 * Creates a track in the default namespace
	 *
	 * @param value the value
	 * @return the escalation track
	 */
	public static EscalationTrack createDefault(String value) {
		return create(DEFAULT_NAMESPACE, value);
	}

	/**
	 * Gets the namespace
	 *
	 * @return the namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Gets the value
	 *
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EscalationTrack that = (EscalationTrack) o;
		return namespace.equals(that.namespace) && value.equals(that.value);
	}

	@Override
	public int hashCode() {
		int result = namespace.hashCode();
		result = 31 * result + value.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "EscalationTrack{" +
				"namespace='" + namespace + '\'' +
				", value='" + value + '\'' +
				'}';
	}

}
