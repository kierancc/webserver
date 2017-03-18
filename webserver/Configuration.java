package webserver;

public class Configuration
{
    private static Configuration configSingleton = null;
    
    public static Configuration GetConfiguration()
    {
        if (configSingleton == null)
        {
            configSingleton = new Configuration();
            configSingleton.loadConfiguration();
        }
        
        return configSingleton;
    }
    
    
    // Path to saved configuration file on filesystem
    private static final String configPath = "config.xml";

    // Configurable members

    // Listening port
    private int port;

    // Thread pooling
    private boolean enableThreadPool;
    private int numThreads;

    // Client-side Caching
    private boolean enableClientCaching;

    // Server-side Caching Options
    private boolean enableContentCaching;
    private int maxContentCacheSize; // Maximum cache size in MB per working thread
    private boolean enableOutputCaching;
    private int maxOutputCacheSize;

    // HTTP 1.1 KeepAlive
    private boolean enableHTTPKeepAlive;
    private int httpKeepAliveTimeout;
    private int httpKeepAliveMax;

    // Logging level
    private int loggingLevel;

    // Root directory
    private String rootDirectory;

    // Default document
    private String defaultDocument; // Specifies the default document, e.g.
                                    // index.html

    public Configuration()
    {
        this.loadConfiguration();
    }

    // TODO Implement this to load from a file
    private void loadConfiguration()
    {
        this.port = 8080;
        this.enableThreadPool = true;
        this.numThreads = 1;
        this.enableClientCaching = false;
        this.enableContentCaching = false;
        this.maxContentCacheSize = 0;
        this.enableOutputCaching = false;
        this.maxOutputCacheSize = 0;
        this.enableHTTPKeepAlive = false;
        this.httpKeepAliveTimeout = 5;
        this.httpKeepAliveMax = 100;
        this.loggingLevel = 0;
        this.rootDirectory = "C:\\webserver\\content";
        this.defaultDocument = "index.html";
    }

    public int getPort()
    {
        return this.port;
    }

    public boolean isEnableThreadPool()
    {
        return this.enableThreadPool;
    }

    public int getNumThreads()
    {
        return this.numThreads;
    }

    public boolean isEnableClientCaching()
    {
        return this.enableClientCaching;
    }

    public boolean isEnableContentCaching()
    {
        return this.enableContentCaching;
    }

    public int getMaxContentCacheSize()
    {
        return this.maxContentCacheSize;
    }
    
    public boolean isEnableOutputCaching()
    {
        return this.enableOutputCaching;
    }
    
    public int getMaxOutputCacheSize()
    {
        return this.maxOutputCacheSize;
    }

    public boolean isEnableHTTPKeepAlive()
    {
        return this.enableHTTPKeepAlive;
    }
    
    public int getHttpKeepAliveTimeout()
    {
        return this.httpKeepAliveTimeout;
    }
    
    public int getHttpKeepAliveMax()
    {
        return this.httpKeepAliveMax;
    }

    public int getLoggingLevel()
    {
        return this.loggingLevel;
    }

    public String getRootDirectory()
    {
        return this.rootDirectory;
    }

    public String getDefaultDocument()
    {
        return this.defaultDocument;
    }
}