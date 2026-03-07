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
import dev.rarehyperion.hzp.internal.randomaccess.RandomAccessInput;
import dev.rarehyperion.hzp.model.CentralDirectoryFileHeader;
import dev.rarehyperion.hzp.model.LocalFileHeader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipException;

public final class CentralDirectoryParser {

    private static final int FLAG_ENCRYPTED = 0x0001;
    private static final int FLAG_DATA_DESC = 0x0008;
    private static final int FLAG_EFS       = 0x0800;

    public static List<CentralDirectoryFileHeader> parse(final RandomAccessInput raf, final long cdStartAbs, final long cdSize, final long archiveStart, final EnumSet<Flag> flags) throws IOException {
        final long fileLen = raf.length();
        final long available = fileLen - cdStartAbs;

        if(available <= 0) {
            throw new ZipException("Central directory start is beyond end of file.");
        }

        // If an attacker inflates cdSize, we will simply stop reading at the file boundary.
        final int safeCdSize = (int) Math.min(cdSize, Math.min(available, Integer.MAX_VALUE));

        final byte[] cdBuffer = new byte[safeCdSize];
        raf.seek(cdStartAbs);
        raf.readFully(cdBuffer);

        // Roughly estimating capacity.
        final int estimatedCount = Math.max(4, safeCdSize / 80);
        final List<CdFields> rawFields = new ArrayList<>(estimatedCount);
        final Set<String> seenNames = new HashSet<>(estimatedCount * 2);

        int pos = 0;
        long prevLfhOffset = -1L;

        while(pos + 46 <= safeCdSize) {
            if((int) LittleEndian.uint32(cdBuffer, pos) != EocdParser.SIG_CENTRAL_DIR) break;

            final int versionMadeBy = LittleEndian.uint16(cdBuffer, pos + 4);
            final int versionNeeded = LittleEndian.uint16(cdBuffer, pos + 6);
            final int gpFlag        = LittleEndian.uint16(cdBuffer, pos + 8);
            final int compMethod    = LittleEndian.uint16(cdBuffer, pos + 10);
            final int modTime       = LittleEndian.uint16(cdBuffer, pos + 12);
            final int modDate       = LittleEndian.uint16(cdBuffer, pos + 14);
            final long crc32        = LittleEndian.uint32(cdBuffer, pos + 16);
            long compSize           = LittleEndian.uint32(cdBuffer, pos + 20);
            long uncompSize         = LittleEndian.uint32(cdBuffer, pos + 24);
            final int nameLen       = LittleEndian.uint16(cdBuffer, pos + 28);
            final int extraLen      = LittleEndian.uint16(cdBuffer, pos + 30);
            final int commentLen    = LittleEndian.uint16(cdBuffer, pos + 32);
            final int diskStart     = LittleEndian.uint16(cdBuffer, pos + 34);
            final int intAttr       = LittleEndian.uint16(cdBuffer, pos + 36);
            final long extAttr      = LittleEndian.uint32(cdBuffer, pos + 38);
            long localRelOff        = LittleEndian.uint32(cdBuffer, pos + 42);

            final int recordSize = 46 + nameLen + extraLen + commentLen;

            if(pos + recordSize > safeCdSize) break;

            final int extraStart = pos + 46 + nameLen;
            final int commentStart = extraStart + extraLen;
            final byte[] extra = Arrays.copyOfRange(cdBuffer, extraStart, commentStart);
            final byte[] comment = Arrays.copyOfRange(cdBuffer, commentStart, pos + recordSize);

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
                    ? new String(cdBuffer, pos + 46, nameLen, StandardCharsets.UTF_8)
                    : LittleEndian.decodeFilename(Arrays.copyOfRange(cdBuffer, pos + 46, pos + 46 + nameLen));

            if ((gpFlag & FLAG_DATA_DESC) != 0) flags.add(Flag.DATA_DESCRIPTOR);
            if ((gpFlag & FLAG_ENCRYPTED) != 0) flags.add(Flag.ENCRYPTED_ENTRY);
            if (!seenNames.add(name)) flags.add(Flag.DUPLICATE_ENTRY_NAME);

            if (prevLfhOffset >= 0 && localRelOff < prevLfhOffset)
                flags.add(Flag.SHUFFLED_CENTRAL_DIRECTORY);

            prevLfhOffset = localRelOff;

            rawFields.add(
                    new CdFields(
                        versionMadeBy, versionNeeded, gpFlag, compMethod,
                        modTime, modDate, crc32, compSize, uncompSize,
                        diskStart, intAttr, extAttr, localRelOff,
                        name, extra, comment
                    )
            );

            pos += recordSize;
        }

        final int count = rawFields.size();

        final Integer[] sortOrder = new Integer[count];
        for (int i = 0; i < count; i++) sortOrder[i] = i;
        Arrays.sort(sortOrder, Comparator.comparingLong(a -> rawFields.get(a).localRelOff));

        final LocalFileHeader[] lfhs = new LocalFileHeader[count];
        final List<CentralDirectoryFileHeader> entries = new ArrayList<>(count);

        for (final int idx : sortOrder) {
            final CdFields cd = rawFields.get(idx);

            // If the LFH offset doesn't point to a real LFH, then it must be a shadow entry with empty data.
            final long lfhAbsOffset = archiveStart + cd.localRelOff;
            boolean isShadow = false;

            if(lfhAbsOffset >= 0 && lfhAbsOffset + 4 <= raf.length()) {
                final int signature = LittleEndian.peekInt32(raf, lfhAbsOffset);

                if(signature != EocdParser.SIG_LOCAL_FILE) {
                    isShadow = true;
                    flags.add(Flag.SHADOW_LOCAL_HEADERS);
                }
            } else {
                isShadow = true;
                flags.add(Flag.GHOST_LOCAL_HEADERS);
            }

            final byte[] compressedData = isShadow ? new byte[0] : ZipCompressions.readCompressedBytes(raf, cd.name, cd.compSize, cd.localRelOff, archiveStart);
            lfhs[idx] = new LocalFileHeader(cd.name, cd.compSize, cd.uncompSize, cd.crc32, cd.compMethod, cd.gpFlag, cd.localRelOff, compressedData, archiveStart);
        }

        for (int i = 0; i < count; i++) {
            final CdFields cd  = rawFields.get(i);
            final LocalFileHeader lfHeader = lfhs[i];
            final CentralDirectoryFileHeader cdHeader = new CentralDirectoryFileHeader(
                    cd.versionMadeBy, cd.versionNeeded, cd.gpFlag, cd.compMethod, cd.modTime, cd.modDate, cd.crc32, cd.compSize,
                    cd.uncompSize, cd.diskStart, cd.intAttr, cd.extAttr, cd.localRelOff, cd.name, cd.extra, cd.comment, lfHeader
            );

            lfHeader.linkCentralDirectory(cdHeader);
            entries.add(cdHeader);
        }

        return entries;
    }

    private static final class CdFields {

        final int versionMadeBy, versionNeeded, gpFlag, compMethod, modTime, modDate;
        final long crc32, compSize, uncompSize, extAttr, localRelOff;
        final int  diskStart, intAttr;
        final String name;
        final byte[] extra, comment;

        CdFields(final int versionMadeBy, final int  versionNeeded, final int gpFlag, final int compMethod, final int modTime, final int modDate,
                 final long crc32, final long compSize, final long uncompSize, final int diskStart, final int intAttr, final long extAttr,
                 final long localRelOff, final String name, final byte[] extra, final byte[] comment) {
            this.versionMadeBy = versionMadeBy;
            this.versionNeeded = versionNeeded;
            this.gpFlag        = gpFlag;
            this.compMethod    = compMethod;
            this.modTime       = modTime;
            this.modDate       = modDate;
            this.crc32         = crc32;
            this.compSize      = compSize;
            this.uncompSize    = uncompSize;
            this.diskStart     = diskStart;
            this.intAttr       = intAttr;
            this.extAttr       = extAttr;
            this.localRelOff   = localRelOff;
            this.name          = name;
            this.extra         = extra;
            this.comment       = comment;
        }

    }

}
