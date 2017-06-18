package org.roda.wui.cmis.database;

/**
 * POJO class for representing a condition in a WHERE clause.
 */
public class WhereCondition {

    private String field = null;
    private String operator = null;
    private String value = null;

    /**
     * Constructor.
     */
    public WhereCondition() {}

    /**
     * Function responsible for checking is valid. A condition is valid if all
     * the properties (field, operator and value) are filled.
     * @return True if the condition is valid. False otherwise.
     */
    public boolean isValid() {
        return ((this.field != null) && (this.operator != null) && (this.value != null));
    }

    // --- getters and setters ---

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
