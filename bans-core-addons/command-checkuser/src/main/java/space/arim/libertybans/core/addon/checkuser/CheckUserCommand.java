package space.arim.libertybans.core.addon.checkuser;

import jakarta.inject.Inject;
import org.jetbrains.annotations.Nullable;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.commands.AbstractCommandExecution;
import space.arim.libertybans.core.commands.AbstractSubCommandGroup;
import space.arim.libertybans.core.commands.CommandExecution;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.stream.Stream;

public final class CheckUserCommand extends AbstractSubCommandGroup {

	private final PunishmentSelector selector;
	private final UUIDManager uuidManager;
	private final InternalFormatter formatter;
	private final CheckUserAddon checkUserAddon;

	@Inject
	public CheckUserCommand(Dependencies dependencies,
							PunishmentSelector selector, UUIDManager uuidManager, InternalFormatter internalFormatter, CheckUserAddon checkUserAddon) {
		super(dependencies, "checkuser");
		this.selector = selector;
		this.uuidManager = uuidManager;
		this.formatter = internalFormatter;
		this.checkUserAddon = checkUserAddon;
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command);
	}

	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		return Stream.empty();
	}

	@Override
	public boolean hasTabCompletePermission(CmdSender sender, String arg) {
		return hasPermission(sender);
	}

	private boolean hasPermission(CmdSender sender) {
		return sender.hasPermission("libertybans.addon.checkuser.use");
	}

	private final class Execution extends AbstractCommandExecution {

		private final CheckUserConfig config;

		private Execution(CmdSender sender, CommandPackage command) {
			super(sender, command);
			config = checkUserAddon.config();
		}

		@Override
		public @Nullable ReactionStage<Void> execute() {
			if (!hasPermission(sender())) {
				sender().sendMessage(config.noPermission());
				return null;
			}

			if (!command().hasNext()) {
				sender().sendMessage(config.usage());
				return null;
			}

			String name = command().next();

			return uuidManager.lookupPlayer(name).thenCompose(optUuidAddress -> {
				if (optUuidAddress.isEmpty()) {
					sender().sendMessage(config.doesNotExist());
					return completedFuture(null);
				}

				UUIDAndAddress uuidAddress = optUuidAddress.get();
				return selector.getApplicablePunishment(
						uuidAddress.uuid(), uuidAddress.address(), PunishmentType.BAN
				).thenCompose(optPunishment -> {

					if (optPunishment.isEmpty()) {
						return selector.getApplicablePunishment(
								uuidAddress.uuid(), uuidAddress.address(), PunishmentType.MUTE
						).thenCompose(optMute -> {

							if (optMute.isEmpty()) {
								sender().sendMessage(config.noPunishment());
								return completedFuture(null);
							}
							Punishment punishment = optMute.get();
							return formatter.formatWithPunishment(config.layout(), punishment)
									.thenAccept(sender()::sendMessage);
						});
					}
					Punishment punishment = optPunishment.get();
					return formatter.formatWithPunishment(config.layout(), punishment)
							.thenAccept(sender()::sendMessage);
				});
			});
		}
	}
}
