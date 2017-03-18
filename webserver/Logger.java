package webserver;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

public class Logger implements Runnable
{
    private static Logger loggerSingleton = null;
    
    public static void Log(String line)
    {
        getLogger().log(String.format("%s : TID %d : %s", new Date().toString(), Thread.currentThread().getId(), line));
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
    
    private Logger()
    {
        this.lines = new ArrayBlockingQueue<String>(100);
        
        try
        {
            this.logWriter = new PrintWriter(Configuration.GetConfiguration().getLogFile());
        }
        catch (IOException e)
        {
            System.err.println("LOGGER: Could not initialize log file : " + e.getMessage());
        }
    }
    
    private void log(String line)
    {
        this.lines.offer(line);
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
