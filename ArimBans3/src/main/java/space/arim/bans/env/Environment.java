package space.arim.bans.env;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import space.arim.bans.api.Subject;
import space.arim.bans.internal.Configurable;

public interface Environment extends AutoCloseable, Configurable {
	
	void sendMessage(Subject target, String jsonable);
	
	boolean hasPermission(Subject subject, String...permissions);
	
	String getVersion();
	
	Logger logger();
	
	Enforcer enforcer();
	
	Resolver resolver();
	
	boolean isLibrarySupported(EnvLibrary library);
	
	enum EnvLibrary {
		
		BUKKIT("org.bukkit.Bukkit"),
		SPIGOT("org.spigotmc.SpigotConfig"),
		PAPER("com.destroystokyo.paper.PaperConfig"),
		BUNGEE("net.md_5.bungee.api.ProxyConfig");
		
		private final String uniqueClassName;
		
		private EnvLibrary(String uniqueClassName) {
			this.uniqueClassName = uniqueClassName;
		}
		
		public String uniqueClassName() {
			return uniqueClassName;
		}
		
	}
	
	default Set<EnvLibrary> loadLibraries() {
		HashSet<EnvLibrary> libraries = new HashSet<EnvLibrary>();
		for (EnvLibrary lib : EnvLibrary.values()) {
			try {
				Class.forName(lib.uniqueClassName());
				libraries.add(lib);
			} catch (ClassNotFoundException ex) {}
		}
		return libraries;
	}

}