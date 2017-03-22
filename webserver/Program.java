package webserver;

/**
 * @author Kieran Chin Cheong
 * @version 1.0
 * @since 1.0
 */
public class Program
{

    /**
     * Main method of the program.  Instantiates and starts all required objects
     * @param args
     */
    public static void main(String[] args)
    {
        // Start the Logger instance first
        Logger.Start();
        
        // Log the server's effective configuration
        Configuration.GetConfiguration().LogConfiguration();
        
        // Instantiate a Webserver object 
        Webserver server = new Webserver();
        
        try
        {
            // Initialize the server
            server.initialize();
        }
        catch (Exception e)
        {
            // If the server fails to initialize exit with a -1 return code
            System.err.println("Failed to initialize Webserver: \"" + e.toString() + "\"");
            Logger.Log(Logger.ERROR, "Failed to initialize Webserver: \"" + e.toString() + "\"");
            System.exit(-1);
        }
        
        try
        {
            // Run the server
            server.run();
        }
        catch (Exception e)
        {
            // If the system hits an unrecoverable exception exit with a -2 return code
            System.err.println("Error running Webserver: \"" + e.toString() + "\"");
            Logger.Log(Logger.ERROR, "Error running Webserver: \"" + e.toString() + "\"");
            System.exit(-2);
        }
    }

}
