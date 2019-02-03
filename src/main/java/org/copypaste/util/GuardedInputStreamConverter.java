package org.copypaste.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 *
 * Guarded InputStream to String converter. The meaning of guard is in that it will throw exception if the gotten
 * {@link InputStream} is greater than some allowed maximum. This is done in order to drop on long "impossible" server
 * replies.
 *
 * @author Sergey
 */
public class GuardedInputStreamConverter implements Closeable {

    private final int maxBuffer;

    private final int startBuffer;

    private final InputStream inputStream;

    private GuardedInputStreamConverter(InputStream inputStream, int startBuffer, int maxBuffer) {
        Objects.requireNonNull(inputStream, "InputStream cannot be null");
        this.inputStream = inputStream;
        if (startBuffer <= 0 || maxBuffer <= 0) {
            throw new IllegalArgumentException("Buffers should be greater than zero");
        }
        this.startBuffer = startBuffer;
        this.maxBuffer = maxBuffer;
    }

    public String readContent() throws IOException {
        char[] buffer = new char[startBuffer];
        int pos = 0;
        int aByte;
        while((aByte = inputStream.read()) != -1) {
            if (pos == buffer.length) {
                char[] newBuf = new char[buffer.length * 2];
                System.arraycopy(buffer, 0, newBuf, 0, buffer.length);
                buffer = newBuf;
            } else if (pos > maxBuffer - 1) {
                throw new RuntimeException("Too long response, dropping");
            }
            buffer[pos++] = (char)aByte;
        }
        return new String(buffer, 0, pos);
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    public static Builder asDefault() {
        Builder builder = new Builder();
        builder.startBuffer(1024);
        return builder;
    }

    public static class Builder {

        private int maxBuffer;

        private int startBuffer;

        private InputStream inputStream;


        public Builder maxBuffer(int maxBuffer) {
            this.maxBuffer = maxBuffer;
            return this;
        }

        public Builder startBuffer(int startBuffer) {
            this.startBuffer = startBuffer;
            return this;
        }

        public Builder inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public GuardedInputStreamConverter build() {
            return new GuardedInputStreamConverter(inputStream, startBuffer, maxBuffer);
        }

    }
}
