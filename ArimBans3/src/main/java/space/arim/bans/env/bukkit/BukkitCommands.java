package space.arim.bans.env.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import space.arim.bans.api.Subject;

public class BukkitCommands implements AutoCloseable, CommandExecutor {
	private final BukkitEnv environment;

	public BukkitCommands(final BukkitEnv environment) {
		this.environment = environment;
		refreshConfig();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Subject subject;
		if (sender instanceof Player) {
			subject = environment.center().subjects().parseSubject(((Player) sender).getUniqueId());
		} else if (sender instanceof ConsoleCommandSender) {
			subject = Subject.console();
		} else {
			return true;
		}
		if (args.length > 0) {
			this.environment.center().commands().execute(subject, args);
		} else {
			this.environment.center().commands().usage(subject);
		}
		return true;
	}

	public void refreshConfig() {
		
	}
	
	@Override
	public void close() throws Exception {
		
	}
}
