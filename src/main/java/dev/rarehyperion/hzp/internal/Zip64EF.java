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

/**
 * Parser, and holder for the ZIP64 Extended Information Extra Field.
 *
 * <p>When a central-directory header stores sentinel value {@code 0xFFFFFFFF} in its compressed size,
 * uncompressed size, or local-header offsets, the real 64-bit values are read from this extra-field block instead.
 * Unpopulated fields are left as {@code -1}.</p>
 */
public class Zip64EF {

    private static final int HEADER_ID = 0x0001;

    public long uncompressedSize  = -1;
    public long compressedSize    = -1;
    public long localHeaderOffset = -1;

    static Zip64EF parse(final byte[] extra, final long cdCompressed, final long cdUncompressed, final long cdOffset) {
        if(extra == null || extra.length < 4) return null;

        int i = 0;

        while(i + 4 <= extra.length) {
            int headerId = LittleEndian.uint16(extra, i);
            int dataSize = LittleEndian.uint16(extra, i + 2);

            i += 4;

            if(headerId == HEADER_ID && dataSize >= 4) {
                final Zip64EF zip64ef = new Zip64EF();

                int j = i;

                if (cdUncompressed == 0xFFFFFFFFL && j + 8 <= i + dataSize)
                    zip64ef.uncompressedSize = LittleEndian.int64(extra, j); j += 8;

                if (cdCompressed   == 0xFFFFFFFFL && j + 8 <= i + dataSize)
                    zip64ef.compressedSize = LittleEndian.int64(extra, j); j += 8;

                if (cdOffset == 0xFFFFFFFFL && j + 8 <= i + dataSize)
                    zip64ef.localHeaderOffset = LittleEndian.int64(extra, j);

                return zip64ef;
            }

            i += dataSize;
        }

        return null;
    }

}
