package org.roda.wui.cmis;

import org.apache.chemistry.opencmis.commons.definitions.*;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;
import org.roda.wui.cmis.enums.FileBridgeCmisTypeId;
import org.roda.wui.cmis.enums.MetadataDublinCoreFieldId;
import org.roda.wui.cmis.enums.MetadataEadFieldId;
import org.roda.wui.cmis.enums.MetadataKeyValueFieldId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the type definitions for all FileShare repositories.
 */
public class FileBridgeTypeManager {

    private static final Logger LOG = LoggerFactory.getLogger(FileBridgeTypeManager.class);

    private static final String NAMESPACE = "http://chemistry.apache.org/opencmis/fileshare";

    private final TypeDefinitionFactory typeDefinitionFactory;
    private final Map<String, TypeDefinition> typeDefinitions;

    public FileBridgeTypeManager() {
        // set up TypeDefinitionFactory
        typeDefinitionFactory = TypeDefinitionFactory.newInstance();
        typeDefinitionFactory.setDefaultNamespace(NAMESPACE);
        typeDefinitionFactory.setDefaultControllableAcl(false);
        typeDefinitionFactory.setDefaultControllablePolicy(false);
        typeDefinitionFactory.setDefaultQueryable(true);
        typeDefinitionFactory.setDefaultFulltextIndexed(false);
        typeDefinitionFactory.setDefaultTypeMutability(typeDefinitionFactory.createTypeMutability(false, false, false));

        // set up definitions map
        typeDefinitions = new HashMap<String, TypeDefinition>();

        // add base folder type
        MutableFolderTypeDefinition folderType = typeDefinitionFactory.createBaseFolderTypeDefinition(CmisVersion.CMIS_1_1);
        removeQueryableAndOrderableFlags(folderType);
        typeDefinitions.put(folderType.getId(), folderType);

        // add base document type
        MutableDocumentTypeDefinition baseDocumentType = typeDefinitionFactory.createBaseDocumentTypeDefinition(CmisVersion.CMIS_1_1);
        removeQueryableAndOrderableFlags(baseDocumentType);
        typeDefinitions.put(baseDocumentType.getId(), baseDocumentType);

        // add roda document type
        MutableDocumentTypeDefinition rodaDocumentType = typeDefinitionFactory.createDocumentTypeDefinition(CmisVersion.CMIS_1_1, baseDocumentType.getId());
        rodaDocumentType.setId(FileBridgeCmisTypeId.CMIS_RODA_DOCUMENT.value());
        rodaDocumentType.setQueryName(FileBridgeCmisTypeId.CMIS_RODA_DOCUMENT.value());
        rodaDocumentType.setDisplayName("RODA Document");
        rodaDocumentType.setDescription("RODA Document");

        // add roda document EAD metadata properties
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_UNIT_ID.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_UNIT_ID.value(), "EAD Unit Id", "RODA's EAD metadata Unit Id field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_UNIT_TITLE.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_UNIT_TITLE.value(), "EAD Unit Title", "RODA's EAD metadata Unit Title field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_COUNTRY_CODE.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_COUNTRY_CODE.value(), "EAD Country Code", "RODA's EAD metadata Country Code field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_REPOSITORY_CODE.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_REPOSITORY_CODE.value(), "EAD Repository Code", "RODA's EAD metadata Repository Code field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_UNIT_DATE.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_UNIT_DATE.value(), "EAD Unit Date", "RODA's EAD metadata Descriptive Date field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_UNIT_DATE_LABEL.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_UNIT_DATE_LABEL.value(), "EAD Unit Date Label", "RODA's EAD metadata Descriptive Date Label field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_UNIT_DATE_NORMAL.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_UNIT_DATE_NORMAL.value(), "EAD Unit Date Normal", "RODA's EAD metadata Descriptive Date range field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION.value(), "EAD Physical Description", "RODA's EAD metadata Physical Description field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_EXTENT.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_EXTENT.value(), "EAD Physical Description Extent", "RODA's EAD metadata Extent and medium field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_DIMENSIONS.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_DIMENSIONS.value(), "EAD Physical Description Dimensions", "RODA's EAD metadata Physical Description Dimensions field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_APPEARANCE.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_APPEARANCE.value(), "EAD Physical Description Appearance", "RODA's EAD metadata Physical Description Appearance field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_REPOSITORY_NAME.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_REPOSITORY_NAME.value(), "EAD Repository Name", "RODA's EAD metadata Repository Name field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_LANG_MATERIAL.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_LANG_MATERIAL.value(), "EAD Material Language Description", "RODA's EAD metadata Material Language Description field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_LANG_MATERIAL_LANGUAGE.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_LANG_MATERIAL_LANGUAGE.value(), "EAD Material Language", "RODA's EAD metadata Material Language field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_NOTE_SOURCE_DESCRIPTION.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_NOTE_SOURCE_DESCRIPTION.value(), "EAD Note Source Description", "RODA's EAD metadata Note Sources Description field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_NOTE_GENERAL_NOTE.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_NOTE_GENERAL_NOTE.value(), "EAD Note General Note", "RODA's EAD metadata Notes (Notes area) field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_ORIGINATION.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_ORIGINATION.value(), "EAD Origination", "RODA's EAD metadata Origination field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_ORIGINATION_CREATION.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_ORIGINATION_CREATION.value(), "EAD Origination Creation", "RODA's EAD metadata Origination Name of creator(s) field "));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_ORIGINATION_PRODUCTION.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_ORIGINATION_PRODUCTION.value(), "EAD Origination Production", "RODA's EAD metadata Origination Producer field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_ARCHIVE_DESCRIPTION.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_ARCHIVE_DESCRIPTION.value(), "EAD Archive Description", "RODA's EAD metadata Archive Description field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_MATERIAL_SPECIFICATION.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_MATERIAL_SPECIFICATION.value(), "EAD Material Specification", "RODA's EAD metadata Material Specification field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_ODD_LEVEL_OF_DETAIL.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_ODD_LEVEL_OF_DETAIL.value(), "EAD Odd Level Of Detail", "RODA's EAD metadata Level Of Detail field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_ODD_STATUS_DESCRIPTION.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_ODD_STATUS_DESCRIPTION.value(), "EAD Odd Status Description", "RODA's EAD metadata Status Description field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_SCOPE_CONTENT.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_SCOPE_CONTENT.value(), "EAD Scope Content", "RODA's EAD metadata Scope and Content field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_ARRANGEMENT.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_ARRANGEMENT.value(), "EAD Arrangement", "RODA's EAD metadata System of Arrangement field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_APPRAISAL.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_APPRAISAL.value(), "EAD Appraisal", "RODA's EAD metadata Appraisal, destruction and scheduling field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_ACQUISITION_INFO.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_ACQUISITION_INFO.value(), "EAD Acquisition Info", "RODA's EAD metadata Immediate source of acquisition or transfer field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_ACCRUALS.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_ACCRUALS.value(), "EAD Accruals", "RODA's EAD metadata Accruals field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_CUSTODIAL_HISTORY.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_CUSTODIAL_HISTORY.value(), "EAD Custodial History", "RODA's EAD metadata Custodial History field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_PROCESS_INFO_DATE.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_PROCESS_INFO_DATE.value(), "EAD Process Information Date", "RODA's EAD metadata Process Information Date field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_PROCESS_INFO_ARCHIVIST_NOTES.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_PROCESS_INFO_ARCHIVIST_NOTES.value(), "EAD Process Information Archivist Notes", "RODA's EAD metadata Process Information Archivist Notes field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_ORIGINALS_LOCATION.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_ORIGINALS_LOCATION.value(), "EAD Originals Location", "RODA's EAD metadata Existence and location od originals field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_ALTERNATIVE_FORM_AVAILABLE.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_ALTERNATIVE_FORM_AVAILABLE.value(), "EAD Alternative Form Available", "RODA's EAD metadata Existence and location of copies field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_RELATED_MATERIAL.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_RELATED_MATERIAL.value(), "EAD Related Material", "RODA's EAD metadata Related units of description field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_ACCESS_RESTRICTIONS.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_ACCESS_RESTRICTIONS.value(), "EAD Access Restrictions", "RODA's EAD metadata Conditions governing access field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_USE_RESTRICTIONS.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_USE_RESTRICTIONS.value(), "EAD Use Restrictions", "RODA's EAD metadata Conditionings governing reproduction field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_OTHER_FIND_AID.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_OTHER_FIND_AID.value(), "EAD Other Find Aid", "RODA's EAD metadata Other Find Aid field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_PHYSICAL_TECH.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_PHYSICAL_TECH.value(), "EAD Physical Tech", "RODA's EAD metadata Administrative and biographical history (conditions of access and use) field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_BIBLIOGRAPHY.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_BIBLIOGRAPHY.value(), "EAD Bibliography", "RODA's EAD metadata Bibliography field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataEadFieldId.METADATA_EAD_PREFER_CITE.value(),
                this.createPropertyStringDefinition(MetadataEadFieldId.METADATA_EAD_PREFER_CITE.value(), "EAD Prefer Cite", "RODA's EAD metadata Quote field"));

        // add roda document Dublin Core metadata properties
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_TITLE.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_TITLE.value(), "Dublin Core Title", "RODA's Dublin Core metadata Title field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_IDENTIFIER.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_IDENTIFIER.value(), "Dublin Core Identifier", "RODA's Dublin Core metadata Identifier field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_CREATOR.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_CREATOR.value(), "Dublin Core Creator", "RODA's Dublin Core metadata Creator field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_INITIAL_DATE.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_INITIAL_DATE.value(), "Dublin Core Initial Date", "RODA's Dublin Core metadata Initial Date field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_FINAL_DATE.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_FINAL_DATE.value(), "Dublin Core Final Date", "RODA's Dublin Core metadata Final Date field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_DESCRIPTION.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_DESCRIPTION.value(), "Dublin Core Description", "RODA's Dublin Core metadata Description field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_PUBLISHER.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_PUBLISHER.value(), "Dublin Core Publisher", "RODA's Dublin Core metadata Publisher field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_CONTRIBUTOR.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_CONTRIBUTOR.value(), "Dublin Core Contributor", "RODA's Dublin Core metadata Contributor field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_RIGHTS.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_RIGHTS.value(), "Dublin Core Rights", "RODA's Dublin Core metadata Rights field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_LANGUAGE.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_LANGUAGE.value(), "Dublin Core Language", "RODA's Dublin Core metadata Language field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_COVERAGE.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_COVERAGE.value(), "Dublin Core Coverage", "RODA's Dublin Core metadata Coverage field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_FORMAT.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_FORMAT.value(), "Dublin Core Format", "RODA's Dublin Core metadata Format field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_RELATION.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_RELATION.value(), "Dublin Core Relation", "RODA's Dublin Core metadata Relation field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_SUBJECT.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_SUBJECT.value(), "Dublin Core Subject", "RODA's Dublin Core metadata Subject field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_TYPE.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_TYPE.value(), "Dublin Core Core Type", "RODA's Dublin Core metadata Core Type field"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_SOURCE.value(),
                this.createPropertyStringDefinition(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_SOURCE.value(), "Dublin Core Core Source", "RODA's Dublin Core metadata Core Source field"));

        // add roda document Dublin Core metadata properties
        rodaDocumentType.getPropertyDefinitions().put(MetadataKeyValueFieldId.METADATA_KEY_VALUE_ID.value(),
                this.createPropertyStringDefinition(MetadataKeyValueFieldId.METADATA_KEY_VALUE_ID.value(), "Key-Value Id", "Id"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataKeyValueFieldId.METADATA_KEY_VALUE_TITLE.value(),
                this.createPropertyStringDefinition(MetadataKeyValueFieldId.METADATA_KEY_VALUE_TITLE.value(), "Key-Value Title", "Title"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataKeyValueFieldId.METADATA_KEY_VALUE_PRODUCER.value(),
                this.createPropertyStringDefinition(MetadataKeyValueFieldId.METADATA_KEY_VALUE_PRODUCER.value(), "Key-Value Producer", "Producer"));
        rodaDocumentType.getPropertyDefinitions().put(MetadataKeyValueFieldId.METADATA_KEY_VALUE_DATE.value(),
                this.createPropertyStringDefinition(MetadataKeyValueFieldId.METADATA_KEY_VALUE_DATE.value(), "Key-Value Date", "Date"));

        typeDefinitions.put(rodaDocumentType.getId(), rodaDocumentType);
    }

    /**
     * Adds a type definition.
     */
    public synchronized void addTypeDefinition(TypeDefinition type) {
        if (type == null) {
            throw new IllegalArgumentException("Type must be set!");
        }

        if (type.getId() == null || type.getId().trim().length() == 0) {
            throw new IllegalArgumentException("Type must have a valid id!");
        }

        if (type.getParentTypeId() == null || type.getParentTypeId().trim().length() == 0) {
            throw new IllegalArgumentException("Type must have a valid parent id!");
        }

        TypeDefinition parentType = typeDefinitions.get(type.getParentTypeId());
        if (parentType == null) {
            throw new IllegalArgumentException("Parent type doesn't exist!");
        }

        MutableTypeDefinition newType = typeDefinitionFactory.copy(type, true);

        // copy parent type property definitions and mark them as inherited
        for (PropertyDefinition<?> propDef : parentType.getPropertyDefinitions().values()) {
            MutablePropertyDefinition<?> basePropDef = typeDefinitionFactory.copy(propDef);
            basePropDef.setIsInherited(true);
            newType.addPropertyDefinition(basePropDef);
        }

        typeDefinitions.put(newType.getId(), newType);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Added type '{}'.", type.getId());
        }
    }

    /**
     * Removes the queryable and orderable flags from the property definitions
     * of a type definition because this implementations does neither support
     * queries nor can order objects.
     */
    private void removeQueryableAndOrderableFlags(MutableTypeDefinition type) {
        for (PropertyDefinition<?> propDef : type.getPropertyDefinitions().values()) {
            MutablePropertyDefinition<?> mutablePropDef = (MutablePropertyDefinition<?>) propDef;
            mutablePropDef.setIsQueryable(false);
            mutablePropDef.setIsOrderable(false);
        }
    }

    /**
     * Returns the internal type definition.
     */
    public synchronized TypeDefinition getInternalTypeDefinition(String typeId) {
        return typeDefinitions.get(typeId);
    }

    /**
     * Returns all internal type definitions.
     */
    public synchronized Collection<TypeDefinition> getInternalTypeDefinitions() {
        return typeDefinitions.values();
    }

    // --- service methods ---

    public TypeDefinition getTypeDefinition(CallContext context, String typeId) {
        TypeDefinition type = typeDefinitions.get(typeId);
        if (type == null) {
            throw new CmisObjectNotFoundException("Type '" + typeId + "' is unknown!");
        }

        return typeDefinitionFactory.copy(type, true, context.getCmisVersion());
    }

    public TypeDefinitionList getTypeChildren(CallContext context, String typeId, Boolean includePropertyDefinitions,
                                              BigInteger maxItems, BigInteger skipCount) {
        return typeDefinitionFactory.createTypeDefinitionList(typeDefinitions, typeId, includePropertyDefinitions,
                maxItems, skipCount, context.getCmisVersion());
    }

    public List<TypeDefinitionContainer> getTypeDescendants(CallContext context, String typeId, BigInteger depth,
                                                            Boolean includePropertyDefinitions) {
        return typeDefinitionFactory.createTypeDescendants(typeDefinitions, typeId, depth, includePropertyDefinitions,
                context.getCmisVersion());
    }

    // --- Types methods ---

    private PropertyStringDefinitionImpl createPropertyStringDefinition(String id, String displayName, String description) {

        PropertyStringDefinitionImpl propertyStringDefinition = new PropertyStringDefinitionImpl();
        propertyStringDefinition.setId(id);
        propertyStringDefinition.setLocalName(id);
        propertyStringDefinition.setQueryName(id);
        propertyStringDefinition.setDisplayName(displayName);
        propertyStringDefinition.setDescription(description);
        propertyStringDefinition.setPropertyType(PropertyType.STRING);
        propertyStringDefinition.setCardinality(Cardinality.SINGLE);
        propertyStringDefinition.setUpdatability(Updatability.READWRITE);
        propertyStringDefinition.setIsInherited(false);
        propertyStringDefinition.setIsQueryable(true);
        propertyStringDefinition.setIsOrderable(true);
        propertyStringDefinition.setIsRequired(false);

        return propertyStringDefinition;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (TypeDefinition type : typeDefinitions.values()) {
            sb.append('[');
            sb.append(type.getId());
            sb.append(" (");
            sb.append(type.getBaseTypeId().value());
            sb.append(")]");
        }

        return sb.toString();
    }
}
