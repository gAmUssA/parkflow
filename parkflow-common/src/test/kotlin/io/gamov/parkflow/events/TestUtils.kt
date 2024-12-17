package io.gamov.parkflow.events

import org.apache.avro.specific.SpecificRecord
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.io.EncoderFactory
import org.apache.avro.io.DecoderFactory
import java.io.ByteArrayOutputStream

inline fun <reified T : SpecificRecord> T.toByteArray(): ByteArray {
    val baos = ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(baos, null)
    val writer = SpecificDatumWriter<T>(this.schema)
    writer.write(this, encoder)
    encoder.flush()
    return baos.toByteArray()
}

inline fun <reified T : SpecificRecord> T.fromByteArray(bytes: ByteArray): T {
    val decoder = DecoderFactory.get().binaryDecoder(bytes, null)
    val reader = SpecificDatumReader<T>(this.schema)
    return reader.read(null, decoder)
}
