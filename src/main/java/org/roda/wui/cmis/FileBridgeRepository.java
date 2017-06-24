package org.roda.wui.cmis;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.*;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.definitions.*;
import org.apache.chemistry.opencmis.commons.enums.*;
import org.apache.chemistry.opencmis.commons.exceptions.*;
import org.apache.chemistry.opencmis.commons.impl.Base64;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.chemistry.opencmis.commons.impl.MimeTypes;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.*;
import org.apache.chemistry.opencmis.commons.impl.server.ObjectInfoImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.roda.wui.cmis.database.Database;
import org.roda.wui.cmis.enums.FileBridgeCmisTypeId;
import org.roda.wui.cmis.enums.MetadataDublinCoreFieldId;
import org.roda.wui.cmis.enums.MetadataEadFieldId;
import org.roda.wui.cmis.enums.MetadataKeyValueFieldId;
import org.roda.wui.cmis.metadata.AipMetadata;
import org.roda.wui.cmis.database.Query;

import java.io.*;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implements all repository operations.
 */
public class FileBridgeRepository {

    private static final String ROOT_ID = "@root@";

    private static final String USER_UNKNOWN = "<unknown>";

    private static final String CMIS_READ = "cmis:read";
    private static final String CMIS_WRITE = "cmis:write";
    private static final String CMIS_ALL = "cmis:all";

    private static final int BUFFER_SIZE = 64 * 1024;

    /**
     * Repository id.
     */
    private final String repositoryId;
    /**
     * Root directory.
     */
    private final File root;
    /**
     * Types.
     */
    private final FileBridgeTypeManager typeManager;
    /**
     * Users.
     */
    private final Map<String, Boolean> readWriteUserMap;
    /**
     * CMIS 1.0 repository info.
     */
    private final RepositoryInfo repositoryInfo10;
    /**
     * CMIS 1.1 repository info.
     */
    private final RepositoryInfo repositoryInfo11;

    public FileBridgeRepository(final String repositoryId, final String rootPath,
                                final FileBridgeTypeManager typeManager) {
        // check repository id
        if (repositoryId == null || repositoryId.trim().length() == 0) {
            throw new IllegalArgumentException("Invalid repository id!");
        }

        this.repositoryId = repositoryId;

        // check root folder
        if (rootPath == null || rootPath.trim().length() == 0) {
            throw new IllegalArgumentException("Invalid root folder!");
        }

        root = new File(rootPath);
        if (!root.isDirectory()) {
            throw new IllegalArgumentException("Root is not a directory!");
        }

        // set type manager objects
        this.typeManager = typeManager;

        // set up read-write user map
        readWriteUserMap = new HashMap<String, Boolean>();

        // set up aip metadata map
        aipMetadataMap = new HashMap<String, AipMetadata>();

        // set up database for the repository
        database = new Database("SQLite");
        this.loadRepositoryContentsInfoRecursive(root);

        // set up repository infos
        repositoryInfo10 = createRepositoryInfo(CmisVersion.CMIS_1_0);
        repositoryInfo11 = createRepositoryInfo(CmisVersion.CMIS_1_1);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // --- André Rosa Custom Implementations :: BEGIN ------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * AIPs Metadata Map.
     *
     * This variable is responsible for keeping each AIP's metadata in memory to be used across the various server's
     * methods calls.
     */
    private final Map<String, AipMetadata> aipMetadataMap;

    /**
     * Metadata database interaction.
     *
     * This variable holds a single instance of the Database class. The Database class encapsulates all the interaction
     * between the CMIS server and the supporting database.
     */
    private final Database database;

    /**
     * Can read AIP flag.
     *
     * This boolean variable acts as semaphore to control the access to the AIP information through cycle iterations
     * in the "loadRepositoryContentsInfoRecursive" function.
     */
    private Boolean canReadAIP = false;

    /**
     * Method responsible for loading an AIP metadata into the repository's metadata structures.
     * @param aipId The AIP's unique ID.
     * @param aipJson The aip.json file.
     */
    private void loadAipMetadata(String aipId, File aipJson) {
        //extract the metadata from the AIP metadata files
        AipMetadata aipMetadata = new AipMetadata(aipId);
        aipMetadata.setEad2002Metadata(FileBridgeUtils.getEAD2002Metadata(aipJson.getPath()));
        aipMetadata.setDublinCore20021212Metadata(FileBridgeUtils.getDublinCore20021212Metadata(aipJson.getPath()));
        aipMetadata.setKeyValueMetadata(FileBridgeUtils.getKeyValueMetadata(aipJson.getPath()));

        //System.out.println(aipMetadata.getEad2002Metadata().toString());
        //System.out.println(aipMetadata.getDublinCore20021212Metadata().toString());
        //System.out.println(aipMetadata.getKeyValueMetadata().toString());

        //store the AIPs read metadata
        aipMetadataMap.put(aipId, aipMetadata);
    }

    /**
     * Recursive function responsible for loading all the repository contents to the database.
     * This function is called in this class's constructor as it should only happen once when you bootstrap
     * the repository's CMIS server.
     * @param file The repository's root folder.
     */
    private void loadRepositoryContentsInfoRecursive(File file) {
        if (file == null) { return; }

        //get path length
        String relativePath = file.getPath().replace(root.getPath()+"/", "");
        int pathLength = relativePath.split("/").length;

        // Check the "aip.json" file for AIP read permissions
        if (relativePath.toLowerCase().contains("aip.json")) {
            canReadAIP = FileBridgeUtils.canReadAIP(file.getPath());
            //System.out.println("AIP FOUND. Can read: " + canReadAIP);
            // Load the AIP metadata files
            String aipId = relativePath.split("/")[0];
            if (canReadAIP && !aipMetadataMap.containsKey(aipId)) {
                loadAipMetadata(aipId, file);
            }
        }

        if (!file.isDirectory()) {
            //FILE - cmis:document / cmis:rodaDocument
            //extract info, but only for the files inside the "representations" folder
            if ((pathLength >= 4) && canReadAIP &&
                    (relativePath.toLowerCase().contains("/representations/")) &&
                    (!relativePath.toLowerCase().contains("/metadata/"))) {
                //System.out.println("FILE FOUND: " + file.getPath());
                ObjectInfoImpl objectInfo = new ObjectInfoImpl();
                compileProperties(null, file, null, objectInfo);
            }
        } else {
            //FOLDER - cmis:folder
            //extract info, but only for the folders inside the "representations" folder
            if ((pathLength >= 5) && canReadAIP &&
                    (relativePath.toLowerCase().contains("/representations/")) &&
                    (!relativePath.toLowerCase().contains("/metadata/"))) {
                //System.out.println("FOLDER FOUND: " + file.getPath());
                ObjectInfoImpl objectInfo = new ObjectInfoImpl();
                compileProperties(null, file, null, objectInfo);
            }

            // iterate through the file's children
            for (File child : file.listFiles()) {
                // skip hidden files, for example '.DS_Store'
                if (child.isHidden()) { continue; }
                loadRepositoryContentsInfoRecursive(child);
            }
        }
    }

    // --- CMIS operations ---

    /**
     * CMIS getChildren.
     */
    public ObjectInFolderList getChildren(CallContext context, String folderId,
                                          String filter, Boolean includeAllowableActions,
                                          Boolean includePathSegment, BigInteger maxItems,
                                          BigInteger skipCount, ObjectInfoHandler objectInfos) {

        boolean userReadOnly = checkUser(context, false);

        // split filter
        Set<String> filterCollection = FileBridgeUtils.splitFilter(filter);

        // set defaults if values not set
        boolean iaa = FileBridgeUtils.getBooleanParameter(includeAllowableActions, false);
        boolean ips = FileBridgeUtils.getBooleanParameter(includePathSegment, false);

        // skip and max
        int skip = (skipCount == null ? 0 : skipCount.intValue());
        if (skip < 0) { skip = 0; }

        int max = (maxItems == null ? Integer.MAX_VALUE : maxItems.intValue());
        if (max < 0) { max = Integer.MAX_VALUE; }

        // get the folder
        File folder = getFile(folderId);
        if (!folder.isDirectory()) {
            throw new CmisObjectNotFoundException("Not a folder!");
        }

        // set object info of the the folder
        if (context.isObjectInfoRequired()) {
            compileObjectData(context, folder, null, false, false, userReadOnly, objectInfos);
        }

        // prepare result
        ObjectInFolderListImpl result = new ObjectInFolderListImpl();
        result.setObjects(new ArrayList<ObjectInFolderData>());
        result.setHasMoreItems(false);
        int count = 0;

        // iterate through children
        for (File child : folder.listFiles()) {
            // skip hidden files, for example '.DS_Store'
            if (child.isHidden()) { continue; }

            //************************************************************************************
            //BEGIN - AIP FILE BRIDGE

            String relativePath = child.getPath().replace(root.getPath()+"/", "");
            int pathLength = relativePath.split("/").length;

            if (pathLength == 1) {
                //**********************************************************
                // WE ARE READING THE AIP INITIAL FOLDER
                //**********************************************************
                Boolean canReadAIP = false;

                //**********************************************************
                //we are in the AIPs "<guid>" root folder, iterate the direct children
                for (File firstLevelChild : child.listFiles()) {
                    // skip hidden files, for example '.DS_Store'
                    if (firstLevelChild.isHidden()) { continue; }

                    String firstLevelRelativePath = firstLevelChild.getPath().replace(root.getPath()+"/", "");
                    String[] firstLevelPathElements = firstLevelRelativePath.split("/");

                    //**********************************************************
                    // Check the "aip.json" file for AIP read permissions
                    if ((firstLevelPathElements.length == 2) && (firstLevelPathElements[1].equals("aip.json"))) {
                        canReadAIP = FileBridgeUtils.canReadAIP(firstLevelChild.getPath());
                    }

                    if ((firstLevelPathElements.length == 2) && (firstLevelPathElements[1].equals("representations")) && canReadAIP) {
                        //**********************************************************
                        //we are in the "representations" folder, iterate the direct children
                        for (File secondLevelChild : firstLevelChild.listFiles()) {
                            // skip hidden files, for example '.DS_Store'
                            if (secondLevelChild.isHidden()) { continue; }

                            //**********************************************************
                            //we are in the "repX" folder, iterate the direct children
                            for (File thirdLevelChild : secondLevelChild.listFiles()) {
                                // skip hidden files, for example '.DS_Store'
                                if (thirdLevelChild.isHidden()) { continue; }

                                String thirdLevelRelativePath = thirdLevelChild.getPath().replace(root.getPath()+"/", "");
                                String[] thirdLevelPathElements = thirdLevelRelativePath.split("/");

                                if ((thirdLevelPathElements.length == 4) && (thirdLevelPathElements[3].equals("data"))) {
                                    //**********************************************************
                                    //we are in the "repX/data" folder, iterate the direct children
                                    //these are the files we are exposing through the CMIS server
                                    for (File fourthLevelChild : thirdLevelChild.listFiles()) {
                                        // skip hidden files, for example '.DS_Store'
                                        if (fourthLevelChild.isHidden()) { continue; }

                                        count++;

                                        if (skip > 0) {
                                            skip--;
                                            continue;
                                        }

                                        if (result.getObjects().size() >= max) {
                                            result.setHasMoreItems(true);
                                            continue;
                                        }

                                        ObjectInFolderDataImpl objectInFolder = new ObjectInFolderDataImpl();
                                        objectInFolder.setObject(compileObjectData(context, fourthLevelChild,
                                                filterCollection, iaa, false, userReadOnly, objectInfos));
                                        if (ips) { objectInFolder.setPathSegment(fourthLevelChild.getName()); }

                                        result.getObjects().add(objectInFolder);
                                    }
                                    //END "repX/data" folder
                                    //**********************************************************
                                }
                            }
                            //END "rep1, rep2" folder
                            //**********************************************************
                        }
                        //END "representations" folder
                        //**********************************************************
                    }
                }
                //END AIP root folder
                //**********************************************************
            } else if (pathLength > 1 && pathLength < 6) {
                //*********************************************************************
                // WE ARE IN BETWEEN DIRECTORIES WE DO NOT WANT TO BE ABLE TO BROWSE
                //*********************************************************************

                return this.getChildren(context, ROOT_ID, filter, includeAllowableActions, includePathSegment, maxItems,
                        skipCount, objectInfos);
            } else {
                //*********************************************************************
                // WE ARE ALREADY INSIDE THE AIP, SO WE HAVE PERMISSIONS TO BE THERE ;)
                //*********************************************************************
                count++;

                if (skip > 0) {
                    skip--;
                    continue;
                }

                if (result.getObjects().size() >= max) {
                    result.setHasMoreItems(true);
                    continue;
                }

                ObjectInFolderDataImpl objectInFolder = new ObjectInFolderDataImpl();
                objectInFolder.setObject(compileObjectData(context, child,
                        filterCollection, iaa, false, userReadOnly, objectInfos));
                if (ips) { objectInFolder.setPathSegment(child.getName()); }

                result.getObjects().add(objectInFolder);
            }

            //END - AIP FILE BRIDGE
            //************************************************************************************
        }

        result.setNumItems(BigInteger.valueOf(count));

        return result;
    }

    /**
     * CMIS Query. This function queries the CMIS server objects metadata stored in memory through a custom query parser.
     *
     * This implementation is not being used but is left for educational purposes.
     */
    public ObjectList queryMemoryMetadata(CallContext context, String statement,
                                          Boolean includeAllowableActions, BigInteger maxItems,
                                          BigInteger skipCount, ObjectInfoHandler objectInfos) {
        boolean userReadOnly = checkUser(context, false);

        // get the base folder
        File folder = getFile(ROOT_ID);

        //get the parsed query object
        Query query = new Query(statement);
        if (query.getQueryType() == null) {
            throw new CmisInvalidArgumentException("Invalid or unsupported query.");
        } else if (query.getQueryType().equals("IN_FOLDER")) {
            if (query.getFolderId().length() == 0) {
                throw new CmisInvalidArgumentException("Invalid folder id.");
            }
            folder = getFile(query.getFolderId());
            if (!folder.isDirectory()) {
                throw new CmisInvalidArgumentException("Not a folder!");
            }
        }

        TypeDefinition type = typeManager.getInternalTypeDefinition(query.getTypeId());
        if (type == null) {
            throw new CmisInvalidArgumentException("Unknown type.");
        }

        boolean queryFiles = (type.getBaseTypeId() == BaseTypeId.CMIS_DOCUMENT);

        // set defaults if values not set
        boolean iaa = FileBridgeUtils.getBooleanParameter(includeAllowableActions, false);

        // skip and max
        int skip = (skipCount == null ? 0 : skipCount.intValue());
        if (skip < 0) { skip = 0; }

        int max = (maxItems == null ? Integer.MAX_VALUE : maxItems.intValue());
        if (max < 0) { max = Integer.MAX_VALUE; }

        // prepare result
        ObjectListImpl result = new ObjectListImpl();
        result.setObjects(new ArrayList<ObjectData>());
        result.setHasMoreItems(false);
        int count = 0;

        // iterate through children
        for (File hit : folder.listFiles()) {

            // skip hidden files, for example '.DS_Store'
            if (hit.isHidden()) { continue; }

            //************************************************************************************
            //BEGIN - AIP FILE BRIDGE

            String relativePath = hit.getPath().replace(root.getPath()+"/", "");
            int pathLength = relativePath.split("/").length;

            //**********************************************************
            // WE ARE READING THE AIP INITIAL FOLDER
            //**********************************************************
            Boolean canReadAIP = false;

            //**********************************************************
            //we are in the AIPs "<guid>" root folder, iterate the direct children
            for (File firstLevelChild : hit.listFiles()) {
                // skip hidden files, for example '.DS_Store'
                if (firstLevelChild.isHidden()) { continue; }

                String firstLevelRelativePath = firstLevelChild.getPath().replace(root.getPath()+"/", "");
                String[] firstLevelPathElements = firstLevelRelativePath.split("/");

                //**********************************************************
                // Check the "aip.json" file for AIP read permissions
                if ((firstLevelPathElements.length == 2) && (firstLevelPathElements[1].equals("aip.json"))) {
                    canReadAIP = FileBridgeUtils.canReadAIP(firstLevelChild.getPath());
                }

                if ((firstLevelPathElements.length == 2) && (firstLevelPathElements[1].equals("representations")) && canReadAIP) {
                    //**********************************************************
                    //we are in the "representations" folder, iterate the direct children
                    for (File secondLevelChild : firstLevelChild.listFiles()) {
                        // skip hidden files, for example '.DS_Store'
                        if (secondLevelChild.isHidden()) { continue; }

                        //**********************************************************
                        //we are in the "repX" folder, iterate the direct children
                        for (File thirdLevelChild : secondLevelChild.listFiles()) {
                            // skip hidden files, for example '.DS_Store'
                            if (thirdLevelChild.isHidden()) { continue; }

                            String thirdLevelRelativePath = thirdLevelChild.getPath().replace(root.getPath()+"/", "");
                            String[] thirdLevelPathElements = thirdLevelRelativePath.split("/");

                            if ((thirdLevelPathElements.length == 4) && (thirdLevelPathElements[3].equals("data"))) {
                                //**********************************************************
                                //we are in the "repX/data" folder, iterate the direct children
                                //these are the files we are exposing through the CMIS server
                                for (File fourthLevelChild : thirdLevelChild.listFiles()) {
                                    // skip hidden files, for example '.DS_Store'
                                    if (fourthLevelChild.isHidden()) { continue; }

                                    // skip directory if documents are requested
                                    if (fourthLevelChild.isDirectory() && queryFiles) { continue; }

                                    // skip files if folders are requested
                                    if (fourthLevelChild.isFile() && !queryFiles) { continue; }

                                    count++;

                                    if (skip > 0) { skip--; continue; }

                                    if (result.getObjects().size() >= max) { result.setHasMoreItems(true); continue; }

                                    // build and add child object
                                    ObjectData object = compileObjectData(context, fourthLevelChild, null, iaa, false, userReadOnly, objectInfos);
                                    boolean isMatch = false;

                                    // set query names
                                    for (PropertyData<?> prop : object.getProperties().getPropertyList()) {

                                        //all fields selected - try to extract as many properties / fields as possible from the current object type
                                        if (query.searchAllFields()) {
                                            if (type.getPropertyDefinitions().get(prop.getId()) != null) {
                                                ((MutablePropertyData<?>) prop).setQueryName(type.getPropertyDefinitions().get(prop.getId()).getQueryName());
                                                if (query.hasWhereConditions()) {
                                                    Object value = null;
                                                    if (!((MutablePropertyData<?>) prop).getValues().isEmpty()) {
                                                        value = ((MutablePropertyData<?>) prop).getValues().get(0);
                                                    }
                                                    isMatch = query.isWhereMatch(type.getPropertyDefinitions().get(prop.getId()).getQueryName(),
                                                            value);
                                                    if (isMatch) break;
                                                } else {
                                                    isMatch = true;
                                                }
                                            }
                                        }
                                        //specific fields selected - extract only the selected properties / fields from the current object type
                                        else {
                                            String[] fieldsArray = query.getFieldsArray();

                                            if ((type.getPropertyDefinitions().get(prop.getId()) != null)) {
                                                //only include the requested fields
                                                for (int i = 0; i < fieldsArray.length; i++) {

                                                    if (type.getPropertyDefinitions().get(prop.getId()).getQueryName().equals(fieldsArray[i])) {
                                                        ((MutablePropertyData<?>) prop).setQueryName(type.getPropertyDefinitions().get(prop.getId()).getQueryName());

                                                        if (query.hasWhereConditions()) {
                                                            Object value = null;
                                                            if (!((MutablePropertyData<?>) prop).getValues().isEmpty()) {
                                                                value = ((MutablePropertyData<?>) prop).getValues().get(0);
                                                            }
                                                            if (!isMatch) isMatch = query.isWhereMatch(
                                                                    type.getPropertyDefinitions().get(prop.getId()).getQueryName(),
                                                                    value);
                                                        } else {
                                                            isMatch = true;
                                                        }

                                                    }

                                                }
                                            }

                                        }

                                    }

                                    if (isMatch) result.getObjects().add(object);
                                }
                                //END "repX/data" folder
                                //**********************************************************
                            }
                        }
                        //END "rep1, rep2" folder
                        //**********************************************************
                    }
                    //END "representations" folder
                    //**********************************************************
                }
            }
            //END AIP root folder
            //**********************************************************

            //END - AIP FILE BRIDGE
            //************************************************************************************

        }

        result.setNumItems(BigInteger.valueOf(count));

        return result;
    }

    /**
     * CMIS query.
     */
    public ObjectList query(CallContext context, String statement,
                            Boolean includeAllowableActions, BigInteger maxItems,
                            BigInteger skipCount, ObjectInfoHandler objectInfos) {
        boolean userReadOnly = checkUser(context, false);

        //Query the objects database for results
        List<String> objectPaths = database.query(statement);

        //get the objects type from the query: cmis:folder / cmis:document / cmis:rodaDocument
        TypeDefinition type = typeManager.getInternalTypeDefinition(database.getLastQuery().getTypeId());
        if (type == null) { throw new CmisInvalidArgumentException("Unknown CMIS object type. The supported CMIS object types are: cmis:folder, cmis:document or cmis:rodaDocument."); }

        // set defaults if values not set
        boolean iaa = FileBridgeUtils.getBooleanParameter(includeAllowableActions, false);

        // skip and max
        int skip = (skipCount == null ? 0 : skipCount.intValue());
        if (skip < 0) { skip = 0; }

        int max = (maxItems == null ? Integer.MAX_VALUE : maxItems.intValue());
        if (max < 0) { max = Integer.MAX_VALUE; }

        // prepare result
        ObjectListImpl result = new ObjectListImpl();
        result.setObjects(new ArrayList<ObjectData>());
        result.setHasMoreItems(false);
        int count = 0;

        ArrayList<String> queryProperties = database.getLastQuery().getFieldsArrayList();

        //create objects straight from the database results
        for (String objectPath : objectPaths) {
            count++;
            if (skip > 0) { skip--; continue; }
            if (result.getObjects().size() >= max) { result.setHasMoreItems(true); continue; }

            File resultFile = new File(root.getPath() + objectPath);

            // build and add object
            ObjectData object = compileObjectData(context, resultFile, null, iaa, false, userReadOnly, objectInfos, false);

            // set query names
            for (PropertyData<?> prop : object.getProperties().getPropertyList()) {
                if (database.getLastQuery().searchAllFields()) {
                    //try to extract as many properties / fields as possible from the current object type
                    //if a property is not present (the object type might not have it) do not include it
                    if (type.getPropertyDefinitions().get(prop.getId()) != null) {
                        ((MutablePropertyData<?>) prop).setQueryName(type.getPropertyDefinitions().get(prop.getId()).getQueryName());
                    }
                } else {
                    //extract only the properties requested in the query for the current object type
                    //if a property is not present (the object type might not have it) do not include it
                    if ((type.getPropertyDefinitions().get(prop.getId()) != null) && queryProperties.contains(prop.getId())) {
                        ((MutablePropertyData<?>) prop).setQueryName(type.getPropertyDefinitions().get(prop.getId()).getQueryName());
                    }
                }
            }
            result.getObjects().add(object);
        }
        result.setNumItems(BigInteger.valueOf(count));

        return result;
    }

    // --- helpers ---

    /**
     * [Function Signature #1] Compiles an object type object from a file or folder.
     */
    private ObjectData compileObjectData(CallContext context, File file,
                                         Set<String> filter, boolean includeAllowableActions,
                                         boolean includeAcl, boolean userReadOnly,
                                         ObjectInfoHandler objectInfos) {
        return this.compileObjectData(context, file, filter, includeAllowableActions, includeAcl, userReadOnly, objectInfos, true);
    }

    /**
     * [Function Signature #2] Compiles an object type object from a file or folder.
     */
    private ObjectData compileObjectData(CallContext context, File file,
                                         Set<String> filter, boolean includeAllowableActions,
                                         boolean includeAcl, boolean userReadOnly,
                                         ObjectInfoHandler objectInfos, Boolean updateProperties) {
        ObjectDataImpl result = new ObjectDataImpl();
        ObjectInfoImpl objectInfo = new ObjectInfoImpl();

        result.setProperties(compileProperties(context, file, filter, objectInfo, updateProperties));

        if (includeAllowableActions) {
            result.setAllowableActions(compileAllowableActions(file, userReadOnly));
        }

        if (includeAcl) {
            result.setAcl(compileAcl(file));
            result.setIsExactAcl(true);
        }

        if (context.isObjectInfoRequired()) {
            objectInfo.setObject(result);
            objectInfos.addObjectInfo(objectInfo);
        }

        return result;
    }

    /**
     * [Function Signature #1] Gathers all base properties of a file or folder.
     */
    private Properties compileProperties(CallContext context, File file, Set<String> orgfilter,
                                         ObjectInfoImpl objectInfo) {
        return this.compileProperties(context, file, orgfilter, objectInfo, true);
    }

    /**
     * [Function Signature #2] Gathers all base properties of a file or folder.
     */
    private Properties compileProperties(CallContext context, File file, Set<String> orgfilter,
                                         ObjectInfoImpl objectInfo, Boolean updateProperties) {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null!");
        }

        // we can't gather properties if the file or folder doesn't exist
        if (!file.exists()) {
            throw new CmisObjectNotFoundException("Object not found!");
        }

        // copy filter
        Set<String> filter = (orgfilter == null ? null : new HashSet<String>(
                orgfilter));

        // find base type
        String typeId = null;

        // identify if the file is a doc or a folder/directory
        if (file.isDirectory()) {
            typeId = BaseTypeId.CMIS_FOLDER.value();
            objectInfo.setBaseType(BaseTypeId.CMIS_FOLDER);
            objectInfo.setTypeId(typeId);
            objectInfo.setContentType(null);
            objectInfo.setFileName(null);
            objectInfo.setHasAcl(true);
            objectInfo.setHasContent(false);
            objectInfo.setVersionSeriesId(null);
            objectInfo.setIsCurrentVersion(true);
            objectInfo.setRelationshipSourceIds(null);
            objectInfo.setRelationshipTargetIds(null);
            objectInfo.setRenditionInfos(null);
            objectInfo.setSupportsDescendants(true);
            objectInfo.setSupportsFolderTree(true);
            objectInfo.setSupportsPolicies(false);
            objectInfo.setSupportsRelationships(false);
            objectInfo.setWorkingCopyId(null);
            objectInfo.setWorkingCopyOriginalId(null);
        } else {
            typeId = FileBridgeCmisTypeId.CMIS_RODA_DOCUMENT.value(); //BaseTypeId.CMIS_DOCUMENT.value();
            objectInfo.setBaseType(BaseTypeId.CMIS_DOCUMENT);
            objectInfo.setTypeId(typeId);
            objectInfo.setHasAcl(true);
            objectInfo.setHasContent(true);
            objectInfo.setHasParent(true);
            objectInfo.setVersionSeriesId(null);
            objectInfo.setIsCurrentVersion(true);
            objectInfo.setRelationshipSourceIds(null);
            objectInfo.setRelationshipTargetIds(null);
            objectInfo.setRenditionInfos(null);
            objectInfo.setSupportsDescendants(false);
            objectInfo.setSupportsFolderTree(false);
            objectInfo.setSupportsPolicies(false);
            objectInfo.setSupportsRelationships(false);
            objectInfo.setWorkingCopyId(null);
            objectInfo.setWorkingCopyOriginalId(null);
        }

        // let's do it
        try {
            PropertiesImpl result = new PropertiesImpl();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // id
            String id = fileToId(file);
            addPropertyId(result, typeId, filter, PropertyIds.OBJECT_ID, id);
            objectInfo.setId(id);
            if (!id.equals(ROOT_ID) && updateProperties) { database.createObject(typeId, id); }

            // name
            String name = file.getName();
            addPropertyString(result, typeId, filter, PropertyIds.NAME, name);
            objectInfo.setName(name);
            if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.NAME, name); }

            // created and modified by
            addPropertyString(result, typeId, filter, PropertyIds.CREATED_BY, USER_UNKNOWN);
            if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.CREATED_BY, USER_UNKNOWN); }
            addPropertyString(result, typeId, filter, PropertyIds.LAST_MODIFIED_BY, USER_UNKNOWN);
            if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.LAST_MODIFIED_BY, USER_UNKNOWN); }
            objectInfo.setCreatedBy(USER_UNKNOWN);

            // creation and modification date
            GregorianCalendar lastModified = FileBridgeUtils.millisToCalendar(file.lastModified());
            addPropertyDateTime(result, typeId, filter, PropertyIds.CREATION_DATE, lastModified);
            if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.CREATION_DATE, sdf.format(lastModified.getTime())); }
            addPropertyDateTime(result, typeId, filter, PropertyIds.LAST_MODIFICATION_DATE, lastModified);
            if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.LAST_MODIFICATION_DATE, sdf.format(lastModified.getTime())); }
            objectInfo.setCreationDate(lastModified);
            objectInfo.setLastModificationDate(lastModified);

            // change token - always null
            addPropertyString(result, typeId, filter, PropertyIds.CHANGE_TOKEN, null);
            if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.CHANGE_TOKEN, null); }

            // CMIS 1.1 properties
            if (context != null && context.getCmisVersion() != CmisVersion.CMIS_1_0) {
                addPropertyString(result, typeId, filter, PropertyIds.DESCRIPTION, null);
                if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.DESCRIPTION, null); }
                addPropertyIdList(result, typeId, filter, PropertyIds.SECONDARY_OBJECT_TYPE_IDS, null);
                if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.SECONDARY_OBJECT_TYPE_IDS, null); }
            }

            // directory or file
            if (file.isDirectory()) {
                // base type and type name
                addPropertyId(result, typeId, filter, PropertyIds.BASE_TYPE_ID, BaseTypeId.CMIS_FOLDER.value());
                if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.BASE_TYPE_ID, BaseTypeId.CMIS_FOLDER.value()); }
                addPropertyId(result, typeId, filter, PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_FOLDER.value());
                if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_FOLDER.value()); }
                String path = getRepositoryPath(file);
                addPropertyString(result, typeId, filter, PropertyIds.PATH, path);
                if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.PATH, path); }

                // folder properties
                if (!root.equals(file)) {
                    addPropertyId(result, typeId, filter, PropertyIds.PARENT_ID,
                            (root.equals(file.getParentFile()) ? ROOT_ID : fileToId(file.getParentFile())));
                    objectInfo.setHasParent(true);
                    if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.PARENT_ID,
                            (root.equals(file.getParentFile()) ? ROOT_ID : fileToId(file.getParentFile()))); }
                } else {
                    addPropertyId(result, typeId, filter, PropertyIds.PARENT_ID, null);
                    objectInfo.setHasParent(false);
                    if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.PARENT_ID, null); }
                }

                addPropertyIdList(result, typeId, filter, PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS, null);
                if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS, null); }
            } else {
                // base type and type name
                addPropertyId(result, typeId, filter, PropertyIds.BASE_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
                if (updateProperties) { database.updateField(typeId, id, PropertyIds.BASE_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value()); }
                addPropertyId(result, typeId, filter, PropertyIds.OBJECT_TYPE_ID, FileBridgeCmisTypeId.CMIS_RODA_DOCUMENT.value());
                //BaseTypeId.CMIS_DOCUMENT.value());
                if (updateProperties) { database.updateField(typeId, id, PropertyIds.OBJECT_TYPE_ID, FileBridgeCmisTypeId.CMIS_RODA_DOCUMENT.value()); }
                String path = getRepositoryPath(file);
                if (!id.equals(ROOT_ID) && updateProperties) { database.updateField(typeId, id, PropertyIds.PATH, path); }

                // load file's metadata from the AIP
                String aipMetadataId = null;
                if ((file.getPath().split("aip/")[1] != null) && (file.getPath().split("aip/")[1].split("/")[0] != null)) {
                    aipMetadataId = file.getPath().split("aip/")[1].split("/")[0];
                }

                if ((aipMetadataId != null) && aipMetadataMap.containsKey(aipMetadataId)) {
                    AipMetadata aipMetadata = aipMetadataMap.get(aipMetadataId);

                    // load EAD metadata into RODA Document properties
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_UNIT_ID.value(),
                            aipMetadata.getEad2002Metadata().getUnitId());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_UNIT_ID.value(),
                            aipMetadata.getEad2002Metadata().getUnitId()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_UNIT_TITLE.value(),
                            aipMetadata.getEad2002Metadata().getUnitTitle());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_UNIT_TITLE.value(),
                            aipMetadata.getEad2002Metadata().getUnitTitle()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_COUNTRY_CODE.value(),
                            aipMetadata.getEad2002Metadata().getCountryCode());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_COUNTRY_CODE.value(),
                            aipMetadata.getEad2002Metadata().getCountryCode()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_REPOSITORY_CODE.value(),
                            aipMetadata.getEad2002Metadata().getRepositoryCode());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_REPOSITORY_CODE.value(),
                            aipMetadata.getEad2002Metadata().getRepositoryCode()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_UNIT_DATE.value(),
                            aipMetadata.getEad2002Metadata().getUnitDate());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_UNIT_DATE.value(),
                            aipMetadata.getEad2002Metadata().getUnitDate()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_UNIT_DATE_LABEL.value(),
                            aipMetadata.getEad2002Metadata().getUnitDateLabel());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_UNIT_DATE_LABEL.value(),
                            aipMetadata.getEad2002Metadata().getUnitDateLabel()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_UNIT_DATE_NORMAL.value(),
                            aipMetadata.getEad2002Metadata().getUnitDateNormal());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_UNIT_DATE_NORMAL.value(),
                            aipMetadata.getEad2002Metadata().getUnitDateNormal()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION.value(),
                            aipMetadata.getEad2002Metadata().getPhysicalDescription());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION.value(),
                            aipMetadata.getEad2002Metadata().getPhysicalDescription()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_EXTENT.value(),
                            aipMetadata.getEad2002Metadata().getPhysicalDescriptionExtent());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_EXTENT.value(),
                            aipMetadata.getEad2002Metadata().getPhysicalDescriptionExtent()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_DIMENSIONS.value(),
                            aipMetadata.getEad2002Metadata().getPhysicalDescriptionDimensions());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_DIMENSIONS.value(),
                            aipMetadata.getEad2002Metadata().getPhysicalDescriptionDimensions()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_APPEARANCE.value(),
                            aipMetadata.getEad2002Metadata().getPhysicalDescriptionAppearance());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_PHYSICAL_DESCRIPTION_APPEARANCE.value(),
                            aipMetadata.getEad2002Metadata().getPhysicalDescriptionAppearance()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_REPOSITORY_NAME.value(),
                            aipMetadata.getEad2002Metadata().getRepositoryName());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_REPOSITORY_NAME.value(),
                            aipMetadata.getEad2002Metadata().getRepositoryName()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_LANG_MATERIAL.value(),
                            aipMetadata.getEad2002Metadata().getLangMaterial());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_LANG_MATERIAL.value(),
                            aipMetadata.getEad2002Metadata().getLangMaterial()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_LANG_MATERIAL_LANGUAGE.value(),
                            aipMetadata.getEad2002Metadata().getLangMaterialLanguage());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_LANG_MATERIAL_LANGUAGE.value(),
                            aipMetadata.getEad2002Metadata().getLangMaterialLanguage()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_NOTE_SOURCE_DESCRIPTION.value(),
                            aipMetadata.getEad2002Metadata().getNoteSourcesDescription());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_NOTE_SOURCE_DESCRIPTION.value(),
                            aipMetadata.getEad2002Metadata().getNoteSourcesDescription()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_NOTE_GENERAL_NOTE.value(),
                            aipMetadata.getEad2002Metadata().getNoteGeneralNote());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_NOTE_GENERAL_NOTE.value(),
                            aipMetadata.getEad2002Metadata().getNoteGeneralNote()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_ORIGINATION.value(),
                            aipMetadata.getEad2002Metadata().getOrigination());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_ORIGINATION.value(),
                            aipMetadata.getEad2002Metadata().getOrigination()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_ORIGINATION_CREATION.value(),
                            aipMetadata.getEad2002Metadata().getOriginationCreator());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_ORIGINATION_CREATION.value(),
                            aipMetadata.getEad2002Metadata().getOriginationCreator()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_ORIGINATION_PRODUCTION.value(),
                            aipMetadata.getEad2002Metadata().getOriginationProducer());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_ORIGINATION_PRODUCTION.value(),
                            aipMetadata.getEad2002Metadata().getOriginationProducer()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_ARCHIVE_DESCRIPTION.value(),
                            aipMetadata.getEad2002Metadata().getArchiveDescription());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_ARCHIVE_DESCRIPTION.value(),
                            aipMetadata.getEad2002Metadata().getArchiveDescription()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_MATERIAL_SPECIFICATION.value(),
                            aipMetadata.getEad2002Metadata().getMaterialSpecification());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_MATERIAL_SPECIFICATION.value(),
                            aipMetadata.getEad2002Metadata().getMaterialSpecification()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_ODD_LEVEL_OF_DETAIL.value(),
                            aipMetadata.getEad2002Metadata().getOddLevelOfDetail());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_ODD_LEVEL_OF_DETAIL.value(),
                            aipMetadata.getEad2002Metadata().getOddLevelOfDetail()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_ODD_STATUS_DESCRIPTION.value(),
                            aipMetadata.getEad2002Metadata().getOddStatusDescription());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_ODD_STATUS_DESCRIPTION.value(),
                            aipMetadata.getEad2002Metadata().getOddStatusDescription()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_SCOPE_CONTENT.value(),
                            aipMetadata.getEad2002Metadata().getScopeContent());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_SCOPE_CONTENT.value(),
                            aipMetadata.getEad2002Metadata().getScopeContent()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_ARRANGEMENT.value(),
                            aipMetadata.getEad2002Metadata().getArrangement());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_ARRANGEMENT.value(),
                            aipMetadata.getEad2002Metadata().getArrangement()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_APPRAISAL.value(),
                            aipMetadata.getEad2002Metadata().getAppraisal());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_APPRAISAL.value(),
                            aipMetadata.getEad2002Metadata().getAppraisal()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_ACQUISITION_INFO.value(),
                            aipMetadata.getEad2002Metadata().getAcquisitionInfo());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_ACQUISITION_INFO.value(),
                            aipMetadata.getEad2002Metadata().getAcquisitionInfo()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_ACCRUALS.value(),
                            aipMetadata.getEad2002Metadata().getAccruals());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_ACCRUALS.value(),
                            aipMetadata.getEad2002Metadata().getAccruals()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_CUSTODIAL_HISTORY.value(),
                            aipMetadata.getEad2002Metadata().getCustodialHistory());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_CUSTODIAL_HISTORY.value(),
                            aipMetadata.getEad2002Metadata().getCustodialHistory()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_PROCESS_INFO_DATE.value(),
                            aipMetadata.getEad2002Metadata().getProcessInfoDate());
                    if (updateProperties) {
                        try {
                            SimpleDateFormat sdfProcessInfoDate = new SimpleDateFormat("yyyy-MM-dd");
                            Date parseProcessInfoDate = sdfProcessInfoDate.parse(aipMetadata.getEad2002Metadata().getProcessInfoDate());
                            database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_PROCESS_INFO_DATE.value(),
                                    sdf.format(parseProcessInfoDate));
                        } catch (ParseException ignored ) { /* fail silently */ }
                    }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_PROCESS_INFO_ARCHIVIST_NOTES.value(),
                            aipMetadata.getEad2002Metadata().getProcessInfoArchivistNotes());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_PROCESS_INFO_ARCHIVIST_NOTES.value(),
                            aipMetadata.getEad2002Metadata().getProcessInfoArchivistNotes()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_ORIGINALS_LOCATION.value(),
                            aipMetadata.getEad2002Metadata().getOriginalsLocation());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_ORIGINALS_LOCATION.value(),
                            aipMetadata.getEad2002Metadata().getOriginalsLocation()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_ALTERNATIVE_FORM_AVAILABLE.value(),
                            aipMetadata.getEad2002Metadata().getAlternativeFormAvailable());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_ALTERNATIVE_FORM_AVAILABLE.value(),
                            aipMetadata.getEad2002Metadata().getAlternativeFormAvailable()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_RELATED_MATERIAL.value(),
                            aipMetadata.getEad2002Metadata().getRelatedMaterial());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_RELATED_MATERIAL.value(),
                            aipMetadata.getEad2002Metadata().getRelatedMaterial()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_ACCESS_RESTRICTIONS.value(),
                            aipMetadata.getEad2002Metadata().getAccessRestrictions());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_ACCESS_RESTRICTIONS.value(),
                            aipMetadata.getEad2002Metadata().getAccessRestrictions()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_USE_RESTRICTIONS.value(),
                            aipMetadata.getEad2002Metadata().getUseRestrictions());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_USE_RESTRICTIONS.value(),
                            aipMetadata.getEad2002Metadata().getUseRestrictions()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_OTHER_FIND_AID.value(),
                            aipMetadata.getEad2002Metadata().getOtherFindAid());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_OTHER_FIND_AID.value(),
                            aipMetadata.getEad2002Metadata().getOtherFindAid()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_PHYSICAL_TECH.value(),
                            aipMetadata.getEad2002Metadata().getPhysicalTech());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_PHYSICAL_TECH.value(),
                            aipMetadata.getEad2002Metadata().getPhysicalTech()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_BIBLIOGRAPHY.value(),
                            aipMetadata.getEad2002Metadata().getBibliography());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_BIBLIOGRAPHY.value(),
                            aipMetadata.getEad2002Metadata().getBibliography()); }
                    addPropertyString(result, typeId, filter, MetadataEadFieldId.METADATA_EAD_PREFER_CITE.value(),
                            aipMetadata.getEad2002Metadata().getPreferCite());
                    if (updateProperties) { database.updateField(typeId, id, MetadataEadFieldId.METADATA_EAD_PREFER_CITE.value(),
                            aipMetadata.getEad2002Metadata().getPreferCite()); }

                    // load Dublin Core metadata into RODA Document properties
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_TITLE.value(),
                            aipMetadata.getDublinCore20021212Metadata().getTitle());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_TITLE.value(),
                            aipMetadata.getDublinCore20021212Metadata().getTitle()); }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_IDENTIFIER.value(),
                            aipMetadata.getDublinCore20021212Metadata().getIdentifier());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_IDENTIFIER.value(),
                            aipMetadata.getDublinCore20021212Metadata().getIdentifier()); }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_CREATOR.value(),
                            aipMetadata.getDublinCore20021212Metadata().getCreator());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_CREATOR.value(),
                            aipMetadata.getDublinCore20021212Metadata().getCreator()); }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_INITIAL_DATE.value(),
                            aipMetadata.getDublinCore20021212Metadata().getInitialDate());
                    if (updateProperties) {
                        try {
                            SimpleDateFormat sdfInitialDate = new SimpleDateFormat("yyyy-MM-dd");
                            Date parseInitialDate = sdfInitialDate.parse(aipMetadata.getDublinCore20021212Metadata().getInitialDate());
                            database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_INITIAL_DATE.value(),
                                    sdf.format(parseInitialDate));
                        } catch (ParseException ignored ) { /* fail silently */ }
                    }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_FINAL_DATE.value(),
                            aipMetadata.getDublinCore20021212Metadata().getFinalDate());
                    if (updateProperties) {
                        try {
                            SimpleDateFormat sdfFinalDate = new SimpleDateFormat("yyyy-MM-dd");
                            Date parseFinalDate = sdfFinalDate.parse(aipMetadata.getDublinCore20021212Metadata().getFinalDate());
                            database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_FINAL_DATE.value(),
                                    sdf.format(parseFinalDate));
                        } catch (ParseException ignored ) { /* fail silently */ }
                    }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_DESCRIPTION.value(),
                            aipMetadata.getDublinCore20021212Metadata().getDescription());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_DESCRIPTION.value(),
                            aipMetadata.getDublinCore20021212Metadata().getDescription()); }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_PUBLISHER.value(),
                            aipMetadata.getDublinCore20021212Metadata().getPublisher());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_PUBLISHER.value(),
                            aipMetadata.getDublinCore20021212Metadata().getPublisher()); }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_CONTRIBUTOR.value(),
                            aipMetadata.getDublinCore20021212Metadata().getContributor());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_CONTRIBUTOR.value(),
                            aipMetadata.getDublinCore20021212Metadata().getContributor()); }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_RIGHTS.value(),
                            aipMetadata.getDublinCore20021212Metadata().getRights());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_RIGHTS.value(),
                            aipMetadata.getDublinCore20021212Metadata().getRights()); }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_LANGUAGE.value(),
                            aipMetadata.getDublinCore20021212Metadata().getLanguage());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_LANGUAGE.value(),
                            aipMetadata.getDublinCore20021212Metadata().getLanguage()); }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_COVERAGE.value(),
                            aipMetadata.getDublinCore20021212Metadata().getCoverage());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_COVERAGE.value(),
                            aipMetadata.getDublinCore20021212Metadata().getCoverage()); }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_FORMAT.value(),
                            aipMetadata.getDublinCore20021212Metadata().getFormat());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_FORMAT.value(),
                            aipMetadata.getDublinCore20021212Metadata().getFormat()); }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_RELATION.value(),
                            aipMetadata.getDublinCore20021212Metadata().getRelation());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_RELATION.value(),
                            aipMetadata.getDublinCore20021212Metadata().getRelation()); }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_SUBJECT.value(),
                            aipMetadata.getDublinCore20021212Metadata().getSubject());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_SUBJECT.value(),
                            aipMetadata.getDublinCore20021212Metadata().getSubject()); }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_TYPE.value(),
                            aipMetadata.getDublinCore20021212Metadata().getType());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_TYPE.value(),
                            aipMetadata.getDublinCore20021212Metadata().getType()); }
                    addPropertyString(result, typeId, filter, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_SOURCE.value(),
                            aipMetadata.getDublinCore20021212Metadata().getSource());
                    if (updateProperties) { database.updateField(typeId, id, MetadataDublinCoreFieldId.METADATA_DUBLIN_CORE_SOURCE.value(),
                            aipMetadata.getDublinCore20021212Metadata().getSource()); }

                    // load Key-Value metadata into RODA Document properties
                    addPropertyString(result, typeId, filter, MetadataKeyValueFieldId.METADATA_KEY_VALUE_ID.value(),
                            aipMetadata.getKeyValueMetadata().getId());
                    if (updateProperties) { database.updateField(typeId, id, MetadataKeyValueFieldId.METADATA_KEY_VALUE_ID.value(),
                            aipMetadata.getKeyValueMetadata().getId()); }
                    addPropertyString(result, typeId, filter, MetadataKeyValueFieldId.METADATA_KEY_VALUE_TITLE.value(),
                            aipMetadata.getKeyValueMetadata().getTitle());
                    if (updateProperties) { database.updateField(typeId, id, MetadataKeyValueFieldId.METADATA_KEY_VALUE_TITLE.value(),
                            aipMetadata.getKeyValueMetadata().getTitle()); }
                    addPropertyString(result, typeId, filter, MetadataKeyValueFieldId.METADATA_KEY_VALUE_PRODUCER.value(),
                            aipMetadata.getKeyValueMetadata().getProducer());
                    if (updateProperties) { database.updateField(typeId, id, MetadataKeyValueFieldId.METADATA_KEY_VALUE_PRODUCER.value(),
                            aipMetadata.getKeyValueMetadata().getProducer()); }
                    addPropertyString(result, typeId, filter, MetadataKeyValueFieldId.METADATA_KEY_VALUE_DATE.value(),
                            aipMetadata.getKeyValueMetadata().getDate());
                    if (updateProperties) {
                        try {
                            SimpleDateFormat sdfKeyValueDate = new SimpleDateFormat("yyyy-MM-dd");
                            Date parseKeyValueDate = sdfKeyValueDate.parse(aipMetadata.getKeyValueMetadata().getDate());
                            database.updateField(typeId, id, MetadataKeyValueFieldId.METADATA_KEY_VALUE_DATE.value(),
                                    sdf.format(parseKeyValueDate));
                        } catch (ParseException ignored ) { /* fail silently */ }
                    }
                }

                // file properties
                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_IMMUTABLE, false);
                if (updateProperties) { database.updateField(typeId, id, PropertyIds.IS_IMMUTABLE, "0"); }

                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_LATEST_VERSION, true);
                if (updateProperties) { database.updateField(typeId, id, PropertyIds.IS_LATEST_VERSION, "1"); }

                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_MAJOR_VERSION, true);
                if (updateProperties) { database.updateField(typeId, id, PropertyIds.IS_MAJOR_VERSION, "1"); }

                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_LATEST_MAJOR_VERSION, true);
                if (updateProperties) { database.updateField(typeId, id, PropertyIds.IS_LATEST_MAJOR_VERSION, "1"); }

                addPropertyString(result, typeId, filter, PropertyIds.VERSION_LABEL, file.getName());
                if (updateProperties) { database.updateField(typeId, id, PropertyIds.VERSION_LABEL, file.getName()); }

                addPropertyId(result, typeId, filter, PropertyIds.VERSION_SERIES_ID, fileToId(file));
                if (updateProperties) { database.updateField(typeId, id, PropertyIds.VERSION_SERIES_ID, fileToId(file)); }

                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, false);
                if (updateProperties) { database.updateField(typeId, id, PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, "0"); }

                addPropertyString(result, typeId, filter, PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null);
                if (updateProperties) { database.updateField(typeId, id, PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null); }

                addPropertyString(result, typeId, filter, PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null);
                if (updateProperties) { database.updateField(typeId, id, PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null); }

                addPropertyString(result, typeId, filter, PropertyIds.CHECKIN_COMMENT, "");
                if (updateProperties) { database.updateField(typeId, id, PropertyIds.CHECKIN_COMMENT, ""); }

                if (context != null && context.getCmisVersion() != CmisVersion.CMIS_1_0) {
                    addPropertyBoolean(result, typeId, filter, PropertyIds.IS_PRIVATE_WORKING_COPY, false);
                    if (updateProperties) { database.updateField(typeId, id, PropertyIds.IS_PRIVATE_WORKING_COPY, "0"); }
                }

                if (file.length() == 0) {
                    addPropertyBigInteger(result, typeId, filter, PropertyIds.CONTENT_STREAM_LENGTH, null);
                    if (updateProperties) { database.updateField(typeId, id, PropertyIds.CONTENT_STREAM_LENGTH, null); }
                    addPropertyString(result, typeId, filter, PropertyIds.CONTENT_STREAM_MIME_TYPE, null);
                    if (updateProperties) { database.updateField(typeId, id, PropertyIds.CONTENT_STREAM_MIME_TYPE, null); }
                    addPropertyString(result, typeId, filter, PropertyIds.CONTENT_STREAM_FILE_NAME, null);
                    if (updateProperties) { database.updateField(typeId, id, PropertyIds.CONTENT_STREAM_FILE_NAME, null); }
                    objectInfo.setHasContent(false);
                    objectInfo.setContentType(null);
                    objectInfo.setFileName(null);
                } else {
                    addPropertyInteger(result, typeId, filter, PropertyIds.CONTENT_STREAM_LENGTH, file.length());
                    if (updateProperties) { database.updateField(typeId, id, PropertyIds.CONTENT_STREAM_LENGTH, String.valueOf(file.length())); }
                    addPropertyString(result, typeId, filter, PropertyIds.CONTENT_STREAM_MIME_TYPE, MimeTypes.getMIMEType(file));
                    if (updateProperties) { database.updateField(typeId, id, PropertyIds.CONTENT_STREAM_MIME_TYPE, MimeTypes.getMIMEType(file)); }
                    addPropertyString(result, typeId, filter, PropertyIds.CONTENT_STREAM_FILE_NAME, file.getName());
                    if (updateProperties) { database.updateField(typeId, id, PropertyIds.CONTENT_STREAM_FILE_NAME, file.getName()); }
                    objectInfo.setHasContent(true);
                    objectInfo.setContentType(MimeTypes.getMIMEType(file));
                    objectInfo.setFileName(file.getName());
                }

                addPropertyId(result, typeId, filter, PropertyIds.CONTENT_STREAM_ID, null);
                if (updateProperties) { database.updateField(typeId, id, PropertyIds.CONTENT_STREAM_ID, null); }
            }

            return result;
        } catch (CmisBaseException cbe) {
            throw cbe;
        } catch (Exception e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }


    // -----------------------------------------------------------------------------------------------------------------
    // --- André Rosa Custom Implementations :: END --------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------

    private RepositoryInfo createRepositoryInfo(CmisVersion cmisVersion) {
        assert cmisVersion != null;

        RepositoryInfoImpl repositoryInfo = new RepositoryInfoImpl();

        repositoryInfo.setId(repositoryId);
        repositoryInfo.setName(repositoryId);
        repositoryInfo.setDescription(repositoryId);

        repositoryInfo.setCmisVersionSupported(cmisVersion.value());

        repositoryInfo.setProductName("FileBridge Server");
        repositoryInfo.setProductVersion("1.0");
        repositoryInfo.setVendorName("My Company");

        repositoryInfo.setRootFolder(ROOT_ID);

        repositoryInfo.setThinClientUri("");
        repositoryInfo.setChangesIncomplete(true);

        RepositoryCapabilitiesImpl capabilities = new RepositoryCapabilitiesImpl();
        capabilities.setCapabilityAcl(CapabilityAcl.DISCOVER);
        capabilities.setAllVersionsSearchable(false);
        capabilities.setCapabilityJoin(CapabilityJoin.NONE);
        capabilities.setSupportsMultifiling(false);
        capabilities.setSupportsUnfiling(false);
        capabilities.setSupportsVersionSpecificFiling(false);
        capabilities.setIsPwcSearchable(false);
        capabilities.setIsPwcUpdatable(false);
        capabilities.setCapabilityQuery(CapabilityQuery.METADATAONLY);
        capabilities.setCapabilityChanges(CapabilityChanges.NONE);
        capabilities
                .setCapabilityContentStreamUpdates(CapabilityContentStreamUpdates.ANYTIME);
        capabilities.setSupportsGetDescendants(true);
        capabilities.setSupportsGetFolderTree(true);
        capabilities.setCapabilityRendition(CapabilityRenditions.NONE);

        if (cmisVersion != CmisVersion.CMIS_1_0) {
            capabilities.setOrderByCapability(CapabilityOrderBy.NONE);

            NewTypeSettableAttributesImpl typeSetAttributes = new NewTypeSettableAttributesImpl();
            typeSetAttributes.setCanSetControllableAcl(false);
            typeSetAttributes.setCanSetControllablePolicy(false);
            typeSetAttributes.setCanSetCreatable(false);
            typeSetAttributes.setCanSetDescription(false);
            typeSetAttributes.setCanSetDisplayName(false);
            typeSetAttributes.setCanSetFileable(false);
            typeSetAttributes.setCanSetFulltextIndexed(false);
            typeSetAttributes.setCanSetId(false);
            typeSetAttributes.setCanSetIncludedInSupertypeQuery(false);
            typeSetAttributes.setCanSetLocalName(false);
            typeSetAttributes.setCanSetLocalNamespace(false);
            typeSetAttributes.setCanSetQueryable(false);
            typeSetAttributes.setCanSetQueryName(false);

            capabilities.setNewTypeSettableAttributes(typeSetAttributes);

            CreatablePropertyTypesImpl creatablePropertyTypes = new CreatablePropertyTypesImpl();
            capabilities.setCreatablePropertyTypes(creatablePropertyTypes);
        }

        repositoryInfo.setCapabilities(capabilities);

        AclCapabilitiesDataImpl aclCapability = new AclCapabilitiesDataImpl();
        aclCapability.setSupportedPermissions(SupportedPermissions.BASIC);
        aclCapability.setAclPropagation(AclPropagation.OBJECTONLY);

        // permissions
        List<PermissionDefinition> permissions = new ArrayList<PermissionDefinition>();
        permissions.add(createPermission(CMIS_READ, "Read"));
        permissions.add(createPermission(CMIS_WRITE, "Write"));
        permissions.add(createPermission(CMIS_ALL, "All"));
        aclCapability.setPermissionDefinitionData(permissions);

        // mapping
        List<PermissionMapping> list = new ArrayList<PermissionMapping>();
        list.add(createMapping(PermissionMapping.CAN_CREATE_DOCUMENT_FOLDER,
                CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_CREATE_FOLDER_FOLDER,
                CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_DELETE_CONTENT_DOCUMENT,
                CMIS_WRITE));
        list.add(createMapping(PermissionMapping.CAN_DELETE_OBJECT, CMIS_ALL));
        list.add(createMapping(PermissionMapping.CAN_DELETE_TREE_FOLDER,
                CMIS_ALL));
        list.add(createMapping(PermissionMapping.CAN_GET_ACL_OBJECT, CMIS_READ));
        list.add(createMapping(
                PermissionMapping.CAN_GET_ALL_VERSIONS_VERSION_SERIES,
                CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_GET_CHILDREN_FOLDER,
                CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_GET_DESCENDENTS_FOLDER,
                CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_GET_FOLDER_PARENT_OBJECT,
                CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_GET_PARENTS_FOLDER,
                CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_GET_PROPERTIES_OBJECT,
                CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_MOVE_OBJECT, CMIS_WRITE));
        list.add(createMapping(PermissionMapping.CAN_MOVE_SOURCE, CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_MOVE_TARGET, CMIS_WRITE));
        list.add(createMapping(PermissionMapping.CAN_SET_CONTENT_DOCUMENT,
                CMIS_WRITE));
        list.add(createMapping(PermissionMapping.CAN_UPDATE_PROPERTIES_OBJECT,
                CMIS_WRITE));
        list.add(createMapping(PermissionMapping.CAN_VIEW_CONTENT_OBJECT,
                CMIS_READ));
        Map<String, PermissionMapping> map = new LinkedHashMap<String, PermissionMapping>();
        for (PermissionMapping pm : list) {
            map.put(pm.getKey(), pm);
        }
        aclCapability.setPermissionMappingData(map);

        repositoryInfo.setAclCapabilities(aclCapability);

        return repositoryInfo;
    }

    private PermissionDefinition createPermission(String permission, String description) {
        PermissionDefinitionDataImpl pd = new PermissionDefinitionDataImpl();
        pd.setId(permission);
        pd.setDescription(description);

        return pd;
    }

    private PermissionMapping createMapping(String key, String permission) {
        PermissionMappingDataImpl pm = new PermissionMappingDataImpl();
        pm.setKey(key);
        pm.setPermissions(Collections.singletonList(permission));

        return pm;
    }

    /**
     * Returns the id of this repository.
     */
    public String getRepositoryId() {
        return repositoryId;
    }

    /**
     * Returns the root directory of this repository
     */
    public File getRootDirectory() {
        return root;
    }

    /**
     * Sets read-only flag for the given user.
     */
    public void setUserReadOnly(String user) {
        if (user == null || user.length() == 0) {
            return;
        }

        readWriteUserMap.put(user, true);
    }

    /**
     * Sets read-write flag for the given user.
     */
    public void setUserReadWrite(String user) {
        if (user == null || user.length() == 0) {
            return;
        }

        readWriteUserMap.put(user, false);
    }

    // --- CMIS operations ---

    /**
     * CMIS getRepositoryInfo.
     */
    public RepositoryInfo getRepositoryInfo(CallContext context) {
        checkUser(context, false);

        if (context.getCmisVersion() == CmisVersion.CMIS_1_0) {
            return repositoryInfo10;
        } else {
            return repositoryInfo11;
        }
    }

    /**
     * CMIS getTypesChildren.
     */
    public TypeDefinitionList getTypeChildren(CallContext context,
                                              String typeId, Boolean includePropertyDefinitions,
                                              BigInteger maxItems, BigInteger skipCount) {
        checkUser(context, false);

        return typeManager.getTypeChildren(context, typeId,
                includePropertyDefinitions, maxItems, skipCount);
    }

    /**
     * CMIS getTypesDescendants.
     */
    public List<TypeDefinitionContainer> getTypeDescendants(
            CallContext context, String typeId, BigInteger depth,
            Boolean includePropertyDefinitions) {
        checkUser(context, false);

        return typeManager.getTypeDescendants(context, typeId, depth,
                includePropertyDefinitions);
    }

    /**
     * CMIS getTypeDefinition.
     */
    public TypeDefinition getTypeDefinition(CallContext context, String typeId) {
        checkUser(context, false);

        return typeManager.getTypeDefinition(context, typeId);
    }

    /**
     * Create* dispatch for AtomPub.
     */
    public ObjectData create(CallContext context, Properties properties,
                             String folderId, ContentStream contentStream,
                             VersioningState versioningState, ObjectInfoHandler objectInfos) {
        boolean userReadOnly = checkUser(context, true);

        String typeId = FileBridgeUtils.getObjectTypeId(properties);
        TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
        if (type == null) {
            throw new CmisObjectNotFoundException("Type '" + typeId
                    + "' is unknown!");
        }

        String objectId = null;
        if (type.getBaseTypeId() == BaseTypeId.CMIS_DOCUMENT) {
            objectId = createDocument(context, properties, folderId,
                    contentStream, versioningState);
        } else if (type.getBaseTypeId() == BaseTypeId.CMIS_FOLDER) {
            objectId = createFolder(context, properties, folderId);
        } else {
            throw new CmisObjectNotFoundException(
                    "Cannot create object of type '" + typeId + "'!");
        }

        return compileObjectData(context, getFile(objectId), null, false,
                false, userReadOnly, objectInfos);
    }

    /**
     * CMIS createDocument.
     */
    public String createDocument(CallContext context, Properties properties,
                                 String folderId, ContentStream contentStream,
                                 VersioningState versioningState) {
        checkUser(context, true);

        // check versioning state
        if (VersioningState.NONE != versioningState) {
            throw new CmisConstraintException("Versioning not supported!");
        }

        // get parent File
        File parent = getFile(folderId);
        if (!parent.isDirectory()) {
            throw new CmisObjectNotFoundException("Parent is not a folder!");
        }

        // check properties
        checkNewProperties(properties);

        // check the file
        String name = FileBridgeUtils.getStringProperty(properties,
                PropertyIds.NAME);
        File newFile = new File(parent, name);
        if (newFile.exists()) {
            throw new CmisNameConstraintViolationException(
                    "Document already exists!");
        }

        // create the file
        try {
            newFile.createNewFile();
        } catch (IOException e) {
            throw new CmisStorageException("Could not create file: "
                    + e.getMessage(), e);
        }

        // write content, if available
        if (contentStream != null && contentStream.getStream() != null) {
            writeContent(newFile, contentStream.getStream());
        }

        return getId(newFile);
    }

    /**
     * CMIS createDocumentFromSource.
     */
    public String createDocumentFromSource(CallContext context,
                                           String sourceId, Properties properties, String folderId,
                                           VersioningState versioningState) {
        checkUser(context, true);

        // check versioning state
        if (VersioningState.NONE != versioningState) {
            throw new CmisConstraintException("Versioning not supported!");
        }

        // get parent File
        File parent = getFile(folderId);
        if (!parent.isDirectory()) {
            throw new CmisObjectNotFoundException("Parent is not a folder!");
        }

        // get source File
        File source = getFile(sourceId);
        if (!source.isFile()) {
            throw new CmisObjectNotFoundException("Source is not a document!");
        }

        // check properties
        checkCopyProperties(properties, BaseTypeId.CMIS_DOCUMENT.value());

        // check the name
        String name = null;
        if (properties != null && properties.getProperties() != null) {
            name = FileBridgeUtils.getStringProperty(properties,
                    PropertyIds.NAME);
        }
        if (name == null) {
            name = source.getName();
        }

        File newFile = new File(parent, name);
        if (newFile.exists()) {
            throw new CmisNameConstraintViolationException(
                    "Document already exists.");
        }

        // create the file
        try {
            newFile.createNewFile();
        } catch (IOException e) {
            throw new CmisStorageException("Could not create file: "
                    + e.getMessage(), e);
        }

        // copy content
        try {
            writeContent(newFile, new FileInputStream(source));
        } catch (IOException e) {
            throw new CmisStorageException("Could not roead or write content: "
                    + e.getMessage(), e);
        }

        return getId(newFile);
    }

    /**
     * Writes the content to disc.
     */
    private void writeContent(File newFile, InputStream stream) {
        OutputStream out = null;
        InputStream in = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(newFile),
                    BUFFER_SIZE);
            in = new BufferedInputStream(stream, BUFFER_SIZE);

            byte[] buffer = new byte[BUFFER_SIZE];
            int b;
            while ((b = in.read(buffer)) > -1) {
                out.write(buffer, 0, b);
            }

            out.flush();
        } catch (IOException e) {
            throw new CmisStorageException("Could not write content: "
                    + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * CMIS createFolder.
     */
    public String createFolder(CallContext context, Properties properties,
                               String folderId) {
        checkUser(context, true);

        // check properties
        checkNewProperties(properties);

        // get parent File
        File parent = getFile(folderId);
        if (!parent.isDirectory()) {
            throw new CmisObjectNotFoundException("Parent is not a folder!");
        }

        // create the folder
        String name = FileBridgeUtils.getStringProperty(properties,
                PropertyIds.NAME);
        File newFolder = new File(parent, name);
        if (!newFolder.mkdir()) {
            throw new CmisStorageException("Could not create folder!");
        }

        return getId(newFolder);
    }

    /**
     * CMIS moveObject.
     */
    public ObjectData moveObject(CallContext context, Holder<String> objectId,
                                 String targetFolderId, ObjectInfoHandler objectInfos) {
        boolean userReadOnly = checkUser(context, true);

        if (objectId == null) {
            throw new CmisInvalidArgumentException("Id is not valid!");
        }

        // get the file and parent
        File file = getFile(objectId.getValue());
        File parent = getFile(targetFolderId);

        // build new path
        File newFile = new File(parent, file.getName());
        if (newFile.exists()) {
            throw new CmisStorageException("Object already exists!");
        }

        // move it
        if (!file.renameTo(newFile)) {
            throw new CmisStorageException("Move failed!");
        } else {
            // set new id
            objectId.setValue(getId(newFile));
        }

        return compileObjectData(context, newFile, null, false, false,
                userReadOnly, objectInfos);
    }

    /**
     * CMIS setContentStream, deleteContentStream, and appendContentStream.
     */
    public void changeContentStream(CallContext context,
                                    Holder<String> objectId, Boolean overwriteFlag,
                                    ContentStream contentStream, boolean append) {
        checkUser(context, true);

        if (objectId == null) {
            throw new CmisInvalidArgumentException("Id is not valid!");
        }

        // get the file
        File file = getFile(objectId.getValue());
        if (!file.isFile()) {
            throw new CmisStreamNotSupportedException("Not a file!");
        }

        // check overwrite
        boolean owf = FileBridgeUtils.getBooleanParameter(overwriteFlag, true);
        if (!owf && file.length() > 0) {
            throw new CmisContentAlreadyExistsException(
                    "Content already exists!");
        }

        OutputStream out = null;
        InputStream in = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file, append),
                    BUFFER_SIZE);

            if (contentStream == null || contentStream.getStream() == null) {
                // delete content
                out.write(new byte[0]);
            } else {
                // set content
                in = new BufferedInputStream(contentStream.getStream(),
                        BUFFER_SIZE);

                byte[] buffer = new byte[BUFFER_SIZE];
                int b;
                while ((b = in.read(buffer)) > -1) {
                    out.write(buffer, 0, b);
                }
            }
        } catch (Exception e) {
            throw new CmisStorageException("Could not write content: "
                    + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * CMIS deleteObject.
     */
    public void deleteObject(CallContext context, String objectId) {
        checkUser(context, true);

        // get the file or folder
        File file = getFile(objectId);
        if (!file.exists()) {
            throw new CmisObjectNotFoundException("Object not found!");
        }

        // check if it is a folder and if it is empty
        if (!isFolderEmpty(file)) {
            throw new CmisConstraintException("Folder is not empty!");
        }

        // delete file
        if (!file.delete()) {
            throw new CmisStorageException("Deletion failed!");
        }
    }

    /**
     * CMIS deleteTree.
     */
    public FailedToDeleteData deleteTree(CallContext context, String folderId,
                                         Boolean continueOnFailure) {
        checkUser(context, true);

        boolean cof = FileBridgeUtils.getBooleanParameter(continueOnFailure,
                false);

        // get the file or folder
        File file = getFile(folderId);

        FailedToDeleteDataImpl result = new FailedToDeleteDataImpl();
        result.setIds(new ArrayList<String>());

        // if it is a folder, remove it recursively
        if (file.isDirectory()) {
            deleteFolder(file, cof, result);
        } else {
            throw new CmisConstraintException("Object is not a folder!");
        }

        return result;
    }

    /**
     * Removes a folder and its content.
     */
    private boolean deleteFolder(File folder, boolean continueOnFailure,
                                 FailedToDeleteDataImpl ftd) {
        boolean success = true;

        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                if (!deleteFolder(file, continueOnFailure, ftd)) {
                    if (!continueOnFailure) {
                        return false;
                    }
                    success = false;
                }
            } else {
                if (!file.delete()) {
                    ftd.getIds().add(getId(file));
                    if (!continueOnFailure) {
                        return false;
                    }
                    success = false;
                }
            }
        }

        if (!folder.delete()) {
            ftd.getIds().add(getId(folder));
            success = false;
        }

        return success;
    }

    /**
     * CMIS updateProperties.
     */
    public ObjectData updateProperties(CallContext context,
                                       Holder<String> objectId, Properties properties,
                                       ObjectInfoHandler objectInfos) {
        boolean userReadOnly = checkUser(context, true);

        // check object id
        if (objectId == null || objectId.getValue() == null) {
            throw new CmisInvalidArgumentException("Id is not valid!");
        }

        // get the file or folder
        File file = getFile(objectId.getValue());

        // check the properties
        String typeId = (file.isDirectory() ? BaseTypeId.CMIS_FOLDER.value()
                : BaseTypeId.CMIS_DOCUMENT.value());
        checkUpdateProperties(properties, typeId);

        // get and check the new name
        String newName = FileBridgeUtils.getStringProperty(properties,
                PropertyIds.NAME);
        boolean isRename = (newName != null)
                && (!file.getName().equals(newName));
        if (isRename && !isValidName(newName)) {
            throw new CmisNameConstraintViolationException("Name is not valid!");
        }

        // rename file or folder if necessary
        File newFile = file;
        if (isRename) {
            File parent = file.getParentFile();
            newFile = new File(parent, newName);
            if (!file.renameTo(newFile)) {
                // if something went wrong, throw an exception
                throw new CmisUpdateConflictException(
                        "Could not rename object!");
            } else {
                // set new id
                objectId.setValue(getId(newFile));
            }
        }

        return compileObjectData(context, newFile, null, false, false,
                userReadOnly, objectInfos);
    }

    /**
     * CMIS bulkUpdateProperties.
     */
    public List<BulkUpdateObjectIdAndChangeToken> bulkUpdateProperties(
            CallContext context,
            List<BulkUpdateObjectIdAndChangeToken> objectIdAndChangeToken,
            Properties properties, ObjectInfoHandler objectInfos) {
        checkUser(context, true);

        if (objectIdAndChangeToken == null) {
            throw new CmisInvalidArgumentException("No object ids provided!");
        }

        List<BulkUpdateObjectIdAndChangeToken> result = new ArrayList<BulkUpdateObjectIdAndChangeToken>();

        for (BulkUpdateObjectIdAndChangeToken oid : objectIdAndChangeToken) {
            if (oid == null) {
                // ignore invalid ids
                continue;
            }
            try {
                Holder<String> oidHolder = new Holder<String>(oid.getId());
                updateProperties(context, oidHolder, properties, objectInfos);

                result.add(new BulkUpdateObjectIdAndChangeTokenImpl(
                        oid.getId(), oidHolder.getValue(), null));
            } catch (CmisBaseException e) {
                // ignore exceptions - see specification
            }
        }

        return result;
    }

    /**
     * CMIS getObject.
     */
    public ObjectData getObject(CallContext context, String objectId,
                                String versionServicesId, String filter,
                                Boolean includeAllowableActions, Boolean includeAcl,
                                ObjectInfoHandler objectInfos) {
        boolean userReadOnly = checkUser(context, false);

        // check id
        if (objectId == null && versionServicesId == null) {
            throw new CmisInvalidArgumentException("Object Id must be set.");
        }

        if (objectId == null) {
            // this works only because there are no versions in a file system
            // and the object id and version series id are the same
            objectId = versionServicesId;
        }

        // get the file or folder
        File file = getFile(objectId);

        // set defaults if values not set
        boolean iaa = FileBridgeUtils.getBooleanParameter(
                includeAllowableActions, false);
        boolean iacl = FileBridgeUtils.getBooleanParameter(includeAcl, false);

        // split filter
        Set<String> filterCollection = FileBridgeUtils.splitFilter(filter);

        // gather properties
        return compileObjectData(context, file, filterCollection, iaa, iacl,
                userReadOnly, objectInfos);
    }

    /**
     * CMIS getAllowableActions.
     */
    public AllowableActions getAllowableActions(CallContext context,
                                                String objectId) {
        boolean userReadOnly = checkUser(context, false);

        // get the file or folder
        File file = getFile(objectId);
        if (!file.exists()) {
            throw new CmisObjectNotFoundException("Object not found!");
        }

        return compileAllowableActions(file, userReadOnly);
    }

    /**
     * CMIS getACL.
     */
    public Acl getAcl(CallContext context, String objectId) {
        checkUser(context, false);

        // get the file or folder
        File file = getFile(objectId);
        if (!file.exists()) {
            throw new CmisObjectNotFoundException("Object not found!");
        }

        return compileAcl(file);
    }

    /**
     * CMIS getContentStream.
     */
    public ContentStream getContentStream(CallContext context, String objectId,
                                          BigInteger offset, BigInteger length) {
        checkUser(context, false);

        // get the file
        final File file = getFile(objectId);
        if (!file.isFile()) {
            throw new CmisStreamNotSupportedException("Not a file!");
        }

        if (file.length() == 0) {
            throw new CmisConstraintException("Document has no content!");
        }

        InputStream stream = null;
        try {
            stream = new BufferedInputStream(new FileInputStream(file),
                    4 * 1024);
            if (offset != null || length != null) {
                stream = new ContentRangeInputStream(stream, offset, length);
            }
        } catch (FileNotFoundException e) {
            throw new CmisObjectNotFoundException(e.getMessage(), e);
        }

        // compile data
        ContentStreamImpl result;
        if ((offset != null && offset.longValue() > 0) || length != null) {
            result = new PartialContentStreamImpl();
        } else {
            result = new ContentStreamImpl();
        }

        result.setFileName(file.getName());
        result.setLength(BigInteger.valueOf(file.length()));
        result.setMimeType(MimeTypes.getMIMEType(file));
        result.setStream(stream);

        return result;
    }

    /**
     * CMIS getDescendants.
     */
    public List<ObjectInFolderContainer> getDescendants(CallContext context,
                                                        String folderId, BigInteger depth, String filter,
                                                        Boolean includeAllowableActions, Boolean includePathSegment,
                                                        ObjectInfoHandler objectInfos, boolean foldersOnly) {
        boolean userReadOnly = checkUser(context, false);

        // check depth
        int d = (depth == null ? 2 : depth.intValue());
        if (d == 0) {
            throw new CmisInvalidArgumentException("Depth must not be 0!");
        }
        if (d < -1) {
            d = -1;
        }

        // split filter
        Set<String> filterCollection = FileBridgeUtils.splitFilter(filter);

        // set defaults if values not set
        boolean iaa = FileBridgeUtils.getBooleanParameter(
                includeAllowableActions, false);
        boolean ips = FileBridgeUtils.getBooleanParameter(includePathSegment,
                false);

        // get the folder
        File folder = getFile(folderId);
        if (!folder.isDirectory()) {
            throw new CmisObjectNotFoundException("Not a folder!");
        }

        // set object info of the the folder
        if (context.isObjectInfoRequired()) {
            compileObjectData(context, folder, null, false, false,
                    userReadOnly, objectInfos);
        }

        // get the tree
        List<ObjectInFolderContainer> result = new ArrayList<ObjectInFolderContainer>();
        gatherDescendants(context, folder, result, foldersOnly, d,
                filterCollection, iaa, ips, userReadOnly, objectInfos);

        return result;
    }

    /**
     * Gather the children of a folder.
     */
    private void gatherDescendants(CallContext context, File folder,
                                   List<ObjectInFolderContainer> list, boolean foldersOnly, int depth,
                                   Set<String> filter, boolean includeAllowableActions,
                                   boolean includePathSegments, boolean userReadOnly,
                                   ObjectInfoHandler objectInfos) {
        assert folder != null;
        assert list != null;

        // iterate through children
        for (File child : folder.listFiles()) {
            // skip hidden and shadow files
            if (child.isHidden()) {
                continue;
            }

            // folders only?
            if (foldersOnly && !child.isDirectory()) {
                continue;
            }

            // add to list
            ObjectInFolderDataImpl objectInFolder = new ObjectInFolderDataImpl();
            objectInFolder.setObject(compileObjectData(context, child, filter,
                    includeAllowableActions, false, userReadOnly, objectInfos));
            if (includePathSegments) {
                objectInFolder.setPathSegment(child.getName());
            }

            ObjectInFolderContainerImpl container = new ObjectInFolderContainerImpl();
            container.setObject(objectInFolder);

            list.add(container);

            // move to next level
            if (depth != 1 && child.isDirectory()) {
                container.setChildren(new ArrayList<ObjectInFolderContainer>());
                gatherDescendants(context, child, container.getChildren(),
                        foldersOnly, depth - 1, filter,
                        includeAllowableActions, includePathSegments,
                        userReadOnly, objectInfos);
            }
        }
    }

    /**
     * CMIS getFolderParent.
     */
    public ObjectData getFolderParent(CallContext context, String folderId,
                                      String filter, ObjectInfoHandler objectInfos) {
        List<ObjectParentData> parents = getObjectParents(context, folderId,
                filter, false, false, objectInfos);

        if (parents.size() == 0) {
            throw new CmisInvalidArgumentException(
                    "The root folder has no parent!");
        }

        return parents.get(0).getObject();
    }

    /**
     * CMIS getObjectParents.
     */
    public List<ObjectParentData> getObjectParents(CallContext context,
                                                   String objectId, String filter, Boolean includeAllowableActions,
                                                   Boolean includeRelativePathSegment, ObjectInfoHandler objectInfos) {
        boolean userReadOnly = checkUser(context, false);

        // split filter
        Set<String> filterCollection = FileBridgeUtils.splitFilter(filter);

        // set defaults if values not set
        boolean iaa = FileBridgeUtils.getBooleanParameter(
                includeAllowableActions, false);
        boolean irps = FileBridgeUtils.getBooleanParameter(
                includeRelativePathSegment, false);

        // get the file or folder
        File file = getFile(objectId);

        // don't climb above the root folder
        if (root.equals(file)) {
            return Collections.emptyList();
        }

        // set object info of the the object
        if (context.isObjectInfoRequired()) {
            compileObjectData(context, file, null, false, false, userReadOnly,
                    objectInfos);
        }

        // get parent folder
        File parent = file.getParentFile();
        ObjectData object = compileObjectData(context, parent,
                filterCollection, iaa, false, userReadOnly, objectInfos);

        ObjectParentDataImpl result = new ObjectParentDataImpl();
        result.setObject(object);
        if (irps) {
            result.setRelativePathSegment(file.getName());
        }

        return Collections.<ObjectParentData>singletonList(result);
    }

    /**
     * CMIS getObjectByPath.
     */
    public ObjectData getObjectByPath(CallContext context, String folderPath,
                                      String filter, boolean includeAllowableActions, boolean includeACL,
                                      ObjectInfoHandler objectInfos) {
        boolean userReadOnly = checkUser(context, false);

        // split filter
        Set<String> filterCollection = FileBridgeUtils.splitFilter(filter);

        // check path
        if (folderPath == null || folderPath.length() == 0
                || folderPath.charAt(0) != '/') {
            throw new CmisInvalidArgumentException("Invalid folder path!");
        }

        // get the file or folder
        File file = null;
        if (folderPath.length() == 1) {
            file = root;
        } else {
            String path = folderPath.replace('/', File.separatorChar)
                    .substring(1);
            file = new File(root, path);
        }

        if (!file.exists()) {
            throw new CmisObjectNotFoundException("Path doesn't exist.");
        }

        return compileObjectData(context, file, filterCollection,
                includeAllowableActions, includeACL, userReadOnly, objectInfos);
    }

    // --- helpers ---

    /**
     * Checks a property set for a new object.
     */
    private void checkNewProperties(Properties properties) {
        // check properties
        if (properties == null || properties.getProperties() == null) {
            throw new CmisInvalidArgumentException("Properties must be set!");
        }

        // check the name
        String name = FileBridgeUtils.getStringProperty(properties,
                PropertyIds.NAME);
        if (!isValidName(name)) {
            throw new CmisNameConstraintViolationException("Name is not valid!");
        }

        // check the type
        String typeId = FileBridgeUtils.getObjectTypeId(properties);
        if (typeId == null) {
            throw new CmisNameConstraintViolationException(
                    "Type Id is not set!");
        }

        TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
        if (type == null) {
            throw new CmisObjectNotFoundException("Type '" + typeId
                    + "' is unknown!");
        }

        // check type properties
        checkTypeProperties(properties, typeId, true);

        // check if required properties are missing
        for (PropertyDefinition<?> propDef : type.getPropertyDefinitions()
                .values()) {
            if (propDef.isRequired()
                    && !properties.getProperties().containsKey(propDef.getId())
                    && propDef.getUpdatability() != Updatability.READONLY) {
                throw new CmisConstraintException("Property '"
                        + propDef.getId() + "' is required!");
            }
        }
    }

    /**
     * Checks a property set for a copied document.
     */
    private void checkCopyProperties(Properties properties, String sourceTypeId) {
        // check properties
        if (properties == null || properties.getProperties() == null) {
            return;
        }

        String typeId = sourceTypeId;

        // check the name
        String name = FileBridgeUtils.getStringProperty(properties,
                PropertyIds.NAME);
        if (name != null) {
            if (!isValidName(name)) {
                throw new CmisNameConstraintViolationException(
                        "Name is not valid!");
            }
        }

        // check the type
        typeId = FileBridgeUtils.getObjectTypeId(properties);
        if (typeId == null) {
            typeId = sourceTypeId;
        }

        TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
        if (type == null) {
            throw new CmisObjectNotFoundException("Type '" + typeId
                    + "' is unknown!");
        }

        if (type.getBaseTypeId() != BaseTypeId.CMIS_DOCUMENT) {
            throw new CmisInvalidArgumentException(
                    "Target type must be a document type!");
        }

        // check type properties
        checkTypeProperties(properties, typeId, true);

        // check if required properties are missing
        for (PropertyDefinition<?> propDef : type.getPropertyDefinitions()
                .values()) {
            if (propDef.isRequired()
                    && !properties.getProperties().containsKey(propDef.getId())
                    && propDef.getUpdatability() != Updatability.READONLY) {
                throw new CmisConstraintException("Property '"
                        + propDef.getId() + "' is required!");
            }
        }
    }

    /**
     * Checks a property set for an update.
     */
    private void checkUpdateProperties(Properties properties, String typeId) {
        // check properties
        if (properties == null || properties.getProperties() == null) {
            throw new CmisInvalidArgumentException("Properties must be set!");
        }

        // check the name
        String name = FileBridgeUtils.getStringProperty(properties,
                PropertyIds.NAME);
        if (name != null) {
            if (!isValidName(name)) {
                throw new CmisNameConstraintViolationException(
                        "Name is not valid!");
            }
        }

        // check type properties
        checkTypeProperties(properties, typeId, false);
    }

    /**
     * Checks if the property belong to the type and are settable.
     */
    private void checkTypeProperties(Properties properties, String typeId,
                                     boolean isCreate) {
        // check type
        TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
        if (type == null) {
            throw new CmisObjectNotFoundException("Type '" + typeId
                    + "' is unknown!");
        }

        // check if all required properties are there
        for (PropertyData<?> prop : properties.getProperties().values()) {
            PropertyDefinition<?> propType = type.getPropertyDefinitions().get(
                    prop.getId());

            // do we know that property?
            if (propType == null) {
                throw new CmisConstraintException("Property '" + prop.getId()
                        + "' is unknown!");
            }

            // can it be set?
            if (propType.getUpdatability() == Updatability.READONLY) {
                throw new CmisConstraintException("Property '" + prop.getId()
                        + "' is readonly!");
            }

            if (!isCreate) {
                // can it be set?
                if (propType.getUpdatability() == Updatability.ONCREATE) {
                    throw new CmisConstraintException("Property '"
                            + prop.getId() + "' cannot be updated!");
                }
            }
        }
    }

    private void addPropertyId(PropertiesImpl props, String typeId,
                               Set<String> filter, String id, String value) {
        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyIdImpl(id, value));
    }

    private void addPropertyIdList(PropertiesImpl props, String typeId,
                                   Set<String> filter, String id, List<String> value) {
        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyIdImpl(id, value));
    }

    private void addPropertyString(PropertiesImpl props, String typeId,
                                   Set<String> filter, String id, String value) {
        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyStringImpl(id, value));
    }

    private void addPropertyInteger(PropertiesImpl props, String typeId,
                                    Set<String> filter, String id, long value) {
        addPropertyBigInteger(props, typeId, filter, id,
                BigInteger.valueOf(value));
    }

    private void addPropertyBigInteger(PropertiesImpl props, String typeId,
                                       Set<String> filter, String id, BigInteger value) {
        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyIntegerImpl(id, value));
    }

    private void addPropertyBoolean(PropertiesImpl props, String typeId,
                                    Set<String> filter, String id, boolean value) {
        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyBooleanImpl(id, value));
    }

    private void addPropertyDateTime(PropertiesImpl props, String typeId,
                                     Set<String> filter, String id, GregorianCalendar value) {
        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyDateTimeImpl(id, value));
    }

    private boolean checkAddProperty(Properties properties, String typeId,
                                     Set<String> filter, String id) {
        if ((properties == null) || (properties.getProperties() == null)) {
            throw new IllegalArgumentException("Properties must not be null!");
        }

        if (id == null) {
            throw new IllegalArgumentException("Id must not be null!");
        }

        TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
        if (type == null) {
            throw new IllegalArgumentException("Unknown type: " + typeId);
        }
        if (!type.getPropertyDefinitions().containsKey(id)) {
            throw new IllegalArgumentException("Unknown property: " + id);
        }

        String queryName = type.getPropertyDefinitions().get(id).getQueryName();

        if ((queryName != null) && (filter != null)) {
            if (!filter.contains(queryName)) {
                return false;
            } else {
                filter.remove(queryName);
            }
        }

        return true;
    }

    /**
     * Compiles the allowable actions for a file or folder.
     */
    private AllowableActions compileAllowableActions(File file,
                                                     boolean userReadOnly) {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null!");
        }

        // we can't gather allowable actions if the file or folder doesn't exist
        if (!file.exists()) {
            throw new CmisObjectNotFoundException("Object not found!");
        }

        boolean isReadOnly = !file.canWrite();
        boolean isFolder = file.isDirectory();
        boolean isRoot = root.equals(file);

        Set<Action> aas = EnumSet.noneOf(Action.class);

        addAction(aas, Action.CAN_GET_OBJECT_PARENTS, !isRoot);
        addAction(aas, Action.CAN_GET_PROPERTIES, true);
        addAction(aas, Action.CAN_UPDATE_PROPERTIES, !userReadOnly
                && !isReadOnly);
        addAction(aas, Action.CAN_MOVE_OBJECT, !userReadOnly && !isRoot);
        addAction(aas, Action.CAN_DELETE_OBJECT, !userReadOnly && !isReadOnly
                && !isRoot);
        addAction(aas, Action.CAN_GET_ACL, true);

        if (isFolder) {
            addAction(aas, Action.CAN_GET_DESCENDANTS, true);
            addAction(aas, Action.CAN_GET_CHILDREN, true);
            addAction(aas, Action.CAN_GET_FOLDER_PARENT, !isRoot);
            addAction(aas, Action.CAN_GET_FOLDER_TREE, true);
            addAction(aas, Action.CAN_CREATE_DOCUMENT, !userReadOnly);
            addAction(aas, Action.CAN_CREATE_FOLDER, !userReadOnly);
            addAction(aas, Action.CAN_DELETE_TREE, !userReadOnly && !isReadOnly);
        } else {
            addAction(aas, Action.CAN_GET_CONTENT_STREAM, file.length() > 0);
            addAction(aas, Action.CAN_SET_CONTENT_STREAM, !userReadOnly
                    && !isReadOnly);
            addAction(aas, Action.CAN_DELETE_CONTENT_STREAM, !userReadOnly
                    && !isReadOnly);
            addAction(aas, Action.CAN_GET_ALL_VERSIONS, true);
        }

        AllowableActionsImpl result = new AllowableActionsImpl();
        result.setAllowableActions(aas);

        return result;
    }

    private void addAction(Set<Action> aas, Action action, boolean condition) {
        if (condition) {
            aas.add(action);
        }
    }

    /**
     * Compiles the ACL for a file or folder.
     */
    private Acl compileAcl(File file) {
        AccessControlListImpl result = new AccessControlListImpl();
        result.setAces(new ArrayList<Ace>());

        for (Map.Entry<String, Boolean> ue : readWriteUserMap.entrySet()) {
            // create principal
            AccessControlPrincipalDataImpl principal = new AccessControlPrincipalDataImpl();
            principal.setPrincipalId(ue.getKey());

            // create ACE
            AccessControlEntryImpl entry = new AccessControlEntryImpl();
            entry.setPrincipal(principal);
            entry.setPermissions(new ArrayList<String>());
            entry.getPermissions().add(CMIS_READ);
            if (!ue.getValue().booleanValue() && file.canWrite()) {
                entry.getPermissions().add(CMIS_WRITE);
                entry.getPermissions().add(CMIS_ALL);
            }

            entry.setDirect(true);

            // add ACE
            result.getAces().add(entry);
        }

        return result;
    }

    /**
     * Checks if the given name is valid for a file system.
     *
     * @param name the name to check
     * @return <code>true</code> if the name is valid, <code>false</code>
     * otherwise
     */
    private boolean isValidName(String name) {
        if (name == null || name.length() == 0
                || name.indexOf(File.separatorChar) != -1
                || name.indexOf(File.pathSeparatorChar) != -1) {
            return false;
        }

        return true;
    }

    /**
     * Checks if a folder is empty. A folder is considered as empty if no files
     * or only the shadow file reside in the folder.
     *
     * @param folder the folder
     * @return <code>true</code> if the folder is empty.
     */
    private boolean isFolderEmpty(File folder) {
        if (!folder.isDirectory()) {
            return true;
        }

        String[] fileNames = folder.list();

        if ((fileNames == null) || (fileNames.length == 0)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the user in the given context is valid for this repository and
     * if the user has the required permissions.
     */
    private boolean checkUser(CallContext context, boolean writeRequired) {
        if (context == null) {
            throw new CmisPermissionDeniedException("No user context!");
        }

        Boolean readOnly = readWriteUserMap.get(context.getUsername());
        if (readOnly == null) {
            throw new CmisPermissionDeniedException("Unknown user!");
        }

        if (readOnly.booleanValue() && writeRequired) {
            throw new CmisPermissionDeniedException("No write permission!");
        }

        return readOnly.booleanValue();
    }

    /**
     * Returns the File object by id or throws an appropriate exception.
     */
    private File getFile(String id) {
        try {
            return idToFile(id);
        } catch (Exception e) {
            throw new CmisObjectNotFoundException(e.getMessage(), e);
        }
    }

    /**
     * Converts an id to a File object. A simple and insecure implementation,
     * but good enough for now.
     */
    private File idToFile(String id) throws IOException {
        if (id == null || id.length() == 0) {
            throw new CmisInvalidArgumentException("Id is not valid!");
        }

        if (id.equals(ROOT_ID)) {
            return root;
        }

        return new File(root, (new String(Base64.decode(id.getBytes("US-ASCII")), "UTF-8")).replace('/', File.separatorChar));
    }

    /**
     * Returns the id of a File object or throws an appropriate exception.
     */
    private String getId(File file) {
        try {
            return fileToId(file);
        } catch (Exception e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Creates a File object from an id. A simple and insecure implementation,
     * but good enough for now.
     */
    private String fileToId(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File is not valid!");
        }

        if (root.equals(file)) {
            return ROOT_ID;
        }

        String path = getRepositoryPath(file);

        return Base64.encodeBytes(path.getBytes("UTF-8"));
    }

    private String getRepositoryPath(File file) {
        String path = file.getAbsolutePath()
                .substring(root.getAbsolutePath().length())
                .replace(File.separatorChar, '/');
        if (path.length() == 0) {
            path = "/";
        } else if (path.charAt(0) != '/') {
            path = "/" + path;
        }
        return path;
    }
}
