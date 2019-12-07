package space.arim.bans.extended;

import java.util.HashSet;
import java.util.Set;

import net.md_5.bungee.api.plugin.Plugin;

import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.PunishmentPlugin;
import space.arim.bans.extended.bungee.CommandSkeleton;
import space.arim.registry.UniversalRegistry;

public class ArimBansExtendedBungee extends Plugin implements ArimBansExtendedPluginBase {

	private ArimBansExtended extended = null;
	private Set<CommandSkeleton> cmds = new HashSet<CommandSkeleton>();
	
	private void shutdown(String message) {
		getLogger().warning("ArimBansExtended shutting down! Reason: " + message);
		throw new IllegalStateException("Shuttind down...");
	}
	
	@Override
	public void onEnable() {
		try {
			Class.forName("space.arim.bans.api.ArimBansLibrary");
			Class.forName("space.arim.registry.UniversalRegistry");
		} catch (ClassNotFoundException ex) {
			shutdown("ArimBansLibrary / UniversalRegistry not on classpath!");
			return;
		}
		PunishmentPlugin plugin = UniversalRegistry.getRegistration(PunishmentPlugin.class);
		if (plugin != null) {
			if (plugin instanceof ArimBansLibrary) {
				extended = new ArimBansExtended((ArimBansLibrary) plugin, getDataFolder());
			} else {
				shutdown("PunishmentPlugin is not an instance of ArimBansLibrary.");
			}
		} else {
			shutdown("No PunishmentPlugin's registered!");
		}
		loadCmds();
	}
	
	private void loadCmds() {
		for (String cmd : ArimBansExtended.commands()) {
			cmds.add(new CommandSkeleton(this, cmd));
		}
	}
	
	@Override
	public void onDisable() {
		close();
	}
	
	@Override
	public ArimBansExtended extension() {
		return extended;
	}
	
	public Set<String> getTabComplete(String[] args) {
		Set<String> playerNames = new HashSet<String>();
		if (args.length == 0) {
			getProxy().getPlayers().forEach((player) -> {
				playerNames.add(player.getName());
			});
		} else if (args.length == 1) {
			getProxy().getPlayers().forEach((player) -> {
				if (player.getName().toLowerCase().startsWith(args[0])) {
					playerNames.add(player.getName());
				}
			});
		}
		return playerNames;
	}
	
}
