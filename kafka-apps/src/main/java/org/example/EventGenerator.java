package org.example;


import com.google.flatbuffers.FlatBufferBuilder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.example.models.FBReportEvent;
import org.example.models.FBReportEventStatus;
import org.example.serde.FlatbuffersSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Properties;

public class EventGenerator {
    private static final Logger logger = LoggerFactory.getLogger(EventGenerator.class);

    public static void main(String[] args) {


        // create instance for properties to access producer configs
        Properties props = new Properties();

        //Assign localhost id
        props.put("bootstrap.servers", "localhost:9092");

        //Set acknowledgements for producer requests.
        props.put("acks", "all");

        //If the request fails, the producer can automatically retry,
        props.put("retries", 0);

        //Specify buffer size in config
        props.put("batch.size", 16384);

        //Reduce the no of requests less than 0
        props.put("linger.ms", 1);

        //The buffer.memory controls the total amount of memory available to the producer for buffering.
        props.put("buffer.memory", 33554432);

        FlatbuffersSerde<FBReportEvent> flatbuffersSerde = new FlatbuffersSerde<>(FBReportEvent.class);

        Producer<Long, FBReportEvent> producer = new KafkaProducer<>(props, Serdes.Long().serializer(), flatbuffersSerde.serializer());
        logger.info("Sending");

        generateEvent(producer, 0, FBReportEventStatus.NEW);

        generateEvent(producer, 1, FBReportEventStatus.NEW);
        generateEvent(producer, 1, FBReportEventStatus.DELETE);

        generateEvent(producer, 2, FBReportEventStatus.NEW);
        generateEvent(producer, 2, FBReportEventStatus.UPDATE);
        generateEvent(producer, 2, FBReportEventStatus.UPDATE);
        generateEvent(producer, 2, FBReportEventStatus.DELETE);

        generateEvent(producer, 3, FBReportEventStatus.UPDATE);

        generateEvent(producer, 4, FBReportEventStatus.DELETE);

        logger.info("Message sent successfully");
        producer.close();
    }

    private static void generateEvent(Producer<Long, FBReportEvent> producer, int i, byte status) {
        FlatBufferBuilder builder = new FlatBufferBuilder().forceDefaults(true);

        int what = builder.createString(String.valueOf(Math.random()));
        int who = builder.createString(String.valueOf(Math.random()));

        FBReportEvent.startFBReportEvent(builder);

        FBReportEvent.addId(builder, i);
        FBReportEvent.addWhen(builder, System.nanoTime());

        if (status != FBReportEventStatus.DELETE) {
            FBReportEvent.addWhat(builder, what);
            FBReportEvent.addWho(builder, who);
        }

        FBReportEvent.addStatus(builder, status);

        int root_table = FBReportEvent.endFBReportEvent(builder);
        builder.finish(root_table);

        byte[] bytes = builder.sizedByteArray();

        //wraps around truncated array;
        FBReportEvent event = FBReportEvent.getRootAsFBReportEvent(ByteBuffer.wrap(bytes));

        ProducerRecord<Long, FBReportEvent> record = new ProducerRecord<>("demo.reports", event.id(), event);


        logger.info(record.toString());
        producer.send(record);
    }
}