package org.roda.wui.cmis.enums;

/**
 * Metadata Dublin Core 2002-12-12 Simple Field IDs Enum.
 */
public enum MetadataDublinCoreFieldId {

    METADATA_DUBLIN_CORE_TITLE("metadata:dublinCore:title"),

    METADATA_DUBLIN_CORE_IDENTIFIER("metadata:dublinCore:identifier"),

    METADATA_DUBLIN_CORE_CREATOR("metadata:dublinCore:creator"),

    METADATA_DUBLIN_CORE_INITIAL_DATE("metadata:dublinCore:initialDate"),

    METADATA_DUBLIN_CORE_FINAL_DATE("metadata:dublinCore:finalDate"),

    METADATA_DUBLIN_CORE_DESCRIPTION("metadata:dublinCore:description"),

    METADATA_DUBLIN_CORE_PUBLISHER("metadata:dublinCore:publisher"),

    METADATA_DUBLIN_CORE_CONTRIBUTOR("metadata:dublinCore:contributor"),

    METADATA_DUBLIN_CORE_RIGHTS("metadata:dublinCore:rights"),

    METADATA_DUBLIN_CORE_LANGUAGE("metadata:dublinCore:language"),

    METADATA_DUBLIN_CORE_COVERAGE("metadata:dublinCore:coverage"),

    METADATA_DUBLIN_CORE_FORMAT("metadata:dublinCore:format"),

    METADATA_DUBLIN_CORE_RELATION("metadata:dublinCore:relation"),

    METADATA_DUBLIN_CORE_SUBJECT("metadata:dublinCore:subject"),

    METADATA_DUBLIN_CORE_TYPE("metadata:dublinCore:type"),

    METADATA_DUBLIN_CORE_SOURCE("metadata:dublinCore:source");

    private final String value;

    MetadataDublinCoreFieldId(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MetadataDublinCoreFieldId fromValue(String v) {
        for (MetadataDublinCoreFieldId c : MetadataDublinCoreFieldId.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
