/*
 * Copyright (c) 2015 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.brunel.data.io;

import org.brunel.data.CannedData;
import org.brunel.data.Data;
import org.brunel.data.Dataset;
import org.brunel.data.Field;
import org.junit.Test;

import java.util.Date;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Tests data serialization and deserialization
 */
public class TestSerialize {

    @Test
    public void testSerializeFieldBasics() {
        Field a = Data.makeConstantField("foo", "bar", 10, 1000);
        assertEquals(true, a.hasProperty("numeric"));
        byte[] bytes = Serialize.serializeField(a);
        Field b = (Field) Serialize.deserialize(bytes);
        assertEquals("foo", b.name);
        assertEquals("bar", b.label);
        assertEquals(1000, b.rowCount());
        assertEquals(true, b.hasProperty("numeric"));
        assertEquals(10, b.min(), 1e-6);
    }

    @Test
    public void testSerializeFieldNumericData() {
        Field a = Data.makeColumnField("a", "b", new Object[]{1.0, 2.0, 9.0});
        a = Data.toNumeric(a);

        byte[] bytes = Serialize.serializeField(a);
        Field b = (Field) Serialize.deserialize(bytes);
        assertEquals("a", b.name);
        assertEquals("b", b.label);
        assertEquals(3, b.rowCount());
        assertEquals(4.0, b.getNumericProperty("mean"), 1e-6);
    }

    @Test
    public void testSerializeFieldDateData() {
        Date date1 = new Date();
        Date date2 = new Date(date1.getTime() + 86400000 * 12);
        Field a = Data.makeColumnField("a", "b", new Object[]{date1, date2});
        a = Data.toDate(a);
        assertTrue(a.hasProperty("numeric"));
        assertTrue(a.hasProperty("date"));
        assertEquals(12, a.max() - a.min(), 1e-6);

        byte[] bytes = Serialize.serializeField(a);
        Field b = (Field) Serialize.deserialize(bytes);
        assertEquals(true, b.hasProperty("numeric"));
        assertEquals(true, b.hasProperty("date"));
        assertEquals(12, b.max() - b.min(), 1e-6);
    }

    @Test
    public void testWhiskeyDataset() {
        Dataset dataset = Dataset.make(CSV.read(CannedData.whiskey));
        byte[] bytes = Serialize.serializeDataset(dataset);
        assertEquals(2017, bytes.length);
        assertEquals(Serialize.DATA_SET, bytes[0]);

        Dataset d = (Dataset) Serialize.deserialize(bytes);
        assertEquals(dataset.rowCount(), d.rowCount());
        assertEquals(dataset.field("rating").getNumericProperty("mean"), d.field("rating").getNumericProperty("mean"), 0.001);
        assertEquals("Count", d.field("#count").label);
    }

    @Test
    public void testNoData() {
        String DATA = "a,b\n,";
        Dataset dataset = Dataset.make(CSV.read(DATA));
        byte[] bytes = Serialize.serializeDataset(dataset);
        assertEquals("1 2 2 97 0 65 0 1 3 255 1 0 2 98 0 66 0 1 3 255 1 0", dump(bytes));
    }

    @Test
    public void testNastyData() {
        String DATA = "a,b\n,\né,1.23456789e-213\n\u2026,NaN";
        Dataset dataset = Dataset.make(CSV.read(DATA));
        byte[] bytes = Serialize.serializeDataset(dataset);
        assertEquals("1 2 2 97 0 65 0 3 4 3 195 169 0 226 128 166 0 3 0 1 2 2 98 0 " +
                "66 0 3 4 3 49 46 50 51 52 53 54 55 56 57 101 45 50 49 51 0 78 97 78 0 3 0 1 2", dump(bytes));
    }

    @Test
    public void testBankDataset() {
        Dataset dataset = Dataset.make(CSV.read(CannedData.bank));
        byte[] bytes = Serialize.serializeDataset(dataset);
        assertEquals(589, bytes.length);
        assertEquals(Serialize.DATA_SET, bytes[0]);
    }

    @Test
    public void testEncodingsByte() {
        byte[] bytes;
        bytes = new ByteOutput().addByte(4).asBytes();
        assertEquals("4", dump(bytes));
        assertEquals(4, new ByteInput(bytes).readByte());
    }

    @Test
    public void testEncodingsInteger() {
        byte[] bytes;

        bytes = new ByteOutput().addNumber(null).asBytes();
        assertEquals("255", dump(bytes));
        assertEquals(null, new ByteInput(bytes).readNumber());

        bytes = new ByteOutput().addNumber(0).asBytes();
        assertEquals("0", dump(bytes));
        assertEquals(0, new ByteInput(bytes).readNumber());

        bytes = new ByteOutput().addNumber(4).asBytes();
        assertEquals("4", dump(bytes));
        assertEquals(4, new ByteInput(bytes).readNumber());

        bytes = new ByteOutput().addNumber(200).asBytes();
        assertEquals("200", dump(bytes));
        assertEquals(200, new ByteInput(bytes).readNumber());

        bytes = new ByteOutput().addNumber(267).asBytes();
        assertEquals("253 11 1", dump(bytes));
        assertEquals(267, new ByteInput(bytes).readNumber());

        bytes = new ByteOutput().addNumber(32767).asBytes();
        assertEquals("253 255 127", dump(bytes));
        assertEquals(32767, new ByteInput(bytes).readNumber());

        bytes = new ByteOutput().addNumber(32768).asBytes();
        assertEquals("253 0 128", dump(bytes));
        assertEquals(32768, new ByteInput(bytes).readNumber());

        bytes = new ByteOutput().addNumber(-32767).asBytes();
        assertEquals("254 45 51 50 55 54 55 0", dump(bytes));
        assertEquals(-32767.0, new ByteInput(bytes).readNumber());

        bytes = new ByteOutput().addNumber(-32768).asBytes();
        assertEquals("254 45 51 50 55 54 56 0", dump(bytes));
        assertEquals(-32768.0, new ByteInput(bytes).readNumber());

        bytes = new ByteOutput().addNumber(65535 + 256).asBytes();
        assertEquals("254 54 53 55 57 49 0", dump(bytes));
        assertEquals(65535 + 256.0, new ByteInput(bytes).readNumber());

        bytes = new ByteOutput().addNumber(-1).asBytes();
        assertEquals("254 45 49 0", dump(bytes));
        assertEquals(-1.0, new ByteInput(bytes).readNumber());

        bytes = new ByteOutput().addNumber(-1024).asBytes();
        assertEquals("254 45 49 48 50 52 0", dump(bytes));
        assertEquals(-1024.0, new ByteInput(bytes).readNumber());
    }

    @Test
    public void testEncodingsString() {
        byte[] bytes;

        bytes = new ByteOutput().addString("foo\nis bar").asBytes();
        assertEquals("102 111 111 10 105 115 32 98 97 114 0", dump(bytes));
        assertEquals("foo\nis bar", new ByteInput(bytes).readString());

        bytes = new ByteOutput().addString("foo\nis \u2026").asBytes();
        assertEquals("102 111 111 10 105 115 32 226 128 166 0", dump(bytes));
        assertEquals("foo\nis \u2026", new ByteInput(bytes).readString());

        bytes = new ByteOutput().addString("").asBytes();
        assertEquals("0", dump(bytes));
        assertEquals("", new ByteInput(bytes).readString());

        bytes = new ByteOutput().addString(null).asBytes();
        assertEquals("3", dump(bytes));
        assertEquals(null, new ByteInput(bytes).readString());
    }

    @Test
    public void testEncodingsNumber() {
        byte[] bytes;

        bytes = new ByteOutput().addNumber(Double.NaN).asBytes();
        assertEquals("254 78 97 78 0", dump(bytes));
        assertTrue(Double.isNaN(new ByteInput(bytes).readNumber().doubleValue()));

        bytes = new ByteOutput().addNumber(null).asBytes();
        assertEquals("255", dump(bytes));
        assertEquals(null, new ByteInput(bytes).readNumber());

        bytes = new ByteOutput().addNumber(1.1).asBytes();
        assertEquals("254 49 46 49 0", dump(bytes));
        assertEquals(1.1, new ByteInput(bytes).readNumber().doubleValue(), 1e-6);

        bytes = new ByteOutput().addNumber(-1.1).asBytes();
        assertEquals("254 45 49 46 49 0", dump(bytes));
        assertEquals(-1.1, new ByteInput(bytes).readNumber().doubleValue(), 1e-6);

        bytes = new ByteOutput().addNumber(1.2e200).asBytes();
        assertEquals("254 49 46 50 101 50 48 48 0", dump(bytes));
        assertEquals(1.2e200, new ByteInput(bytes).readNumber().doubleValue(), 1e-6);

        bytes = new ByteOutput().addNumber(-1.2e200).asBytes();
        assertEquals("254 45 49 46 50 101 50 48 48 0", dump(bytes));
        assertEquals(-1.2e200, new ByteInput(bytes).readNumber().doubleValue(), 1e-6);
    }

    private String dump(byte[] bytes) {
        String b = "";
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) b += " ";
            b += (bytes[i] & 0xff);
        }
        return b;
    }
}
