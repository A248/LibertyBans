package space.arim.libertybans.core.punish;

import jakarta.inject.Singleton;
import space.arim.libertybans.api.Operator;

@Singleton
public class UndoDraftCreator {

    //TODO: Add reason to EnforcementOpts
    public UndoDraft fromEnforcementOptions(EnforcementOpts enforcementOpts) {
        return new UndoDraft(enforcementOpts.unOperator(), enforcementOpts.reason());
    }

    public UndoDraft create(Operator operator, String reason) {
        return new UndoDraft(operator, reason);
    }
}
