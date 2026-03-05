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

package dev.rarehyperion.hzp.internal.randomaccess.impl;

import dev.rarehyperion.hzp.internal.randomaccess.RandomAccessInput;

import java.io.EOFException;
import java.io.IOException;

/**
 * Thin {@link RandomAccessInput} adapter over a {@code byte[]} array.
 * <p>Does not copy the array, the caller is still responsible for its lifetime.</p>
 */
public class ByteArrayInput implements RandomAccessInput {

    private final byte[] data;
    private int position;

    public ByteArrayInput(final byte[] data) {
        if(data == null) throw new NullPointerException("Data must not be null");
        this.data = data;
        this.position = 0;
    }

    @Override
    public long length() throws IOException {
        return this.data.length;
    }

    @Override
    public long getPosition() {
        return this.position;
    }

    @Override
    public void seek(final long position) throws IOException {
        if(position < 0 || position > this.data.length) {
            throw new IOException("Seek position out of bounds: " + position + " (length=" + this.data.length + ")");
        }

        this.position = (int) position;
    }

    @Override
    public void readFully(final byte[] bytes) throws IOException {
        this.readFully(bytes, 0, bytes.length);
    }

    @Override
    public void readFully(final byte[] bytes, final int offset, final int length) throws IOException {
        if(this.position + length > this.data.length) {
            throw new EOFException("Requested " + length + " bytes at position " + this.position + " but only " + (this.data.length - this.position) + "remain");
        }

        System.arraycopy(this.data, this.position, bytes, offset, length);
        this.position += length;
    }

    @Override
    public void skipBytes(final int n) throws IOException {
        if(this.position + n > this.data.length) {
            throw new EOFException("Cannot skip " + n + " bytes at position " + this.position + " (length=" + this.data.length + ")");
        }

        this.position += n;
    }

}
