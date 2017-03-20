package webserver;

public class Program
{

    public static void main(String[] args)
    {
        Webserver server = new Webserver();
        Logger.Start();
        Configuration.GetConfiguration().LogConfiguration();
        
        try
        {
            server.initialize();
        }
        catch (Exception e)
        {
            System.err.println("Failed to initialize Webserver: \"" + e.toString() + "\"");
            Logger.Log(Logger.ERROR, "Failed to initialize Webserver: \"" + e.toString() + "\"");
        }
        
        try
        {
            server.run();
        }
        catch (Exception e)
        {
            System.err.println("Error running Webserver: \"" + e.toString() + "\"");
            Logger.Log(Logger.ERROR, "Error running Webserver: \"" + e.toString() + "\"");
        }
    }

}
