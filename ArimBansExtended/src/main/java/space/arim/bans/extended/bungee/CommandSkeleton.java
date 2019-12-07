package space.arim.bans.extended.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.command.ConsoleCommandSender;

import space.arim.bans.api.Subject;
import space.arim.bans.extended.ArimBansExtendedBungee;

public class CommandSkeleton extends Command implements TabExecutor {

	private final ArimBansExtendedBungee plugin;
	
	public CommandSkeleton(ArimBansExtendedBungee plugin, String cmd) {
		super(cmd);
		this.plugin = plugin;
	}
	
	@Override
	public void execute(CommandSender sender, String[] args) {
		if (plugin.enabled()) {
			Subject subject;
			if (sender instanceof ProxiedPlayer) {
				subject = plugin.extension().getLib().fromUUID(((ProxiedPlayer) sender).getUniqueId());
			} else if (sender instanceof ConsoleCommandSender) {
				subject = Subject.console();
			} else {
				return;
			}
			plugin.extension().fireCommand(subject, getName(), args);
		}
	}
	
	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		return plugin.getTabComplete(args);
	}
	
}
