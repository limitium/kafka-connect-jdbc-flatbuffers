package org.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.example.dao.FlatbuffersDAO;
import org.example.dao.generated.ReportDAO;
import org.example.dao.generated.ReportWrapper;
import org.example.models.FBReport;
import org.example.models.FBReportEvent;
import org.example.models.FBReportEventStatus;
import org.example.serde.FlatbuffersSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.KafkaStreamsInfrastructureCustomizer;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;
import org.springframework.stereotype.Component;

import java.util.Map;

@SpringBootApplication(scanBasePackages = {"org.example"})
@Configuration
@EnableKafka
@EnableKafkaStreams
public class ReportKStreamApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportKStreamApplication.class, args);
    }


    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfigs(@Value("${spring.application.name}") String appName) {
        return new KafkaStreamsConfiguration(Map.of(StreamsConfig.APPLICATION_ID_CONFIG, appName, StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"));
    }

    @Bean
    public StreamsBuilderFactoryBeanConfigurer kStreamsFactoryConfigurer(KafkaStreamsInfrastructureCustomizer topologyProvider) {
        return factoryBean -> {
            factoryBean.setInfrastructureCustomizer(topologyProvider);
            factoryBean.setKafkaStreamsCustomizer((ks) -> {
                ks.cleanUp();
            });
        };
    }

    @Component
    public class KStreamInfraCustomizer implements KafkaStreamsInfrastructureCustomizer {

        public static final String REPORT_SOURCE = "src";
        public static final String REPORT_STORE = "store";
        public static final String REPORT_PROCESSOR = "prc";

        @Override
        public void configureTopology(Topology topology) {

            //Serde for incoming event deserialization
            FlatbuffersSerde<FBReportEvent> eventSerde = new FlatbuffersSerde<>(FBReportEvent.class);
            //Serde for FBReport storing in a kafka store
            FlatbuffersSerde<FBReport> reportSerde = new FlatbuffersSerde<>(FBReport.class);

            //Naked kafka store initialization
            StoreBuilder<KeyValueStore<Long, FBReport>> reportStore = Stores.keyValueStoreBuilder(
                    Stores.persistentKeyValueStore(REPORT_STORE),
                    Serdes.Long(),
                    reportSerde);

            //topology definition 'demo.reports' is consumed by a single processor without any output
            topology.addSource(REPORT_SOURCE, Serdes.Long().deserializer(), eventSerde.deserializer(), "demo.reports")
                    .addProcessor(REPORT_PROCESSOR, ReportProcessor::new, REPORT_SOURCE)
                    .addStateStore(reportStore, REPORT_PROCESSOR);
        }

        /**
         * CUD processor for FBReport table, example of ReportDAO usage
         */
        public static class ReportProcessor implements Processor<Long, FBReportEvent, Long, FBReportEvent> {
            private static final Logger logger = LoggerFactory.getLogger(ReportProcessor.class);
            private ProcessorContext<Long, FBReportEvent> context;
            private ReportDAO reportDAO;

            @Override
            public void init(ProcessorContext<Long, FBReportEvent> context) {
                Processor.super.init(context);
                this.context = context;

                //DAO creation around kafka store, store identified by id
                reportDAO = new ReportDAO(context, REPORT_STORE);
            }

            @Override
            public void process(Record<Long, FBReportEvent> record) {
                byte status = record.value().status();

                logger.info("Event:{},{}", record.key(), FBReportEventStatus.name(status));

                switch (status) {
                    case FBReportEventStatus.NEW:
                        ReportWrapper report = reportDAO.create(record.key());

                        report.setWhat(record.value().what());
                        report.setWhen(record.value().when());
                        report.setWho(record.value().who());

                        reportDAO.put(report);
                        break;
                    case FBReportEventStatus.UPDATE:
                        report = reportDAO.get(record.key());
                        if (report != null) {

                            report.setWhat(record.value().what());
                            report.setWhen(record.value().when());
                            report.setWho(record.value().who());

                            reportDAO.put(report);
                        }
                        break;
                    case FBReportEventStatus.DELETE:
                        reportDAO.delete(record.key());
                        break;
                }
            }
        }


    }

}