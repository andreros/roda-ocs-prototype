package org.roda.wui.cmis;

import org.apache.chemistry.opencmis.commons.exceptions.CmisPermissionDeniedException;
import org.apache.chemistry.opencmis.commons.server.CallContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages users for the FileShare repository.
 */
public class FileBridgeUserManager {

    private final Map<String, String> logins;

    public FileBridgeUserManager() {
        logins = new HashMap<String, String>();
    }

    /**
     * Returns all logins.
     */
    public synchronized Collection<String> getLogins() {
        return logins.keySet();
    }

    /**
     * Adds a login.
     */
    public synchronized void addLogin(String username, String password) {
        if (username == null || password == null) {
            return;
        }

        logins.put(username.trim(), password);
    }

    /**
     * Takes user and password from the CallContext and checks them.
     */
    public synchronized String authenticate(CallContext context) {
        // check user and password
        if (!authenticate(context.getUsername(), context.getPassword())) {
            throw new CmisPermissionDeniedException("Invalid username or password.");
        }

        return context.getUsername();
    }

    /**
     * Authenticates a user against the configured logins.
     */
    private synchronized boolean authenticate(String username, String password) {
        String pwd = logins.get(username);
        if (pwd == null) {
            return false;
        }

        return pwd.equals(password);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (String user : logins.keySet()) {
            sb.append('[');
            sb.append(user);
            sb.append(']');
        }

        return sb.toString();
    }
}
