/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.kyoribundle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AdventureInitializationTest {

	private final HelperClassLoader helper = new HelperClassLoader();

	@Test
	public void initializeComponent() {
		String className = "net.kyori.adventure.text.Component";
		helper.assertNotHasClass(className);
		// Call Component.empty()
		helper.callStaticMethod(className, "empty");
		helper.assertHasClass(className);
		helper.assertHasClass("net.kyori.examination.Examinable");
		helper.assertHasClass("net.kyori.adventure.key.Key");
		helper.assertNotHasClass("net.kyori.examination.string.StringExaminer");
	}

	@Test
	public void initializeStyle() {
		String className = "net.kyori.adventure.text.format.Style";
		helper.assertNotHasClass(className);
		// Call Style.empty().toString()
		Object identity = helper.callStaticMethod(className, "empty");
		assertDoesNotThrow(identity::toString); // Style#toString uses StringExaminer
		helper.assertHasClass(className);
		helper.assertHasClass("net.kyori.examination.Examinable");
		helper.assertHasClass("net.kyori.examination.string.StringExaminer");
	}

}
