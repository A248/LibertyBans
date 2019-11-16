package space.arim.bans;

import net.md_5.bungee.api.plugin.Plugin;

import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.env.bungee.BungeeEnv;

public class ArimBansBungee extends Plugin implements AutoCloseable {

	private ArimBans center;
	private BungeeEnv environment;
	
	private ArimBansLibrary library;
	
	private void load() {
		environment = new BungeeEnv(this);
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
