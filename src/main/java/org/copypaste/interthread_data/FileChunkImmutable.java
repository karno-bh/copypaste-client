package org.copypaste.interthread_data;

import org.copypaste.data.FileChunk;

import java.util.Base64;

/**
 *
 * Immutable files chunk DTO. Immutability is chosen as inter-thread safe data passing.
 *
 * @author Sergey
 */
public class FileChunkImmutable {

    private final boolean hasNextChunk;

    private final String chunkData;

    private final String chunkHexMD5;

    private volatile byte[] cachedEncodedContent;

    public boolean isHasNextChunk() {
        return hasNextChunk;
    }

    public String getChunkData() {
        return chunkData;
    }

    public String getChunkHexMD5() {
        return chunkHexMD5;
    }

    public byte[] getChunkEncodedContent() {
        if (cachedEncodedContent != null) {
            return cachedEncodedContent;
        }
        cachedEncodedContent = Base64.getDecoder().decode(chunkData);
        return cachedEncodedContent;
    }

    public FileChunkImmutable(String chunkData, String chunkHexMD5, boolean hasNextChunk) {
        this.hasNextChunk = hasNextChunk;
        this.chunkData = chunkData;
        this.chunkHexMD5 = chunkHexMD5;
    }

    public FileChunkImmutable(FileChunk fileChunk) {
        this(fileChunk.getChunkData(), fileChunk.getChunkHexMD5(), fileChunk.isHasNextChunk());
    }

}