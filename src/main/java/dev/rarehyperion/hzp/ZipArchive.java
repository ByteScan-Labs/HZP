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

package dev.rarehyperion.hzp;

import dev.rarehyperion.hzp.model.CentralDirectoryFileHeader;
import dev.rarehyperion.hzp.model.EndOfCentralDirectory;
import dev.rarehyperion.hzp.model.LocalFileHeader;

import java.util.*;

/**
 * The top-level result object returned after parsing a ZIP Archive.
 *
 * <p>It provides a lazy, thread-safe name index via {@link #getEntry(String)}, allowing O(1) lookup by filename without paying the construction cost unless needed.</p>
 * <p>Instances are created exclusively by the internal parser, and are immutable after construction.</p>
 */
public final class ZipArchive {

    private final List<LocalFileHeader> localFiles;
    private final List<CentralDirectoryFileHeader> centralDirectories;
    private final EndOfCentralDirectory end;
    private final EnumSet<Flag> flags;
    private final long archiveStart;

    // Name -> CD entry map for O(1) lookup; built lazily and stored.
    private volatile Map<String, CentralDirectoryFileHeader> nameIndex;

    ZipArchive(final List<LocalFileHeader> localFiles, final List<CentralDirectoryFileHeader> centralDirectories, final EndOfCentralDirectory end, final EnumSet<Flag> flags, final long archiveStart) {
        this.localFiles = localFiles;
        this.centralDirectories = centralDirectories;
        this.end = end;
        this.flags = flags;
        this.archiveStart = archiveStart;
    }

    public List<LocalFileHeader> getLocalFiles() {
        return this.localFiles;
    }

    public List<CentralDirectoryFileHeader> getCentralDirectories() {
        return this.centralDirectories;
    }

    public EndOfCentralDirectory getEnd() {
        return this.end;
    }

    public Optional<CentralDirectoryFileHeader> getEntry(final String name) {
        return Optional.ofNullable(this.buildNameIndex().get(name));
    }

    public EnumSet<Flag> getFlags() {
        return this.flags;
    }

    public int size() {
        return this.centralDirectories.size();
    }

    private Map<String, CentralDirectoryFileHeader> buildNameIndex() {
        if(this.nameIndex != null) return nameIndex;

        synchronized (this) {
            if(this.nameIndex != null) return nameIndex;

            final Map<String, CentralDirectoryFileHeader> index = new LinkedHashMap<>(this.centralDirectories.size() * 2);

            for(final CentralDirectoryFileHeader cd : this.centralDirectories) {
                index.put(cd.getName(), cd);
            }

            this.nameIndex = index;
        }

        return nameIndex;
    }

    @Override
    public String toString() {
        return "ZipArchive{entries=" + this.centralDirectories.size()
                + ", archiveStart=0x" + Long.toHexString(this.archiveStart)
                + ", evasionFlags=" + this.flags + "}";
    }

}
