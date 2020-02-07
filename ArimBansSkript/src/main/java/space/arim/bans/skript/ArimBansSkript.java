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
package space.arim.bans.skript;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.skript.syntax.eff.EffCreatePunishment;
import space.arim.bans.skript.syntax.expr.ExprLastError;
import space.arim.bans.skript.syntax.expr.ExprLastPunishment;
import space.arim.bans.skript.syntax.expr.ExprTypeOfPunishment;

import space.arim.universal.util.AutoClosable;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.registrations.Classes;

public class ArimBansSkript implements AutoClosable {
	
	private final ArimBansLibrary lib;
	private final Logger logger;
	
	private final HashMap<Class<?>, Object> syntaxTrading = new HashMap<Class<?>, Object>();
	
	ArimBansSkript(ArimBansLibrary lib, Logger logger) {
		this.lib = lib;
		this.logger = logger;
	}
	
	public ArimBansLibrary lib() {
		return lib;
	}
	
	void registerAll() {
		registerTypes();
		registerConditions();
		registerEffects();
		registerExpressions();
	}
	
	private Logger logger() {
		return logger;
	}
	
	private void registerConditions() {
		logger().log(Level.INFO, "Loaded conditions!");
	}
	
	private void registerEffects() {
		EffCreatePunishment.setCenter(this);
		Skript.registerEffect(EffCreatePunishment.class, "[arimbans] create new punishment [(from|with) [(info|information)]] type %punishmenttype%, subject %subject%, operator %subject%, reason %string%, expiration %number%");
		logger().log(Level.INFO, "Loaded effects!");
	}
	
	private void registerExpressions() {
		ExprLastError.setCenter(this);
		Skript.registerExpression(ExprLastError.class, String.class, ExpressionType.SIMPLE, "[arimbans] last error");
		ExprLastPunishment.setCenter(this);
		Skript.registerExpression(ExprLastPunishment.class, Punishment.class, ExpressionType.SIMPLE, "[arimbans] [the] last created punishment");
		Skript.registerExpression(ExprTypeOfPunishment.class, PunishmentType.class, ExpressionType.PROPERTY, "[arimbans] punishment type (of|for) %punishment%");
		logger().log(Level.INFO, "Loaded expressions!");
	}
	
	private void registerTypes() {
		registerType(PunishmentType.class, new Parser<PunishmentType>() {
			@Override
			public String toString(PunishmentType type, int var2) {
				return type.toString();
			}

			@Override
			public String toVariableNameString(PunishmentType type) {
				return "punishment_type:" + type.toString();
			}

			@Override
			public String getVariableNamePattern() {
				return "[A-Za-z]{3,4}";
			}
			
		});
		registerType(Subject.class, new Parser<Subject>() {
			@Override
			public String toString(Subject subject, int var2) {
				return subject.toString();
			}

			@Override
			public String toVariableNameString(Subject subject) {
				return "subject:" + subject;
			}
			
			@Override
			public String getVariableNamePattern() {
				return ".{7,53}";
			}
		});
		registerType(Punishment.class, new Parser<Punishment>() {
			@Override
			public String toString(Punishment punishment, int arg1) {
				return punishment.toString();
			}

			@Override
			public String toVariableNameString(Punishment punishment) {
				return "punishment:" + punishment.id() + "," + punishment.type() + "," + punishment.subject() + "," + punishment.operator() + "," + punishment.reason() + "," + punishment.expiration() + "," + punishment.date();
			}
			
			@Override
			public String getVariableNamePattern() {
				return "punishment:[0-9]+,[A-Za-z]{3,4},.{7,53},.{7,53},.+,[0-9]+,[0-9]+";
			}
		});
		logger().log(Level.INFO, "Loaded types!");
	}
	
	private <T> void registerType(Class<T> clazz, Parser<T> parser) {
		Classes.registerClass(new ClassInfo<T>(clazz, clazz.getSimpleName().toLowerCase()).parser(parser));
	}

	@SuppressWarnings("unchecked")
	public <T> T getLatest(Class<T> type) {
		return (T) syntaxTrading.get(type);
	}
	
	public <T> void setLatest(Class<T> type, T object) {
		syntaxTrading.put(type, object);
	}
	
	@Override
	public void close() {
		
	}
	
}
