package space.arim.libertybans.api.punish;

import space.arim.libertybans.api.Operator;

import java.time.Instant;

/**
 * This interface encapsulates additional information present for historical punishments
 * that have been undone.
 */
public interface UndoAttachment {

    Operator operator();

    String reason();

    Instant time();

}
