/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.csv.issues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

class JiraCsv249Test {

    @Test
    void testJiraCsv249() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.builder().setEscape('\\').get();
        final StringWriter stringWriter = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(stringWriter, format)) {
            printer.printRecord("foo \\", "bar");
        }
        final StringReader reader = new StringReader(stringWriter.toString());
        final List<CSVRecord> records;
        try (CSVParser parser = CSVParser.builder().setReader(reader).setFormat(format).get()) {
            records = parser.getRecords();
        }
        records.forEach(record -> {
            assertEquals("foo \\", record.get(0));
            assertEquals("bar", record.get(1));
        });

    }
}
