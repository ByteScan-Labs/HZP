package dev.rarehyperion.hzp.internal.randomaccess;

import java.io.IOException;

/**
 * Minimal random-access read interface shared by both file-backed and in-memory parsing paths.
 */
public interface RandomAccessInput {

    /** The total length of the underlying source in bytes. */
    long length() throws IOException;

    /** Returns the current read position. */
    long getPosition() throws IOException;

    /** Set the current read position to {@code position}. */
    void seek(final long position) throws IOException;

    /** Reads exactly {@code bytes.length} bytes into {@code bytes}, blocking until all bytes are available. */
    void readFully(final byte[] bytes) throws IOException;

    /** Reads exactly {@code length} bytes into {@code bytes} starting at {@code offset}. */
    void readFully(final byte[] bytes, final int offset, final int length) throws IOException;

    /** Skips over exactly {@code n} bytes, advancing the read position. */
    void skipBytes(final int n) throws IOException;

}
