package org.copypaste.data;

import java.util.List;

public class FileMetaResponse extends Response {

    private List<FileSummary> payload;

    public List<FileSummary> getPayload() {
        return payload;
    }

    public void setPayload(List<FileSummary> payload) {
        this.payload = payload;
    }
}
