package space.arim.bans.api.exception;

import java.io.File;

public class ConfigLoadException extends InternalStateException {

	private static final long serialVersionUID = -6838417590106589911L;

	public ConfigLoadException(File file) {
		super("File " + file.getPath() + " is invalid!");
	}
	
	public ConfigLoadException(File file, Exception cause) {
		super("File " + file.getPath() + " is invalid!", cause);
	}
	
	public ConfigLoadException(String file, Exception cause) {
		super("Configuration for " + file + " encountered an error!", cause);
	}

}
