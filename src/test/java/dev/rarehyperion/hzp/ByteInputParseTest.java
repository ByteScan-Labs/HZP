package dev.rarehyperion.hzp;

import dev.rarehyperion.hzp.internal.ZipCompressions;
import dev.rarehyperion.hzp.model.LocalFileHeader;
import dev.rarehyperion.hzp.utility.Utility;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class ByteInputParseTest {

    @ParameterizedTest()
    @ValueSource(strings = { "mangled.jar" })
    void testByteInputParsing(final String name) throws Exception {
        final File file = Utility.getResource(name);
        assertNotNull(file, "The zip archive used for this test does not exist.");

        final byte[] bytes = Files.readAllBytes(file.toPath());
        final ZipArchive archive = ZipIO.read(bytes);
        assertFalse(archive.getLocalFiles().isEmpty(), "No local file headers found.");

        for(final LocalFileHeader header : archive.getLocalFiles()) {
            if(header.getName().endsWith(".class/")) {
                final byte[] compressed = header.getCompressedData();
                assertNotEquals(0, compressed.length, "Failed to parse data from fake directory.");

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
