/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.api;

/**
 * The console as an {@link Operator}
 * 
 * @author A248
 *
 */
public final class ConsoleOperator extends Operator {

	/**
	 * The console operator instance
	 * 
	 */
	public static final ConsoleOperator INSTANCE = new ConsoleOperator();
	
	private ConsoleOperator() {}
	
	/**
	 * Gets this operator's type: {@link OperatorType#CONSOLE}
	 * 
	 */
	@Override
	public OperatorType getType() {
		return OperatorType.CONSOLE;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public boolean equals(Object object) {
		return this == object;
	}
	
	@Override
	public String toString() {
		return "ConsoleOperator.INSTANCE";
	}
	
}
