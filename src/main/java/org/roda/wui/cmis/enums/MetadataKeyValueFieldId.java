package org.roda.wui.cmis.enums;

/**
 * Metadata Key-Value Field IDs Enum.
 */
public enum MetadataKeyValueFieldId {

    METADATA_KEY_VALUE_ID("metadata:keyValue:id"),

    METADATA_KEY_VALUE_TITLE("metadata:keyValue:title"),

    METADATA_KEY_VALUE_PRODUCER("metadata:keyValue:producer"),

    METADATA_KEY_VALUE_DATE("metadata:keyValue:date");

    private final String value;

    MetadataKeyValueFieldId(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MetadataKeyValueFieldId fromValue(String v) {
        for (MetadataKeyValueFieldId c : MetadataKeyValueFieldId.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
