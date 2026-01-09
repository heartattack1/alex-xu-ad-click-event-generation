package com.adstream.serialization;

import com.adstream.model.ClickEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ClickEventSerde implements Serde<ClickEvent> {
    private final Schema schema;

    public ClickEventSerde() {
        this.schema = loadSchema();
    }

    @Override
    public Serializer<ClickEvent> serializer() {
        return new Serializer<>() {
            @Override
            public byte[] serialize(String topic, ClickEvent data) {
                if (data == null) {
                    return null;
                }
                GenericRecord record = new GenericData.Record(schema);
                record.put("event_id", data.getEventId());
                record.put("ad_id", data.getAdId());
                record.put("click_ts", data.getClickTs());
                record.put("user_id", data.getUserId());
                record.put("ip", data.getIp());
                record.put("country", data.getCountry());
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
                    GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
                    writer.write(record, encoder);
                    encoder.flush();
                    return outputStream.toByteArray();
                } catch (IOException ex) {
                    throw new SerializationException("Failed to serialize ClickEvent", ex);
                }
            }
        };
    }

    @Override
    public Deserializer<ClickEvent> deserializer() {
        return new Deserializer<>() {
            @Override
            public ClickEvent deserialize(String topic, byte[] data) {
                if (data == null || data.length == 0) {
                    return null;
                }
                try {
                    GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
                    BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
                    GenericRecord record = reader.read(null, decoder);
                    return new ClickEvent(
                        record.get("event_id").toString(),
                        record.get("ad_id").toString(),
                        (Long) record.get("click_ts"),
                        record.get("user_id").toString(),
                        record.get("ip").toString(),
                        record.get("country").toString()
                    );
                } catch (IOException ex) {
                    throw new SerializationException("Failed to deserialize ClickEvent", ex);
                }
            }
        };
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }

    private Schema loadSchema() {
        try (InputStream input = ClickEventSerde.class.getClassLoader().getResourceAsStream("click-event.avsc")) {
            if (input == null) {
                throw new SerializationException("click-event.avsc not found in resources");
            }
            return new Schema.Parser().parse(input);
        } catch (IOException ex) {
            throw new SerializationException("Failed to read click-event.avsc", ex);
        }
    }
}
