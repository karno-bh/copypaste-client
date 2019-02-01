package org.copypaste;

import org.copypaste.consts.Global;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;

@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final Map<String, Predicate<String>> knownValuesValidator = new HashMap<String, Predicate<String>>() {{
        put(Global.TIME_OUT_MS_KEY, Application::greaterThanZeroInt);
        put(Global.RETRIES_NUMBER_KEY, Application::greaterThanZeroInt);
    }};

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplicationBuilder()
                .sources(Application.class)
                .web(WebApplicationType.NONE)
                .build();
        springApplication.run(args);
    }

    @Bean
    public Map<String, String> configData() {

        Map<String, String> config = defaultConfiguration();
        File configDir = new File(Global.CONFIG_DIRECTORY);
        if (!configDir.exists()) {
            boolean configCreated = configDir.mkdir();
            if (!configCreated) {
                log.warn("Cannot create config directory.");
                return Collections.unmodifiableMap(config);
            }
            saveConfigAsProps(config);
            return Collections.unmodifiableMap(config);
        } else if (!Paths.get(Global.CONFIG_DIRECTORY, Global.CONFIG_FILE).toFile().exists()) {
            saveConfigAsProps(config);
            return Collections.unmodifiableMap(config);
        }

        loadAndMergeProperties(config);

        return Collections.unmodifiableMap(config);
    }


    private Map<String, String> defaultConfiguration() {
        Map<String, String> config = new HashMap<>();
        config.put(Global.SERVER_URL_KEY, Global.SERVER_URL);
        config.put(Global.TIME_OUT_MS_KEY, "60000");
        config.put(Global.RETRIES_NUMBER_KEY, "3");
        return config;
    }

    private void saveConfigAsProps(Map<String, String> config) {
        Properties props = new Properties();
        props.putAll(config);
        try {
            props.store(new FileOutputStream(Paths.get(Global.CONFIG_DIRECTORY, Global.CONFIG_FILE).toFile()), null);
        } catch (IOException e) {
            log.warn("Cannot save config file", e);
        }
    }

    /**
     * This method will mutate the passed map.
     * @param config
     */
    private void loadAndMergeProperties(Map<String, String> config) {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(Paths.get(Global.CONFIG_DIRECTORY, Global.CONFIG_FILE).toFile()));
        } catch (IOException e) {
            log.warn("Cannot load config file", e);
        }
        props.forEach((property, value) -> {
            String propStr = ("" + property).trim();
            String valStr = ("" + value).trim();
            if (validatePropery(propStr, valStr)) {
                config.put(propStr, valStr);
            }
        });
    }

    private boolean validatePropery(String key, String value) {
        Predicate<String> validator = knownValuesValidator.get(key);
        // unknown values should be added to map
        return validator == null || validator.test(value);
    }

    private static boolean greaterThanZeroInt(String value) {
        int timeout = -1;
        try {
            timeout = Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            log.warn("Cannot parse value");
        }
        return timeout > 0;
    }

}
