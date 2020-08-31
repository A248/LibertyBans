/* 
 * LibertyBans-env-bungee
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-bungee is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-bungee is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-bungee. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.bungee;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This is here in case we ever need to use it. At the moment, it is unused.
 * 
 * @author A248
 *
 */
@SuppressWarnings("unused")
public class NoMoreSecurityManager {

	static final boolean LEGIT_SECURITY_MANAGER;
	
	static {
		boolean legitSm = false;
		try {
			legitSm = Boolean.getBoolean("space.arim.libertybans.env.bungee.legitSecurityManager");
		} catch (SecurityException ignored) {}
		LEGIT_SECURITY_MANAGER = legitSm;
	}
	
	static void informAboutSecurityManagerIfNeeded(Logger logger) {
		SecurityManager sm = System.getSecurityManager();
		if (sm == null) {
			logger.info("Thank you for using Waterfall and not BungeeCord. Waterfall helps us and you.");

		} else {
			logger.warning(
					"You are using BungeeCord and its 'BungeeSecurityManager' is enabled. LibertyBans requires permissions "
					+ "to create reasonable thread pools per efficient connecting pooling. You may encounter some console spam.");
		}
	}
	
	/*static boolean attemptRemove(SecurityManager sm) {
		return nullifySystemFieldWithVarHandle() || replaceSeenSetWithReflection(sm);
	}*/
	
	private static boolean nullifySystemFieldWithVarHandle() {
		try {
			MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(System.class, MethodHandles.lookup());
			VarHandle handle = lookup.findStaticVarHandle(System.class, "security", SecurityManager.class);
			handle.setVolatile((SecurityManager) null);
			return System.getSecurityManager() == null;

		} catch (NoSuchFieldException | IllegalAccessException | SecurityException ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	private static boolean replaceSeenSetWithReflection(SecurityManager bungeeSecurityManager) {
		Class<?> managerClass = bungeeSecurityManager.getClass();
		if (managerClass.getSimpleName().equals("BungeeSecurityManager")) {
			try {
				Field seenField = managerClass.getDeclaredField("seen");
				seenField.setAccessible(true);
				Set<String> failingSet = new SilentlyFailingSet<String>();
				seenField.set(bungeeSecurityManager, failingSet);
				return seenField.get(bungeeSecurityManager) == failingSet;

			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
				ex.printStackTrace();
			}
		}
		return false;
	}
	
	private static class SilentlyFailingSet<T> extends HashSet<T> {

		/**
		 * Serial version uid
		 */
		private static final long serialVersionUID = -5172577632547946612L;
		
		@Override
		public boolean add(T element) {
			return false;
		}
		
	}
	
}
