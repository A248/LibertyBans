package space.arim.bans.env.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;
import space.arim.bans.api.Subject;

public class BungeeCommands extends Command implements AutoCloseable {

	private BungeeEnv environment;
	
	public BungeeCommands(BungeeEnv environment) {
		super("arimbans");
		this.environment = environment;
		refreshConfig();
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		Subject subject;
		if (sender instanceof ProxiedPlayer) {
			subject = environment.center().subjects().parseSubject(((ProxiedPlayer) sender).getUniqueId());
		} else if (sender instanceof ConsoleCommandSender){
			subject = Subject.console();
		} else {
			return;
		}
		if (args.length > 0) {
			environment.center().commands().execute(subject, args);
		} else {
			environment.center().commands().usage(subject);
		}
	}
	
	@Override
	public void close() throws Exception {
		
	}
	
	public void refreshConfig() {
		
	}

}
