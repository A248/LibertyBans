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
import space.arim.bans.skript.ArimBansSkript;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

public class ExprLastPunishment extends SimpleExpression<Punishment> {

	private static ArimBansSkript main;
	
	public static void setCenter(ArimBansSkript center) {
		main = center;
	}
	
	@Override
	public Class<? extends Punishment> getReturnType() {
		return Punishment.class;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public boolean init(Expression<?>[] arg0, int arg1, Kleenean arg2, ParseResult arg3) {
		return true;
	}

	@Override
	public String toString(@Nullable Event evt, boolean debug) {
		Punishment punishment = main.getLatest(Punishment.class);
		return (punishment == null) ? null : punishment.toString();
	}

	@Override
	@Nullable
	protected Punishment[] get(Event evt) {
		return new Punishment[] {main.getLatest(Punishment.class)};
	}

}
