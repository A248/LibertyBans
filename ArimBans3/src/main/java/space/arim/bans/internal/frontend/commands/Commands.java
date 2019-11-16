package space.arim.bans.internal.frontend.commands;

import space.arim.bans.ArimBans;
import space.arim.bans.api.CommandType;
import space.arim.bans.api.Subject;

// TODO Make this class work
public class Commands implements CommandsMaster {
	private ArimBans center;
	private String perm_display;

	public Commands(ArimBans center) {
		this.center = center;
		refreshConfig();
	}

	public String formatTime(long unix) {
		if (unix < 0) {
			return perm_display;
		}
		return center.formatter().fromUnix(unix);
	}

	private String[] chopOffOne(String[] input) {
		String[] output = new String[input.length - 2];
		for (int n = 0; n < output.length; n++) {
			output[n] = input[n + 1];
		}
		return output;
	}
	
	@Override
	public void execute(Subject subject, String[] rawArgs) {
		try {
			CommandType type = parseCommand(rawArgs[0]);
			if (rawArgs.length > 1) {
				execute(subject, type, chopOffOne(rawArgs));
			} else {
				usage(subject, type);
			}
		} catch (IllegalArgumentException ex) {
			usage(subject);
		}
	}
	
	@Override
	public void execute(Subject subject, CommandType command, String[] extraArgs) {
		if (extraArgs.length > 0) {
			exec(subject, command, extraArgs);
		}
		usage(subject);
	}
	
	private void exec(Subject subject, CommandType command, String[] args) {
		
	}

	@Override
	public void usage(Subject subject, CommandType command) {

	}

	@Override
	public void usage(Subject subject) {

	}
	
	@Override
	public void close() {
		
	}

	@Override
	public void refreshConfig() {
		perm_display = center.config().getString("formatting.permanent-display");
	}

	@Override
	public void noPermission(Subject subject) {
		
	}

	@Override
	public void noPermission(Subject subject, CommandType command) {
		
	}
}
