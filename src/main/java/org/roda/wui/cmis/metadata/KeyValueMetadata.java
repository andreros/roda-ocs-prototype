package org.roda.wui.cmis.metadata;

/**
 * Key-Value Metadata POJO CLass.
 */
public class KeyValueMetadata {

    private String id = "";
    private String title = "";
    private String producer = "";
    private String date = "";

    /**
     * Constructor.
     */
    public KeyValueMetadata() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String toString() {
        return "Key-Value Metadata " + System.getProperty("line.separator") +
                "Id: " + this.getId() + System.getProperty("line.separator") +
                "Title: " + this.getTitle() + System.getProperty("line.separator") +
                "Producer: " + this.getProducer() + System.getProperty("line.separator") +
                "Date: " + this.getDate();
    }

}
