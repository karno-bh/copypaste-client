package org.copypaste;

import org.copypaste.util.GuardedInputStreamConverter;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class GuardedConverterTest {

    final String data = "The quick brown fox jump over the lazy dog";

    @Test
    public void guardedConverterOkTest() throws IOException {
        InputStream testStream = inputStreamFromString(data);
        GuardedInputStreamConverter converter = GuardedInputStreamConverter.asDefault()
                .inputStream(testStream)
                .maxBuffer(1024)
                .build();
        String content = converter.readContent();
        Assert.assertEquals(data, content);
    }

    @Test
    public void guardedConverterOkLengthTest() throws IOException {
        InputStream testStream = inputStreamFromString(data);
        GuardedInputStreamConverter converter = GuardedInputStreamConverter.asDefault()
                .inputStream(testStream)
                .maxBuffer(data.length())
                .build();
        String content = converter.readContent();
        Assert.assertEquals(data, content);
    }

    @Test (expected = RuntimeException.class)
    public void guardedConverterNotOkLengthTest() throws IOException {
        InputStream testStream = inputStreamFromString(data);
        GuardedInputStreamConverter converter = GuardedInputStreamConverter.asDefault()
                .inputStream(testStream)
                .maxBuffer(data.length() - 1)
                .build();
        String content = converter.readContent();
        Assert.assertEquals(data, content);
    }

    private InputStream inputStreamFromString(String str) {
        try {
            return new ByteArrayInputStream(data.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ignore) {}
        return null;
    }
}
