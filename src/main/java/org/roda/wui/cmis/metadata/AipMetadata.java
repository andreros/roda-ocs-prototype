package org.roda.wui.cmis.metadata;

/**
 * Class responsible for holding a file's metadata.
 */
public class AipMetadata {

    private String id = null;
    private Ead2002Metadata ead2002Metadata = null;
    private DublinCore20021212Metadata dublinCore20021212Metadata = null;
    private KeyValueMetadata keyValueMetadata = null;

    /**
     * Constructor.
     */
    public AipMetadata(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Ead2002Metadata getEad2002Metadata() {
        return ead2002Metadata;
    }

    public void setEad2002Metadata(Ead2002Metadata ead2002Metadata) {
        this.ead2002Metadata = ead2002Metadata;
    }

    public DublinCore20021212Metadata getDublinCore20021212Metadata() {
        return dublinCore20021212Metadata;
    }

    public void setDublinCore20021212Metadata(DublinCore20021212Metadata dublinCore20021212Metadata) {
        this.dublinCore20021212Metadata = dublinCore20021212Metadata;
    }

    public KeyValueMetadata getKeyValueMetadata() {
        return keyValueMetadata;
    }

    public void setKeyValueMetadata(KeyValueMetadata keyValueMetadata) {
        this.keyValueMetadata = keyValueMetadata;
    }

}
