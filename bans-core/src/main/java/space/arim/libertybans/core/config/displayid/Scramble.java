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

package space.arim.libertybans.core.config.displayid;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

interface Scramble {

    /**
     * Scrambles the input ID in a random-looking but reversible fashion
     *
     * @param id the regular ID
     * @return the scrambled value
     */
    long scramble(long id);

    /**
     * Descrambles the result of {@link #scramble(long)}. Guaranteed to round-trip for all values
     *
     * @param input the scrambled value
     * @return the descrambled ID
     */
    long descramble(long input);

    class NoOp implements Scramble {

        @Override
        public long scramble(long id) {
            return id;
        }

        @Override
        public long descramble(long input) {
            return input;
        }
    }

    class BitFiddle implements Scramble {

        @Override
        public long scramble(long id) {
            long output = 0L;
            long lastBitOnly = 1L;
            // We want to interlace the bit values: https://stackoverflow.com/a/3190731/
            // bit32 -> bit64, bit64 -> bit63, bit31 -> bit62, bit63 -> bit61, etc.
            for (int idx = 0; idx < 32; idx++) {
                long bringUpBit = (id >>> (31 - idx)) & lastBitOnly; // bit32, bit31, bit30, ...
                long pushDownBit = (id >>> (63 - idx)) & lastBitOnly; // bit64, bit63, bit62, ...
                // Assemble
                // bit32, bit64
                //              bit32, bit63
                //                           bit30, bit62
                output <<= 1;
                output |= bringUpBit;
                output <<= 1;
                output |= pushDownBit;
            }
            return output;
        }

        @Override
        public long descramble(long input) {
            long id = 0L;
            long firstBitOnly = 1L << 63;
            for (int idx = 0; idx < 32; idx++) {
                long leadBit = input & firstBitOnly;
                input <<= 1;
                long secondLeadBit = input & firstBitOnly;
                input <<= 1;
                // Put each bit back in its proper place!
                // leadBit -> bit32, secondLeadBit -> bit64
                // leadBit -> bit31, secondLeadBit -> bit63
                // leadBit -> bit30, secondLeadBit -> bit62
                // etc.
                id |= (leadBit >>> (32 + idx));
                id |= (secondLeadBit >>> idx);
            }
            assert input == 0L : "consumed all bits";
            return id;
        }
    }

    class BlockEncrypt implements Scramble {

        private final Cipher cipher;

        private static final byte[] KEY = new byte[] {
                0x41, 0x22, 0x35, 0x17,
                0x39, 0x10, 0x12, 0x49,
        };

        BlockEncrypt() {
            try {
                cipher = Cipher.getInstance("DES/ECB/NoPadding");
            } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
                throw new IllegalStateException("Broken JVM implementation. No DES encryption available", ex);
            }
        }

        static byte[] longToBytes(long val) {
            byte[] byteArray = new byte[8];
            for (int i = 7; i >= 0; i--) {
                byteArray[i] = (byte) (val & 0xffL);
                val >>= 8;
            }
            return byteArray;
        }

        static long longFromBytes(byte[] data) {
            return (data[0] & 0xffL) << 56 | (data[1] & 0xffL) << 48 | (data[2] & 0xffL) << 40
                    | (data[3] & 0xffL) << 32 | (data[4] & 0xffL) << 24 | (data[5] & 0xffL) << 16
                    | (data[6] & 0xffL) << 8 | (data[7] & 0xffL);
        }

        @Override
        public long scramble(long id) {
            try {
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "DES"));
                byte[] idBytes = longToBytes(id);
                byte[] encrypted = cipher.doFinal(idBytes);
                return longFromBytes(encrypted);
            } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public long descramble(long input) {
            try {
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "DES"));
                byte[] inputBytes = longToBytes(input);
                byte[] decrypted = cipher.doFinal(inputBytes);
                return longFromBytes(decrypted);
            } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
