package org.copypaste.entry;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.copypaste.consts.Global;
import org.copypaste.data.ChunkResponse;
import org.copypaste.data.FileChunk;
import org.copypaste.data.FileMetaResponse;
import org.copypaste.data.FileSummary;
import org.copypaste.interthread_data.FileChunkImmutable;
import org.copypaste.service.AsyncSaveService;
import org.copypaste.util.HttpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MainRunner implements CommandLineRunner{

    private static final Logger log = LoggerFactory.getLogger(MainRunner.class);

    private Map<String, String> configMap;

    private AsyncSaveService asyncSaveService;

    @Autowired
    public void setConfigMap(Map<String, String> configMap) {
        this.configMap = configMap;
    }

    @Autowired
    public void setAsyncSaveService(AsyncSaveService asyncSaveService) {
        this.asyncSaveService = asyncSaveService;
    }

    public void run(String... args) throws Exception {

        createIncomingIfAbsent();

        Thread asyncThread = asyncSaveService.startAndWaitForInput();

        int timeout = Integer.parseInt(configMap.get(Global.TIME_OUT_MS_KEY));
        int retries = Integer.parseInt(configMap.get(Global.RETRIES_NUMBER_KEY));

        CloseableHttpClient httpClient = null;

        try {
            httpClient = HttpClients.createDefault();
            HttpConnector<FileMetaResponse> fileMetaResponseHttpConnector =
                    HttpConnector.as(FileMetaResponse.class)
                            .httpClient(httpClient)
                            .serverUrl(Global.SERVER_URL)
                            .endPoint(Global.META_END_POINT)
                            .maxBuffer(1024 * 256) // 256KB -- the list should not be too long!
                            .timeOutMS(timeout)
                            .retries(retries)
                            .build();

            FileMetaResponse fileMetaResponse = fileMetaResponseHttpConnector.executeWithRetries();
            if (!fileMetaResponse.isSuccess()) {
                log.error("Remote side signalizes error on files metadata {}. Giving up.", fileMetaResponse.getException());
                throw new RuntimeException("Cannot retrieve metadata");
            }
            List<FileSummary> fileSummaries = fileMetaResponse.getPayload();
            FileSummary newestFileSummary = fileSummaries.get(fileSummaries.size() - 1);
            log.info("Newest file: {}", newestFileSummary.getName());

            asyncSaveService.setFileName(newestFileSummary.getName());
            asyncSaveService.setFileCheckSum(newestFileSummary.getCheckSum());


            boolean hasNextChunk = true;
            int chunkNum = 0;
            while (hasNextChunk) {
                if (asyncSaveService.getThrowable() != null) {
                    throw new RuntimeException("Async save error. Giving up", asyncSaveService.getThrowable());
                }
                Map<String, String> parameters = new HashMap<>();
                parameters.put(Global.FILE_PARAM, newestFileSummary.getName());
                parameters.put(Global.CHUNK_NUM_PARAM, "" + chunkNum++);
                HttpConnector<ChunkResponse> chunkResponseHttpConnector =
                        HttpConnector.as(ChunkResponse.class)
                                .httpClient(httpClient)
                                .serverUrl(Global.SERVER_URL)
                                .endPoint(Global.CHUNK_END_POINT)
                                .maxBuffer(1024 * 1024 * 5) // 5MB
                                .timeOutMS(timeout)
                                .retries(retries)
                                .params(parameters)
                                .build();
                ChunkResponse chunkResponse = chunkResponseHttpConnector.executeWithRetries();
                if (!chunkResponse.isSuccess()) {
                    throw new RuntimeException("Remote error on chunk " + chunkNum + " . Remote reply is: " + chunkResponse.getException());
                }
                FileChunk fileChunk = chunkResponse.getPayload();
                FileChunkImmutable fileChunkImmutable = new FileChunkImmutable(fileChunk);
                asyncSaveService.put(fileChunkImmutable);
                hasNextChunk = fileChunk.isHasNextChunk();
            }
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
            if (asyncSaveService.getThrowable() == null) {
                asyncThread.join();
                /*if (asyncSaveService.getThrowable() != null) {
                    ???
                }*/
            }
            asyncSaveService.clearTemp();
        }
    }

    private void createIncomingIfAbsent() {
        File incoming = new File(Global.INCOMING_DIRECTORY);
        if (incoming.exists() && !incoming.isDirectory()) {
            throw new IllegalStateException(
                    MessageFormat.format("Incoming directory \"{0}\" exists as not a directory",
                            Global.INCOMING_DIRECTORY));
        } else if (!incoming.exists()) {
            boolean incomingCreated = incoming.mkdir();
            if (!incomingCreated) {
                throw new RuntimeException("Cannot create incoming directory");
            }
        }
    }

}
