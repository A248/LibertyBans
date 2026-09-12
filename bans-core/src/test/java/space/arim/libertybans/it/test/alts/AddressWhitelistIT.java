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

package space.arim.libertybans.it.test.alts;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.alts.AddressWhitelist;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.pagination.KeysetAnchor;
import space.arim.libertybans.core.service.SettableTime;
import space.arim.libertybans.it.InjectionInvocationContextProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.when;
import static space.arim.libertybans.it.util.RandomUtil.randomAddress;

@ExtendWith(MockitoExtension.class)
@ExtendWith(InjectionInvocationContextProvider.class)
public class AddressWhitelistIT {

    private final Provider<QueryExecutor> queryExecutor;
	private final AddressWhitelist addressWhitelist;
    private final SettableTime time;

    @Inject
    public AddressWhitelistIT(Provider<QueryExecutor> queryExecutor, AddressWhitelist addressWhitelist, SettableTime time) {
        this.queryExecutor = queryExecutor;
        this.addressWhitelist = addressWhitelist;
        this.time = time;
    }

    private boolean queryIsWhitelisted(NetworkAddress address) {
        return queryExecutor.get().query(context -> addressWhitelist.isWhitelisted(context, address)).join();
    }

    @TestTemplate
    public void addToList() {
        assertTrue(addressWhitelist.add(randomAddress(), ConsoleOperator.INSTANCE).join());
    }

    @TestTemplate
    public void isOnList() {
        NetworkAddress address = randomAddress();
        Operator operator = PlayerOperator.of(UUID.randomUUID());
        assumeTrue(addressWhitelist.add(address, operator).join());
        assertTrue(queryIsWhitelisted(address));
    }

    @TestTemplate
    public void alreadyOnList() {
        NetworkAddress address = randomAddress();
        Operator operator = PlayerOperator.of(UUID.randomUUID());
        assumeTrue(addressWhitelist.add(address, operator).join());
        assertFalse(addressWhitelist.add(address, operator).join(), "already on whitelist");
        assertFalse(addressWhitelist.add(address, ConsoleOperator.INSTANCE).join(), "on whitelist by different operator");
        assumeTrue(queryIsWhitelisted(address));
        // repeat assertions (disavow caching interference)
        assertFalse(addressWhitelist.add(address, operator).join());
        assertFalse(addressWhitelist.add(address, ConsoleOperator.INSTANCE).join());
    }

    @TestTemplate
    public void removeFromList() {
        NetworkAddress address = randomAddress();
        Operator operator = PlayerOperator.of(UUID.randomUUID());
        assumeTrue(addressWhitelist.add(address, operator).join());
        assertTrue(addressWhitelist.remove(address).join());
    }

    @TestTemplate
    public void notOnList() {
        NetworkAddress address = randomAddress();
        assertFalse(addressWhitelist.remove(address).join());
        Operator operator = PlayerOperator.of(UUID.randomUUID());
        assumeTrue(addressWhitelist.add(address, operator).join());
        assertTrue(addressWhitelist.remove(address).join());
        assertFalse(addressWhitelist.remove(address).join());
    }

    @TestTemplate
    public void listDisplay(@Mock CommandPackage command) {
        NetworkAddress address1 = randomAddress();
        NetworkAddress address2 = randomAddress();
        NetworkAddress address3 = randomAddress();
        Operator operator1 = PlayerOperator.of(UUID.randomUUID());
        Operator operator2 = ConsoleOperator.INSTANCE;
        Instant date1 = time.currentTimestamp();
        assumeTrue(addressWhitelist.add(address1, operator1).join());
        time.advanceBy(Duration.ofMinutes(1L));
        Instant date2 = time.currentTimestamp();
        assumeTrue(addressWhitelist.add(address2, operator2).join());
        time.advanceBy(Duration.ofDays(1L));
        Instant date3 = time.currentTimestamp();
        assumeTrue(addressWhitelist.add(address3, operator1).join());

        when(command.hasNext()).thenReturn(false);
        var page1 = addressWhitelist.list(new AddressWhitelist.ListRequest(
                2, KeysetAnchor.instantThenAddress(command), 0
        )).join();
        assertEquals(List.of(
                new AddressWhitelist.Entry(
                        address1, operator1, date1, null
                ),
                new AddressWhitelist.Entry(
                        address2, operator2, date2, null
                )),
                page1.data()
        );
        when(command.hasNext()).thenReturn(true);
        when(command.next()).thenReturn(page1.nextPageAnchor().chatCode(page1.borderValueHandle()));
        var page2 = addressWhitelist.list(new AddressWhitelist.ListRequest(
                2, KeysetAnchor.instantThenAddress(command), 0
        )).join();
        assertEquals(List.of(
                        new AddressWhitelist.Entry(
                                address3, operator1, date3, null
                        )),
                page2.data()
        );
    }
}
