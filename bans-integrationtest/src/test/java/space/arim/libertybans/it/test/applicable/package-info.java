/**
 * The tests in this package operate on the premise of 3 users.
 * 2 of them have 1 address in common, and 1 address distinct
 * The other user is completely unrelated. <br>
 * <br>
 * Typically, one or both of the first two users are banned, and
 * the enforcement of such is tested according to a certain
 * {@link AddressStrictness}.
 * 
 */
package space.arim.libertybans.it.test.applicable;

import space.arim.libertybans.core.selector.AddressStrictness;