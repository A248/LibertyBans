package space.arim.bans.internal.async;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Async implements AsyncMaster {
	private final ExecutorService threads;
	public Async() {
		refreshConfig();
		threads = Executors.newCachedThreadPool();
	}

	@Override
	public void execute(Runnable command) {
		threads.execute(command);
	}
	
	@Override
	public boolean isShutdown() {
		return (threads != null) ? threads.isShutdown() : true;
	}
	
	@Override
	public void shutdown() throws InterruptedException {
		try {
			threads.shutdown();
			threads.awaitTermination(12L, TimeUnit.SECONDS);
		} catch (InterruptedException ex) {
			throw ex;
		}
	}
	
	@Override
	public List<Runnable> shutdownNow() {
		return threads.shutdownNow();
	}
	
	@Override
	public void close() throws InterruptedException {
		if (!isShutdown()) {
			shutdown();
		}
	}

	@Override
	public void refreshConfig() {
		
	}
	
}
