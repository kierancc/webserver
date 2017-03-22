package webserver;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Kieran Chin Cheong
 * @version 1.0
 * @since 1.0
 */
public class Configuration
{
    // Static variables
    
    // This is the singleton instance
    private static Configuration configSingleton = null;
    
    // Path to saved configuration file on filesystem
    private static final String CONFIG_PATH = "./config.xml";
    
    // Static methods
    
    /**
     * Method called to retrieve the global configuration object
     * 
     * @return the singleton Configuration object
     */
    public static Configuration GetConfiguration()
    {
        // If the singleton has not been created yet, create it
        if (configSingleton == null)
        {
            configSingleton = Configuration.LoadConfiguration();
        }
        
        // Return singleton
        return configSingleton;
    }
    
    /**
     * Saves a configuration to an XML file
     * @param config The configuration object to save
     */
    public static void SaveConfiguration(Configuration config)
    {
        try
        {
            try(XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(Configuration.CONFIG_PATH))))
            {
                encoder.writeObject(config);
            }
        }
        catch(IOException e)
        {
            System.err.println(String.format("Error saving configuration file : %s", e.toString()));
        }
    }
    
    /**
     * Loads the server configuration from a saved XML file
     * <p>
     * If the saved configuration cannot be read, this method will return a Configuration object set with
     * the default configuration
     * @return A Configuration object populated with saved or default configuration
     */
    public static Configuration LoadConfiguration()
    {
        Configuration config;
        
        // Attempt to deserialize a Configuration object from the configuration XML file
        try
        {
            try(XMLDecoder decoder = new XMLDecoder(new FileInputStream(Configuration.CONFIG_PATH)))
            {
                config = (Configuration) decoder.readObject();
            }
        }
        catch (IOException e)
        {
            // We failed to read the saved configuration, so create a new Configuration object with the default
            // settings and return it
            System.err.println("Error loading saved configuration, using default configuration");
            config = new Configuration();
            config.SetDefaultConfiguration();
        }
        
        return config;
    }
    
    // Member variables

    // Listening port
    private int port;

    // Thread pooling
    private boolean enableThreadPool;
    private int numThreads;

    // Client-side Caching
    private boolean enableClientCaching;

    // HTTP 1.1 KeepAlive
    private boolean enableHTTPKeepAlive;
    private int httpKeepAliveTimeout;
    private int httpKeepAliveMax;

    // Logging level
    private int loggingLevel;
    
    // Log file
    private String logFile;

    // Root directory
    private String rootDirectory;

    // Default document
    private String defaultDocument; // Specifies the default document, e.g.
                                    // index.html
    
    // Debug mode
    private boolean debugMode;

    /**
     * Blank constructor required for de/serialization
     */
    public Configuration()
    {
    }
    
    /**
     * Used to reset all member variables in this Configuration object to default values
     */
    public void SetDefaultConfiguration()
    {
        this.port = 8080;
        this.enableThreadPool = true;
        this.numThreads = 10;
        this.enableClientCaching = false;
        this.enableHTTPKeepAlive = true;
        this.httpKeepAliveTimeout = 3;
        this.httpKeepAliveMax = 5;
        this.loggingLevel = 1;
        this.logFile = "./server_log.txt";
        this.rootDirectory = "C:\\webserver\\content";
        this.defaultDocument = "index.html";
        this.debugMode = true;
    }
    
    /**
     * Used to output the values of all configurable options to the log
     */
    public void LogConfiguration()
    {
        Logger.Log(Logger.ALWAYS, String.format("CONFIG: port %d", this.port));
        Logger.Log(Logger.ALWAYS, String.format("CONFIG: enableThreadPool %s", this.enableThreadPool));
        Logger.Log(Logger.ALWAYS, String.format("CONFIG: numThreads %d", this.numThreads));
        Logger.Log(Logger.ALWAYS, String.format("CONFIG: enableClientCaching %s", this.enableClientCaching));
        Logger.Log(Logger.ALWAYS, String.format("CONFIG: enableHTTPKeepAlive %s", this.enableHTTPKeepAlive));
        Logger.Log(Logger.ALWAYS, String.format("CONFIG: httpKeepAliveTimeout %d", this.httpKeepAliveTimeout));
        Logger.Log(Logger.ALWAYS, String.format("CONFIG: httpKeepAliveMax %d", this.httpKeepAliveMax));
        Logger.Log(Logger.ALWAYS, String.format("CONFIG: loggingLevel %d", this.loggingLevel));
        Logger.Log(Logger.ALWAYS, String.format("CONFIG: logFile %s", this.logFile));
        Logger.Log(Logger.ALWAYS, String.format("CONFIG: rootDirectory %s", this.rootDirectory));
        Logger.Log(Logger.ALWAYS, String.format("CONFIG: defaultDocument %s", this.defaultDocument));
        Logger.Log(Logger.ALWAYS, String.format("CONFIG: debugMode %s", this.debugMode));
    }

    // Getters and setters
    /**
     * @return
     */
    public int getPort()
    {
        return this.port;
    }
    
    /**
     * @param port
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * @return
     */
    public boolean isEnableThreadPool()
    {
        return this.enableThreadPool;
    }
    
    /**
     * @param enableThreadPool
     */
    public void setEnableThreadPool(boolean enableThreadPool)
    {
        this.enableThreadPool = enableThreadPool;
    }

    /**
     * @return
     */
    public int getNumThreads()
    {
        return this.numThreads;
    }
    
    /**
     * @param numThreads
     */
    public void setNumThreads(int numThreads)
    {
        this.numThreads = numThreads;
    }

    /**
     * @return
     */
    public boolean isEnableClientCaching()
    {
        return this.enableClientCaching;
    }
    
    /**
     * @param enableClientCaching
     */
    public void setEnableClientCaching(boolean enableClientCaching)
    {
        this.enableClientCaching = enableClientCaching;
    }

    /**
     * @return
     */
    public boolean isEnableHTTPKeepAlive()
    {
        return this.enableHTTPKeepAlive;
    }
    
    /**
     * @param enableHTTPKeepAlive
     */
    public void setEnableHTTPKeepAlive(boolean enableHTTPKeepAlive)
    {
        this.enableHTTPKeepAlive = enableHTTPKeepAlive;
    }
    
    /**
     * @return
     */
    public int getHttpKeepAliveTimeout()
    {
        return this.httpKeepAliveTimeout;
    }
    
    /**
     * @param httpKeepAliveTimeout
     */
    public void setHttpKeepAliveTimeout(int httpKeepAliveTimeout)
    {
        this.httpKeepAliveTimeout = httpKeepAliveTimeout;
    }
    
    /**
     * @return
     */
    public int getHttpKeepAliveMax()
    {
        return this.httpKeepAliveMax;
    }
    
    /**
     * @param httpKeepAliveMax
     */
    public void setHttpKeepAliveMax(int httpKeepAliveMax)
    {
        this.httpKeepAliveMax = httpKeepAliveMax;
    }

    /**
     * @return
     */
    public int getLoggingLevel()
    {
        return this.loggingLevel;
    }
    
    /**
     * @param loggingLevel
     */
    public void setLoggingLevel(int loggingLevel)
    {
        this.loggingLevel = loggingLevel;
    }
    
    /**
     * @return
     */
    public String getLogFile()
    {
        return this.logFile;
    }
    
    /**
     * @param logFile
     */
    public void setLogFile(String logFile)
    {
        this.logFile = logFile;
    }

    /**
     * @return
     */
    public String getRootDirectory()
    {
        return this.rootDirectory;
    }
    
    /**
     * @param rootDirectory
     */
    public void setRootDirectory(String rootDirectory)
    {
        this.rootDirectory = rootDirectory;
    }

    /**
     * @return
     */
    public String getDefaultDocument()
    {
        return this.defaultDocument;
    }
    
    /**
     * @param defaultDocument
     */
    public void setDefaultDocument(String defaultDocument)
    {
        this.defaultDocument = defaultDocument;
    }
    
    /**
     * @return
     */
    public boolean isDebugMode()
    {
        return this.debugMode;
    }
    
    /**
     * @param debugMode
     */
    public void setDebugMode(boolean debugMode)
    {
        this.debugMode = debugMode;
    }
}
