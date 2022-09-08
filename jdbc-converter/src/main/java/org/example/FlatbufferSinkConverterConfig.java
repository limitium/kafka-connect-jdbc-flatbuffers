package org.example;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.storage.ConverterConfig;
import org.example.models.ModelConverter;

import java.util.Map;

import static org.apache.kafka.common.config.ConfigDef.NO_DEFAULT_VALUE;

/**
 * Holds config for {@link FlatbufferSinkConverter}, the main variable is `fb.converter.class`
 */
public class FlatbufferSinkConverterConfig extends ConverterConfig {
    public static final String FB_CONVERTER_CONFIG = "fb.converter.class";
    private static final String FB_CONVERTER_DOC = "The name of the Java character set to use for encoding strings as byte arrays.";
    private static final String FB_CONVERTER_DISPLAY = "Flatbuffers converter";
    private static final ConfigDef.Validator FB_CONVERTER_VALIDATOR = (name, value) -> {
        if (value == null) {
            throw new ConfigException(name, value, "Flatbuffers converter is required");
        }
        /**
         * Safe cast, checked in {@link org.apache.kafka.common.config.ConfigDef}
         */
        if (!ModelConverter.class.isAssignableFrom((Class<?>) value)) {
            throw new ConfigException(name, value, "Flatbuffers converter must extends " + ModelConverter.class.getName());
        }
    };

    private final static ConfigDef CONFIG;


    static {
        CONFIG = ConverterConfig.newConfigDef();

        CONFIG.define(FB_CONVERTER_CONFIG, ConfigDef.Type.CLASS, NO_DEFAULT_VALUE, FB_CONVERTER_VALIDATOR, ConfigDef.Importance.HIGH, FB_CONVERTER_DOC, null, -1, ConfigDef.Width.MEDIUM,
                FB_CONVERTER_DISPLAY);
    }

    public FlatbufferSinkConverterConfig(Map<String, ?> props) {
        super(CONFIG, props);
    }

    public static ConfigDef configDef() {
        return CONFIG;
    }

    @SuppressWarnings("unchecked")
    public Class<ModelConverter<?>> flatBufferConverter() {
        return (Class<ModelConverter<?>>) getClass(FB_CONVERTER_CONFIG);
    }
}