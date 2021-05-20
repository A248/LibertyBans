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

package space.arim.libertybans.env.spigot;

import org.bukkit.BanEntry;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public final class SimpleBanEntry implements BanEntry {

	private final String target;

	private Date created;
	private String source;
	private Date expiration;
	private String reason;

	public SimpleBanEntry(String target) {
		this.target = Objects.requireNonNull(target, "target");
	}

	@Override
	public String getTarget() {
		return target;
	}

	@Override
	public Date getCreated() {
		return created;
	}

	@Override
	public void setCreated(Date created) {
		this.created = created;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public void setSource(String source) {
		this.source = source;
	}

	@Override
	public Date getExpiration() {
		return expiration;
	}

	@Override
	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}

	@Override
	public String getReason() {
		return reason;
	}

	@Override
	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public void save() {
		throw new UnsupportedOperationException();
	}

	public static Builder forUser(String user) {
		return new Builder(user);
	}

	public static Builder forAddress(InetAddress address) {
		return new Builder(address.getHostAddress());
	}

	public static final class Builder {

		private final String target;

		private Date created;
		private String source;
		private Date expiration;
		private String reason;

		private Builder(String target) {
			this.target = Objects.requireNonNull(target, "target");
		}

		public Builder created(Instant created) {
			this.created = Date.from(created);
			return this;
		}

		public Builder source(String source) {
			this.source = source;
			return this;
		}

		public Builder expiration(Instant expiration) {
			this.expiration = Date.from(expiration);
			return this;
		}

		public Builder reason(String reason) {
			this.reason = reason;
			return this;
		}

		public BanEntry build() {
			BanEntry entry = new SimpleBanEntry(target);
			entry.setCreated(Objects.requireNonNull(created, "missing created"));
			entry.setSource(Objects.requireNonNull(source, "missing source"));
			entry.setExpiration(expiration);
			entry.setReason(reason);
			return entry;
		}
	}
}
