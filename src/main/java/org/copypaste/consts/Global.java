package org.copypaste.consts;

/**
 * Global constants used in program
 */
public interface Global {

    String SERVER_URL = "http://localhost:8080";

    String META_END_POINT = "/files";

    String CHUNK_END_POINT = "/chunk";

    String INCOMING_DIRECTORY = "in";

    String CONFIG_DIRECTORY = "config";

    String CONFIG_FILE = "config.properties";

    String SERVER_URL_KEY = "server_url";

    String TIME_OUT_MS_KEY = "time_out_ms";

    String RETRIES_NUMBER_KEY = "retries_number";

    // 2 MIN
    String TIME_OUT_MS_VAL = "120000";

    String RETRIES_NUMBER_VAL = "3";

    String FILE_PARAM = "file";

    String CHUNK_NUM_PARAM = "chunkNum";

}
