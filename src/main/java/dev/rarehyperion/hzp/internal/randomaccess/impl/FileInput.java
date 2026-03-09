package dev.rarehyperion.hzp.internal.randomaccess.impl;

import dev.rarehyperion.hzp.internal.randomaccess.RandomAccessInput;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Thin {@link RandomAccessInput} adapter over a {@link RandomAccessFile}.
 * <p>Does not own the file, the caller is still responsible for opening and closing it.</p>
 */
public class FileInput implements RandomAccessInput {

    private final RandomAccessFile raf;

    public FileInput(final RandomAccessFile raf) {
        if(raf == null) throw new NullPointerException("RAF must not be null");
        this.raf = raf;
    }

    @Override
    public long length() throws IOException {
        return this.raf.length();
    }

    @Override
    public long getPosition() throws IOException {
        return this.raf.getFilePointer();
    }

    @Override
    public void seek(final long position) throws IOException {
        this.raf.seek(position);
    }

    @Override
    public void readFully(final byte[] bytes) throws IOException {
        this.raf.readFully(bytes);
    }

    @Override
    public void readFully(final byte[] bytes, final int offset, final int length) throws IOException {
        this.raf.readFully(bytes, offset, length);
    }

    @Override
    public void skipBytes(final int n) throws IOException {
        this.raf.skipBytes(n);
    }

}
