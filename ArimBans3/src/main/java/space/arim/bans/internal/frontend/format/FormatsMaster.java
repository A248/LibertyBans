package space.arim.bans.internal.frontend.format;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.Subject;
import space.arim.bans.internal.Replaceable;

public interface FormatsMaster extends Replaceable {
	
	String format(Punishment punishment);
	
	String format(Subject subj);
	
	String fromUnix(long unix);
	
	long toUnix(String timespan);
	
}
