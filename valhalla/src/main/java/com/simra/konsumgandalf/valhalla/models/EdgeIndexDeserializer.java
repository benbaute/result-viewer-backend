package com.simra.konsumgandalf.valhalla.models;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.math.BigInteger;

public class EdgeIndexDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        BigInteger value = p.getBigIntegerValue();

        if (value == null || value.bitLength() > 31) {
            return null;
        }

        return value.intValue();
    }
}
