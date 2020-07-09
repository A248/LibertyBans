/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.commands;

public class ArrayCommandPackage extends CommandPackage {

	private final String[] args;
	
	private transient int position = 0;
	
	public ArrayCommandPackage(String command, String[] args) {
		super(command);
		this.args = args;
	}
	
	@Override
	public String next() {
		return args[position++];
	}
	
	@Override
	public String peek() {
		return args[position];
	}
	
	@Override
	public boolean hasNext() {
		return args.length > position;
	}
	
	@Override
	public String allRemaining() {
		StringBuilder result = new StringBuilder();
		for (int n = position; n < args.length; n++) {
			if (n != position) {
				result.append(' ');
			}
			result.append(args[n]);
		}
		return result.toString();
	}

	@Override
	public CommandPackage clone() {
		ArrayCommandPackage result = new ArrayCommandPackage(getCommand(), args);
		result.position = position;
		return result;
	}

}
