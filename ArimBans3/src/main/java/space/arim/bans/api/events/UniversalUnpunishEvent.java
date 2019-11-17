package space.arim.bans.api.events;

public interface UniversalUnpunishEvent {
	/**
	 * Whenever ArimBans internally retrieves its active punishments list,
	 * it will clear all expired punishments.
	 * 
	 * <br><br>In such cases, a UnpunishEvent is deemed <b>automatic</b>
	 * 
	 * @return true if this UnpunishEvent is automatic
	 */
	boolean isAutomatic();
}
