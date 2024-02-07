package space.arim.libertybans.core.punish;

import jakarta.inject.Singleton;
import org.jooq.RecordMapper;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.punish.UndoAttachment;

import java.time.Instant;

@Singleton
public class SecureUndoAttachmentCreator {

    public UndoAttachment createUndoAttachment(Operator operator, String reason, Instant time)  {
        return new SecureUndoAttachment(operator, reason, time);
    }

}
