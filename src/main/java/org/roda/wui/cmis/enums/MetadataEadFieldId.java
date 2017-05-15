package org.roda.wui.cmis.enums;

/**
 * Metadata EAD 2002 Field IDs Enum.
 */
public enum MetadataEadFieldId {

    METADATA_EAD_UNIT_ID("metadata:ead:unitId"),

    METADATA_EAD_UNIT_TITLE("metadata:ead:unitTitle");

    private final String value;

    MetadataEadFieldId(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MetadataEadFieldId fromValue(String v) {
        for (MetadataEadFieldId c : MetadataEadFieldId.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
