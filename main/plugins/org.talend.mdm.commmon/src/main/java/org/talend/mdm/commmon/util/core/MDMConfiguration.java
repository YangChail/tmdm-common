// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.mdm.commmon.util.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Handles the mdm.conf file
 */
public final class MDMConfiguration {
    
    /**
     * This is the MDM (mdm.conf) configuration property to indicate current server is running in a clustered
     * environment. Setting this property to <code>true</code> may have impacts on the choice of implementation for
     * internal components.
     * 
     * @see com.amalto.core.save.generator.AutoIncrementGenerator
     */
    private static final String SYSTEM_CLUSTER = "system.cluster"; //$NON-NLS-1$

    private static final Logger logger = Logger.getLogger(MDMConfiguration.class);

    private static MDMConfiguration instance;

    private String location;

    private Properties properties = null;

    private MDMConfiguration(String location) {
        this.location = location;
    }

    public static synchronized MDMConfiguration createConfiguration(String location, boolean ignoreIfNotFound) {
        if (instance != null) {
            throw new IllegalStateException();
        }
        instance = new MDMConfiguration(location);
        instance.getProperties(true, ignoreIfNotFound);
        return instance;
    }

    public static synchronized Properties getConfiguration() {
        return getConfiguration(false);
    }

    public static synchronized Properties getConfiguration(boolean reload) {
        if (instance == null) {
            throw new IllegalStateException();
        }
        return instance.getProperties(reload, false);
    }

    public static synchronized void save() {
        if (instance == null) {
            throw new IllegalStateException();
        }
        instance.saveProperties();
    }
    
    public static boolean isClusterEnabled(){
        Properties properties = MDMConfiguration.getConfiguration();
        return Boolean.parseBoolean(properties.getProperty(SYSTEM_CLUSTER, Boolean.FALSE.toString()));
    }

    private Properties getProperties(boolean reload, boolean ignoreIfNotFound) {
        if (reload) {
            properties = null;
        }
        if (properties != null) {
            return properties;
        }
        properties = new Properties();

        File file = new File(location);
        if (file.exists()) {
            logger.info("MDM Configuration: found in '" + file.getAbsolutePath() + "'."); //$NON-NLS-1$ //$NON-NLS-2$
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);                
                properties.load(in);
                String adminPassword = properties.getProperty("admin.password"); //$NON-NLS-1$
                String tPassword = properties.getProperty("technical.password"); //$NON-NLS-1$
                boolean isUpdated = false;
                if (adminPassword != null && !adminPassword.endsWith(Crypt.ENCRYPT)) {
                    adminPassword = Crypt.encrypt(adminPassword);
                    properties.setProperty("admin.password", adminPassword); //$NON-NLS-1$
                    isUpdated = true;
                }
                if (tPassword != null && !tPassword.endsWith(Crypt.ENCRYPT)) {
                    tPassword = Crypt.encrypt(tPassword);
                    properties.setProperty("technical.password", tPassword); //$NON-NLS-1$
                    isUpdated = true;
                }
                if (isUpdated) {
                    save();
                }
                properties.setProperty("admin.password", Crypt.decrypt(adminPassword)); //$NON-NLS-1$
                properties.setProperty("technical.password", Crypt.decrypt(tPassword)); //$NON-NLS-1$                
            } catch (Exception e) {
                if (!ignoreIfNotFound) {
                    throw new IllegalStateException("Unable to load MDM configuration from '" //$NON-NLS-1$
                            + file.getAbsolutePath() + "'", e); //$NON-NLS-1$
                }
                logger.warn("Unable to load MDM configuration from '" + file.getAbsolutePath() + "'", e); //$NON-NLS-1$ //$NON-NLS-2$
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
        } else {
            if (!ignoreIfNotFound) {
                throw new IllegalStateException("Unable to load MDM configuration from '" + file.getAbsolutePath() //$NON-NLS-1$
                        + "'"); //$NON-NLS-1$
            }
            logger.warn("Unable to load MDM configuration from '" + file.getAbsolutePath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        }        
        return properties;
    }

    /**
     * save configure file
     */
    private void saveProperties() {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(location);
            properties.store(out, "MDM configuration file"); //$NON-NLS-1$
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    public static EDBType getDBType() {
        Object dbType = getConfiguration().get("xmldb.type"); //$NON-NLS-1$
        if (dbType != null && dbType.toString().equals(EDBType.QIZX.getName())) {
            return EDBType.QIZX;
        }
        return EDBType.EXIST;
    }

    public static boolean isExistDb() {
        Object dbType = getConfiguration().get("xmldb.type"); //$NON-NLS-1$
        return !(dbType != null && !dbType.toString().equals(EDBType.EXIST.getName()));
    }

    public static String getAdminPassword() {
        String password = getConfiguration().getProperty("admin.password"); //$NON-NLS-1$
        password = password == null ? "talend" : password; //$NON-NLS-1$
        return password;
    }

    public static String getAdminUser() {
        String user = getConfiguration().getProperty("admin.user"); //$NON-NLS-1$
        user = user == null ? "admin" : user; //$NON-NLS-1$
        return user;
    }

    public static int getAutoEntityFindThreshold() {
        String value = getConfiguration().getProperty("autoentityfind.item.max"); //$NON-NLS-1$
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

}
