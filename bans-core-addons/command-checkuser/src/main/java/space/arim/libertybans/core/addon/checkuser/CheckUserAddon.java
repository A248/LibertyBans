package space.arim.libertybans.core.addon.checkuser;

import jakarta.inject.Inject;
import space.arim.libertybans.core.addon.AbstractAddon;
import space.arim.libertybans.core.addon.AddonCenter;

public final class CheckUserAddon extends AbstractAddon<CheckUserConfig> {

    @Inject
    public CheckUserAddon(AddonCenter addonCenter) {
        super(addonCenter);
    }

    @Override
    public void startup() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public Class<CheckUserConfig> configInterface() {
        return CheckUserConfig.class;
    }

    @Override
    public String identifier() {
        return "command-checkuser";
    }
}
