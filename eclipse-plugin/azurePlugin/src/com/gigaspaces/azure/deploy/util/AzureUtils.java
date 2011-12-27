package com.gigaspaces.azure.deploy.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import com.gigaspaces.azure.deploy.azureconfig.AzureDeploymentException;
import com.gigaspaces.azure.deploy.azureconfig.AzureSlot;

public class AzureUtils {

    // .NET wrapper for azure API commands
    private static final String AZURECONFIG_EXE = "utils/azureconfig.exe";

    // Helper util for adding certificates to remote desktop
    private static final String ENCUTIL_EXE = "utils/encutil.exe";
    
    // azure related properties
    private static final String AZURE_PROPERTIES_FILENAME = "utils/azure.properties";

    private static final String TEMP_ROOT_FOLDER = ".";
    
    private static File toAbsoluteFile(String filepath) {
        File file = new File(filepath);
        if (!file.isAbsolute()) {
            file = new File(TEMP_ROOT_FOLDER, filepath);
        }
        return file;
    }

    public static File getAzureConfigEXE() {
        return toAbsoluteFile(AZURECONFIG_EXE);
    }
    
    public static File getEncUtilEXE() {
        return toAbsoluteFile(ENCUTIL_EXE);
    }
    
    public static File getAzurePropertiesFile() {
        return toAbsoluteFile(AZURE_PROPERTIES_FILENAME);
    }

    public static Properties getAzureProperties() throws IOException {
        File propertiesFile = getAzurePropertiesFile();
        return loadProperties(propertiesFile);
    }

    public static String getProperty(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null) {
            throw new IllegalArgumentException(AzureUtils.getAzurePropertiesFile().getAbsolutePath()  + " does not contain property " + name);
        }
        return value;
    }
    
    public static File getFileProperty(Properties properties, String name) throws AzureDeploymentException {
        String pathname = getProperty(properties,name);
        if (pathname == null || pathname.length() == 0) {
            throw new AzureDeploymentException("Cannot find property " + name + " in file " + AzureUtils.getAzurePropertiesFile().getAbsolutePath());
        }
        File file = toAbsoluteFile(pathname);
        if (!file.exists()) {
            throw new AzureDeploymentException(file.getAbsolutePath() + " does not exist.");
        }
        return file;
    }
    
    public static File createTempCscfgFile(String hostedServiceName, AzureSlot slot) throws AzureDeploymentException {
        try {
            File tempFile = File.createTempFile(hostedServiceName + "_"+ slot.getSlot(), ".cscfg");
            tempFile.deleteOnExit();
            return tempFile;
        } catch (IOException e) {
            throw new AzureDeploymentException("Cannot read azure configuration", e);
        }
    }
   
    public static Properties loadProperties(File propertiesFile)
            throws FileNotFoundException, IOException {
        Properties properties = new Properties();
        FileInputStream fis = new FileInputStream(propertiesFile);
        try {
            properties.load(fis);
        } finally {
            fis.close();
        }
        return properties;
    }   
    
    public static long millisUntil(String errorMessage, long end)
            throws TimeoutException {
        long millisUntilEnd = end - System.currentTimeMillis();
        if (millisUntilEnd < 0) {
            throw new TimeoutException(errorMessage);
        }
        return millisUntilEnd;
    }

}
