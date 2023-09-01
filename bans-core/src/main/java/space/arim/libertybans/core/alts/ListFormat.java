/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.core.alts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.core.config.InternalFormatter;

import java.util.ArrayList;
import java.util.List;

class ListFormat<T> {

	private final InternalFormatter formatter;
	private final ElementFormat<T> elementFormat;

	ListFormat(InternalFormatter formatter, ElementFormat<T> elementFormat) {
		this.formatter = formatter;
		this.elementFormat = elementFormat;
	}

	Component formatMessage(ComponentText header, String target, List<? extends T> data) {
		List<ComponentLike> messages = new ArrayList<>(data.size() + 1);
		messages.add(formatter.prefix(header.replaceText("%TARGET%", target)));
		for (T datum : data) {
			messages.add(Component.newline());
			messages.add(elementFormat.format(target, datum));
		}
		return TextComponent.ofChildren(messages.toArray(ComponentLike[]::new));
	}

	interface ElementFormat<T> {

		ComponentLike format(String target, T element);
	}
}
