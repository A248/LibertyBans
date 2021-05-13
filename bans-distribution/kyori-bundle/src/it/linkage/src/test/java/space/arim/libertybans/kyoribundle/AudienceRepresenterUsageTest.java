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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import space.arim.api.env.bukkit.BukkitAudienceRepresenter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AudienceRepresenterUsageTest {

	private final HelperClassLoader helper = new HelperClassLoader();

	private void sendMessageToSender0(CommandSender sender)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		// new BukkitAudienceRepresenter().toAudience(sender).sendMessage(Component.empty())
		Object audienceRepresenter = helper.constructObject(BukkitAudienceRepresenter.class);
		Method toAudience = helper.translateClass(BukkitAudienceRepresenter.class).getMethod("toAudience", CommandSender.class);
		Object audience = toAudience.invoke(audienceRepresenter, sender);
		Class<?> componentClass = helper.translateClass(Component.class);
		Object emptyComponent = helper.callStaticMethod(componentClass, "empty");
		Method sendMessageMethod = helper.translateClass(Audience.class).getMethod("sendMessage", componentClass);
		assertDoesNotThrow(() -> sendMessageMethod.invoke(audience, emptyComponent));
		helper.assertHasClass("net.kyori.adventure.audience.Audience");
	}

	private void sendMessageToSender(CommandSender sender) {
		try {
			sendMessageToSender0(sender);
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
			throw Assertions.<RuntimeException>fail(ex);
		}
	}

	@Test
	public void sendMessageToConsole() {
		sendMessageToSender(mock(CommandSender.class));
		helper.assertHasClass("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
	}

	@Test
	public void sendMessageToPlayer() {
		Player player = mock(Player.class);
		when(player.spigot()).thenReturn(mock(Player.Spigot.class));
		sendMessageToSender(player);
		helper.assertHasClass("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
	}
}
