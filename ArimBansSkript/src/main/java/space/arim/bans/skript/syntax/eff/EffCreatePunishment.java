/* 
 * ArimBansSkript, a skript addon for ArimBans
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansSkript is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansSkript is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansSkript. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.skript.syntax.eff;

import org.eclipse.jdt.annotation.Nullable;

import org.bukkit.event.Event;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.skript.ArimBansSkript;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;

public class EffCreatePunishment extends Effect {

	private Expression<PunishmentType> type;
	private Expression<Subject> subject;
	private Expression<Subject> operator;
	private Expression<String> reason;
	private Expression<Number> expiration;
	
	private static ArimBansSkript main;
	
	public static void setCenter(ArimBansSkript center) {
		main = center;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] exprs, int var2, Kleenean var3, ParseResult var4) {
		type = (Expression<PunishmentType>) exprs[0];
		subject = (Expression<Subject>) exprs[1];
		operator = (Expression<Subject>) exprs[2];
		reason = (Expression<String>) exprs[3];
		expiration = (Expression<Number>) exprs[4];
		return true;
	}

	@Override
	public String toString(@Nullable Event evt, boolean debug) {
		return "create new punishment from type " + type.toString(evt, debug) + ", subject " + subject.toString(evt, debug) + ", operator " + operator.toString(evt, debug) + ", reason " + reason.toString(evt, debug) + ", expiration " + expiration.toString(evt, debug);
	}

	@Override
	protected void execute(Event evt) {
		Punishment punishment = new Punishment(main.lib().getNextAvailablePunishmentId(), type.getSingle(evt), subject.getSingle(evt), operator.getSingle(evt), reason.getSingle(evt), expiration.getSingle(evt).longValue());
		main.setLatest(Punishment.class, punishment);
	}

}
