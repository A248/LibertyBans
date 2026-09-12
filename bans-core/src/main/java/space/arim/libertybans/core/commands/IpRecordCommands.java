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

package space.arim.libertybans.core.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.alts.AddressManagement;
import space.arim.libertybans.core.alts.AddressWhitelist;
import space.arim.libertybans.core.alts.AddressWhitelistFormatter;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.database.pagination.InstantThenAddress;
import space.arim.libertybans.core.database.pagination.KeysetAnchor;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.Locale;
import java.util.stream.Stream;

@Singleton
public final class IpRecordCommands extends AbstractSubCommandGroup {

    private final AddressManagement addressManagement;
    private final AddressWhitelist addressWhitelist;
    private final AddressWhitelistFormatter addressWhitelistFormatter;

    @Inject
    public IpRecordCommands(Dependencies dependencies, AddressManagement addressManagement, AddressWhitelist addressWhitelist, AddressWhitelistFormatter addressWhitelistFormatter) {
        super(dependencies, "ip-records");
        this.addressManagement = addressManagement;
        this.addressWhitelist = addressWhitelist;
        this.addressWhitelistFormatter = addressWhitelistFormatter;
    }

    @Override
    public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
        return new Execution(sender, command, messages().ipRecords());
    }

    @Override
    public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
        return Stream.empty();
    }

    @Override
    public boolean hasTabCompletePermission(CmdSender sender, String arg) {
        for (SubCmd subCmd : SubCmd.values()) {
            if (sender.hasPermission(subCmd.permission())) {
                return true;
            }
        }
        return false;
    }

    private enum SubCmd {
        PURGE,
        WHITELIST_ADD,
        WHITELIST_REMOVE,
        WHITELIST_LIST;

        String permission() {
            return "libertybans.admin.iprecord." + name().toLowerCase(Locale.ROOT).replace('_', '.');
        }
    }

    private enum WhitelistCmd {
        ADD,
        REMOVE,
        LIST,
        ;

        SubCmd toSubCmd() {
            return switch (this) {
                case ADD -> SubCmd.WHITELIST_ADD;
                case REMOVE -> SubCmd.WHITELIST_REMOVE;
                case LIST -> SubCmd.WHITELIST_LIST;
            };
        }

        static @Nullable WhitelistCmd from(CommandPackage command) {
            String arg = command.hasNext() ? command.next() : "";
            return switch (arg) {
                case "add" -> ADD;
                case "remove" -> REMOVE;
                case "list" -> LIST;
                default -> null;
            };
        }
    }

    private class Execution extends AbstractCommandExecution {

        private final MessagesConfig.IpRecordsSection section;

        protected Execution(CmdSender sender, CommandPackage command, MessagesConfig.IpRecordsSection section) {
            super(sender, command);
            this.section = section;
        }

        private @Nullable NetworkAddress parseAddress() {
            if (!command().hasNext()) {
                return null;
            }
            String targetArg = command().next();
            NetworkAddress parsed = argumentParser().parseAddress(targetArg);
            if (parsed == null) {
                sender().sendMessage(section.notAnAddress().replaceText("%TARGET%", targetArg));
            }
            return parsed;
        }

        @Override
        public @Nullable ReactionStage<Void> execute() {
            String sub1 = command().hasNext() ? command().next() : "";
            return switch (sub1) {
                case "purge" -> {
                    if (!sender().hasPermission(SubCmd.PURGE.permission())) {
                        sender().sendMessage(section.purge().permission());
                        yield null;
                    }
                    NetworkAddress address = parseAddress();
                    if (address == null) {
                        yield null;
                    }
                    yield addressManagement.purgeRecords(address).thenAccept(count -> {
                        sender().sendMessage(section.purge().success()
                                .replaceText("%COUNT%", Integer.toString(count))
                                .replaceText("%ADDRESS%", address.toString()));
                    });
                }
                case "whitelist" -> {
                    WhitelistCmd whitelistCmd = WhitelistCmd.from(command());
                    var whitelistConf = section.whitelist();
                    if (whitelistCmd == null) {
                        sender().sendMessage(whitelistConf.usage());
                        yield null;
                    }
                    if (!sender().hasPermission(whitelistCmd.toSubCmd().permission())) {
                        sender().sendMessage(switch (whitelistCmd) {
                            case ADD -> whitelistConf.add().permission();
                            case REMOVE -> whitelistConf.remove().permission();
                            case LIST -> whitelistConf.list().permission();
                        });
                        yield null;
                    }
                    if (!config().enforcement().ipWhitelist().enable()) {
                        sender().sendMessage(whitelistConf.notEnabled());
                        yield null;
                    }
                    yield switch (whitelistCmd) {
                        case ADD, REMOVE -> {
                            NetworkAddress address = parseAddress();
                            if (address == null) {
                                yield null;
                            }
                            CentralisedFuture<Boolean> operation;
                            MessagesConfig.IpRecordsSection.Whitelist.AddOrRemove addRemoveConf;
                            if (whitelistCmd == WhitelistCmd.ADD) {
                                operation = addressWhitelist.add(address, sender().getOperator());
                                addRemoveConf = whitelistConf.add();
                            } else {
                                operation = addressWhitelist.remove(address);
                                addRemoveConf = whitelistConf.remove();
                            }
                            yield operation.thenAccept(success -> {
                                ComponentText message = success ? addRemoveConf.success() : addRemoveConf.failed();
                                sender().sendMessage(message.replaceText("%ADDRESS%", address.toString()));
                            });
                        }
                        case LIST -> {
                            var listConf = whitelistConf.list();
                            int page;
                            AddressWhitelist.ListRequest request;
                            {
                                KeysetAnchor<InstantThenAddress> anchor = KeysetAnchor.instantThenAddress(command());
                                if (anchor == null) {
                                    sender().sendMessage(listConf.usage());
                                    yield completedFuture(null);
                                }
                                page = anchor.page();
                                int pageSize = listConf.perPage();
                                int skipCount = 0;
                                if (anchor.borderValue() == null) {
                                    // Traditional pagination
                                    skipCount = (page - 1) * pageSize;
                                }
                                request = new AddressWhitelist.ListRequest(pageSize, anchor, skipCount);
                            }
                            yield addressWhitelist.list(request).thenAccept(response -> {
                                if (response.data().isEmpty()) {
                                    if (page == 1) {
                                        sender().sendMessage(listConf.noPages());
                                    } else {
                                        sender().sendMessage(listConf.maxPages().replaceText("%PAGE%", Integer.toString(page)));
                                    }
                                    return;
                                }
                                sender().sendMessage(addressWhitelistFormatter.formatMessage(response, page));
                            });
                        }
                    };
                }
                default -> {
                    sender().sendMessage(section.usage());
                    yield null;
                }
            };
        }
    }
}
