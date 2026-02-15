package com.s4apps.processlog;

import java.util.List;

/**
 *
 * @author mat
 */
public class ConfigData {

    // Somewhere to store the configuration data
    private List<String> ipsToIgnore = null;
    private List<String> methodsToIgnore = null;
    private List<String> serversToIgnore = null;
    private List<String> urlsToIgnore = null;
    private List<String> ipsToDelete = null;
    private List<String> methodsToDelete = null;
    private List<String> serversToDelete = null;
    private List<String> urlsToDelete = null;

    public ConfigData() {

        // All the config data is in the database so get a connection
        JpaAccess jpa = new JpaAccess();

        // Get the data that we want
        ipsToIgnore = jpa.getIgnoreIps();
        methodsToIgnore = jpa.getIgnoreMethods();
        serversToIgnore = jpa.getIgnoreServers();
        urlsToIgnore = jpa.getIgnoreUrls();
        ipsToDelete = jpa.getDeleteIps();
        methodsToDelete = jpa.getDeleteMethods();
        serversToDelete = jpa.getDeleteServers();
        urlsToDelete = jpa.getDeleteUrls();

        // All done so close down
        jpa.close();

    }

    /**
     * @return the ipsToIgnore
     */
    public List<String> getIpsToIgnore() {
        return ipsToIgnore;
    }

    /**
     * @return the methodsToIgnore
     */
    public List<String> getMethodsToIgnore() {
        return methodsToIgnore;
    }

    /**
     * @return the serversToIgnore
     */
    public List<String> getServersToIgnore() {
        return serversToIgnore;
    }

    /**
     * @return the urlsToIgnore
     */
    public List<String> getUrlsToIgnore() {
        return urlsToIgnore;
    }

    /**
     * @return the ipsToDelete
     */
    public List<String> getIpsToDelete() {
        return ipsToDelete;
    }

    /**
     * @return the methodsToDelete
     */
    public List<String> getMethodsToDelete() {
        return methodsToDelete;
    }

    /**
     * @return the serversToDelete
     */
    public List<String> getServersToDelete() {
        return serversToDelete;
    }

    /**
     * @return the urlsToDelete
     */
    public List<String> getUrlsToDelete() {
        return urlsToDelete;
    }
}
