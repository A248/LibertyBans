package space.arim.libertybans.core.addon.checkuser;

import space.arim.libertybans.core.addon.AddonBindModule;
import space.arim.libertybans.core.addon.AddonProvider;

public final class CheckUserProvider implements AddonProvider {
    @Override
    public AddonBindModule[] bindModules() {
        return new AddonBindModule[] {new CheckUserModule()};
    }
}
