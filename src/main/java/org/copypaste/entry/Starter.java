package org.copypaste.entry;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.copypaste.consts.Global;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class Starter implements CommandLineRunner{

    private Map<String, String> configMap;

    @Autowired
    public void setConfigMap(Map<String, String> configMap) {
        this.configMap = configMap;
    }

    public void run(String... args) throws Exception {

        createIncomingIfAbsent();

        int timeout = Integer.parseInt(configMap.get(Global.TIME_OUT_MS_KEY));

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            RequestConfig custom = RequestConfig.custom()
                    .setConnectionRequestTimeout(timeout)
                    .setConnectTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .build();
            HttpGet getMeta = new HttpGet(configMap.get(Global.SERVER_URL_KEY) + Global.META_END_POINT);
            getMeta.setConfig(custom);
            try (CloseableHttpResponse response = client.execute(getMeta)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IllegalStateException("Response status != 200");
                }
                HttpEntity entity = response.getEntity();
                String result = readContent(entity);
                System.out.println("Result" + result);
            }
        } catch(IOException ioe) {
            throw new RuntimeException("Cannot connect to server", ioe);
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

    private String readContent(HttpEntity httpEntity) throws IOException{
        try (InputStream content = new BufferedInputStream(httpEntity.getContent())) {
            // List<Byte> buffer = new ArrayList<>(4096);
            char[] buffer = new char[1024];
            int pos = 0;
            int aByte;
            while((aByte = content.read()) != -1) {
                if (pos == buffer.length) {
                    char[] newBuf = new char[buffer.length * 2];
                    System.arraycopy(newBuf, 0, newBuf, 0, buffer.length);
                    buffer = newBuf;
                } else if (pos > 1024 * 1024 * 10) {
                    throw new RuntimeException("Too long response, dropping");
                }
                buffer[pos++] = (char)aByte;
            }
            return new String(buffer, 0, pos + 1);
        }
    }
}
