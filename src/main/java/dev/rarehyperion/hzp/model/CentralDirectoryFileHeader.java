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

/**
 * Immutable public view of the Central Directory File Header record.
 *
 * <p>The central directory is the authoritative entry index of a ZIP archive. Each instance is linked to a {@link LocalFileHeader} that provides access to the compressed data.</p>
 */
public final class CentralDirectoryFileHeader {

    private static final int FLAG_ENCRYPTED = 0x0001;
    private static final int FLAG_DATA_DESC = 0x0008;

    private final int versionMadeBy;
    private final int versionNeeded;
    private final int generalPurposeFlag;
    private final int compressionMethod;
    private final int lastModTime;
    private final int lastModDate;
    private final long crc32;
    private final long compressedSize;
    private final long uncompressedSize;
    private final int diskNumberStart;
    private final int internalAttributes;
    private final long externalAttributes;
    private final long localHeaderRelOffset;
    private final String name;
    private final byte[] extraField;
    private final byte[] fileComment;

    private final LocalFileHeader linkedLfh;

    // What the fuck have I done with this constructor... it's bigger than Americas obesity problem.
    public CentralDirectoryFileHeader(final int versionMadeBy, final int versionNeeded, final int generalPurposeFlag, final int compressionMethod, final int lastModTime,
                                      final int lastModDate, final long crc32, final long compressedSize, final long uncompressedSize, final int diskNumberStart,
                                      final int internalAttributes, final long externalAttributes, final long localHeaderRelOffset, final String name,
                                      final byte[] extraField, final byte[] fileComment, final LocalFileHeader linkedLfh) {

        this.versionMadeBy = versionMadeBy;
        this.versionNeeded  = versionNeeded;
        this.generalPurposeFlag = generalPurposeFlag;
        this.compressionMethod = compressionMethod;
        this.lastModTime = lastModTime;
        this.lastModDate = lastModDate;
        this.crc32 = crc32;
        this.compressedSize = compressedSize;
        this.uncompressedSize = uncompressedSize;
        this.diskNumberStart = diskNumberStart;
        this.internalAttributes = internalAttributes;
        this.externalAttributes = externalAttributes;
        this.localHeaderRelOffset = localHeaderRelOffset;
        this.name = name;
        this.extraField = extraField;
        this.fileComment = fileComment;
        this.linkedLfh = linkedLfh;
    }

    public int getVersionMadeBy() {
        return this.versionMadeBy;
    }

    public int getVersionNeeded() {
        return this.versionNeeded;
    }

    public int getGeneralPurposeFlag() {
        return this.generalPurposeFlag;
    }

    public int getCompressionMethod() {
        return this.compressionMethod;
    }

    public int getLastModTime() {
        return this.lastModTime;
    }

    public int getLastModDate() {
        return this.lastModDate;
    }

    public long getCrc32() {
        return this.crc32;
    }

    public long getCompressedSize()  {
        return this.compressedSize;
    }

    public long getUncompressedSize() {
        return this.uncompressedSize;
    }

    public int getDiskNumberStart() {
        return this.diskNumberStart;
    }

    public int getInternalAttributes() {
        return this.internalAttributes;
    }

    public long getExternalAttributes() {
        return this.externalAttributes;
    }

    public long getLocalHeaderRelOffset() {
        return this.localHeaderRelOffset;
    }

    public String getName() {
        return this.name;
    }

    public byte[] getExtraField() {
        return this.extraField.clone();
    }

    public byte[] getFileComment() {
        return this.fileComment.clone();
    }

    public boolean hasDataDescriptor() {
        return (this.generalPurposeFlag & FLAG_DATA_DESC) != 0;
    }

    public boolean isEncrypted() {
        return (this.generalPurposeFlag & FLAG_ENCRYPTED) != 0;
    }

    public boolean isDirectory() {
        return this.name.endsWith("/");
    }

    /** @return The linked local file header. */
    public LocalFileHeader getLinkedLFH() { return this.linkedLfh; }

    @Override
    public String toString() {
        return "CentralDirectoryFileHeader{name='" + this.name + "'"
                + ", compSize=" + this.compressedSize
                + ", uncompSize=" + this.uncompressedSize
                + ", relOffset=0x" + Long.toHexString(this.localHeaderRelOffset) + "}";
    }

}
