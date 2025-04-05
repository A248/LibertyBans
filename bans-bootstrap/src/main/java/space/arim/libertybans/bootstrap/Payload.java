/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

package space.arim.libertybans.bootstrap;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record Payload<P>(P plugin, PlatformId platformId, Path pluginFolder, List<Object> attachments) {

    public static final Object NO_PLUGIN = new Object();

    public Payload {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(pluginFolder, "pluginFolder");
        attachments = List.copyOf(attachments);
    }

    public Payload(P plugin, PlatformId platformId, Path pluginFolder) {
        this(plugin, platformId, pluginFolder, List.of());
    }

    public <A> A getAttachment(int index, Class<A> attachmentType) {
        if (index > attachments.size()) {
            throw new IllegalArgumentException("Attachment requested does not exist: " + index);
        }
        Object attachment = attachments.get(index);
        if (!attachmentType.isInstance(attachment)) {
            throw new IllegalArgumentException(
                    "Attachment " + index + " is not ot type " + attachmentType + "; received " + attachment);
        }
        return attachmentType.cast(attachments.get(index));
    }

}
