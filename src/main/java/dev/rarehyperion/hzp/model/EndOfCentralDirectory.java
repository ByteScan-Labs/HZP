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

import dev.rarehyperion.hzp.internal.EocdInfo;

/**
 * Immutable public view of the End of Central Directory record.
 */
public final class EndOfCentralDirectory {

    private final EocdInfo raw;

    public EndOfCentralDirectory(final EocdInfo raw) {
        this.raw = raw;
    }

    // Do I even need to have any of this java documentation? Surely if someone is using this they know what they are doing, right?
    // Well who cares, it has already been written, it's staying.

    /** Absolute file offset at which the (ZIP32) EOCD signature was found. */
    public long getEocdOffset() {
        return this.raw.eocdPos;
    }

    /** Disk number on which this EOCD record resides (should be 0). */
    public int getDiskNumber() {
        return this.raw.diskNumber;
    }

    /** Disk number on which the central directory starts (should be 0). */
    public int getCdStartDisk() {
        return this.raw.cdStartDisk;
    }

    /** Total number of central directory entries across all disks. */
    public long getTotalEntries() {
        return this.raw.totalEntries;
    }

    /** Byte size of the central directory. */
    public long getCentralDirSize() {
        return this.raw.centralDirSize;
    }

    /** Stored offset of the central directory start (may be relative). */
    public long getCentralDirOffset() {
        return this.raw.centralDirOffset;
    }

    /** Length of the EOCD comment field in bytes. */
    public int getCommentLength() {
        return this.raw.commentLength;
    }

    /** Raw bytes of the EOCD comment (empty array if none). */
    public byte[] getComment() {
        return this.raw.comment.clone();
    }

    /** Returns {@code true} if this record was parsed from a ZIP64 EOCD. */
    public boolean isZip64() {
        return this.raw.isZip64;
    }

    @Override
    public String toString() {
        return "EndOfCentralDirectory{eocdOffset=0x" + Long.toHexString(this.raw.eocdPos)
                + ", totalEntries=" + this.raw.totalEntries
                + ", cdSize=" + this.raw.centralDirSize
                + ", zip64=" + this.raw.isZip64 + "}";
    }


}
