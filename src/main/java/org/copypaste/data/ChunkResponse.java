package org.copypaste.data;

public class ChunkResponse extends Response {

    private FileChunk payload;

    public FileChunk getPayload() {
        return payload;
    }

    public void setPayload(FileChunk payload) {
        this.payload = payload;
    }
}
