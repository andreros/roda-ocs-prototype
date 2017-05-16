package org.roda.wui.cmis.enums;

/**
 * Metadata EAD 2002 Field IDs Enum.
 */
public enum MetadataEadFieldId {

    METADATA_EAD_UNIT_ID("metadata:ead:unitId"),

    METADATA_EAD_UNIT_TITLE("metadata:ead:unitTitle"),

    METADATA_EAD_COUNTRY_CODE("metadata:ead:countryCode"),

    METADATA_EAD_REPOSITORY_CODE("metadata:ead:repositoryCode"),

    METADATA_EAD_UNIT_DATE("metadata:ead:unitDate"),

    METADATA_EAD_UNIT_DATE_LABEL("metadata:ead:unitDateLabel"),

    METADATA_EAD_UNIT_DATE_NORMAL("metadata:ead:unitDateNormal"),

    METADATA_EAD_PHYSICAL_DESCRIPTION("metadata:ead:physicalDescription"),

    METADATA_EAD_PHYSICAL_DESCRIPTION_EXTENT("metadata:ead:physicalDescriptionExtent"),

    METADATA_EAD_PHYSICAL_DESCRIPTION_DIMENSIONS("metadata:ead:physicalDescriptionDimensions"),

    METADATA_EAD_PHYSICAL_DESCRIPTION_APPEARANCE("metadata:ead:physicalDescriptionAppearance"),

    METADATA_EAD_REPOSITORY_NAME("metadata:ead:repositoryName"),

    METADATA_EAD_LANG_MATERIAL("metadata:ead:langMaterial"),

    METADATA_EAD_LANG_MATERIAL_LANGUAGE("metadata:ead:langMaterialLanguage"),

    METADATA_EAD_NOTE_SOURCE_DESCRIPTION("metadata:ead:noteSourcesDescription"),

    METADATA_EAD_NOTE_GENERAL_NOTE("metadata:ead:noteGeneralNote"),

    METADATA_EAD_ORIGINATION("metadata:ead:origination"),

    METADATA_EAD_ORIGINATION_CREATION("metadata:ead:originationCreator"),

    METADATA_EAD_ORIGINATION_PRODUCTION("metadata:ead:originationProducer"),

    METADATA_EAD_ARCHIVE_DESCRIPTION("metadata:ead:archiveDescription"),

    METADATA_EAD_MATERIAL_SPECIFICATION("metadata:ead:materialSpecification"),

    METADATA_EAD_ODD_LEVEL_OF_DETAIL("metadata:ead:oddLevelOfDetail"),

    METADATA_EAD_ODD_STATUS_DESCRIPTION("metadata:ead:oddStatusDescription"),

    METADATA_EAD_SCOPE_CONTENT("metadata:ead:scopeContent"),

    METADATA_EAD_ARRANGEMENT("metadata:ead:arrangement"),

    METADATA_EAD_APPRAISAL("metadata:ead:appraisal"),

    METADATA_EAD_ACQUISITION_INFO("metadata:ead:acquisitionInfo"),

    METADATA_EAD_ACCRUALS("metadata:ead:accruals"),

    METADATA_EAD_CUSTODIAL_HISTORY("metadata:ead:custodialHistory"),

    METADATA_EAD_PROCESS_INFO_DATE("metadata:ead:processInfoDate"),

    METADATA_EAD_PROCESS_INFO_ARCHIVIST_NOTES("metadata:ead:processInfoArchivistNotes"),

    METADATA_EAD_ORIGINALS_LOCATION("metadata:ead:originalsLocation"),

    METADATA_EAD_ALTERNATIVE_FORM_AVAILABLE("metadata:ead:alternativeFormAvailable"),

    METADATA_EAD_RELATED_MATERIAL("metadata:ead:relatedMaterial"),

    METADATA_EAD_ACCESS_RESTRICTIONS("metadata:ead:accessRestrictions"),

    METADATA_EAD_USE_RESTRICTIONS("metadata:ead:useRestrictions"),

    METADATA_EAD_OTHER_FIND_AID("metadata:ead:otherFindAid"),

    METADATA_EAD_PHYSICAL_TECH("metadata:ead:physicalTech"),

    METADATA_EAD_BIBLIOGRAPHY("metadata:ead:bibliography"),

    METADATA_EAD_PREFER_CITE("metadata:ead:preferCite");

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
