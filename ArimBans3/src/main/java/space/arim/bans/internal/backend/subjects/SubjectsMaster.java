package space.arim.bans.internal.backend.subjects;

import java.util.UUID;

import space.arim.bans.api.Subject;
import space.arim.bans.internal.Replaceable;

public interface SubjectsMaster extends Replaceable {
	
	String display(Subject subject);
	
	Subject parseSubject(String input);
	
	Subject parseSubject(UUID input);
	
	boolean checkUUID(UUID uuid);

}
