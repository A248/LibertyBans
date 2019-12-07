package space.arim.bans.extended.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import space.arim.bans.api.Subject;
import space.arim.bans.extended.ArimBansExtendedBukkit;

public class CommandListener implements CommandExecutor {

	private final ArimBansExtendedBukkit plugin;
	
	public CommandListener(ArimBansExtendedBukkit main) {
		this.plugin = main;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (plugin.enabled()) {
			Subject subject;
			if (sender instanceof Player) {
				subject = plugin.extension().getLib().fromUUID(((Player) sender).getUniqueId());
			} else if (sender instanceof ConsoleCommandSender) {
				subject = Subject.console();
			} else {
				return true;
			}
			plugin.extension().fireCommand(subject, command.getName(), args);
			return true;
		}
		return false;
	}
	
}
