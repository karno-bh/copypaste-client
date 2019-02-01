package org.copypaste.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.copypaste.data.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Objects;

public class HttpConnector<T extends Response> {

    private static final Logger log = LoggerFactory.getLogger(HttpConnector.class);

    private final String serverUrl;

    private final String endPoint;

    private final int timeOutMS;

    private final Class<T> resultClass;

    private final int maxBuffer;

    private final int retries;

    private final Map<String, String> params;

    private final CloseableHttpClient httpClient;


    private HttpConnector(String serverUrl, String endPoint, int timeOutMS, Class<T> resultClass,
                          int maxBuffer, int retries, Map<String, String> params, CloseableHttpClient httpClient) {
        if (serverUrl == null || "".equals(serverUrl)) {
            throw new IllegalArgumentException("Server url cannot be empty");
        }
        this.serverUrl = serverUrl;
        if (endPoint == null || "".equals(endPoint)) {
            throw new IllegalArgumentException("End point cannot be empty");
        }
        this.endPoint = endPoint;
        this.timeOutMS = timeOutMS;
        Objects.requireNonNull(resultClass, "Result class cannot should not be null");
        this.resultClass = resultClass;
        if (maxBuffer < 1024) {
            throw new IllegalArgumentException("Max Buffer should not be less than 1KB");
        }
        this.maxBuffer = maxBuffer;
        if (retries < 0) {
            throw  new IllegalArgumentException("Retries should be greater than zero");
        }
        this.retries = retries;
        this.params = params;
        Objects.requireNonNull(httpClient, "Http Client cannot be null");
        this.httpClient = httpClient;
    }

    public static<U extends Response> Builder<U> as(Class<U> resultClass) {
        Builder<U> builder = new Builder<>();
        builder.resultClass(resultClass);
        return builder;
    }


    public T execute() {
        // try (CloseableHttpClient client = HttpClients.createDefault()) {
        try {
            RequestConfig custom = RequestConfig.custom()
                    .setConnectionRequestTimeout(timeOutMS)
                    .setConnectTimeout(timeOutMS)
                    .setSocketTimeout(timeOutMS)
                    .build();
            HttpGet getMeta = new HttpGet(constructURL());
            getMeta.setConfig(custom);
            try (CloseableHttpResponse response = httpClient.execute(getMeta)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IllegalStateException("Response status != 200");
                }
                HttpEntity entity = response.getEntity();
                String result = readContent(entity);
                // System.out.println("Result" + result);
                ObjectMapper objectMapper = new ObjectMapper();
                //T responseObject = objectMapper.readValue(result, resultClass);
                // System.out.println(responseObject);
                //return responseObject;
                return objectMapper.readValue(result, resultClass);
            }
        } catch(IOException ioe) {
            throw new RuntimeException("IO error", ioe);
        }
    }

    public T executeWithRetries() {
        int retry = 0;
        do {
            try {
                return execute();
            } catch (Exception e) {
                log.info("Error while connecting to end point {} on server {} will retry {} attempts",
                        endPoint, serverUrl, retries - retry, e);
            }
        } while (retry++ < retries);
        throw new RuntimeException("Failed to retrieve from end point: " + endPoint + ". Giving up");
    }

    private String constructURL() {
        StringBuilder sb = new StringBuilder(128);
        sb.append(serverUrl).append(endPoint);
        if (params == null) {
            return sb.toString();
        }
        sb.append('?');
        boolean first = true;
        for (Map.Entry<String, String> parameter : params.entrySet()) {
            String prefix;
            if (first) {
                prefix = "";
                first = false;
            } else {
                prefix = "&";
            }
            sb.append(prefix);
            sb.append(parameter.getKey()).append('=').append(encodeURI(parameter.getValue()));
        }
        return sb.toString();
    }

    /**
     * From here: https://stackoverflow.com/questions/607176/java-equivalent-to-javascripts-encodeuricomponent-that-produces-identical-outpu
     *
     * @param s
     * @return
     */
    private String encodeURI(String s) {
        String result;
        try {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            result = s;
        }
        return result;
    }

    private String readContent(HttpEntity httpEntity) throws IOException{
        try (InputStream content = new BufferedInputStream(httpEntity.getContent())) {
            char[] buffer = new char[1024];
            int pos = 0;
            int aByte;
            while((aByte = content.read()) != -1) {
                if (pos == buffer.length) {
                    char[] newBuf = new char[buffer.length * 2];
                    System.arraycopy(buffer, 0, newBuf, 0, buffer.length);
                    buffer = newBuf;
                } else if (pos > maxBuffer) {
                    throw new RuntimeException("Too long response, dropping");
                }
                buffer[pos++] = (char)aByte;
            }
            return new String(buffer, 0, pos + 1);
        }
    }



    public static class Builder<U extends Response> {

        private String serverUrl;

        private String endPoint;

        private int timeOutMS;

        private Class<U> resultClass;

        private int maxBuffer;

        private int retries;

        private Map<String, String> params;

        private CloseableHttpClient httpClient;

        public Builder<U> serverUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder<U> endPoint(String endPoint) {
            this.endPoint = endPoint;
            return this;
        }

        public Builder<U> timeOutMS(int timeOutMS) {
            this.timeOutMS = timeOutMS;
            return this;
        }

        public Builder<U> resultClass(Class<U> resultClass) {
            this.resultClass = resultClass;
            return this;
        }

        public Builder<U> maxBuffer(int maxBuffer) {
            this.maxBuffer = maxBuffer;
            return this;
        }

        public Builder<U> retries(int retries) {
            this.retries = retries;
            return this;
        }

        public Builder<U> params(Map<String, String> params) {
            this.params = params;
            return this;
        }

        public Builder<U> httpClient(CloseableHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public HttpConnector<U> build() {
            return new HttpConnector<>(serverUrl, endPoint, timeOutMS, resultClass, maxBuffer, retries, params, httpClient);
        }

    }

}
