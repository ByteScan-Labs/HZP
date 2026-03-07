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

import dev.rarehyperion.hzp.Flag;
import dev.rarehyperion.hzp.model.CentralDirectoryFileHeader;
import dev.rarehyperion.hzp.model.LocalFileHeader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CentralDirectoryParser {

    private static final int FLAG_ENCRYPTED = 0x0001;
    private static final int FLAG_DATA_DESC = 0x0008;
    private static final int FLAG_EFS       = 0x0800;

    public static List<CentralDirectoryFileHeader> parse(final RandomAccessFile raf, final long cdStartAbs, final long cdSize, final long archiveStart, final EnumSet<Flag> flags) throws IOException {
        final long fileLen = raf.length();
        final List<CentralDirectoryFileHeader> entries = new ArrayList<>();
        final Set<String> seenNames = new HashSet<>();

        long readBytes = 0;
        long prevLfhOffset = -1L;

        while(readBytes < cdSize) {
            final long recordStart = cdStartAbs + readBytes;
            if(recordStart + 46 > fileLen) break;

            raf.seek(recordStart);

            if(LittleEndian.readInt32LE(raf) != EocdParser.SIG_CENTRAL_DIR)
                break;

            final int versionMadeBy = LittleEndian.readUInt16LE(raf);
            final int versionNeeded = LittleEndian.readUInt16LE(raf);
            final int gpFlag = LittleEndian.readUInt16LE(raf);
            final int compMethod =LittleEndian. readUInt16LE(raf);
            final int modTime = LittleEndian.readUInt16LE(raf);
            final int modDate = LittleEndian.readUInt16LE(raf);
            final long crc32 = LittleEndian.readUInt32LE(raf);
            long compSize = LittleEndian.readUInt32LE(raf);
            long uncompSize = LittleEndian.readUInt32LE(raf);
            final int nameLen = LittleEndian.readUInt16LE(raf);
            final int extraLen = LittleEndian.readUInt16LE(raf);
            final int commentLen = LittleEndian.readUInt16LE(raf);
            final int diskStart = LittleEndian.readUInt16LE(raf);
            final int intAttr = LittleEndian.readUInt16LE(raf);
            final long extAttr = LittleEndian.readUInt32LE(raf);
            long localRelOff = LittleEndian.readUInt32LE(raf);

            final byte[] nameBytes = new byte[nameLen];
            raf.readFully(nameBytes);

            final byte[] extra = new byte[extraLen];
            raf.readFully(extra);

            final byte[] comment = new byte[commentLen];
            raf.readFully(comment);

            // ZIP64 extra field resolution, if it wasn't obvious.
            if (compSize == 0xFFFFFFFFL || uncompSize == 0xFFFFFFFFL || localRelOff == 0xFFFFFFFFL) {
                final Zip64EF z64 = Zip64EF.parse(extra, compSize, uncompSize, localRelOff);

                if (z64 != null) {
                    if (compSize == 0xFFFFFFFFL && z64.compressedSize >= 0) {
                        compSize = z64.compressedSize;
                    }

                    if (uncompSize == 0xFFFFFFFFL && z64.uncompressedSize  >= 0) {
                        uncompSize = z64.uncompressedSize;
                    }

                    if (localRelOff == 0xFFFFFFFFL && z64.localHeaderOffset >= 0) {
                        localRelOff = z64.localHeaderOffset;
                    }

                    flags.add(Flag.ZIP64);
                }
            }

            final String name = ((gpFlag & FLAG_EFS) != 0)
                    ? new String(nameBytes, StandardCharsets.UTF_8)
                    : LittleEndian.decodeFilename(nameBytes);

            readBytes += 46L + nameLen + extraLen + commentLen;

            if ((gpFlag & FLAG_DATA_DESC) != 0) flags.add(Flag.DATA_DESCRIPTOR);
            if ((gpFlag & FLAG_ENCRYPTED) != 0) flags.add(Flag.ENCRYPTED_ENTRY);
            if (!seenNames.add(name)) flags.add(Flag.DUPLICATE_ENTRY_NAME);

            if (prevLfhOffset >= 0 && localRelOff < prevLfhOffset) {
                flags.add(Flag.SHUFFLED_CENTRAL_DIRECTORY);
            }

            prevLfhOffset = localRelOff;

            // Block of cheese or something:
            final byte[] compressedData = ZipCompressions.readCompressedBytes(raf, name, compSize, localRelOff, archiveStart);
            final LocalFileHeader lfh = new LocalFileHeader(name, compSize, uncompSize, crc32, compMethod, gpFlag, localRelOff, compressedData, archiveStart);
            final CentralDirectoryFileHeader cd = new CentralDirectoryFileHeader(versionMadeBy, versionNeeded, gpFlag, compMethod,
                    modTime, modDate, crc32, compSize, uncompSize, diskStart, intAttr, extAttr, localRelOff, name, extra, comment, lfh);

            lfh.linkCentralDirectory(cd);
            entries.add(cd);
        }

        return entries;
    }

}
