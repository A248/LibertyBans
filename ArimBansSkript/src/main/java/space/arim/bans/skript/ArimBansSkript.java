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

import java.util.logging.Logger;

import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.Punishment;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.registrations.Classes;

public class ArimBansSkript implements AutoCloseable {
	
	private final Logger logger;
	private final ArimBansLibrary lib;
	
	ArimBansSkript(Logger logger, ArimBansLibrary lib) {
		this.logger = logger;
		this.lib = lib;
	}
	
	void registerAll() {
		registerEvents();
		registerConditions();
		registerEffects();
		registerExpressions();
		registerTypes();
	}
	
	private void registerEvents() {
		
	}
	
	private void registerConditions() {
		
	}
	
	private void registerEffects() {
		
	}
	
	private void registerExpressions() {
		
	}
	
	private void registerTypes() {
		Classes.registerClass(new ClassInfo<Punishment>(Punishment.class, "Punishment").parser(new Parser<Punishment>() {

			@Override
			public String getVariableNamePattern() {
				return null;
			}

			@Override
			public String toString(Punishment arg0, int arg1) {
				return null;
			}

			@Override
			public String toVariableNameString(Punishment punishment) {
				return punishment.toString();
			}
			
		}));
	}

	@Override
	public void close() {
		
	}
	
}
