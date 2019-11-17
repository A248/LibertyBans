package space.arim.bans.env;

import space.arim.bans.api.Punishment;
import space.arim.bans.internal.Configurable;

public interface Enforcer extends Configurable, AutoCloseable {
	void enforce(Punishment punishment);
	
	boolean callPunishEvent(Punishment punishment, boolean retro);
	
	boolean callUnpunishEvent(Punishment punishment, boolean automatic);
	
	void callPostPunishEvent(Punishment punishment, boolean retro);
	
	void callPostUnpunishEvent(Punishment punishment, boolean automatic);
}
