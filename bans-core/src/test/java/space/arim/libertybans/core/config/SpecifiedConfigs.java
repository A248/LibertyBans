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

package space.arim.libertybans.core.config;

import jakarta.inject.Inject;
import space.arim.libertybans.core.importing.ImportConfig;
import space.arim.libertybans.core.selector.EnforcementConfig;
import space.arim.libertybans.core.uuid.RemoteApiBundle;
import space.arim.libertybans.core.uuid.ServerType;
import space.arim.libertybans.core.uuid.UUIDResolutionConfig;
import space.arim.libertybans.it.ConfigSpec;
import space.arim.libertybans.it.DatabaseInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SpecifiedConfigs implements Configs {

	private final StandardConfigs delegate;
	private final ConfigSpec spec;
	private final DatabaseInfo databaseInfo;

	@Inject
	public SpecifiedConfigs(StandardConfigs delegate, ConfigSpec spec, DatabaseInfo databaseInfo) {
		this.delegate = delegate;
		this.spec = spec;
		this.databaseInfo = databaseInfo;
	}
	
	@Override
	public SqlConfig getSqlConfig() {
		return new Delegator<>(SqlConfig.class, delegate.getSqlConfig()) {
			@Override
			Object replacementFor(SqlConfig original, String methodName) {
				if (methodName.equals("vendor")) {
					return spec.vendor();
				}
				if (methodName.equals("authDetails")) {
					return new SqlConfig.AuthDetails() {
						@Override
						public String host() {
							return "127.0.0.1";
						}
						@Override
						public int port() {
							return databaseInfo.port();
						}
						@Override
						public String database() {
							return databaseInfo.database();
						}
						@Override
						public String username() {
							return spec.vendor().userForITs();
						}
						@Override
						public String password() {
							return spec.vendor().passwordForITs();
						}
					};
				}
				return null;
			}
		}.proxy();
	}

	@Override
	public MainConfig getMainConfig() {
		return new Delegator<>(MainConfig.class, delegate.getMainConfig()) {
			@Override
			Object replacementFor(MainConfig original, String methodName) {
				if (methodName.equals("enforcement")) {
					return enforcement(original);
				}
				if (methodName.equals("uuidResolution")) {
					return uuidResolution();
				}
				return null;
			}
			private EnforcementConfig enforcement(MainConfig original) {
				return new Delegator<>(EnforcementConfig.class, original.enforcement()) {
					@Override
					Object replacementFor(EnforcementConfig original, String methodName) {
						if (methodName.equals("addressStrictness")) {
							return spec.addressStrictness();
						}
						return null;
					}
				}.proxy();
			}
			private UUIDResolutionConfig uuidResolution() {
				return new UUIDResolutionConfig() {
					@Override
					public ServerType serverType() {
						return spec.serverType();
					}

					@Override
					public RemoteApiBundle remoteApis() {
						return new RemoteApiBundle(List.of());
					}

					@Override
					public String forceGeyserPrefix() {
						return "";
					}
				};
			}
			
		}.proxy();
	}

	@Override
	public void startup() {
		delegate.startup();
	}

	@Override
	public void restart() {
		delegate.restart();
	}

	@Override
	public void shutdown() {
		delegate.shutdown();
	}

	@Override
	public MessagesConfig getMessagesConfig() {
		return delegate.getMessagesConfig();
	}

	@Override
	public ImportConfig getImportConfig() {
		return delegate.getImportConfig();
	}

	@Override
	public ScopeConfig getScopeConfig() {
		return delegate.getScopeConfig();
	}

	@Override
	public CompletableFuture<Boolean> reloadConfigs() {
		return delegate.reloadConfigs();
	}

}
