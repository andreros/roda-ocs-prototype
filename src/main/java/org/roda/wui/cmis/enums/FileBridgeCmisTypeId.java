package org.roda.wui.cmis.enums;

/**
 * File Bridge CMIS Object Type IDs Enum.
 */
public enum FileBridgeCmisTypeId {

    CMIS_RODA_DOCUMENT("cmis:rodaDocument");

    private final String value;

    FileBridgeCmisTypeId(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static FileBridgeCmisTypeId fromValue(String v) {
        for (FileBridgeCmisTypeId c : FileBridgeCmisTypeId.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
