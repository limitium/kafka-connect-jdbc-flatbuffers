package org.example;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.storage.Converter;
import org.apache.kafka.connect.storage.ConverterType;
import org.apache.kafka.connect.storage.StringConverterConfig;
import org.example.models.ModelConverter;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class FlatbufferSinkConverter implements Converter {

    private ModelConverter<?> flatbuffersConverter;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Map<String, Object> confs = new HashMap<>(configs);
        confs.put(StringConverterConfig.TYPE_CONFIG, isKey ? ConverterType.KEY.getName() : ConverterType.VALUE.getName());

        FlatbufferSinkConverterConfig conf = new FlatbufferSinkConverterConfig(confs);
        try {
            flatbuffersConverter = conf.flatBufferConverter().getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new ConfigException("Unable to create flatbuffers converter: " + e.getMessage());
        }
    }

    @Override
    public byte[] fromConnectData(String topic, Schema schema, Object value) {
        throw new RuntimeException("Only topic to database direction is supported");
    }

    @Override
    public byte[] fromConnectData(String topic, Headers headers, Schema schema, Object value) {
        return Converter.super.fromConnectData(topic, headers, schema, value);
    }

    @Override
    public SchemaAndValue toConnectData(String topic, byte[] value) {
        return flatbuffersConverter.convert(value);
    }

    @Override
    public SchemaAndValue toConnectData(String topic, Headers headers, byte[] value) {
        return Converter.super.toConnectData(topic, headers, value);
    }

    @Override
    public ConfigDef config() {
        return FlatbufferSinkConverterConfig.configDef();
    }

}