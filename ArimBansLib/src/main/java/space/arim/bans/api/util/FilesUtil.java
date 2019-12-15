/*
 * ArimBans3, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans3. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Files utility.
 * 
 * @author A248
 *
 */
public class FilesUtil {

	private FilesUtil() {}
	
	/**
	 * Utilises class <code>com.google.common.io.ByteStreams</code> <br>
	 * <br>
	 * <b>If that class is not on the classpath do not call this method!</b>
	 * 
	 * @param target - the file to save to
	 * @param input - the source from which to save. Use <code>YourClass.class.getResourceAsStream(File.separator + "config.yml")<code>
	 * @return true if the saving was successful
	 * @throws IOException if an error occurred
	 */
	public static boolean saveFromStream(File target, InputStream input) throws IOException {
		if (target.createNewFile()) {
			try (FileOutputStream output = new FileOutputStream(target)){
				com.google.common.io.ByteStreams.copy(input, output);
				return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getFromConfigMap(Map<String, Object> map, String key, Class<T> type) {
		if (!key.contains(".")) {
			Object obj = map.get(key);
			return (type.isInstance(obj)) ? (T) obj : null;
		}
		return getFromConfigMap((Map<String, Object>) map.get(key.substring(0, key.indexOf("."))), key.substring(key.indexOf(".") + 1), type);
	}
	
	public static boolean generateBlankFile(File file) {
		if (file.exists() && file.canRead() && file.canWrite()) {
			return true;
		} else if (file.exists()) {
			file.delete();
		}
		try {
			return file.createNewFile();
		} catch (IOException ex) {
			return false;
		}
	}
	
}
