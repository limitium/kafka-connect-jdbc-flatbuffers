package org.example.dao;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;
import org.example.models.FBAudit;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public abstract class AbstractFlatbufferWrapper<T extends Table> {
    protected T fbObject;
    protected boolean needFullRebuild = false;
    protected Set<String> dirtFields = new HashSet<>();

    protected AbstractFlatbufferWrapper(T fbObject) {
        this.fbObject = fbObject;
    }

    public T toFlatbuffers() {
        if (needFullRebuild) {
            fbObject = fullRebuild();
            needFullRebuild = false;
            dirtFields.clear();
        }
        return fbObject;
    }

    protected <TF> TF fieldData(String fieldName, TF value, Supplier<TF> getter) {
        if (dirtFields.contains(fieldName)) {
            return value;
        }
        return getter.get();
    }

    protected abstract T fullRebuild();

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

    public abstract long getId();

    public abstract FBAudit getFBAudit();
}
