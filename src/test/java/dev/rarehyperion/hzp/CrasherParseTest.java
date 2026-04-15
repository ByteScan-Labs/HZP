package dev.rarehyperion.hzp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassReader;

import dev.rarehyperion.hzp.internal.ZipCompressions;
import dev.rarehyperion.hzp.model.LocalFileHeader;
import dev.rarehyperion.hzp.utility.Utility;

public class CrasherParseTest {
    @ParameterizedTest()
    @ValueSource(strings = { "crasher.jar", "appended-613kb.jar" })
    @Timeout(10) // This timeout doesn't actually work, might need to put it in another thread if a regression is hit.
    void testCrasherZipParsing(final String name) {
        final ZipArchive archive = Utility.getArchive(name);

        System.out.println("Zip flags: " + archive.getFlags());

        for(final LocalFileHeader header : archive.getLocalFiles()) {
            if(header.getName().contains("Pass.class")) { // The crasher works through other file entries, but this is the real code that would run in java -jar.
                final byte[] compressed = header.getCompressedData(); 
                assertNotEquals(0, compressed.length, "Failed to parse data from fake '" + header.getName() + "'.");

                final byte[] decompressed = assertDoesNotThrow(() -> ZipCompressions.decompress(header), "Failed to decompress file data.");

                assertDoesNotThrow(() -> {
                    final ClassWriter cw = new ClassWriter(0);
                    final ClassReader cr = new ClassReader(decompressed);
                    cr.accept(cw, 0);
                }, "Failed to read class, decompression failed?");
            }
        }
    }
}
