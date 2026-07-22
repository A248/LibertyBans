/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import space.arim.api.jsonchat.adventure.util.Adventure5Compat;
import space.arim.injector.Identifier;
import space.arim.injector.InjectorBuilder;
import space.arim.injector.SpecificationSupport;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.Payload;
import space.arim.libertybans.bootstrap.PlatformId;
import space.arim.libertybans.bootstrap.PlatformLauncher;
import space.arim.libertybans.core.ApiBindModule;
import space.arim.libertybans.core.CommandsModule;
import space.arim.libertybans.core.PillarOneBindModule;
import space.arim.libertybans.core.PillarTwoBindModule;
import space.arim.libertybans.core.addon.AddonLoader;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.libertybans.env.adventure5common.Adventure5CompatLayer;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

import java.nio.file.Path;

public final class VelocityLauncher implements PlatformLauncher {

	private final Payload<PluginContainer> payload;
	private final ProxyServer server;
	private final Omnibus omnibus;

	public VelocityLauncher(Payload<PluginContainer> payload, ProxyServer server) {
		this(payload, server, OmnibusProvider.getOmnibus());
	}

	public VelocityLauncher(Payload<PluginContainer> payload, ProxyServer server, Omnibus omnibus) {
		this.payload = payload;
		this.server = server;
		this.omnibus = omnibus;
	}

	// Visible for testing
	static int extractMajorVer(String version) {
		// Strip anything after these characters, so we get a plain X.Y.Z version
		for (char discardAfter : new char[] {' ', '+', '-'}) {
			int discardAfterIdx = version.indexOf(discardAfter);
			if (discardAfterIdx != -1) {
				version = version.substring(0, discardAfterIdx);
			}
		}
		int firstPeriod = version.indexOf('.');
		if (firstPeriod != -1) {
			version = version.substring(0, firstPeriod);
		}
		try {
			return Integer.parseInt(version);
		} catch (NumberFormatException ignored) {
			return -1;
		}
	}

	@Override
	public BaseFoundation launch() {
		String proxyVersion = server.getVersion().getVersion();
		int majorVersion = extractMajorVer(proxyVersion);
		Adventure5Compat adventure5Compat = switch (majorVersion) {
			case 3 -> Adventure5Compat.DEFAULT;
			case 4 -> new Adventure5CompatLayer();
			default ->
				throw new UnsupportedOperationException(
						"Velocity major version unknown or not supported. Received proxy version '" + proxyVersion +
								"', from which extracted major version " + majorVersion + '.'
				);
		};
		return new InjectorBuilder()
				.bindInstance(PluginContainer.class, payload.plugin())
				.bindInstance(ProxyServer.class, server)
				.bindInstance(PlatformId.class, payload.platformId())
				.bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), payload.pluginFolder())
				.bindInstance(InstanceType.class, InstanceType.PROXY)
				.bindInstance(Omnibus.class, omnibus)
				.addBindModules(
						new ApiBindModule(),
						new PillarOneBindModule(),
						new PillarTwoBindModule(),
						new CommandsModule(),
						new VelocityBindModule())
				.bindInstance(Adventure5Compat.class, adventure5Compat)
				.addBindModules(AddonLoader.loadAddonBindModules())
				.specification(SpecificationSupport.JAKARTA)
				.multiBindings(true)
				.build()
				.request(BaseFoundation.class);
	}
}
