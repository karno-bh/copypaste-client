package org.copypaste.data;

public class FileChunk {

    private boolean hasNextChunk;

    private String chunkData;

    private String chunkHexMD5;

    public boolean isHasNextChunk() {
        return hasNextChunk;
    }

    public void setHasNextChunk(boolean hasNextChunk) {
        this.hasNextChunk = hasNextChunk;
    }

    public String getChunkData() {
        return chunkData;
    }

    public void setChunkData(String chunkData) {
        this.chunkData = chunkData;
    }

    public String getChunkHexMD5() {
        return chunkHexMD5;
    }

    public void setChunkHexMD5(String chunkHexMD5) {
        this.chunkHexMD5 = chunkHexMD5;
    }
}
