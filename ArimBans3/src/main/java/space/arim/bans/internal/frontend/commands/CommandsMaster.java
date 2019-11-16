package space.arim.bans.internal.frontend.commands;

import space.arim.bans.api.CommandType;
import space.arim.bans.api.Subject;
import space.arim.bans.internal.Replaceable;

public interface CommandsMaster extends Replaceable {
	
	void execute(Subject subject, String[] rawArgs);
	
	void execute(Subject subject, CommandType command, String[] extraArgs);

	default CommandType parseCommand(String input) {
		for (CommandType type : CommandType.values()) {
			if (type.toString().equalsIgnoreCase(input)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Input '" + input + "' could not be parsed as a CommandType!");
	}

	void usage(Subject subject);
	
	void usage(Subject subject, CommandType command);
	
	void noPermission(Subject subject);
	
	void noPermission(Subject subject, CommandType command);

}
