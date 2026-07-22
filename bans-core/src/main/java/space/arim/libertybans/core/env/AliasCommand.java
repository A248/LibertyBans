/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

package space.arim.libertybans.core.env;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface AliasCommand {

    AliasCommand NO_OP_SUCCESS = new AliasCommand() {
        @Override
        public void register(RegisterOutcome outcome) {
            outcome.success();
        }

        @Override
        public void unregister() {}
    };

    void register(RegisterOutcome outcome);

    interface RegisterOutcome {

        void success();

        void deregistrationUnavailable();

        void alreadyRegistered(Object existingCommand, @Nullable String belongingTo);

        void disappear();

    }

    void unregister();

    default PlatformListener asListener(RegisterOutcome registerOutcome) {
        class AsListener implements PlatformListener {
            @Override
            public void register() {
                AliasCommand.this.register(registerOutcome);
            }

            @Override
            public void unregister() {
                AliasCommand.this.unregister();
            }
        }
        return new AsListener();
    }
}
