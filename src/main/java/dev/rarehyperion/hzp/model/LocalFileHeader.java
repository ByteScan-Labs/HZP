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

package dev.rarehyperion.hzp.model;

import dev.rarehyperion.hzp.internal.ZipCompressions;

import java.io.IOException;
import java.util.zip.DataFormatException;

/**
 * Immutable public view of the Local File Header record.
 *
 * <p>Always linked to exactly one {@link CentralDirectoryFileHeader}, which is the authoritative source for sizes and offsets.</p>
 */
public final class LocalFileHeader {

    // GP flag bits.
    private static final int FLAG_ENCRYPTED = 0x0001;
    private static final int FLAG_DATA_DESC = 0x0008;

    private final String name;
    private final long compressedSize;
    private final long uncompressedSize;
    private final long crc32;
    private final int compressionMethod;
    private final int generalPurposeFlag;
    private final long localHeaderRelOffset;

    private final byte[] compressedData;
    private final long archiveStart;

    private CentralDirectoryFileHeader linkedCd;

    public LocalFileHeader(final String name, final long compressedSize, final long uncompressedSize, final long crc32, final int compressionMethod, final int generalPurposeFlag, final long localHeaderRelOffset, final byte[] compressedData, final long archiveStart) {
        this.name = name;
        this.compressedSize = compressedSize;
        this.uncompressedSize = uncompressedSize;
        this.crc32 = crc32;
        this.compressionMethod = compressionMethod;
        this.generalPurposeFlag = generalPurposeFlag;
        this.localHeaderRelOffset = localHeaderRelOffset;

        this.compressedData = compressedData;
        this.archiveStart = archiveStart;
    }

    public String getName() {
        return this.name;
    }

    public long getCompressedSize() {
        return this.compressedSize;
    }

    public long getUncompressedSize() {
        return this.uncompressedSize;
    }

    public long getCrc32() {
        return this.crc32;
    }

    public int getCompressionMethod() {
        return this.compressionMethod;
    }

    public int getGeneralPurposeFlag() {
        return this.generalPurposeFlag;
    }

    public long getLocalHeaderRelOffset() {
        return this.localHeaderRelOffset;
    }

    public boolean hasDataDescriptor() {
        return (this.generalPurposeFlag & FLAG_DATA_DESC) != 0;
    }

    public boolean isEncrypted() {
        return (this.generalPurposeFlag & FLAG_ENCRYPTED) != 0;
    }

    public byte[] getCompressedData() {
        return this.compressedData;
    }

    public long getArchiveStart() {
        return this.archiveStart;
    }

    public CentralDirectoryFileHeader getLinkedCD() {
        return this.linkedCd;
    }

    public byte[] decompress() throws IOException, DataFormatException {
        return ZipCompressions.decompress(this);
    }

    public void linkCentralDirectory(final CentralDirectoryFileHeader cd) {
        this.linkedCd = cd;
    }

    @Override
    public String toString() {
        return "LocalFileHeader{name='" + this.name + "'"
                + ", compSize=" + this.compressedSize
                + ", uncompSize=" + this.uncompressedSize
                + ", relOffset=0x" + Long.toHexString(this.localHeaderRelOffset) + "}";
    }
}
