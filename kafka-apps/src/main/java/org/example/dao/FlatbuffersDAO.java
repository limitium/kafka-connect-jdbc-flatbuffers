package org.example.dao;

import com.google.flatbuffers.Table;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.example.models.FBAudit;


public class FlatbuffersDAO<FB extends Table, WRAPPER extends AbstractFlatbufferWrapper<FB>> {
    private final KeyValueStore<Long, FB> store;
    private final WrapperFactory<FB, WRAPPER> wrapperFactory;

    private ProcessorContext<?, ?> context;

    public FlatbuffersDAO(ProcessorContext<?, ?> context, String storeName, Class<? extends FlatbuffersDAO<FB, WRAPPER>> daoClass) {
        this.context = context;
        this.store = context.getStateStore(storeName);
        this.wrapperFactory = new WrapperFactory<>(daoClass);
    }

    public WRAPPER create() {
        int partition = context.recordMetadata().get().partition();
        //generate id via sequencer from partition and context.currentStreamTimeMs()

        long id = System.currentTimeMillis() % 1000;

        return create(id);
    }

    public WRAPPER create(long id) {
        return wrapperFactory.create(id);
    }

    public WRAPPER get(long key) {
        FB storedObject = store.get(key);
        if (storedObject != null) {
            return wrapperFactory.wrap(storedObject);
        }
        return null;
    }

    public void put(WRAPPER object) {
        long key = object.getId();

        WRAPPER prevObject = get(key);

        int version = 1;
        long createdAt = context.currentStreamTimeMs();

        if (prevObject != null) {
            FBAudit prevAudit = prevObject.getFBAudit();
            version = prevAudit.version() + 1;
            createdAt = prevAudit.createdAt();
        }

        object.toFlatbuffers(); //build FB if needed

        //Mutate audit data
        FBAudit audit = object.getFBAudit();
        audit.mutateVersion(version);
        audit.mutateCreatedAt(createdAt);
        audit.mutateModifiedAt(context.currentStreamTimeMs());
        //todo: audit modified by from context put modified in headers or metarecord
        //todo: audit trace id from context

        store.put(key, object.toFlatbuffers()); //get mutated bytebuffer
    }

    public void delete(long id) {
        long key = id;
        WRAPPER prevObject = get(key);
        if (prevObject != null) {
            FBAudit audit = prevObject.getFBAudit();

            audit.mutateVersion(audit.version() + 1);
            audit.mutateModifiedAt(context.currentSystemTimeMs());
            audit.mutateRemoved(true);
            //todo: audit modified by from context put modified in headers or metarecord
            //todo: audit trace id from context

            store.put(key, prevObject.fbObject); //insert removed state
            store.delete(key); //leads to tombstone record only
        }
    }
}
