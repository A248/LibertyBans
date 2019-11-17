package space.arim.bans.internal.config;

import space.arim.bans.internal.Replaceable;

public interface ConfigMaster extends Replaceable {
	
	String getMessage(String key);
	
	String[] getMessages(String key);
	
	public String getString(String key);

	public String[] getStrings(String key);

	public boolean getBoolean(String key);

	public int getInt(String key);
}
