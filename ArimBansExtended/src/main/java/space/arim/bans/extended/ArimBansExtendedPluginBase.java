package space.arim.bans.extended;

public interface ArimBansExtendedPluginBase extends AutoCloseable {
	
	ArimBansExtended extension();
	
	default boolean enabled() {
		return extension() != null;
	}
	
	@Override
	default void close() {
		if (enabled()) {
			extension().close();
		}
	}
	
}
