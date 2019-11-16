package space.arim.bans;

import org.bukkit.plugin.java.JavaPlugin;

import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.env.bukkit.BukkitEnv;

public class ArimBansBukkit extends JavaPlugin implements AutoCloseable {
	
	private ArimBans center;
	private BukkitEnv environment;
	
	private ArimBansLibrary library;
	
	private void load() {
		environment = new BukkitEnv(this);
		center = new ArimBans(this.getDataFolder(), environment);
		environment.setCenter(center);
		library = new ArimBansLibrary(center);
	}
	
	public ArimBansLibrary getLibrary() {
		return library;
	}
	
	@Override
	public void onEnable() {
		load();
	}

	@Override
	public void onDisable() {
		close();
	}
	
	@Override
	public void close() {
		try {
			center.close();
			environment.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void reload() {
		close();
		load();
	}
}
