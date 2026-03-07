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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.EnumSet;
import java.util.zip.ZipException;

/**
 * Locates and parses the End of Central Directory (EOCD) record from a ZIP archive, including detection of structural anomalies such as fake EOCDS and prepended data.
 */
public class EocdParser {

    /** {@code PK\x03\x04} */
    public static final int SIG_LOCAL_FILE     = 0x04034b50;

    /** {@code PK\x01\x02} */
    public static final int SIG_CENTRAL_DIR    = 0x02014b50;

    /**  {@code PK\x06\x07} */
    public static final int SIG_EOCD64_LOCATOR = 0x07064b50;

    /** {@code PK\x06\x06} */
    public static final int SIG_EOCD64         = 0x06064b50;

    public static final int EOCD_FIXED_SIZE    = 22;
    public static final int EOCD64_FIXED_SIZE  = 56;
    public static final int EOCD64_LOC_SIZE    = 20;
    private static final int MAX_EOCD_SEARCH    = 65557;

    /**
     * Scans the tail of the file for a valid EOCD or EOCD64 record.
     * Sets relevant {@link Flag} values for anomalies encountered during the search.
     *
     * @return A populated {@link EocdInfo}, or {@code null} if no valid EOCD was found.
     */
    public static EocdInfo findEocd(final RandomAccessFile raf, final EnumSet<Flag> flags) throws IOException {
        final long fileLen = raf.length();
        final long maxSearch = Math.min(fileLen, MAX_EOCD_SEARCH);
        final long scanStart = fileLen - maxSearch;

        final byte[] buffer = new byte[(int) maxSearch];
        raf.seek(scanStart);
        raf.readFully(buffer);

        int fakeEocdCount = 0;

        for(int i = buffer.length - EOCD_FIXED_SIZE; i >= 0; i--) {
            // Matching PK\x05\x06
            if ((buffer[i] & 0xFF) != 0x50 || (buffer[i+1] & 0xFF) != 0x4b ||
                    (buffer[i+2] & 0xFF) != 0x05 || (buffer[i+3] & 0xFF) != 0x06)
                continue;

            final int commentLen = LittleEndian.uint16(buffer, i + 20);

            // The comment must fill exactly to the end of the file.
            if(scanStart + i + EOCD_FIXED_SIZE + commentLen != fileLen)
                continue;

            final long eocdPos = scanStart + i;
            final EocdInfo candidate = EocdParser.readFromBuffer(buffer, i, eocdPos);

            if(EocdParser.isValidCd(raf, candidate, fileLen, flags)) {
                if(fakeEocdCount > 0) flags.add(Flag.FAKE_EOCD);
                EocdParser.checkZip64Locator(raf, eocdPos, fileLen, flags);
                return candidate;
            }

            if(eocdPos >= EOCD64_LOC_SIZE) {
                raf.seek(eocdPos - EOCD64_LOC_SIZE);

                if(LittleEndian.readInt32LE(raf) == SIG_EOCD64_LOCATOR) {
                    /* disk */ LittleEndian.readUInt32LE(raf);
                    final long eocd64Pos = LittleEndian.readUInt64LE(raf);

                    if(eocd64Pos + EOCD64_FIXED_SIZE <= fileLen) {
                        raf.seek(eocd64Pos);

                        if(LittleEndian.readInt32LE(raf) == SIG_EOCD64) {
                            flags.add(Flag.ZIP64);
                            if(fakeEocdCount > 0) flags.add(Flag.FAKE_EOCD64);
                            return EocdParser.readZip64(raf, eocdPos);
                        }
                    }

                    flags.add(Flag.MISLEADING_EOCD64_LOCATOR);
                }
            }

            fakeEocdCount++;
        }

        if(fakeEocdCount > 0) {
            // The cake was a lie... I should've known.
            // Please report the thrown error caused by this if you ever get it.
            flags.add(Flag.FAKE_EOCD64);
        }

        return null;
    }

    private static boolean isValidCd(final RandomAccessFile raf, final EocdInfo candidate, final long fileLen, final EnumSet<Flag> flags) throws IOException {
        final long cdA = candidate.eocdPos - candidate.centralDirSize;

        if(LittleEndian.inBounds(cdA, candidate.centralDirSize, fileLen)) {
            raf.seek(cdA);

            if(LittleEndian.peekInt32(raf) == SIG_CENTRAL_DIR) {
                return true;
            }
        }

        final long cdB = candidate.centralDirOffset;

        if(LittleEndian.inBounds(cdB, candidate.centralDirSize, fileLen)) {
            raf.seek(cdB);

            if(LittleEndian.peekInt32(raf) == SIG_CENTRAL_DIR) {
                flags.add(Flag.CD_OFFSET_ANOMALY);
                return true;
            }
        }

        return false;
    }

    private static void checkZip64Locator(final RandomAccessFile raf, final long eocdPos, final long fileLen, final EnumSet<Flag> flags) throws IOException {
        if(eocdPos < EOCD64_LOC_SIZE) return;
        raf.seek(eocdPos - EOCD64_LOC_SIZE);

        if(LittleEndian.readInt32LE(raf) != SIG_EOCD64_LOCATOR) return;
        /* disk */ LittleEndian.readUInt32LE(raf);
        final long eocd64Pos = LittleEndian.readUInt64LE(raf);

        if(eocd64Pos + EOCD64_FIXED_SIZE <= fileLen) {
            raf.seek(eocd64Pos);

            if(LittleEndian.readInt32LE(raf) == SIG_EOCD64) {
                flags.add(Flag.ZIP64);
            }
        }

        flags.add(Flag.MISLEADING_EOCD64_LOCATOR);
    }

    private static EocdInfo readFromBuffer(final byte[] buffer, final int i, final long eocdPos) {
        final EocdInfo info = new EocdInfo();

        info.eocdPos          = eocdPos;
        info.diskNumber       = LittleEndian.uint16(buffer, i + 4);
        info.cdStartDisk      = LittleEndian.uint16(buffer, i + 6);
        info.totalEntries     = LittleEndian.uint16(buffer, i + 10);
        info.centralDirSize   = LittleEndian.uint32(buffer, i + 12);
        info.centralDirOffset = LittleEndian.uint32(buffer, i + 16);
        info.commentLength    = LittleEndian.uint16(buffer, i + 20);

        if(info.commentLength > 0) {
            info.comment = new byte[info.commentLength];
            System.arraycopy(buffer, i + EOCD_FIXED_SIZE, info.comment, 0, info.commentLength);
        }

        return info;
    }

    private static EocdInfo readZip64(final RandomAccessFile raf, final long eocdPos) throws IOException {
        /* record size   */ LittleEndian.readUInt64LE(raf);
        /* versionMadeBy */ LittleEndian.readUInt16LE(raf);
        /* versionNeeded */ LittleEndian.readUInt16LE(raf);

        final EocdInfo info = new EocdInfo();
        info.eocdPos          = eocdPos;
        info.diskNumber       = (int) LittleEndian.readUInt32LE(raf);
        info.cdStartDisk      = (int) LittleEndian.readUInt32LE(raf);
        /* entries disk  */     LittleEndian.readUInt64LE(raf);
        info.totalEntries     = LittleEndian.readUInt64LE(raf);
        info.centralDirSize   = LittleEndian.readUInt64LE(raf);
        info.centralDirOffset = LittleEndian.readUInt64LE(raf);
        info.commentLength    = 0;
        info.isZip64          = true;

        return info;
    }

    public static long[] resolveCdPosition(final RandomAccessFile raf, final EocdInfo eocd, final EnumSet<Flag> flags) throws IOException {
        final long fileLen = raf.length();

        long cdStartAbs = -1L;

        final long candidateA = eocd.eocdPos - eocd.centralDirSize;
        final long candidateB = eocd.centralDirOffset;

        if(LittleEndian.inBounds(candidateA, eocd.centralDirSize, fileLen)) {
            raf.seek(candidateA);

            if(LittleEndian.peekInt32(raf) == SIG_CENTRAL_DIR) {
                cdStartAbs = candidateB;
                flags.add(Flag.CD_OFFSET_ANOMALY);
            }
        }

        if(cdStartAbs < 0 && LittleEndian.inBounds(candidateB, eocd.centralDirSize, fileLen)) {
            raf.seek(candidateB);

            if(LittleEndian.peekInt32(raf) == SIG_CENTRAL_DIR) {
                cdStartAbs = candidateB;
                flags.add(Flag.CD_OFFSET_ANOMALY);
            }
        }

        if(cdStartAbs < 0) {
            if(LittleEndian.inBounds(candidateA, eocd.centralDirSize, fileLen)) cdStartAbs = candidateA;
            else if(LittleEndian.inBounds(candidateB, eocd.centralDirSize, fileLen)) {
                cdStartAbs = candidateB;
                flags.add(Flag.CD_OFFSET_ANOMALY);
            } else {
                throw new ZipException("Central directory out of bounds or not found");
            }
        }

        long archiveStart = cdStartAbs - eocd.centralDirOffset;

        if(archiveStart < 0) {
            archiveStart = 0;
            cdStartAbs = eocd.centralDirOffset;
            flags.add(Flag.CD_OFFSET_ANOMALY);
        } else if(archiveStart > 0) {
            flags.add(Flag.PREPENDED_DATA);
        }

        return new long[] { cdStartAbs, archiveStart };
    }

    private EocdParser() { /* no-op */ }

}
