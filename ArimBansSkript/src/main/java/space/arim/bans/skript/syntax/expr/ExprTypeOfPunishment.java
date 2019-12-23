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
package space.arim.bans.skript.syntax.expr;

import org.eclipse.jdt.annotation.Nullable;

import org.bukkit.event.Event;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

public class ExprTypeOfPunishment extends SimpleExpression<PunishmentType> {

	private Expression<Punishment> punishment;
	
	@Override
	public Class<? extends PunishmentType> getReturnType() {
		return PunishmentType.class;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] exprs, int arg1, Kleenean arg2, ParseResult arg3) {
		punishment = (Expression<Punishment>) exprs[0];
		return true;
	}
	
	@Override
	public String toString(@Nullable Event evt, boolean debug) {
		return "punishment type of " + punishment.toString(evt, debug);
	}
	
	@Override
	@Nullable
	public PunishmentType[] get(Event evt) {
		return new PunishmentType[] {punishment.getSingle(evt).type()};
	}
	
}
