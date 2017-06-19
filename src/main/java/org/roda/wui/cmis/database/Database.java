package org.roda.wui.cmis.database;

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

        // SELECT fields
        if (!queryParser.searchAllFields()) {
            String fieldsClause = "";
            for (String field : queryParser.getFieldsArray()) {
                fieldsClause += "[" + field + "], ";
            }
            statement = statement.replaceAll("(?i)SELECT " + queryParser.getFieldsClause(), "SELECT " + fieldsClause + "[cmis:path]");
        }

        // FROM table
        statement = statement.replace("[cmis:folder]", "cmis:folder").replace("cmis:folder", "[cmis:folder]");
        statement = statement.replace("[cmis:document]", "cmis:document").replace("cmis:document", "[cmis:rodaDocument]");
        statement = statement.replace("[cmis:rodaDocument]", "cmis:rodaDocument").replace("cmis:rodaDocument", "[cmis:rodaDocument]");

        // WHERE fields
        if (queryParser.getQueryType().equals("WHERE")) {
            String whereClause = queryParser.getWhereClause();
            String[] whereTokens = whereClause.split(" ");
            String resultWhereClause = "";
            for (String token : whereTokens) {
                if (token.contains("cmis:") || token.contains("metadata:")) {
                    resultWhereClause += "[" + token + "] ";
                } else {
                    resultWhereClause += token + " ";
                }
            }
            statement = statement.replaceAll("(?i)WHERE " + whereClause, "WHERE " + resultWhereClause);
            statement = statement.replaceAll("(?i)\\[NOT\\(", "NOT([");
            statement = statement.replaceAll("(?i)\\)]", "])");
        }

        // IN_FOLDER
        if (queryParser.getQueryType().equals("IN_FOLDER")) {

        }

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
