/*
 * LibertyBans
 * Copyright © 2020 Anand Beh
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

/**
 * Implements importing from other plugins. <br>
 * <br>
 * How punishment data is stored differs across plugins, especially with regards to UUIDs
 * and names. Importers for specific plugins implement {@link space.arim.libertybans.core.importing.ImportSource} <br>
 * <br>
 * <b>AdvancedBan</b> <br>
 * AdvancedBan has no dedicated persistent uuid/name cache. The plugin does store victim names
 * and UUIDs alongside each punishment. Also, it does not retain the operator uuid, but rather
 * uses operator names. This requires a lookup during the import process. <br>
 * <br>
 * <b>LiteBans</b> <br>
 * LiteBans maintains a uuid and name store in litebans_history.
 * Additionally, it puts the victim name and uuid, and the operator name and uuid,
 * alongside each punishment. <br>
 * <br>
 * <b>Self</b> <br>
 * The self-import process is intentionally special-cased. It has no ImportSource implementation
 * but rather uses {@link space.arim.libertybans.core.importing.SelfImportProcess}
 *
 */
package space.arim.libertybans.core.importing;
