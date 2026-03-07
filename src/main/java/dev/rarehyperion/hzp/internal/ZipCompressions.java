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

import dev.rarehyperion.hzp.model.CentralDirectoryFileHeader;
import dev.rarehyperion.hzp.model.LocalFileHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

public final class ZipCompressions {

    public static final int METHOD_STORED  = 0;
    public static final int METHOD_DEFLATE = 8;

    // LFH fixed-header size (without name/extra).
    private static final int LFH_FIXED_SIZE = 30;

    private static final long DATA_DESC_SCAN_LIMIT = 64L * 1024 * 1024;

    public static byte[] decompress(final LocalFileHeader lfh) throws IOException, DataFormatException {
        return ZipCompressions.inflate(lfh.getCompressedData(), lfh);
    }

    public static byte[] decompress(final CentralDirectoryFileHeader cd) throws IOException, DataFormatException {
        return ZipCompressions.decompress(cd.getLinkedLFH());
    }

    static byte[] readCompressedBytes(final RandomAccessFile raf, final String name, final long cdCompSize, final long localRelOff, final long archiveStart) throws IOException {
        final long lfhAbsOffset = archiveStart + localRelOff;

        raf.seek(lfhAbsOffset);

        final int sig = LittleEndian.readInt32LE(raf);

        if(sig != EocdParser.SIG_LOCAL_FILE) {
            throw new ZipException(String.format(
                    "Local file header signature missing at 0x%X (got 0x%08X); archiveStart=0x%X relOffset=0x%X name='%s'",
                    lfhAbsOffset, sig, archiveStart, localRelOff, name
            ));
        }

        /* versionNeeded        */ LittleEndian.readUInt16LE(raf);
        final int localGpFlag    = LittleEndian.readUInt16LE(raf);
        /* method               */ LittleEndian.readUInt16LE(raf);
        /* modTime              */ LittleEndian.readUInt16LE(raf);
        /* modDate              */ LittleEndian.readUInt16LE(raf);
        /* crc32                */ LittleEndian.readUInt32LE(raf);
        final long localCompSize = LittleEndian.readUInt32LE(raf);
        final long localUncomp   = LittleEndian.readUInt32LE(raf);
        final int nameLen        = LittleEndian.readUInt16LE(raf);
        final int extraLen       = LittleEndian.readUInt16LE(raf);

        raf.skipBytes(nameLen);

        final byte[] localExtra = new byte[extraLen];
        raf.readFully(localExtra);

        final long dataStart = lfhAbsOffset + LFH_FIXED_SIZE + nameLen + extraLen;

        long compSize = cdCompSize; // CD is authoritative.

        // ZIP64 fallback: if CD has no size info (e.g. data descriptor path), try local extra.
        if(compSize == 0xFFFFFFFFL) {
            final Zip64EF z64 = Zip64EF.parse(localExtra, compSize, localUncomp, 0);
            if(z64 != null && z64.compressedSize > 0) compSize = z64.compressedSize;
        }

        // Data descriptor fallback: scan forward for 'PK\x07\x08'.
        if(compSize == 0 && (localGpFlag & 0x0008) != 0) {
            compSize = ZipCompressions.findDataDescriptorSize(raf, dataStart, name);
        } else if(compSize == 0 & localCompSize != 0) {
            compSize = localCompSize;
        }

        if(compSize < 0 || compSize > Integer.MAX_VALUE) {
            throw new ZipException("Compressed size out of range: " + compSize + " for entry: " + name);
        }

        raf.seek(dataStart);

        final byte[] compressed = new byte[(int) compSize];
        raf.readFully(compressed);

        return compressed;
    }

    private static byte[] inflate(final byte[] compressed, final LocalFileHeader lfh) throws IOException, DataFormatException {
        switch (lfh.getCompressionMethod()) {
            case METHOD_STORED:
                return /*un*/compressed;

            case METHOD_DEFLATE: {
                // I'd prefer if this were a fully custom implementation, but I genuinely don't have time for that right now.
                try (final Inflater inflater = new Inflater(true)) {
                    inflater.setInput(compressed);

                    final int initCap = (lfh.getUncompressedSize() > 0 && lfh.getUncompressedSize() <= 64 << 20)
                            ? (int) lfh.getUncompressedSize()
                            : Math.max(compressed.length * 3, 8192);

                    final ByteArrayOutputStream bos = new ByteArrayOutputStream(initCap);
                    final byte[] buffer = new byte[65536];

                    while (true) {
                        final int n = inflater.inflate(buffer);

                        if (n > 0) {
                            bos.write(buffer, 0, n);
                        } else if (inflater.finished()) {
                            break;
                        } else if (inflater.needsInput()) {
                            break;
                        } else if (inflater.needsDictionary()) {
                            throw new ZipException("Inflater requires external dictionary for: " + lfh.getName());
                        }
                    }

                    return bos.toByteArray();
                }
            }

            default:
                throw new ZipException("Unsupported compression method '" + lfh.getCompressionMethod() + "' for entry: " + lfh.getName());
        }
    }

    private static long findDataDescriptorSize(final RandomAccessFile raf, final long dataStart, final String entryName) throws IOException {
        final long fileLen = raf.length();
        final long scanEnd = Math.min(dataStart + DATA_DESC_SCAN_LIMIT, fileLen);
        final byte[] window = new byte[65536];

        long pos = dataStart;

        while(pos < scanEnd) {
            final int toRead = (int) Math.min(window.length, scanEnd - pos);
            raf.seek/*Help*/(pos);
            raf.readFully(window, 0, toRead);

            for(int i = 0; i < toRead - 3; i++) {
                if ((window[i] & 0xFF) == 0x50 && (window[i+1] & 0xFF) == 0x4b &&
                        (window[i+2] & 0xFF) == 0x07 && (window[i+3] & 0xFF) == 0x08) {
                    return (pos + i) - dataStart;
                }
            }

            pos += toRead - 3;
        }

        throw new ZipException("DATA_DESCRIPTOR bit set but PK\\x07\\x08 not found for: " + entryName);
    }

    /** If you see this, you are using this wrong. You are meant to access the methods statically. */
    private ZipCompressions() { /* no-op */ }

}
