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

import dev.rarehyperion.hzp.internal.CentralDirectoryParser;
import dev.rarehyperion.hzp.internal.EocdInfo;
import dev.rarehyperion.hzp.internal.EocdParser;
import dev.rarehyperion.hzp.model.CentralDirectoryFileHeader;
import dev.rarehyperion.hzp.model.EndOfCentralDirectory;
import dev.rarehyperion.hzp.model.LocalFileHeader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.zip.ZipException;

/**
 * The entry point for reading ZIP Archives. Orchestrates EOCD location, central-directory parsing, and assembly of the resulting {@link ZipArchive}.
 */
public final class ZipIO {

    /** Parses the given file as a ZIP Archive. */
    public static ZipArchive read(final File file) throws IOException {
        try(final RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            return ZipIO.parse(raf);
        }
    }

    /** Parses the given path as a ZIP Archive. */
    public static ZipArchive read(final Path path) throws IOException {
        return ZipIO.read(path.toFile());
    }

    private static ZipArchive parse(final RandomAccessFile raf) throws IOException {
        final EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);

        final EocdInfo eocd = EocdParser.findEocd(raf, flags);
        if(eocd == null) throw new ZipException("EOCD record not found. Not a valid ZIP Archive.");

        if(eocd.diskNumber != 0 || eocd.cdStartDisk != 0) {
            flags.add(Flag.MULTI_DISK);
        }

        if(eocd.commentLength > 0) {
            flags.add(Flag.EOCD_COMMENT_PADDING);
        }

        final long[] positions = EocdParser.resolveCdPosition(raf, eocd, flags);
        final long cdStartAbs = positions[0];
        final long archiveStart = positions[1];

        final List<CentralDirectoryFileHeader> cdEntries =
                CentralDirectoryParser.parse(raf, cdStartAbs, eocd.centralDirSize, archiveStart, flags);

        final List<LocalFileHeader> lfhList = new ArrayList<>(cdEntries.size());

        for(final CentralDirectoryFileHeader cd : cdEntries) {
            lfhList.add(cd.getLinkedLFH());
        }

        final EndOfCentralDirectory endRecord = new EndOfCentralDirectory(eocd);
        return new ZipArchive(lfhList, cdEntries, endRecord, flags, archiveStart);
    }

    /** If you see this, you are using this wrong. You are meant to access the methods statically. */
    private ZipIO() { /* no-op */ }

}
