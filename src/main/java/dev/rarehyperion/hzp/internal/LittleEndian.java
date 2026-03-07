/*
 * Copyright 2026 RareHyperIon
 *
 * Licensed under the Apache License, Version 2.0 with Commons Clause (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://raw.githubusercontent.com/ByteScan-Labs/HZP/refs/heads/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.rarehyperion.hzp.internal;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class LittleEndian {

    private static final Charset CP437 = Charset.forName("IBM437");

    /**
     * Read an unsigned 16-bit integer in little-endian byte order.
     */
    static int uint16(final byte[] b, final int o) {
        return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8);
    }

    /**
     * Read an unsigned 32-bit integer in little-endian byte order.
     */
    static long uint32(final byte[] b, final int o) {
        return (b[o] & 0xFFL)
                | ((b[o + 1] & 0xFFL) << 8)
                | ((b[o + 2] & 0xFFL) << 16)
                | ((b[o + 3] & 0xFFL) << 24);
    }

    /**
     * Read a signed 64-bit integer in little-endian byte order.
     */
    static long int64(final byte[] b, final int o) {
        return uint32(b, o) | (uint32(b, o + 4) << 32);
    }

    /**
     * Peeks the next 4 bytes interpreted as little-endian signed int without moving the file pointer permanently.
     */
    static int peekInt32(final RandomAccessFile raf) throws IOException {
        try {
            long cur = raf.getFilePointer();
            int v = LittleEndian.readInt32LE(raf);
            raf.seek(cur);
            return v;
        } catch (final EOFException exception) {
            return 0;
        }
    }

    /**
     * Read a signed 32-bit integer in little-endian byte order.
     */
    static int readInt32LE(final RandomAccessFile raf) throws IOException {
        final byte[] buffer = new byte[4];
        raf.readFully(buffer);
        return (buffer[0] & 0xFF) | ((buffer[1] & 0xFF) << 8) | ((buffer[2] & 0xFF) << 16) | ((buffer[3] & 0xFF) << 24);
    }

    /**
     * Read an unsigned 16-bit integer in little-endian byte order.
     */
    static int readUInt16LE(final RandomAccessFile raf) throws IOException {
        final byte[] buffer = new byte[2];
        raf.readFully(buffer);
        return (buffer[0] & 0xFF) | ((buffer[1] & 0xFF) << 8);
    }

    /**
     * Read am unsigned 32-bit integer in little-endian byte order.
     */
    static long readUInt32LE(final RandomAccessFile raf) throws IOException {
        final int v = LittleEndian.readInt32LE(raf);
        return Integer.toUnsignedLong(v);
    }

    /**
     * Read an unsigned 64-bit value in little-endian byte order.
     */
    static long readUInt64LE(final RandomAccessFile raf) throws IOException {
        final byte[] buffer = new byte[8];
        raf.readFully(buffer);
        return (buffer[0] & 0xFFL) | ((buffer[1] & 0xFFL) << 8) | ((buffer[2] & 0xFFL) << 16) | ((buffer[3] & 0xFFL) << 24)
                | ((buffer[4] & 0xFFL) << 32) | ((buffer[5] & 0xFFL) << 40) | ((buffer[6] & 0xFFL) << 48) | ((buffer[7] & 0xFFL) << 56);
    }

    /**
     * Decode a filename byte-array.
     */
    static String decodeFilename(final byte[] b) {
        String s = new String(b, StandardCharsets.UTF_8);
        if (s.getBytes(StandardCharsets.UTF_8).length == b.length) return s;
        return new String(b, CP437);
    }

    static boolean inBounds(final long start, final long size, final long fileLen) {
        return start >= 0 && size >= 0 && start + size <= fileLen;
    }

    private LittleEndian() { /* no-op */ }

}
