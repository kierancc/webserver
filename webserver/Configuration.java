package webserver;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Configuration
{
    private static Configuration configSingleton = null;
    
    public static Configuration GetConfiguration()
    {
        if (configSingleton == null)
        {
            configSingleton = Configuration.LoadConfiguration();
        }
        
        return configSingleton;
    }
    
    public static void SaveConfigurationToXML(Configuration config)
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
    
    public static Configuration LoadConfiguration()
    {
        Configuration config;
        
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
            // settings and return that
            System.err.println("Error loading saved configuration, using default configuration");
            config = new Configuration();
            config.SetDefaultConfiguration();
        }
        
        return config;
    }
    
    // Path to saved configuration file on filesystem
    private static final String CONFIG_PATH = "config.xml";

    // Configurable members

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

    public Configuration()
    {
    }
    
    public void SetDefaultConfiguration()
    {
        this.port = 8080;
        this.enableThreadPool = true;
        this.numThreads = 10;
        this.enableClientCaching = false;
        this.enableHTTPKeepAlive = true;
        this.httpKeepAliveTimeout = 3;
        this.httpKeepAliveMax = 5;
        this.loggingLevel = 4;
        this.logFile = "server_log.txt";
        this.rootDirectory = "C:\\webserver\\content";
        this.defaultDocument = "index.html";
        this.debugMode = true;
    }
    
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

    public int getPort()
    {
        return this.port;
    }
    
    public void setPort(int port)
    {
        this.port = port;
    }

    public boolean isEnableThreadPool()
    {
        return this.enableThreadPool;
    }
    
    public void setEnableThreadPool(boolean enableThreadPool)
    {
        this.enableThreadPool = enableThreadPool;
    }

    public int getNumThreads()
    {
        return this.numThreads;
    }
    
    public void setNumThreads(int numThreads)
    {
        this.numThreads = numThreads;
    }

    public boolean isEnableClientCaching()
    {
        return this.enableClientCaching;
    }
    
    public void setEnableClientCaching(boolean enableClientCaching)
    {
        this.enableClientCaching = enableClientCaching;
    }

    public boolean isEnableHTTPKeepAlive()
    {
        return this.enableHTTPKeepAlive;
    }
    
    public void setEnableHTTPKeepAlive(boolean enableHTTPKeepAlive)
    {
        this.enableHTTPKeepAlive = enableHTTPKeepAlive;
    }
    
    public int getHttpKeepAliveTimeout()
    {
        return this.httpKeepAliveTimeout;
    }
    
    public void setHttpKeepAliveTimeout(int httpKeepAliveTimeout)
    {
        this.httpKeepAliveTimeout = httpKeepAliveTimeout;
    }
    
    public int getHttpKeepAliveMax()
    {
        return this.httpKeepAliveMax;
    }
    
    public void setHttpKeepAliveMax(int httpKeepAliveMax)
    {
        this.httpKeepAliveMax = httpKeepAliveMax;
    }

    public int getLoggingLevel()
    {
        return this.loggingLevel;
    }
    
    public void setLoggingLevel(int loggingLevel)
    {
        this.loggingLevel = loggingLevel;
    }
    
    public String getLogFile()
    {
        return this.logFile;
    }
    
    public void setLogFile(String logFile)
    {
        this.logFile = logFile;
    }

    public String getRootDirectory()
    {
        return this.rootDirectory;
    }
    
    public void setRootDirectory(String rootDirectory)
    {
        this.rootDirectory = rootDirectory;
    }

    public String getDefaultDocument()
    {
        return this.defaultDocument;
    }
    
    public void setDefaultDocument(String defaultDocument)
    {
        this.defaultDocument = defaultDocument;
    }
    
    public boolean isDebugMode()
    {
        return this.debugMode;
    }
    
    public void setDebugMode(boolean debugMode)
    {
        this.debugMode = debugMode;
    }
}
