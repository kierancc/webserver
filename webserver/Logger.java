package webserver;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author Kieran Chin Cheong
 * @version 1.0
 * @since 1.0
 */
public class Logger implements Runnable
{
    // Static variables
    
    // Logging levels
    public static final int ALWAYS = 0;
    public static final int CONNECTION = 1;
    public static final int ERROR = 2;
    public static final int WARNING = 3;
    public static final int INFORMATION = 4;
    
    // This is the singleton instance
    private static Logger loggerSingleton = null;
    
    /**
     * Static method to add a line to be logged to the server log
     * <p>
     * The line will be logged if the specified log level is below the specified log threshold. Note that this is best-effort only
     * If for some reason, there is no more room to queue the line to be added, it will be dropped so as to prevent blocking server threads
     * @param logLevel The log level
     * @param line the The line to be logged
     */
    public static void Log(int logLevel, String line)
    {
        getLogger().log(logLevel, String.format("%s : TID %d : %s", new Date().toString(), Thread.currentThread().getId(), line));
    }
    
    /**
     * Static method to log a completed request/response pair. Modeled after the Microsoft IIS connection log
     * @param request HTTPRequest object recieved
     * @param response HTTPResponse object sent
     * @param clientRemoteAddress The client's remote address
     * @param serverRemoteAddress The server's remote address
     */
    public static void LogConnection(HTTPRequest request, HTTPResponse response, String clientRemoteAddress, String serverRemoteAddress)
    {
        // It is possible to have a null request (for instance, if a bad request was received)
        // So in this case, do not include request details in the log line, since there aren't any
        if (request != null)
        {
            Log(Logger.CONNECTION, String.format("%s %s %s %s %d %s", clientRemoteAddress, serverRemoteAddress, 
                                                                                  request.getRequestMethod(), request.getRequestTarget(), 
                                                                                  response.getResponseCode().toCode(), request.getUserAgent()));
        }
        else
        {
            Log(Logger.CONNECTION, String.format("%s %s %s %s %d %s", clientRemoteAddress, serverRemoteAddress, 
                    "-", "-", 
                    response.getResponseCode().toCode(), "-"));
        }
    }
    
    /**
     * Static method to start the Logger
     */
    public static void Start()
    {
        new Thread(getLogger()).start();
    }
    
    /**
     * Static singleton accessor. Note that this is only accessible internally
     * @return the singleton Logger
     */
    private static Logger getLogger()
    {
        if (loggerSingleton == null)
        {
            loggerSingleton = new Logger();
        }
        
        return loggerSingleton;
    }
    
    // Member variables
    
    private ArrayBlockingQueue<String> lines;
    private PrintWriter logWriter;
    private int logLevel;
    
    /**
     * Private constructor.  Initializes the log queue and opens the Writer
     */
    private Logger()
    {
        this.lines = new ArrayBlockingQueue<String>(100);
        this.logLevel = Configuration.GetConfiguration().getLoggingLevel();
        
        try
        {
            this.logWriter = new PrintWriter(Configuration.GetConfiguration().getLogFile());
        }
        catch (IOException e)
        {
            System.err.println("LOGGER: Could not initialize log file : " + e.getMessage());
        }
    }
    
    /**
     * Method to queue a line to log. May only be internally used
     * <p>
     * @param logLevel The log level
     * @param line The line to queue
     */
    private void log(int logLevel, String line)
    {
        // Only queue the line if the log level is less than or equal to the threshold value
        if (logLevel <= this.logLevel)
        {
            this.lines.offer(line);
        }
    }
    
    /**
     * This is the main execution loop of the Logger
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        try
        {
            while (true)
            {
                try
                {
                    // Block on taking work from the queue, and then simply write the line
                    String line = this.lines.take();
                    this.logWriter.println(line);
                    this.logWriter.flush();
                }
                catch (InterruptedException e)
                {
                    // If the thread was interrupted while retrieving work. Simply continue
                    System.err.println("LOGGER: Caught InterruptedException, continuing...");
                    continue;
                }
                
            }
        }
        finally // Ensure that the writer flushes and properly closes if the program exits
        {
            this.logWriter.flush();
            this.logWriter.close();
        }
    }
}
