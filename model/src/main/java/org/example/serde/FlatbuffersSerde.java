package org.example.serde;

import com.google.flatbuffers.Table;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class FlatbuffersSerde<T extends Table> implements Serde<T> {

    private final Method rootMaker;

    @SuppressWarnings("unchecked")
    public FlatbuffersSerde(Class<T> clazz) {
        try {
            rootMaker = clazz.getMethod("getRootAs" + clazz.getSimpleName(), ByteBuffer.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Serializer<T> serializer() {
        return (topic, data) -> {
            //@todo: check for nulls and tombstone

            //BB must be truncated via
            //builder.finish(root_table)
            //byte[] bytes = builder.sizedByteArray();
            //Report.getRootAsReport(ByteBuffer.wrap(bytes))

            //wraps around truncated array;
            return data.getByteBuffer().array();
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public Deserializer<T> deserializer() {
        return (topic, data) -> {
            try {
                return data != null ? (T) rootMaker.invoke(null, ByteBuffer.wrap(data)) : null;
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
