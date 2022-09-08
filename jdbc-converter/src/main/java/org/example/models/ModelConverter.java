package org.example.models;

import com.google.flatbuffers.Table;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.example.serde.FlatbuffersSerde;

/**
 * Collects schema definition from generated children classes and fills {@link Struct}
 *
 * @param <T> - flatbuffers table
 */
public abstract class ModelConverter<T extends Table> {
    final Deserializer<T> deserializer = new FlatbuffersSerde<T>(getClazz()).deserializer();


    public Schema getSchema() {
        return fillSchema(
                SchemaBuilder
                        .struct()
                        .name(getClazz().getCanonicalName()))
                .build();
    }

    public SchemaAndValue convert(byte[] data) {
        Schema schema = getSchema();
        Struct struct = new Struct(schema);

        if (data != null) {
            fillStruct(struct, deserializer.deserialize(null, data));
        } else {
            struct = null;
        }

        return new SchemaAndValue(schema, struct);
    }

    public abstract SchemaBuilder fillSchema(SchemaBuilder struct);

    public abstract void fillStruct(Struct struct, T obj);

    protected abstract Class<T> getClazz();
}
