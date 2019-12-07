/*
 * ArimBansExtended, an extension for the ArimBans core
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansExtended is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansExtended is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansExtended. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.extended.bungee;

import de.exceptionflug.protocolize.api.event.PacketReceiveEvent;
import de.exceptionflug.protocolize.api.handler.PacketAdapter;
import de.exceptionflug.protocolize.api.protocol.Stream;
import de.exceptionflug.protocolize.items.packet.BlockPlacement;
import space.arim.bans.api.PunishmentResult;
import space.arim.bans.extended.ArimBansExtendedBungee;

public class SignInterceptor extends PacketAdapter<BlockPlacement> {

	private final ArimBansExtendedBungee plugin;
	
	public SignInterceptor(ArimBansExtendedBungee plugin) {
		super(Stream.UPSTREAM, BlockPlacement.class);
		this.plugin = plugin;
	}
	
	@SuppressWarnings("unused")
	@Override
	public void receive(PacketReceiveEvent<BlockPlacement> evt) {
		if (plugin.enabled() && plugin.extension().antiSignEnabled() && !evt.getPacket().isCancelSend()) {
			if (false) { // Need to check if placed block is a sign
				PunishmentResult result = plugin.extension().getLib().getApplicableBan(evt.getPlayer().getUniqueId(), evt.getPlayer().getAddress().getAddress().getHostAddress());
				if (result.hasPunishment()) {
					evt.getPacket().setCancelSend(true);
					plugin.extension().getLib().sendMessage(evt.getPlayer().getUniqueId(), result.getApplicableMessage());
				}
			}
		}
	}

}
