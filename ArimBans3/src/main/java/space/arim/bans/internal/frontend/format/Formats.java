package space.arim.bans.internal.frontend.format;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.Subject;

//TODO Make this class work
public class Formats implements FormatsMaster {
	private ArimBans center;
	private SimpleDateFormat dateformatter;

	public Formats(ArimBans center) {
		this.center = center;
		refreshConfig();
	}

	@Override
	public String format(Punishment punishment) {
		
		return center.config().getString("messages");
	}
	
	@Override
	public String format(Subject subj) {
		return null;
	}

	@Override
	public String fromUnix(long unix) {
		return dateformatter.format(new Date(unix));
	}
	
	@Override
	public long toUnix(String timespan) {
		return -1L;
	}
	
	@Override
	public void close() {
		
	}
	
	@Override
	public void refreshConfig() {
		dateformatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		dateformatter.setTimeZone(TimeZone.getDefault());
	}

}
