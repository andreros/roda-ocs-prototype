package org.roda.wui.cmis.database;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.roda.wui.cmis.enums.MetadataDublinCoreFieldId;
import org.roda.wui.cmis.enums.MetadataEadFieldId;
import org.roda.wui.cmis.enums.MetadataKeyValueFieldId;
import org.sqlite.SQLiteException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible for interacting with the database.
 */
public class Database {

    /**
     * JDBC Driver.
     */
    private String jdbcDriver = null;

    /**
     * Database Name.
     */
    private String databaseName = null;

    /**
     * JDBC connection string / URL.
     */
    private String connectionUrl = null;

    /**
     * Database connection.
     */
    private Connection connection = null;

    /**
     * Query parser.
     */
    private Query queryParser = null;

    /**
     * Constructor.
     *
     * @param provider The database provider name.
     */
    public Database(String provider) {
        switch (provider.toLowerCase()) {
            //TODO: implement in the future
            //case "mysql":
            //    break;
            case "sqlite":
            default:
                this.jdbcDriver = "org.sqlite.JDBC";
                this.databaseName = "roda-cmis.db";
                this.connectionUrl = "jdbc:sqlite:" + this.databaseName;
                break;
        }
        this.initialize();
    }

    /**
     * Function responsible for opening a newconnection to the database.
     * otherwise returns the existing one.
     */
    public void connect() {
        try {
            if ((this.connection == null) || (this.connection.isClosed())) {
                Class.forName(this.jdbcDriver);
                this.connection = DriverManager.getConnection(this.connectionUrl);
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Method responsible for initializing the database.
     */
    private void initialize() {
        Statement stmt;
        try {
            this.connect();
            stmt = this.connection.createStatement();

            //Drop the "cmis:folder" table
            String sql = "DROP TABLE [cmis:folder]";
            try {
                stmt.executeUpdate(sql);
            } catch (SQLiteException e) { /* table does not exist, fail silently */ }

            //Create the "cmis:folder" table
            sql = "CREATE TABLE [cmis:folder] (" +
                " [cmis:objectId] VARCHAR(500) PRIMARY KEY NOT NULL," +
                " [cmis:name] VARCHAR(500), " +
                " [cmis:createdBy] VARCHAR(500), " +
                " [cmis:lastModifiedBy] VARCHAR(500), " +
                " [cmis:creationDate] DATETIME, " +
                " [cmis:lastModificationDate] DATETIME, " +
                " [cmis:changeToken] VARCHAR(500), " +
                " [cmis:description] VARCHAR(500), " +
                " [cmis:secondaryObjectTypeIds] VARCHAR(500), " +
                " [cmis:baseTypeId] VARCHAR(500), " +
                " [cmis:objectTypeId] VARCHAR(500), " +
                " [cmis:path] VARCHAR(5000), " +
                " [cmis:parentId] VARCHAR(500), " +
                " [cmis:allowedChildObjectTypeIds] VARCHAR(500)" +
                ")";
            try {
                stmt.executeUpdate(sql);
            } catch (SQLiteException e) { /* table already exists, fail silently */ }

            //Drop the "cmis:rodaDocument" table
            sql = "DROP TABLE [cmis:rodaDocument]";
            try {
                stmt.executeUpdate(sql);
            } catch (SQLiteException e) { /* table does not exist, fail silently */ }

            //Create the "cmis:rodaDocument" table
            sql = "CREATE TABLE [cmis:rodaDocument] (" +
                " [cmis:objectId] VARCHAR(500) PRIMARY KEY NOT NULL," +
                " [cmis:name] VARCHAR(500), " +
                " [cmis:createdBy] VARCHAR(500), " +
                " [cmis:lastModifiedBy] VARCHAR(500), " +
                " [cmis:creationDate] DATETIME, " +
                " [cmis:lastModificationDate] DATETIME, " +
                " [cmis:changeToken] VARCHAR(500), " +
                " [cmis:description] VARCHAR(500), " +
                " [cmis:secondaryObjectTypeIds] VARCHAR(500), " +
                " [cmis:baseTypeId] VARCHAR(500), " +
                " [cmis:objectTypeId] VARCHAR(500), " +
                " [cmis:path] VARCHAR(5000), " +
                " [cmis:isImmutable] BOOLEAN, " +
                " [cmis:isLatestVersion] BOOLEAN, " +
                " [cmis:isMajorVersion] BOOLEAN, " +
                " [cmis:isLatestMajorVersion] BOOLEAN, " +
                " [cmis:versionLabel] VARCHAR(500), " +
                " [cmis:versionSeriesId] VARCHAR(500), " +
                " [cmis:isVersionSeriesCheckedOut] BOOLEAN, " +
                " [cmis:versionSeriesCheckedOutBy] VARCHAR(500), " +
                " [cmis:versionSeriesCheckedOutId] VARCHAR(500), " +
                " [cmis:checkinComment] VARCHAR(500), " +
                " [cmis:isPrivateWorkingCopy] BOOLEAN, " +
                " [cmis:contentStreamLength] VARCHAR(500), " +
                " [cmis:contentStreamMimeType] VARCHAR(500), " +
                " [cmis:contentStreamFileName] VARCHAR(500), " +
                " [cmis:contentStreamId] VARCHAR(500), " +
                " [metadata:ead:unitId] VARCHAR(500), " +
                " [metadata:ead:unitTitle] VARCHAR(500), " +
                " [metadata:ead:countryCode] VARCHAR(500), " +
                " [metadata:ead:repositoryCode] VARCHAR(500), " +
                " [metadata:ead:unitDate] VARCHAR(500), " +
                " [metadata:ead:unitDateLabel] VARCHAR(500), " +
                " [metadata:ead:unitDateNormal] VARCHAR(500), " +
                " [metadata:ead:physicalDescription] VARCHAR(500), " +
                " [metadata:ead:physicalDescriptionExtent] VARCHAR(500), " +
                " [metadata:ead:physicalDescriptionDimensions] VARCHAR(500), " +
                " [metadata:ead:physicalDescriptionAppearance] VARCHAR(500), " +
                " [metadata:ead:repositoryName] VARCHAR(500), " +
                " [metadata:ead:langMaterial] VARCHAR(500), " +
                " [metadata:ead:langMaterialLanguage] VARCHAR(500), " +
                " [metadata:ead:noteSourcesDescription] VARCHAR(500), " +
                " [metadata:ead:noteGeneralNote] VARCHAR(500), " +
                " [metadata:ead:origination] VARCHAR(500), " +
                " [metadata:ead:originationCreator] VARCHAR(500), " +
                " [metadata:ead:originationProducer] VARCHAR(500), " +
                " [metadata:ead:archiveDescription] VARCHAR(500), " +
                " [metadata:ead:materialSpecification] VARCHAR(500), " +
                " [metadata:ead:oddLevelOfDetail] VARCHAR(500), " +
                " [metadata:ead:oddStatusDescription] VARCHAR(500), " +
                " [metadata:ead:scopeContent] VARCHAR(500), " +
                " [metadata:ead:arrangement] VARCHAR(500), " +
                " [metadata:ead:appraisal] VARCHAR(500), " +
                " [metadata:ead:acquisitionInfo] VARCHAR(500), " +
                " [metadata:ead:accruals] VARCHAR(500), " +
                " [metadata:ead:custodialHistory] VARCHAR(500), " +
                " [metadata:ead:processInfoDate] DATETIME, " +
                " [metadata:ead:processInfoArchivistNotes] VARCHAR(500), " +
                " [metadata:ead:originalsLocation] VARCHAR(500), " +
                " [metadata:ead:alternativeFormAvailable] VARCHAR(500), " +
                " [metadata:ead:relatedMaterial] VARCHAR(500), " +
                " [metadata:ead:accessRestrictions] VARCHAR(500), " +
                " [metadata:ead:useRestrictions] VARCHAR(500), " +
                " [metadata:ead:otherFindAid] VARCHAR(500), " +
                " [metadata:ead:physicalTech] VARCHAR(500), " +
                " [metadata:ead:bibliography] VARCHAR(500), " +
                " [metadata:ead:preferCite] VARCHAR(500), " +
                " [metadata:dublinCore:title] VARCHAR(500), " +
                " [metadata:dublinCore:identifier] VARCHAR(500), " +
                " [metadata:dublinCore:creator] VARCHAR(500), " +
                " [metadata:dublinCore:initialDate] DATETIME, " +
                " [metadata:dublinCore:finalDate] DATETIME, " +
                " [metadata:dublinCore:description] VARCHAR(500), " +
                " [metadata:dublinCore:publisher] VARCHAR(500), " +
                " [metadata:dublinCore:contributor] VARCHAR(500), " +
                " [metadata:dublinCore:rights] VARCHAR(500), " +
                " [metadata:dublinCore:language] VARCHAR(500), " +
                " [metadata:dublinCore:coverage] VARCHAR(500), " +
                " [metadata:dublinCore:format] VARCHAR(500), " +
                " [metadata:dublinCore:relation] VARCHAR(500), " +
                " [metadata:dublinCore:subject] VARCHAR(500), " +
                " [metadata:dublinCore:type] VARCHAR(500), " +
                " [metadata:dublinCore:source] VARCHAR(500), " +
                " [metadata:keyValue:id] VARCHAR(500), " +
                " [metadata:keyValue:title] VARCHAR(500), " +
                " [metadata:keyValue:producer] VARCHAR(500), " +
                " [metadata:keyValue:date] DATETIME" +
                ")";
            try {
                stmt.executeUpdate(sql);
            } catch (SQLiteException e) { /* table already exists, fail silently */ }

            stmt.close();
            this.connection.close();

        } catch (SQLException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Function responsible for returning a full list of metadata fields.
     * @return The list of metadata fields.
     */
    private ArrayList<String> getMetadataFields() {
        ArrayList<String> metadataFields = new ArrayList<>();

        //CMIS base fields
        metadataFields.add(PropertyIds.OBJECT_ID);
        metadataFields.add(PropertyIds.NAME);
        metadataFields.add(PropertyIds.CREATED_BY);
        metadataFields.add(PropertyIds.LAST_MODIFIED_BY);
        metadataFields.add(PropertyIds.CREATION_DATE);
        metadataFields.add(PropertyIds.LAST_MODIFICATION_DATE);
        metadataFields.add(PropertyIds.CHANGE_TOKEN);
        metadataFields.add(PropertyIds.DESCRIPTION);
        metadataFields.add(PropertyIds.SECONDARY_OBJECT_TYPE_IDS);
        metadataFields.add(PropertyIds.BASE_TYPE_ID);
        metadataFields.add(PropertyIds.OBJECT_TYPE_ID);
        metadataFields.add(PropertyIds.PATH);
        metadataFields.add(PropertyIds.PARENT_ID);
        metadataFields.add(PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS);
        metadataFields.add(PropertyIds.IS_IMMUTABLE);
        metadataFields.add(PropertyIds.IS_LATEST_VERSION);
        metadataFields.add(PropertyIds.IS_MAJOR_VERSION);
        metadataFields.add(PropertyIds.IS_LATEST_MAJOR_VERSION);
        metadataFields.add(PropertyIds.VERSION_LABEL);
        metadataFields.add(PropertyIds.VERSION_SERIES_ID);
        metadataFields.add(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT);
        metadataFields.add(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY);
        metadataFields.add(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID);
        metadataFields.add(PropertyIds.CHECKIN_COMMENT);
        metadataFields.add(PropertyIds.IS_PRIVATE_WORKING_COPY);
        metadataFields.add(PropertyIds.CONTENT_STREAM_LENGTH);
        metadataFields.add(PropertyIds.CONTENT_STREAM_MIME_TYPE);
        metadataFields.add(PropertyIds.CONTENT_STREAM_FILE_NAME);
        metadataFields.add(PropertyIds.CONTENT_STREAM_ID);

        //EAD 2002 metadata fields
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_UNIT_ID.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_UNIT_TITLE.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_COUNTRY_CODE.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_REPOSITORY_CODE.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_UNIT_DATE.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_UNIT_DATE_LABEL.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_UNIT_DATE_NORMAL.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_EXTENT.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_DIMENSIONS.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_APPEARANCE.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_REPOSITORY_NAME.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_LANG_MATERIAL.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_LANG_MATERIAL_LANGUAGE.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_NOTE_SOURCE_DESCRIPTION.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_NOTE_GENERAL_NOTE.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_ORIGINATION.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_ORIGINATION_CREATION.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_ORIGINATION_PRODUCTION.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_ARCHIVE_DESCRIPTION.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_MATERIAL_SPECIFICATION.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_ODD_LEVEL_OF_DETAIL.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_ODD_STATUS_DESCRIPTION.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_SCOPE_CONTENT.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_ARRANGEMENT.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_APPRAISAL.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_ACQUISITION_INFO.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_ACCRUALS.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_CUSTODIAL_HISTORY.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_PROCESS_INFO_DATE.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_PROCESS_INFO_ARCHIVIST_NOTES.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_ORIGINALS_LOCATION.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_ALTERNATIVE_FORM_AVAILABLE.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_RELATED_MATERIAL.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_ACCESS_RESTRICTIONS.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_USE_RESTRICTIONS.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_OTHER_FIND_AID.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_PHYSICAL_TECH.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_BIBLIOGRAPHY.value());
        metadataFields.add(MetadataEadFieldId.METADATA_EAD_PREFER_CITE.value());

        //Dublin Core 2002 12 12 metadata fields
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_TITLE.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_IDENTIFIER.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_CREATOR.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_INITIAL_DATE.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_FINAL_DATE.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_DESCRIPTION.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_PUBLISHER.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_CONTRIBUTOR.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_RIGHTS.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_LANGUAGE.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_COVERAGE.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_FORMAT.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_RELATION.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_SUBJECT.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_TYPE.value());
        metadataFields.add(MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_SOURCE.value());

        //Key Value metadata fields
        metadataFields.add(MetadataKeyValueFieldId.METADATA_KEY_VALUE_ID.value());
        metadataFields.add(MetadataKeyValueFieldId.METADATA_KEY_VALUE_TITLE.value());
        metadataFields.add(MetadataKeyValueFieldId.METADATA_KEY_VALUE_PRODUCER.value());
        metadataFields.add(MetadataKeyValueFieldId.METADATA_KEY_VALUE_DATE.value());

        return metadataFields;
    }

    /**
     * Method responsible for inserting a new object into the correct table given its objectId.
     * @param table The table name.
     * @param objectId The new object's objectId.
     */
    public void createObject(String table, String objectId) {
        if (table == null) { System.err.println("Missing parameter 'table' for the Database.createObject method."); return; }
        if (objectId == null) { System.err.println("Missing parameter 'objectId' for the Database.createObject method."); return; }

        Statement stmt;
        try {
            this.connect();
            stmt = this.connection.createStatement();

            //Insert the new record
            String sql = "INSERT INTO [" + table + "] ([cmis:objectId])" +
                    " VALUES ('" + objectId + "') ";
            try { stmt.executeUpdate(sql); }
            catch (SQLiteException e) { /* fail silently */ }

            stmt.close();
            this.connection.close();

        } catch (SQLException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Method responsible for updating a metadata field value in a table in the database.
     * @param table The table name.
     * @param objectId The objectId of the object being updated.
     * @param fieldName the field name of the field being updated.
     * @param value The new value for the field.
     */
    public void updateField(String table, String objectId, String fieldName, String value) {
        if (table == null) { System.err.println("Missing parameter 'table' for the Database.updateField method."); return; }
        if (objectId == null) { System.err.println("Missing parameter 'objectId' for the Database.updateField method."); return; }
        if (fieldName == null) { System.err.println("Missing parameter 'fieldName' for the Database.updateField method."); return; }

        Statement stmt;
        try {
            this.connect();
            stmt = this.connection.createStatement();

            //Update table
            String fieldValue = "NULL";
            if (value != null) { fieldValue = " '" + value + "' "; }
            String sql = "UPDATE [" + table + "] " +
                        " SET [" + fieldName+ "] = " + fieldValue +
                        " WHERE [cmis:objectId] LIKE '" + objectId + "' ";
            try {
                stmt.executeUpdate(sql);
            } catch (SQLiteException e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
            }

            stmt.close();
            this.connection.close();

        } catch (SQLException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Function responsible for running a query against the objects database and retrieving a list of matching results.
     * @param statement The SQL statement.
     * @return The list of matching results.
     */
    public List<String> query(String statement) {
        if (statement == null) { System.err.println("Missing parameter 'statement' for the Database.query method."); return null; }

        queryParser = new Query(statement);

        List<String> objects = new ArrayList<>();

        // query preparation

        // IN_FOLDER
        if (queryParser.getQueryType().equals("IN_FOLDER")) {
            String subQuery = "(cmis:path LIKE (SELECT cmis:path FROM cmis:folder WHERE cmis:objectId = '" + queryParser.getFolderId() + "') || '/' || [cmis:name])";
            statement = statement.replaceAll("(?i)IN_FOLDER\\('" + queryParser.getFolderId() + "'\\)", subQuery);
        }

        // IN_TREE
        if (queryParser.getQueryType().equals("IN_TREE")) {
            String subQuery = "(cmis:path LIKE (SELECT cmis:path FROM cmis:folder WHERE cmis:objectId = '" + queryParser.getFolderId() + "') || '%')";
            statement = statement.replaceAll("(?i)IN_TREE\\('" + queryParser.getFolderId() + "'\\)", subQuery);
        }

        // Prepare query for execution
        for (String metadataField : getMetadataFields()) {
            statement = statement.replaceAll("(?i)\\[" + metadataField + "\\]", metadataField);
            statement = statement.replaceAll("(?i)" + metadataField, "[" + metadataField + "]");
        }
        statement = statement.replaceAll("(?i)\\[cmis:folder\\]", "cmis:folder").replaceAll("(?i)cmis:folder", "[cmis:folder]");
        statement = statement.replaceAll("(?i)\\[cmis:document\\]", "cmis:document").replaceAll("(?i)cmis:document", "[cmis:rodaDocument]");
        statement = statement.replaceAll("(?i)\\[cmis:rodaDocument\\]", "cmis:rodaDocument").replaceAll("(?i)cmis:rodaDocument", "[cmis:rodaDocument]");
        statement = statement.replaceAll("(?i)FROM", ", [cmis:path] FROM");
        statement = statement.replaceAll("(?i)\\[cmis:path\\] \\, \\[cmis:path\\]", "[cmis:path]");

        Statement stmt;
        try {
            this.connect();
            stmt = this.connection.createStatement();

            //execute query
            try {
                stmt.executeUpdate(statement);
                ResultSet rs = stmt.executeQuery(statement);
                while ( rs.next() ) {
                    String cmisPath = rs.getString("cmis:path");
                    objects.add(cmisPath);
                }
                rs.close();
            } catch (SQLiteException e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
            }

            stmt.close();
            this.connection.close();

        } catch (SQLException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }

        return objects;
    }

    /**
     * Function responsible for returning the query parse with the last executed query.
     * @return The FileBridgeQuery query parser.
     */
    public Query getLastQuery() { return queryParser; }

}
