package space.arim.libertybans.core.punish;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.punish.UndoAttachment;

import java.time.Instant;

//TODO: Why is {@link SecurePunishment} called secure? Should this be called Secure too?
public class SecureUndoAttachment implements UndoAttachment {

    private final Operator operator;
    private final String reason;
    private final Instant time;

    SecureUndoAttachment(Operator operator, String reason, Instant time) {
        this.operator = operator;
        this.reason = reason;
        this.time = time;
    }

    @Override
    public Operator operator() {
        return operator;
    }

    @Override
    public String reason() {
        return reason;
    }

    @Override
    public Instant time() {
        return time;
    }
}
