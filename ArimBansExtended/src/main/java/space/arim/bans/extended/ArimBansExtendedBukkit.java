package space.arim.bans.extended;

import org.bukkit.plugin.java.JavaPlugin;

import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.PunishmentPlugin;
import space.arim.bans.extended.bukkit.CommandListener;
import space.arim.registry.UniversalRegistry;

public class ArimBansExtendedBukkit extends JavaPlugin implements ArimBansExtendedPluginBase {

	private ArimBansExtended extended = null;
	private CommandListener cmds;
	
	private void shutdown(String message) {
		getLogger().warning("ArimBansExtended shutting down! Reason: " + message);
		getServer().getPluginManager().disablePlugin(this);
		throw new IllegalStateException("Shutting down...");
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
		cmds = new CommandListener(this);
		registerCommands();
	}
	
	private void registerCommands() {
		for (String cmd : ArimBansExtended.commands()) {
			getServer().getPluginCommand(cmd).setExecutor(cmds);
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

}
