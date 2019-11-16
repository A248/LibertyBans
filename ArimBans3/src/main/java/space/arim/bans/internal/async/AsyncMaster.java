package space.arim.bans.internal.async;

import java.util.List;

import space.arim.bans.internal.Replaceable;

public interface AsyncMaster extends Replaceable {
	public void execute(Runnable command);
	
	public boolean isShutdown();
	
	public void shutdown() throws Exception;
	
	public List<Runnable> shutdownNow();
}
