package com.corebank.payment.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentEventStreamProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventStreamProcessor.class);

    private final ObjectMapper objectMapper;
    private final Map<String, Object> streamProperties;

    public PaymentEventStreamProcessor(ObjectMapper objectMapper, Map<String, Object> streamProperties) {
        this.objectMapper = objectMapper;
        this.streamProperties = streamProperties;
    }

    public KafkaStreams buildAndStart() {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> stream = builder.stream(
            "payments.txn.completed",
            Consumed.with(Serdes.String(), Serdes.String())
                .withOffsetResetPolicy(Topology.AutoOffsetReset.LATEST)
        );

        stream.peek((key, value) -> log.info("NAMNM STREAM received key={} payload={} ", key, value))
            .mapValues(value -> PaymentEventTransformer.transform(objectMapper, value))
            .to("payments.analytics.completed", Produced.with(Serdes.String(), Serdes.String()));

        Topology topology = builder.build();
        KafkaStreams streams = new KafkaStreams(topology, new StreamsConfig(streamProperties));
        streams.setUncaughtExceptionHandler((thread, throwable) -> {
            log.error("NAMNM STREAM unhandled error on thread {}", thread.getName(), throwable);
            streams.close(Duration.ofSeconds(5));
        });
        streams.start();
        log.info("NAMNM STREAM topology started {}", topology.describe());
        return streams;
    }
}
