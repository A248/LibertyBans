package space.arim.bans.internal.config;

import space.arim.bans.internal.Replaceable;

public interface ConfigMaster extends Replaceable {
	public String getString(String key);

	public String[] getStrings(String key);

	public boolean parseBoolean(String key);

	public int parseInt(String key);
}
