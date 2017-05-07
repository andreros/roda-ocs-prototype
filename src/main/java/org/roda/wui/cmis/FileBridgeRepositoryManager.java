package org.roda.wui.cmis;

import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all repositories.
 */
public class FileBridgeRepositoryManager {

    private final Map<String, FileBridgeRepository> repositories;

    public FileBridgeRepositoryManager() {
        repositories = new HashMap<String, FileBridgeRepository>();
    }

    /**
     * Adds a repository object.
     */
    public void addRepository(FileBridgeRepository fsr) {
        if (fsr == null || fsr.getRepositoryId() == null) {
            return;
        }

        repositories.put(fsr.getRepositoryId(), fsr);
    }

    /**
     * Gets a repository object by id.
     */
    public FileBridgeRepository getRepository(String repositoryId) {
        FileBridgeRepository result = repositories.get(repositoryId);
        if (result == null) {
            throw new CmisObjectNotFoundException("Unknown repository '" + repositoryId + "'!");
        }

        return result;
    }

    /**
     * Returns all repository objects.
     */
    public Collection<FileBridgeRepository> getRepositories() {
        return repositories.values();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (FileBridgeRepository repository : repositories.values()) {
            sb.append('[');
            sb.append(repository.getRepositoryId());
            sb.append(" -> ");
            sb.append(repository.getRootDirectory().getAbsolutePath());
            sb.append(']');
        }

        return sb.toString();
    }
}