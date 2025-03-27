package space.arim.libertybans.core.punish;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.UndoBuilder;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

//TODO: Why is {@link SecurePunishment} called secure? Should this be called Secure too?
public class SecureUndoBuilder implements UndoBuilder {

    private final SecurePunishmentCreator creator;
    private final SecurePunishment punishment;
    private final Operator operator;
    private final String reason;
    private final EnforcementOptions enforcementOptions;

    SecureUndoBuilder(SecurePunishmentCreator creator, SecurePunishment punishment, Operator operator, String reason, EnforcementOptions enforcementOptions) {
        this.creator = creator;
        this.punishment = punishment;
        this.operator = operator;
        this.reason = reason;
        this.enforcementOptions = enforcementOptions;
    }

    @Override
    public UndoBuilder operator(Operator operator) {
        return new SecureUndoBuilder(creator, punishment, operator, reason, enforcementOptions);
    }

    @Override
    public UndoBuilder operatorAndReason(Operator operator, String reason) {
        return new SecureUndoBuilder(creator, punishment, operator, reason, enforcementOptions);
    }

    @Override
    public UndoBuilder clearOperatorAndReason() {
        return new SecureUndoBuilder(creator, punishment, operator, reason, enforcementOptions);
    }

    @Override
    public UndoBuilder enforcementOptions(EnforcementOptions enforcementOptions) {
        return new SecureUndoBuilder(creator, punishment, operator, reason, enforcementOptions);
    }

    @Override
    public ReactionStage<Boolean> undoPunishment() {
        return creator.revoker().undoPunishment(punishment, operator, reason)
                .thenCompose((undone) -> {
                    if (!undone) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return punishment.unenforcePunishment(enforcementOptions).thenApply((ignore) -> true);
                });
    }

    @Override
    public ReactionStage<Punishment> undoAndGetPunishment() {
        return undoPunishment()
                .thenCompose(aBoolean -> {

                });
    }
}
