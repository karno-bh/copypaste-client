package org.copypaste.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.copypaste.data.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 *
 * Utility class that connects to server with given URL and endpoint. It does not create HttpClient but expects it as
 * input
 *
 * @param <T> type of the Response object. Passed as parameter to the builder factory method
 */
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

        try {
            RequestConfig custom = RequestConfig.custom()
                    .setConnectionRequestTimeout(timeOutMS)
                    .setConnectTimeout(timeOutMS)
                    .setSocketTimeout(timeOutMS)
                    .build();
            GetMethodUrlConstructor constructor = new GetMethodUrlConstructor();
            HttpGet getMeta = new HttpGet(constructor.construct(serverUrl, endPoint, params));
            getMeta.setConfig(custom);
            try (CloseableHttpResponse response = httpClient.execute(getMeta)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IllegalStateException("Response status != 200");
                }
                HttpEntity entity = response.getEntity();

                String result;
                try(GuardedInputStreamConverter converter =
                            GuardedInputStreamConverter.asDefault()
                                    .inputStream(entity.getContent())
                                    .maxBuffer(maxBuffer).build()) {
                    result = converter.readContent();
                }

                ObjectMapper objectMapper = new ObjectMapper();
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
