package space.arim.libertybans.core.punish;

import jakarta.inject.Singleton;
import space.arim.libertybans.api.Operator;

import java.time.Instant;

@Singleton
public class UndoDraftCreator {

    public UndoDraft create(Operator operator, String reason) {
        return new UndoDraft(operator, reason);
    }
}
