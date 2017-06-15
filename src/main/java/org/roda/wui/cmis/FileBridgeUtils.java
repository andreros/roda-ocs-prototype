package org.roda.wui.cmis;

import org.roda.wui.cmis.metadata.Ead2002Metadata;
import org.roda.wui.cmis.metadata.DublinCore20021212Metadata;
import org.roda.wui.cmis.metadata.KeyValueMetadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.*;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public final class FileBridgeUtils {

    private FileBridgeUtils() {
    }

    /**
     * Returns the boolean value of the given value or the default value if the
     * given value is <code>null</code>.
     */
    public static boolean getBooleanParameter(Boolean value, boolean def) {
        if (value == null) {
            return def;
        }

        return value.booleanValue();
    }

    /**
     * Converts milliseconds into a {@link GregorianCalendar} object, setting
     * the timezone to GMT and cutting milliseconds off.
     */
    public static GregorianCalendar millisToCalendar(long millis) {
        GregorianCalendar result = new GregorianCalendar();
        result.setTimeZone(TimeZone.getTimeZone("GMT"));
        result.setTimeInMillis((long) (Math.ceil((double) millis / 1000) * 1000));

        return result;
    }

    /**
     * Splits a filter statement into a collection of properties. If
     * <code>filter</code> is <code>null</code>, empty or one of the properties
     * is '*' , an empty collection will be returned.
     */
    public static Set<String> splitFilter(String filter) {
        if (filter == null) {
            return null;
        }

        if (filter.trim().length() == 0) {
            return null;
        }

        Set<String> result = new HashSet<String>();
        for (String s : filter.split(",")) {
            s = s.trim();
            if (s.equals("*")) {
                return null;
            } else if (s.length() > 0) {
                result.add(s);
            }
        }

        // set a few base properties
        // query name == id (for base type properties)
        result.add(PropertyIds.OBJECT_ID);
        result.add(PropertyIds.OBJECT_TYPE_ID);
        result.add(PropertyIds.BASE_TYPE_ID);

        return result;
    }

    /**
     * Gets the type id from a set of properties.
     */
    public static String getObjectTypeId(Properties properties) {
        PropertyData<?> typeProperty = properties.getProperties().get(PropertyIds.OBJECT_TYPE_ID);
        if (!(typeProperty instanceof PropertyId)) {
            throw new CmisInvalidArgumentException("Type Id must be set!");
        }

        String typeId = ((PropertyId) typeProperty).getFirstValue();
        if (typeId == null) {
            throw new CmisInvalidArgumentException("Type Id must be set!");
        }

        return typeId;
    }

    /**
     * Returns the first value of an id property.
     */
    public static String getIdProperty(Properties properties, String name) {
        PropertyData<?> property = properties.getProperties().get(name);
        if (!(property instanceof PropertyId)) {
            return null;
        }

        return ((PropertyId) property).getFirstValue();
    }

    /**
     * Returns the first value of a string property.
     */
    public static String getStringProperty(Properties properties, String name) {
        PropertyData<?> property = properties.getProperties().get(name);
        if (!(property instanceof PropertyString)) {
            return null;
        }

        return ((PropertyString) property).getFirstValue();
    }

    /**
     * Returns the first value of a datetime property.
     */
    public static GregorianCalendar getDateTimeProperty(Properties properties, String name) {
        PropertyData<?> property = properties.getProperties().get(name);
        if (!(property instanceof PropertyDateTime)) {
            return null;
        }

        return ((PropertyDateTime) property).getFirstValue();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // --- André Rosa Custom Implementations :: BEGIN ------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------

    /******************************************************************************************************************/
    /** AIP Methods                                                                                                   */
    /******************************************************************************************************************/

    /**
     * Method responsible for checking if a given AIP has CMIS reading permissions.
     * @param aipFilePath The path to the "aip.json" file.
     * @return True if the AIP has CMIS read permissions, false otherwise.
     */
    public static boolean canReadAIP(String aipFilePath) {
        try {
            String json = new String(Files.readAllBytes(Paths.get(aipFilePath)));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            // check for reading permissions on this aip files
            if ((node.get("permissions") != null) && (node.get("permissions").get("groups") != null) &&
                    (node.get("permissions").get("groups").get("READ") != null) &&
                    node.get("permissions").get("groups").get("READ").isArray()) {

                for (JsonNode groupPermission : node.get("permissions").get("groups").get("READ")) {
                    if (groupPermission.asText().equals("cmis")) { return true; }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Method responsible for loading EAD 2002 metadata from AIP if there is any present.
     * @param aipFilePath The aip.json file path.
     * @return The EAD 2002 metadata object.
     */
    public static Ead2002Metadata getEAD2002Metadata(String aipFilePath) {
        Ead2002Metadata ead2002Metadata = new Ead2002Metadata();
        try {
            String json = new String(Files.readAllBytes(Paths.get(aipFilePath)));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            // check for descriptive metadata files
            if ((node.get("descriptiveMetadata") != null) && node.get("descriptiveMetadata").isArray()) {
                for (JsonNode metadataFiles : node.get("descriptiveMetadata")) {
                    if (metadataFiles.get("id") != null) {
                        String metadataFilename = metadataFiles.get("id").asText();
                        String metadataFilePath = aipFilePath.replace("aip.json", "") + "metadata/descriptive/" + metadataFilename;

                        //Parse EAD 2002 Files
                        if (metadataFilename.equals("ead2002.xml") || metadataFilename.equals("ead_2002.xml")) {
                            try {
                                File ead2002File = new File(metadataFilePath);
                                ead2002Metadata = FileBridgeUtils.parseEAD2002(ead2002File);
                            } catch (NullPointerException e) {
                                System.err.println("Error reading XML EAD 2002 file '" + metadataFilePath + "'");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ead2002Metadata;
    }

    /**
     * Method responsible for loading Dublin Core 2002-12-12 Simple metadata from AIP if there is any present.
     * @param aipFilePath The aip.json file path.
     * @return The Dublin Core 2002-12-12 Simple metadata object.
     */
    public static DublinCore20021212Metadata getDublinCore20021212Metadata(String aipFilePath) {
        DublinCore20021212Metadata dublinCore20021212Metadata = new DublinCore20021212Metadata();
        try {
            String json = new String(Files.readAllBytes(Paths.get(aipFilePath)));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            // check for descriptive metadata files
            if ((node.get("descriptiveMetadata") != null) && node.get("descriptiveMetadata").isArray()) {
                for (JsonNode metadataFiles : node.get("descriptiveMetadata")) {
                    if (metadataFiles.get("id") != null) {
                        String metadataFilename = metadataFiles.get("id").asText();
                        String metadataFilePath = aipFilePath.replace("aip.json", "") + "metadata/descriptive/" + metadataFilename;

                        //Parse Dublin Core Simple 2002-12-12 File
                        if (metadataFilename.equals("dc_SimpleDC20021212.xml")) {
                            try {
                                File dcSimpleDC20021212File = new File(metadataFilePath);
                                dublinCore20021212Metadata = FileBridgeUtils.parseDublinCoreSimple20021212(dcSimpleDC20021212File);
                            } catch (NullPointerException e) {
                                System.out.println("Error reading XML Dublin Core Simple 2002-12-12 file '" + metadataFilePath + "'");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dublinCore20021212Metadata;
    }

    /**
     * Method responsible for loading Key-Value metadata from AIP if there is any present.
     * @param aipFilePath The aip.json file path.
     * @return The Key-Value metadata object.
     */
    public static KeyValueMetadata getKeyValueMetadata(String aipFilePath) {
        KeyValueMetadata keyValueMetadata = new KeyValueMetadata();
        try {
            String json = new String(Files.readAllBytes(Paths.get(aipFilePath)));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            // check for descriptive metadata files
            if ((node.get("descriptiveMetadata") != null) && node.get("descriptiveMetadata").isArray()) {
                for (JsonNode metadataFiles : node.get("descriptiveMetadata")) {
                    if (metadataFiles.get("id") != null) {
                        String metadataFilename = metadataFiles.get("id").asText();
                        String metadataFilePath = aipFilePath.replace("aip.json", "") + "metadata/descriptive/" + metadataFilename;

                        //Parse Key-value File
                        if (metadataFilename.equals("key-value.xml") || metadataFilename.equals("metadata.xml")) {
                            try {
                                File keyValueFile = new File(metadataFilePath);
                                keyValueMetadata = FileBridgeUtils.parseKeyValue(keyValueFile);
                            } catch (NullPointerException e) {
                                System.out.println("Error reading XML Key-value file '" + metadataFilePath + "'");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return keyValueMetadata;
    }

    /******************************************************************************************************************/
    /** Metadata Parsers                                                                                              */
    /******************************************************************************************************************/

    /**
     * Method responsible for parsing an XML metadata file in the EAD2002 format.
     * @param file The filename of the file to be parsed.
     */
    public static Ead2002Metadata parseEAD2002(File file) {

        Ead2002Metadata ead2002Metadata = new Ead2002Metadata();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            String rootElement = doc.getDocumentElement().getNodeName();
            //System.out.println("---------------------------------------------------------------------------------------");
            //System.out.println("EAD2002 Parser");
            //System.out.println("---------------------------------------------------------------------------------------");
            //System.out.println(rootElement);

            // *********************************************************************************************************
            //archdesc element
            Node archdescNode = doc.getElementsByTagName("archdesc").item(0);
            //System.out.println(".." + archdescNode.getNodeName());
            for (int i = 0; i < archdescNode.getAttributes().getLength(); i++) {
                Node archdescAttributeNode = archdescNode.getAttributes().item(i);

                if (archdescAttributeNode.getNodeName().equals("level")) {
                    ead2002Metadata.setArchiveDescription(archdescAttributeNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println(".." + archdescNode.getNodeName() + " level: " + ead2002Metadata.getArchiveDescription());
                }
            }

            // *********************************************************************************************************
            //did element
            Node didNode = archdescNode.getChildNodes().item(1);
            //System.out.println(".." + didNode.getNodeName());

            //did child nodes
            NodeList didNodeChildren = didNode.getChildNodes();

            for (int i = 0; i < didNodeChildren.getLength(); i++) {
                Node didNodeChild = didNodeChildren.item(i);
                String didNodeName = didNodeChild.getNodeName();

                if (didNodeName.equals("unittitle")) {
                    ead2002Metadata.setUnitTitle(didNodeChild.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("....unittitle: " + ead2002Metadata.getUnitTitle());
                }

                if (didNodeName.equals("unitid")) {
                    ead2002Metadata.setUnitId(didNodeChild.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("....unitid: " + ead2002Metadata.getUnitId());

                    for (int j = 0; j < didNodeChild.getAttributes().getLength(); j++) {
                        Node unitidNode = didNodeChild.getAttributes().item(j);
                        String unitidNodeName = unitidNode.getNodeName();

                        if (unitidNodeName.equals("countrycode")) {
                            ead2002Metadata.setCountryCode(unitidNode.getChildNodes().item(0)
                                    .getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("....unitid countrycode: " + ead2002Metadata.getCountryCode());
                        }

                        if (unitidNodeName.equals("repositorycode")) {
                            ead2002Metadata.setRepositoryCode(unitidNode.getChildNodes().item(0)
                                    .getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("....unitid repositorycode: " + ead2002Metadata.getRepositoryCode());
                        }
                    }
                }

                if (didNodeName.equals("unitdate")) {
                    ead2002Metadata.setUnitDate(didNodeChild.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("....unitdate: " + ead2002Metadata.getUnitDate());

                    for (int j = 0; j < didNodeChild.getAttributes().getLength(); j++) {
                        Node unitdateNode = didNodeChild.getAttributes().item(j);
                        String unitdateNodeName = unitdateNode.getNodeName();

                        if (unitdateNodeName.equals("label")) {
                            ead2002Metadata.setUnitDateLabel(unitdateNode.getChildNodes().item(0)
                                    .getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("....unitdate label: " + ead2002Metadata.getUnitDateLabel());
                        }

                        if (unitdateNodeName.equals("normal")) {
                            ead2002Metadata.setUnitDateNormal(unitdateNode.getChildNodes().item(0)
                                    .getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("....unitdate normal: " + ead2002Metadata.getUnitDateNormal());
                        }
                    }
                }

                if (didNodeName.equals("physdesc")) {
                    for (int j = 0; j < didNodeChild.getChildNodes().getLength(); j++) {
                        Node physdescNode = didNodeChild.getChildNodes().item(j);
                        String physdescNodeName = physdescNode.getNodeName();

                        if (physdescNodeName.equals("extent")) {
                            ead2002Metadata.setPhysicalDescriptionExtent(physdescNode.getChildNodes().item(0)
                                    .getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("....physdesc extent: " + ead2002Metadata.getPhysicalDescriptionExtent());
                        }

                        if (physdescNodeName.equals("dimensions")) {
                            ead2002Metadata.setPhysicalDescriptionDimensions(physdescNode.getChildNodes().item(0)
                                    .getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("....physdesc dimensions: " + ead2002Metadata.getPhysicalDescriptionDimensions());
                        }

                        if (physdescNodeName.equals("physfacet")) {
                            ead2002Metadata.setPhysicalDescriptionAppearance(physdescNode.getChildNodes().item(0)
                                    .getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("....physdesc physfacet: " + ead2002Metadata.getPhysicalDescriptionAppearance());
                        }

                        if (physdescNodeName.equals("#text")) {
                            String physdesc = physdescNode.getNodeValue().replaceAll("\\n", "").trim();
                            if (physdesc.length() > 0) {
                                ead2002Metadata.setPhysicalDescription(physdesc);
                                //System.out.println("....physdesc: " + ead2002Metadata.getPhysicalDescription());
                            }
                        }
                    }
                }

                if (didNodeName.equals("repository")) {
                    for (int j = 0; j < didNodeChild.getChildNodes().getLength(); j++) {
                        Node repositoryNode = didNodeChild.getChildNodes().item(j);
                        String repositoryNodeName = repositoryNode.getNodeName();

                        if (repositoryNodeName.equals("corpname")) {
                            ead2002Metadata.setRepositoryName(repositoryNode.getChildNodes().item(0)
                                    .getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("....repository corpname: " + ead2002Metadata.getRepositoryName());
                        }
                    }
                }

                if (didNodeName.equals("langmaterial")) {
                    for (int j = 0; j < didNodeChild.getChildNodes().getLength(); j++) {
                        Node langmaterialNode = didNodeChild.getChildNodes().item(j);
                        String langmaterialNodeName = langmaterialNode.getNodeName();

                        if (langmaterialNodeName.equals("language")) {
                            ead2002Metadata.setLangMaterialLanguage(langmaterialNode.getChildNodes().item(0)
                                    .getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("....langmaterial language: " + ead2002Metadata.getLangMaterialLanguage());
                        }

                        if (langmaterialNodeName.equals("#text")) {
                            String langmaterial = langmaterialNode.getNodeValue().replaceAll("\\n", "").trim();
                            if (langmaterial.length() > 0) {
                                ead2002Metadata.setLangMaterial(langmaterial);
                                //System.out.println("....langmaterial: " + ead2002Metadata.getLangMaterial());
                            }
                        }
                    }
                }
                if (didNodeName.equals("note")) {
                    for (int j = 0; j < didNodeChild.getAttributes().getLength(); j++) {
                        Node noteAttributeNode = didNodeChild.getAttributes().item(j);
                        String noteAttributeNodeValue = noteAttributeNode.getNodeValue();

                        if (noteAttributeNodeValue.equals("sourcesDescription")) {
                            ead2002Metadata.setNoteSourcesDescription(didNodeChild.getChildNodes().item(1)
                                    .getChildNodes().item(0).getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("....note sourcesDescription: " + ead2002Metadata.getNoteSourcesDescription());
                        }

                        if (noteAttributeNodeValue.equals("generalNote")) {
                            ead2002Metadata.setNoteGeneralNote(didNodeChild.getChildNodes().item(1)
                                    .getChildNodes().item(0).getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("....note generalNote: " + ead2002Metadata.getNoteGeneralNote());
                        }
                    }
                }

                if (didNodeName.equals("origination")) {
                    String origination = didNodeChild.getChildNodes().item(0).getNodeValue().replaceAll("\\n", "").trim();
                    if (origination.length() > 0) {
                        ead2002Metadata.setOrigination(origination);
                        //System.out.println("....origination: " + ead2002Metadata.getOrigination());
                    }

                    for (int j = 0; j < didNodeChild.getAttributes().getLength(); j++) {
                        Node originationAttributeNode = didNodeChild.getAttributes().item(j);
                        String originationAttributeNodeName = originationAttributeNode.getNodeValue();

                        if (originationAttributeNodeName.equals("creator")) {
                            ead2002Metadata.setOriginationCreator(didNodeChild.getChildNodes().item(1)
                                    .getChildNodes().item(0).getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("....origination creator: " + ead2002Metadata.getOriginationCreator());
                        }

                        if (originationAttributeNodeName.equals("producer")) {
                            ead2002Metadata.setOriginationProducer(didNodeChild.getChildNodes().item(1)
                                    .getChildNodes().item(0).getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("....origination producer: " + ead2002Metadata.getOriginationProducer());
                        }
                    }
                }

                if (didNodeName.equals("materialspec")) {
                    ead2002Metadata.setMaterialSpecification(didNodeChild.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("....materialspec: " + ead2002Metadata.getMaterialSpecification());
                }
            }

            // *********************************************************************************************************

            for (int i = 0; i < archdescNode.getChildNodes().getLength(); i++) {
                Node archdescChildNode = archdescNode.getChildNodes().item(i);
                String archdescChildNodeName = archdescChildNode.getNodeName();

                if (archdescChildNodeName.equals("odd")) {
                    for (int j = 0; j < archdescChildNode.getAttributes().getLength(); j++) {
                        Node oddAttributeNode = archdescChildNode.getAttributes().item(j);
                        String oddAttributeNodeName = oddAttributeNode.getNodeValue();

                        if (oddAttributeNodeName.equals("levelOfDetail")) {
                            ead2002Metadata.setOddLevelOfDetail(archdescChildNode.getChildNodes().item(1)
                                    .getChildNodes().item(0).getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("..odd levelOfDetail: " + ead2002Metadata.getOddLevelOfDetail());
                        }

                        if (oddAttributeNodeName.equals("statusDescription")) {
                            ead2002Metadata.setOddStatusDescription(archdescChildNode.getChildNodes().item(1)
                                    .getChildNodes().item(0).getNodeValue().replaceAll("\\n", "").trim());
                            //System.out.println("..odd statusDescription: " + ead2002Metadata.getOddStatusDescription());
                        }
                    }

                }

                if (archdescChildNodeName.equals("scopecontent")) {
                    ead2002Metadata.setScopeContent(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..scopecontent: " + ead2002Metadata.getScopeContent());
                }

                if (archdescChildNodeName.equals("arrangement")) {
                    ead2002Metadata.setArrangement(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..arrangement: " + ead2002Metadata.getArrangement());
                }

                if (archdescChildNodeName.equals("appraisal")) {
                    ead2002Metadata.setAppraisal(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..appraisal: " + ead2002Metadata.getAppraisal());
                }

                if (archdescChildNodeName.equals("acqinfo")) {
                    ead2002Metadata.setAcquisitionInfo(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..acqinfo: " + ead2002Metadata.getAcquisitionInfo());
                }

                if (archdescChildNodeName.equals("accruals")) {
                    ead2002Metadata.setAccruals(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..accruals: " + ead2002Metadata.getAccruals());
                }

                if (archdescChildNodeName.equals("custodhist")) {
                    ead2002Metadata.setCustodialHistory(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..custodhist: " + ead2002Metadata.getCustodialHistory());
                }

                if (archdescChildNodeName.equals("processinfo")) {
                    for (int j = 0; j < archdescChildNode.getChildNodes().getLength(); j++) {
                        Node processinfoNode = archdescChildNode.getChildNodes().item(j);
                        if (processinfoNode.getChildNodes().getLength() > 0) {
                            if ((processinfoNode.getChildNodes().item(0) != null) &&
                                    processinfoNode.getChildNodes().item(0).getNodeName().equals("date")) {
                                ead2002Metadata.setProcessInfoDate(processinfoNode.getChildNodes().item(0).getChildNodes().item(0)
                                        .getNodeValue().replaceAll("\\n", "").trim());
                                //System.out.println("..processinfo date: " + ead2002Metadata.getProcessInfoDate());
                            } else if ((processinfoNode.getChildNodes().item(1) != null) &&
                                    processinfoNode.getChildNodes().item(1).getNodeName().equals("date")) {
                                ead2002Metadata.setProcessInfoDate(processinfoNode.getChildNodes().item(1).getChildNodes().item(0)
                                        .getNodeValue().replaceAll("\\n", "").trim());
                                //System.out.println("..processinfo date: " + ead2002Metadata.getProcessInfoDate());
                            } else {
                                ead2002Metadata.setProcessInfoArchivistNotes(processinfoNode.getChildNodes().item(0)
                                        .getNodeValue().replaceAll("\\n", "").trim());
                                //System.out.println("..processinfo archivist notes: " + ead2002Metadata.getProcessInfoArchivistNotes());
                            }
                        }
                    }
                }

                if (archdescChildNodeName.equals("originalsloc")) {
                    ead2002Metadata.setOriginalsLocation(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..originalsloc: " + ead2002Metadata.getOriginalsLocation());
                }

                if (archdescChildNodeName.equals("altformavail")) {
                    ead2002Metadata.setAlternativeFormAvailable(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..altformavail: " + ead2002Metadata.getAlternativeFormAvailable());
                }

                if (archdescChildNodeName.equals("relatedmaterial")) {
                    ead2002Metadata.setRelatedMaterial(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..relatedmaterial: " + ead2002Metadata.getRelatedMaterial());
                }

                if (archdescChildNodeName.equals("accessrestrict")) {
                    ead2002Metadata.setAccessRestrictions(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..accessrestrict: " + ead2002Metadata.getAccessRestrictions());
                }

                if (archdescChildNodeName.equals("userestrict")) {
                    ead2002Metadata.setUseRestrictions(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..userestrict: " + ead2002Metadata.getUseRestrictions());
                }

                if (archdescChildNodeName.equals("otherfindaid")) {
                    ead2002Metadata.setOtherFindAid(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..otherfindaid: " + ead2002Metadata.getOtherFindAid());
                }

                if (archdescChildNodeName.equals("phystech")) {
                    ead2002Metadata.setPhysicalTech(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..phystech: " + ead2002Metadata.getPhysicalTech());
                }

                if (archdescChildNodeName.equals("bibliography")) {
                    ead2002Metadata.setBibliography(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..bibliography: " + ead2002Metadata.getBibliography());
                }

                if (archdescChildNodeName.equals("prefercite")) {
                    ead2002Metadata.setPreferCite(archdescChildNode.getChildNodes().item(1).getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..prefercite: " + ead2002Metadata.getPreferCite());
                }
            }

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        return ead2002Metadata;
    }

    /**
     * Method responsible for parsing an XML metadata file in the Dublin Core Simple 2002-12-12 format.
     * @param file The filename of the file to be parsed.
     */
    public static DublinCore20021212Metadata parseDublinCoreSimple20021212(File file) {

        DublinCore20021212Metadata dublinCore20021212Metadata = new DublinCore20021212Metadata();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            String rootElement = doc.getDocumentElement().getNodeName();
            //System.out.println("---------------------------------------------------------------------------------------");
            //System.out.println("Dublin Core Simple 2002-12-12 Parser");
            //System.out.println("---------------------------------------------------------------------------------------");
            //System.out.println("Root element: " + rootElement);

            // *********************************************************************************************************
            //simpledc root element
            //System.out.println(rootElement);

            for (int i = 0; i < doc.getDocumentElement().getChildNodes().getLength(); i++) {
                Node simpledcNode = doc.getDocumentElement().getChildNodes().item(i);
                String nodeName = simpledcNode.getNodeName();

                if (nodeName.equals("title")) {
                    dublinCore20021212Metadata.setTitle(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..title: " + dublinCore20021212Metadata.getTitle());
                }

                if (nodeName.equals("identifier")) {
                    dublinCore20021212Metadata.setIdentifier(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..identifier: " + dublinCore20021212Metadata.getIdentifier());
                }

                if (nodeName.equals("creator")) {
                    dublinCore20021212Metadata.setCreator(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..creator: " + dublinCore20021212Metadata.getCreator());
                }

                if (nodeName.equals("date")) {
                    if (dublinCore20021212Metadata.getInitialDate().equals("")) {
                        dublinCore20021212Metadata.setInitialDate(simpledcNode.getChildNodes().item(0)
                                .getNodeValue().replaceAll("\\n", "").trim());
                        //System.out.println("..initial date: " + dublinCore20021212Metadata.getInitialDate());
                    } else {
                        dublinCore20021212Metadata.setFinalDate(simpledcNode.getChildNodes().item(0)
                                .getNodeValue().replaceAll("\\n", "").trim());
                        //System.out.println("..final date: " + dublinCore20021212Metadata.getFinalDate());
                    }
                }

                if (nodeName.equals("description")) {
                    dublinCore20021212Metadata.setDescription(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..description: " + dublinCore20021212Metadata.getDescription());
                }

                if (nodeName.equals("publisher")) {
                    dublinCore20021212Metadata.setPublisher(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..publisher: " + dublinCore20021212Metadata.getPublisher());
                }

                if (nodeName.equals("contributor")) {
                    dublinCore20021212Metadata.setContributor(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..contributor: " + dublinCore20021212Metadata.getContributor());
                }

                if (nodeName.equals("rights")) {
                    dublinCore20021212Metadata.setRights(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..rights: " + dublinCore20021212Metadata.getRights());
                }

                if (nodeName.equals("language")) {
                    dublinCore20021212Metadata.setLanguage(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..language: " + dublinCore20021212Metadata.getLanguage());
                }

                if (nodeName.equals("coverage")) {
                    dublinCore20021212Metadata.setCoverage(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..coverage: " + dublinCore20021212Metadata.getCoverage());
                }

                if (nodeName.equals("format")) {
                    dublinCore20021212Metadata.setFormat(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..format: " + dublinCore20021212Metadata.getFormat());
                }

                if (nodeName.equals("relation")) {
                    dublinCore20021212Metadata.setRelation(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..relation: " + dublinCore20021212Metadata.getRelation());
                }

                if (nodeName.equals("subject")) {
                    dublinCore20021212Metadata.setSubject(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..subject: " + dublinCore20021212Metadata.getSubject());
                }

                if (nodeName.equals("type")) {
                    dublinCore20021212Metadata.setType(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..type: " + dublinCore20021212Metadata.getType());
                }

                if (nodeName.equals("source")) {
                    dublinCore20021212Metadata.setSource(simpledcNode.getChildNodes().item(0)
                            .getNodeValue().replaceAll("\\n", "").trim());
                    //System.out.println("..source: " + dublinCore20021212Metadata.getSource());
                }
            }

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        return dublinCore20021212Metadata;
    }

    /**
     * Method responsible for parsing an XML metadata file in the Key-value format.
     * @param file The filename of the file to be parsed.
     */
    public static KeyValueMetadata parseKeyValue(File file) {

        KeyValueMetadata keyValueMetadata = new KeyValueMetadata();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            String rootElement = doc.getDocumentElement().getNodeName();
            //System.out.println("---------------------------------------------------------------------------------------");
            //System.out.println("Key-value Parser");
            //System.out.println("---------------------------------------------------------------------------------------");
            //System.out.println("Root element: " + rootElement);

            // *********************************************************************************************************
            //metadata root element
            //System.out.println(rootElement);

            for (int i = 0; i < doc.getDocumentElement().getChildNodes().getLength(); i++) {
                Node metadataNode = doc.getDocumentElement().getChildNodes().item(i);

                if (metadataNode.getNodeName().equals("field")) {

                    String attributeName = metadataNode.getAttributes().item(0).getNodeValue();

                    if (attributeName.equals("id")) {
                        keyValueMetadata.setId(metadataNode.getChildNodes().item(0)
                                .getNodeValue().replaceAll("\\n", "").trim());
                        //System.out.println("..field id: " + keyValueMetadata.getId());
                    }

                    if (attributeName.equals("title")) {
                        keyValueMetadata.setTitle(metadataNode.getChildNodes().item(0)
                                .getNodeValue().replaceAll("\\n", "").trim());
                        //System.out.println("..field title: " + keyValueMetadata.getTitle());
                    }

                    if (attributeName.equals("producer")) {
                        keyValueMetadata.setProducer(metadataNode.getChildNodes().item(0)
                                .getNodeValue().replaceAll("\\n", "").trim());
                        //System.out.println("..field producer: " + keyValueMetadata.getProducer());
                    }

                    if (attributeName.equals("date")) {
                        keyValueMetadata.setDate(metadataNode.getChildNodes().item(0)
                                .getNodeValue().replaceAll("\\n", "").trim());
                        //System.out.println("..field date: " + keyValueMetadata.getDate());
                    }
                }
            }

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        return keyValueMetadata;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // --- André Rosa Custom Implementations :: END --------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------

}
