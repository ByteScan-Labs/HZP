package dev.rarehyperion.hzp.utility;

import dev.rarehyperion.hzp.ZipArchive;
import dev.rarehyperion.hzp.ZipIO;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public final class Utility {

    public static ZipArchive getArchive(final String name) {
        final File file = Utility.getResource(name);
        assertNotNull(file, "The zip archive used for this test does not exist.");

        final ZipArchive archive = assertDoesNotThrow(() -> ZipIO.read(file), "Failed to parse zip file.");
        assertFalse(archive.getLocalFiles().isEmpty(), "No local file headers found.");

        return archive;
    }

    public static File getResource(final String name) {
        final URL resource = Utility.class.getClassLoader().getResource("zips/" + name);
        if(resource == null) return null;
        return new File(resource.getPath());
    }

}
