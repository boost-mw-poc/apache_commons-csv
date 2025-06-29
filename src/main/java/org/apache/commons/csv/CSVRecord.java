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

package org.apache.commons.csv;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A CSV record parsed from a CSV file.
 *
 * <p>
 * Note: Support for {@link Serializable} is scheduled to be removed in version 2.0.
 * In version 1.8 the mapping between the column header and the column index was
 * removed from the serialized state. The class maintains serialization compatibility
 * with versions pre-1.8 for the record values; these must be accessed by index
 * following deserialization. There will be a loss of any functionally linked to the header
 * mapping when transferring serialized forms pre-1.8 to 1.8 and vice versa.
 * </p>
 */
public final class CSVRecord implements Serializable, Iterable<String> {

    private static final long serialVersionUID = 1L;

    /**
     * The start position of this record as a character position in the source stream. This may or may not correspond to the byte position depending on the
     * character set.
     */
    private final long characterPosition;

    /**
     * The starting position of this record in the source stream, measured in bytes.
     */
    private final long bytePosition;

    /** The accumulated comments (if any). */
    private final String comment;

    /** The record number. */
    private final long recordNumber;

    /** The values of the record. */
    private final String[] values;

    /** The parser that originates this record. This is not serialized. */
    private final transient CSVParser parser;

    CSVRecord(final CSVParser parser, final String[] values,  final String comment, final long recordNumber,
            final long characterPosition, final long bytePosition) {
        this.recordNumber = recordNumber;
        this.values = values != null ? values : Constants.EMPTY_STRING_ARRAY;
        this.parser = parser;
        this.comment = comment;
        this.characterPosition = characterPosition;
        this.bytePosition = bytePosition;
    }

    /**
     * Returns a value by {@link Enum}.
     *
     * @param e
     *            an enum
     * @return the String at the given enum String
     */
    public String get(final Enum<?> e) {
        return get(e == null ? null : e.name());
    }

    /**
     * Returns a value by index.
     *
     * @param i
     *            a column index (0-based)
     * @return the String at the given index
     */
    public String get(final int i) {
        return values[i];
    }

    /**
     * Returns a value by name. If multiple instances of the header name exists, only the last occurrence is returned.
     *
     * <p>
     * Note: This requires a field mapping obtained from the original parser.
     * A check using {@link #isMapped(String)} should be used to determine if a
     * mapping exists from the provided {@code name} to a field index. In this case an
     * exception will only be thrown if the record does not contain a field corresponding
     * to the mapping, that is the record length is not consistent with the mapping size.
     * </p>
     *
     * @param name
     *            the name of the column to be retrieved.
     * @return the column value, maybe null depending on {@link CSVFormat#getNullString()}.
     * @throws IllegalStateException
     *             if no header mapping was provided.
     * @throws IllegalArgumentException
     *             if {@code name} is not mapped or if the record is inconsistent.
     * @see #isMapped(String)
     * @see #isConsistent()
     * @see #getParser()
     * @see CSVFormat.Builder#setNullString(String)
     */
    public String get(final String name) {
        final Map<String, Integer> headerMap = getHeaderMapRaw();
        if (headerMap == null) {
            throw new IllegalStateException("No header mapping was specified, the record values can't be accessed by name");
        }
        final Integer index = headerMap.get(name);
        if (index == null) {
            throw new IllegalArgumentException(String.format("Mapping for %s not found, expected one of %s", name, headerMap.keySet()));
        }
        try {
            return values[index.intValue()]; // Explicit (un)boxing is intentional
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                    String.format("Index for header '%s' is %d but CSVRecord only has %d values!", name, index, Integer.valueOf(values.length))); // Explicit
                                                                                                                                                  // (un)boxing
                                                                                                                                                  // is
                                                                                                                                                  // intentional
        }
    }

    /**
     * Returns the starting position of this record in the source stream, measured in bytes.
     *
     * @return the byte position of this record in the source stream.
     * @since 1.13.0
     */
    public long getBytePosition() {
        return bytePosition;
    }

    /**
     * Returns the start position of this record as a character position in the source stream. This may or may not
     * correspond to the byte position depending on the character set.
     *
     * @return the position of this record in the source stream.
     */
    public long getCharacterPosition() {
        return characterPosition;
    }

    /**
     * Returns the comment for this record, if any.
     * Note that comments are attached to the following record.
     * If there is no following record (that is, the comment is at EOF),
     * then the comment will be ignored.
     *
     * @return the comment for this record, or null if no comment for this record is available.
     */
    public String getComment() {
        return comment;
    }

    private Map<String, Integer> getHeaderMapRaw() {
        return parser == null ? null : parser.getHeaderMapRaw();
    }

    /**
     * Returns the parser.
     *
     * <p>
     * Note: The parser is not part of the serialized state of the record. A null check
     * should be used when the record may have originated from a serialized form.
     * </p>
     *
     * @return the parser.
     * @since 1.7
     */
    public CSVParser getParser() {
        return parser;
    }

    /**
     * Returns the number of this record in the parsed CSV file.
     *
     * <p>
     * <strong>NOTE:</strong>If your CSV input has multi-line values, the returned number does not correspond to
     * the current line number of the parser that created this record.
     * </p>
     *
     * @return the number of this record.
     * @see CSVParser#getCurrentLineNumber()
     */
    public long getRecordNumber() {
        return recordNumber;
    }

    /**
     * Checks whether this record has a comment, false otherwise.
     * Note that comments are attached to the following record.
     * If there is no following record (that is, the comment is at EOF),
     * then the comment will be ignored.
     *
     * @return true if this record has a comment, false otherwise.
     * @since 1.3
     */
    public boolean hasComment() {
        return comment != null;
    }

    /**
     * Tells whether the record size matches the header size.
     *
     * <p>
     * Returns true if the sizes for this record match and false if not. Some programs can export files that fail this
     * test but still produce parsable files.
     * </p>
     *
     * @return true of this record is valid, false if not.
     */
    public boolean isConsistent() {
        final Map<String, Integer> headerMap = getHeaderMapRaw();
        return headerMap == null || headerMap.size() == values.length;
    }

    /**
     * Checks whether a given column is mapped, that is, its name has been defined to the parser.
     *
     * @param name
     *            the name of the column to be retrieved.
     * @return whether a given column is mapped.
     */
    public boolean isMapped(final String name) {
        final Map<String, Integer> headerMap = getHeaderMapRaw();
        return headerMap != null && headerMap.containsKey(name);
    }

    /**
     * Checks whether a column with a given index has a value.
     *
     * @param index
     *         a column index (0-based).
     * @return whether a column with a given index has a value.
     */
    public boolean isSet(final int index) {
        return 0 <= index && index < values.length;
    }

    /**
     * Checks whether a given column is mapped and has a value.
     *
     * @param name
     *            the name of the column to be retrieved.
     * @return whether a given column is mapped and has a value.
     */
    public boolean isSet(final String name) {
        return isMapped(name) && getHeaderMapRaw().get(name).intValue() < values.length; // Explicit (un)boxing is intentional
    }

    /**
     * Returns an iterator over the values of this record.
     *
     * @return an iterator over the values of this record.
     */
    @Override
    public Iterator<String> iterator() {
        return toList().iterator();
    }

    /**
     * Puts all values of this record into the given Map.
     *
     * @param <M> the map type.
     * @param map The Map to populate.
     * @return the given map.
     * @since 1.9.0
     */
    public <M extends Map<String, String>> M putIn(final M map) {
        if (getHeaderMapRaw() == null) {
            return map;
        }
        getHeaderMapRaw().forEach((key, value) -> {
            if (value < values.length) {
                map.put(key, values[value]);
            }
        });
        return map;
    }

    /**
     * Returns the number of values in this record.
     *
     * @return the number of values.
     */
    public int size() {
        return values.length;
    }

    /**
     * Returns a sequential ordered stream whose elements are the values.
     *
     * @return the new stream.
     * @since 1.9.0
     */
    public Stream<String> stream() {
        return Stream.of(values);
    }

    /**
     * Converts the values to a new List.
     * <p>
     * Editing the list does not update this instance.
     * </p>
     *
     * @return a new List
     * @since 1.9.0
     */
    public List<String> toList() {
        return stream().collect(Collectors.toList());
    }

    /**
     * Copies this record into a new Map of header name to record value. If multiple instances of a header name exist,
     * then only the last occurrence is mapped.
     *
     * <p>
     * Editing the map does not update this instance.
     * </p>
     *
     * @return A new Map. The map is empty if the record has no headers.
     */
    public Map<String, String> toMap() {
        return putIn(new LinkedHashMap<>(values.length));
    }

    /**
     * Returns a string representation of the contents of this record. The result is constructed by comment, mapping,
     * recordNumber and by passing the internal values array to {@link Arrays#toString(Object[])}.
     *
     * @return a String representation of this record.
     */
    @Override
    public String toString() {
        return "CSVRecord [comment='" + comment + "', recordNumber=" + recordNumber + ", values=" + Arrays.toString(values) + "]";
    }

    /**
     * Gets the values for this record. This is <strong>not</strong> a copy.
     *
     * @return the values for this record, never null.
     * @since 1.10.0
     */
    public String[] values() {
        return values;
    }

}
