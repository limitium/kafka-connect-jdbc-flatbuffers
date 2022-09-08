package org.example.dao.generated;

import com.google.flatbuffers.FlatBufferBuilder;
import org.example.dao.AbstractFlatbufferWrapper;
import org.example.models.FBAudit;
import org.example.models.FBReport;

import java.nio.ByteBuffer;

/**
 * todo: add internal objects
 */
public class ReportWrapper extends AbstractFlatbufferWrapper<FBReport> {

    private long id;
    private String what;
    private String who;
    private long when;

    public ReportWrapper(FBReport fbObject) {
        super(fbObject);
        this.id = fbObject.id();
    }

    public ReportWrapper(long id) {
        super(new FBReport());
        this.id = id;
        needFullRebuild = true;
    }

    @Override
    protected FBReport fullRebuild() {
        FlatBufferBuilder builder = new FlatBufferBuilder().forceDefaults(true);

        int auditOffset = buildAudit(builder, fbObject.audit() != null ? fbObject.audit().modifiedBy() : "");

        int whatOffset = builder.createString(fieldData("what", what, fbObject::what));
        int whoOffset = builder.createString(fieldData("who", who, fbObject::who));

        FBReport.startFBReport(builder);

        FBReport.addId(builder, id);
        FBReport.addWhen(builder, fieldData("when", when, fbObject::when));
        FBReport.addWhat(builder, whatOffset);
        FBReport.addWho(builder, whoOffset);

        FBReport.addAudit(builder, auditOffset);

        int root_table = FBReport.endFBReport(builder);
        builder.finish(root_table);

        byte[] bytes = builder.sizedByteArray();

        //wraps around truncated array;
        return FBReport.getRootAsFBReport(ByteBuffer.wrap(bytes));
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public FBAudit getFBAudit() {
        return fbObject.audit();
    }

    public void setWhat(String what) {
        this.what = what;
        needFullRebuild = true;
        dirtFields.add("what");
    }

    public void setWho(String who) {
        this.who = who;
        needFullRebuild = true;
        dirtFields.add("who");
    }

    public void setWhen(long when) {
        this.when = when;
        dirtFields.add("when");
        fbObject.mutateWhen(when); //directly mutate
    }
}
