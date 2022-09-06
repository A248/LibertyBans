package space.arim.libertybans.core.addon.checkuser;

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.libertybans.core.addon.AddonConfig;

public interface CheckUserConfig extends AddonConfig {

    @ConfKey("no-permission")
    @ConfDefault.DefaultString("&cYou do not have permission to check user data")
    Component noPermission();

    @ConfDefault.DefaultString("&cUsage: /libertybans checkuser <player>")
    Component usage();

    @ConfKey("player-does-not-exist")
    @ConfDefault.DefaultString("&cThat player does not exist")
    Component doesNotExist();

    @ConfKey("punishment-does-not-exist")
    @ConfDefault.DefaultString("&cThat player doesn't have any active punishment.")
    Component noPunishment();

    @ConfDefault.DefaultStrings({
            "&7Active punishment for player &e%VICTIM%",
            "&7Type: &e%TYPE%",
            "&7Reason: &e%REASON%",
            "&7Operator: &e%OPERATOR%",
            "Time remaining: &e%TIME_REMAINING%",
    })
    ComponentText layout();
}
