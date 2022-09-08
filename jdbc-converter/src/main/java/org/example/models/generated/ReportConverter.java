package org.example.models.generated;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.example.models.FBReport;
import org.example.models.ModelConverter;

/**
 * This class should be generated from .fbs definition
 */
public class ReportConverter extends ModelConverter<FBReport> {
    @Override
    protected Class<FBReport> getClazz() {
        return FBReport.class;
    }

    public SchemaBuilder fillSchema(SchemaBuilder builder) {
        return builder

                .field("id", Schema.INT64_SCHEMA)
                .field("who", Schema.STRING_SCHEMA)
                .field("what", Schema.STRING_SCHEMA)
                .field("when", Schema.INT64_SCHEMA)

                .field("audit__trace_id", Schema.INT64_SCHEMA)
                .field("audit__version", Schema.INT32_SCHEMA)
                .field("audit__created_at", Schema.INT64_SCHEMA)
                .field("audit__modified_at", Schema.INT64_SCHEMA)
                .field("audit__modified_by", Schema.STRING_SCHEMA)
                .field("audit__removed", Schema.BOOLEAN_SCHEMA)

                ;
    }

    @Override
    public void fillStruct(Struct struct, FBReport obj) {

        struct.put("id", obj.id());
        struct.put("who", obj.who());
        struct.put("what", obj.what());
        struct.put("when", obj.when());

        struct.put("audit__trace_id", obj.audit().traceId());
        struct.put("audit__version", obj.audit().version());
        struct.put("audit__created_at", obj.audit().createdAt());
        struct.put("audit__modified_at", obj.audit().modifiedAt());
        struct.put("audit__modified_by", obj.audit().modifiedBy());
        struct.put("audit__removed", obj.audit().removed());
    }

}
