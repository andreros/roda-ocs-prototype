package org.roda.wui.cmis.metadata;

/**
 * Dublin Core 2002-12-12 Simple Metadata POJO CLass.
 */
public class DublinCore20021212Metadata {

    private String title = "";
    private String identifier = "";
    private String creator = "";
    private String initialDate = "";
    private String finalDate = "";
    private String description = "";
    private String publisher = "";
    private String contributor = "";
    private String rights = "";
    private String language = "";
    private String coverage = "";
    private String format = "";
    private String relation = "";
    private String subject = "";
    private String type = "";
    private String source = "";

    /**
     * Constructor.
     */
    public DublinCore20021212Metadata() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getInitialDate() {
        return initialDate;
    }

    public void setInitialDate(String initialDate) {
        this.initialDate = initialDate;
    }

    public String getFinalDate() {
        return finalDate;
    }

    public void setFinalDate(String finalDate) {
        this.finalDate = finalDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getContributor() {
        return contributor;
    }

    public void setContributor(String contributor) {
        this.contributor = contributor;
    }

    public String getRights() {
        return rights;
    }

    public void setRights(String rights) {
        this.rights = rights;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCoverage() {
        return coverage;
    }

    public void setCoverage(String coverage) {
        this.coverage = coverage;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String toString() {
        return "Dublin Core 2002-12-12 Simple Metadata " + System.getProperty("line.separator") +
                "Title: " + this.getTitle() + System.getProperty("line.separator") +
                "Identifier: " + this.getIdentifier() + System.getProperty("line.separator") +
                "Creator: " + this.getCreator() + System.getProperty("line.separator") +
                "Initial Date: " + this.getInitialDate() + System.getProperty("line.separator") +
                "Final Date: " + this.getFinalDate() + System.getProperty("line.separator") +
                "Description: " + this.getDescription() + System.getProperty("line.separator") +
                "Publisher: " + this.getPublisher() + System.getProperty("line.separator") +
                "Contributor: " + this.getContributor() + System.getProperty("line.separator") +
                "Language: " + this.getLanguage() + System.getProperty("line.separator") +
                "Coverage: " + this.getCoverage() + System.getProperty("line.separator") +
                "Format: " + this.getFormat() + System.getProperty("line.separator") +
                "Relation: " + this.getRelation() + System.getProperty("line.separator") +
                "Subject: " + this.getSubject() + System.getProperty("line.separator") +
                "Type: " + this.getType() + System.getProperty("line.separator") +
                "Source: " + this.getSource();
    }

}
