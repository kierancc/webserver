package webserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * @author Kieran Chin Cheong
 *
 */
public class Webserver
{
    public static final String HTTP_VERSION = "HTTP/1.1";
    public static final String SERVER_VERSION = "Kieran's Webserver/1.1";
    private ServerSocket listeningSocket;
    private ExecutorService workerThreadPool;
    
    /**
     * Creates a new instance of the Webserver class, configured using the provided Configuration object
     * @param config
     */
    public Webserver()
    {
        
    }
    
    public void initialize() throws Exception
    {
        this.workerThreadPool = Executors.newFixedThreadPool(Configuration.GetConfiguration().getNumThreads());
        this.listeningSocket = new ServerSocket(Configuration.GetConfiguration().getPort());
    }
    
    public void run() throws Exception
    {
        // This is the main execution loop of the Webserver
        // Since all of the processing done for the request/response mechanism is done by worker threads
        // all that needs to be done here is queuing the incoming requests
        
        Logger.Log(String.format("Webserver listening on address %s", this.listeningSocket.getLocalSocketAddress().toString()));
        
        while (true)
        {
            // Block waiting on an incoming connection
            Socket connectionSocket = this.listeningSocket.accept();
            
            // Queue a new work item
            this.workerThreadPool.submit(new Worker(connectionSocket));
        }
    }
}
