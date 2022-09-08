package org.example.dao;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;
import org.example.models.FBAudit;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Wraps flatbuffers table for the mutation control
 *
 * @param <T> flatbuffers table
 */
public abstract class AbstractFlatbufferWrapper<T extends Table> {
    protected T fbObject;

    //if nonrpimary is changed this flag must be set to true
    protected boolean needFullRebuild = false;
    //Set of dirty fields after mutation
    protected Set<String> dirtFields = new HashSet<>();

    protected AbstractFlatbufferWrapper(T fbObject) {
        this.fbObject = fbObject;
    }

    /**
     * Generates flatbuffers object from wrapper. There are to options
     * <ul>
     * <li>New flatbuffer generated - if previous flatbuffer was empty(new object) or non primitive mutation occurs
     * <li>Old flatbuffer - if flatbuffer wasn't empty and only primitives were mutated
     * </ul>
     * @return flatbuffers object
     */
    public T toFlatbuffers() {
        if (needFullRebuild) {
            fbObject = fullRebuild();
            needFullRebuild = false;
            dirtFields.clear();
        }
        return fbObject;
    }

    /**
     * Return original value from flatbuffers if it wasn't mutated in the wrapper
     *
     * @param fieldName
     * @param value mutated value from the wrapper
     * @param getter supplier of an original value from the flatbuffers
     * @return
     * @param <TF> type of the fiels
     */
    protected <TF> TF fieldData(String fieldName, TF value, Supplier<TF> getter) {
        if (dirtFields.contains(fieldName)) {
            return value;
        }
        return getter.get();
    }

    /**
     * Implementation of a full flatbuffers build for a Table
     *
     * @return
     */
    protected abstract T fullRebuild();

    /**
     * Generic implementation for Audit data
     *
     * @param builder
     * @param modifiedBy
     * @return
     */
    protected int buildAudit(FlatBufferBuilder builder, String modifiedBy) {
        int modifiedByOffset = builder.createString(modifiedBy);
        FBAudit.startFBAudit(builder);
        FBAudit.addVersion(builder, 0);
        FBAudit.addTraceId(builder, 0);
        FBAudit.addCreatedAt(builder, 0);
        FBAudit.addModifiedAt(builder, 0);
        FBAudit.addModifiedBy(builder, modifiedByOffset);
        FBAudit.addRemoved(builder, false);
        int auditOffset = FBAudit.endFBAudit(builder);
        return auditOffset;
    }

    /**
     * Object public_id, also used as a key in store and kafka topic
     *
     * @return
     */
    public abstract long getId();

    /**
     * Generic audit table from a wrapper
     *
     * @return
     */
    public abstract FBAudit getFBAudit();
}
