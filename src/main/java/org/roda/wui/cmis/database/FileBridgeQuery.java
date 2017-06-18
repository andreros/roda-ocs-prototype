package org.roda.wui.cmis.database;

import org.apache.chemistry.opencmis.commons.PropertyIds;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class responsible for holding a query.
 */
public class FileBridgeQuery {

    //Query statement
    private String statement = "";
    //Query type
    private String queryType = null;
    //Simple query pattern
    private Pattern SIMPLE_QUERY_PATTERN = Pattern.compile("(?i)select\\s+(.+)from\\s+(\\S*)");
    //Simple query pattern
    private Pattern WHERE_QUERY_PATTERN = Pattern.compile("(?i)select\\s+(.+)from\\s+(\\S*)\\s+where\\s(.+)");
    //In folder query pattern
    private Pattern IN_FOLDER_QUERY_PATTERN = Pattern.compile("(?i)select\\s+(.+)from\\s+(\\S*).*\\s+where\\s+in_folder\\('(.*)'\\)");

    private String fieldsClause = null;

    private String typeId = null;

    private String whereClause = null;

    private List<WhereCondition> whereAndConditions = new ArrayList<>();

    private List<WhereCondition> whereOrConditions = new ArrayList<>();

    private String folderId = null;

    /**
     * Contructor.
     * @param statement The query's statement.
     */
    public FileBridgeQuery(String statement) {
        this.statement = statement;
        this.parse(this.statement);
    }

    /**
     * Method responsible for parsing a query given its statement.
     * @param statement The query's statement.
     */
    public void parse(String statement) {
        //reset the variables
        this.statement = statement;
        this.fieldsClause = null;
        this.typeId = null;
        this.whereClause = null;
        this.folderId = null;

        //Test for "SIMPLE" query
        Matcher simpleMatcher = SIMPLE_QUERY_PATTERN.matcher(statement.trim());
        if (simpleMatcher.matches()) {
            this.queryType = "SIMPLE";
            this.fieldsClause = simpleMatcher.group(1).trim();
            this.typeId = simpleMatcher.group(2).trim();
        }

        //Test for "WHERE" query
        Matcher whereMatcher = WHERE_QUERY_PATTERN.matcher(statement.trim());
        if (whereMatcher.matches()) {
            this.queryType = "WHERE";
            this.fieldsClause = whereMatcher.group(1).trim();
            this.typeId = whereMatcher.group(2).trim();
            this.whereClause = whereMatcher.group(3).trim();

            String[] conditions = this.whereClause.split(" ");

            WhereCondition whereCondition = new WhereCondition();
            for (int i = 0; i < conditions.length; i++) {
                if (!conditions[i].equals("AND") && !conditions[i].equals("OR")) {
                    //field detection
                    if (conditions[i].contains(":")) { whereCondition.setField(conditions[i]); }
                    //operator detection
                    if (conditions[i].toLowerCase().equals("like")) { whereCondition.setOperator(conditions[i]); }
                    if (conditions[i].toLowerCase().equals("is")) { whereCondition.setOperator(conditions[i]); }
                    if (conditions[i].toLowerCase().equals("is not")) { whereCondition.setOperator(conditions[i]); }
                    if (conditions[i].equals("=")) { whereCondition.setOperator(conditions[i]); }
                    if (conditions[i].equals("<>")) { whereCondition.setOperator(conditions[i]); }
                    //value detection
                    if (conditions[i].contains("\"")) { whereCondition.setValue(conditions[i].replace("\"", "")); }
                    if (conditions[i].contains("'")) { whereCondition.setValue(conditions[i].replace("'", "")); }
                    //insert condition to conditions list
                    if (whereCondition.isValid()) {
                        this.whereAndConditions.add(whereCondition);
                        whereCondition = new WhereCondition();
                    }
                } else {
                    //TODO: For now all conditions go to the AND list. Split AND from OR conditions.
                }
            }

        }

        //Test for "IN_FOLDER" query
        Matcher inFolderMatcher = IN_FOLDER_QUERY_PATTERN.matcher(statement.trim());
        if (inFolderMatcher.matches()) {
            this.queryType = "IN_FOLDER";
            this.fieldsClause = inFolderMatcher.group(1).trim();
            this.typeId = inFolderMatcher.group(2).trim();
            this.folderId = inFolderMatcher.group(3).trim();
        }

    }

    /**
     * Function responsible for checking if the search is to all fields.
     * @return True if it is a search to all fields. False otherwise.
     */
    public boolean searchAllFields() {
        return this.fieldsClause.trim().equals("*");
    }

    /**
     * Function responsible for checking if a field / value pair matches the ones stated in the condition.
     * @param field The field name in the statement.
     * @param value The value in the statement.
     * @return True if the field / value pair matches. False otherwise.
     */
    public boolean isWhereMatch(String field, Object value) {
        if (field == null || value == null) {
            return false;
        }
        boolean isMatch = false;
        if (!this.whereAndConditions.isEmpty()) {
            //there are fields in the where condition, evaluate
            for (WhereCondition whereCondition : this.whereAndConditions) {
                if (whereCondition.getField().equals(field) &&
                        (whereCondition.getOperator().equals("LIKE") || whereCondition.getOperator().equals("=")) &&
                        whereCondition.getValue().equals(value)) {
                    isMatch = true;
                }
            }
        } else {
            //there are no fields in the where condition, pass
            isMatch = true;
        }
        return isMatch;
    }

    /**
     * Function responsible for checking if there are Where conditions to be checked or not.
     * @return True if there are Where conditions. False otherwise.
     */
    public boolean hasWhereConditions() {
        return (!this.whereAndConditions.isEmpty() || !this.whereOrConditions.isEmpty());
    }

    // --- getters and setters ---

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
        this.parse(this.statement);
    }

    public String getQueryType() {
        return queryType;
    }

    public String getFieldsClause() {
        return fieldsClause;
    }

    public String[] getFieldsArray() {
        String fields = "";
        if (fieldsClause != null) { fields = fieldsClause; }
        //always include these fields to facilitate results visualization
        if (!fields.contains(PropertyIds.OBJECT_ID)) fields = PropertyIds.OBJECT_ID + "," + fields;
        if (!fields.contains(PropertyIds.NAME)) fields = PropertyIds.NAME + "," + fields;
        return fields.replace(" ", "").split(",");
    }

    public ArrayList<String> getFieldsArrayList() {
        String[] fields = getFieldsArray();
        ArrayList fieldsArrayList = new ArrayList();
        for (String field : fields) {
            if (!fieldsArrayList.contains(field)) {
                fieldsArrayList.add(field);
            }
        }
        return fieldsArrayList;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getWhereClause() { return whereClause; }

    public String getFolderId() {
        return folderId;
    }

}
