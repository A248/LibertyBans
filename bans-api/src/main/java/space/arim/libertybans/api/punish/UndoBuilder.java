package space.arim.libertybans.api.punish;

import space.arim.libertybans.api.Operator;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Instant;

/**
 * This interface encapsulates additional information present for historical punishments
 * that have been undone.
 */
public interface UndoBuilder {

    UndoBuilder operator(Operator operator);

    UndoBuilder operatorAndReason(Operator operator, String reason);

    UndoBuilder clearOperatorAndReason();

    UndoBuilder enforcementOptions(EnforcementOptions enforcementOptions);

    ReactionStage<Boolean> undoPunishment();

    ReactionStage<Punishment> undoAndGetPunishment();

}
