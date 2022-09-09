package space.arim.libertybans.core.addon.checkuser;

import space.arim.injector.MultiBinding;
import space.arim.libertybans.core.addon.Addon;
import space.arim.libertybans.core.addon.AddonBindModule;
import space.arim.libertybans.core.commands.SubCommandGroup;

public final class CheckUserModule extends AddonBindModule {

	@MultiBinding
	public Addon<?> checkUserAddon(CheckUserAddon checkUserAddon) {
		return checkUserAddon;
	}

	@MultiBinding
	public SubCommandGroup checkUserCommand(CheckUserCommand checkUserCommand) {
		return checkUserCommand;
	}
}
