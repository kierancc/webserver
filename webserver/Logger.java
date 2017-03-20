package webserver;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

public class Logger implements Runnable
{
    // Logging levels
    public static final int ALWAYS = 0;
    public static final int CONNECTION = 1;
    public static final int ERROR = 2;
    public static final int WARNING = 3;
    public static final int INFORMATION = 4;
    
    private static Logger loggerSingleton = null;
    
    public static void Log(int logLevel, String line)
    {
        getLogger().log(logLevel, String.format("%s : TID %d : %s", new Date().toString(), Thread.currentThread().getId(), line));
    }
    
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
    
    public static void Start()
    {
        new Thread(getLogger()).start();
    }
    
    private static Logger getLogger()
    {
        if (loggerSingleton == null)
        {
            loggerSingleton = new Logger();
        }
        
        return loggerSingleton;
    }
    
    private ArrayBlockingQueue<String> lines;
    private PrintWriter logWriter;
    private int logLevel;
    
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
    
    private void log(int logLevel, String line)
    {
        if (logLevel <= this.logLevel)
        {
            this.lines.offer(line);
        }
    }
    
    @Override
    public void run()
    {
        try
        {
            while (true)
            {
                try
                {
                    String line = this.lines.take();
                    this.logWriter.println(line);
                    this.logWriter.flush();
                }
                catch (InterruptedException e)
                {
                    System.err.println("LOGGER: Caught InterruptedException, continuing...");
                    continue;
                }
                
            }
        }
        finally
        {
            this.logWriter.flush();
            this.logWriter.close();
        }
    }
}
