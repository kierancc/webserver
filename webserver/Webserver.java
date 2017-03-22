package webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Kieran Chin Cheong
 * @version 1.0
 * @since 1.0
 */
public class Webserver
{
    // Static variables
    
    public static final String HTTP_VERSION = "HTTP/1.1"; // Server's declared HTTP version
    public static final String SERVER_VERSION = "Kieran's Webserver 1.0"; // Server's version string
    
    // Member variables
    
    private ServerSocket listeningSocket;
    private ExecutorService workerThreadPool;
    
    /**
     * Constructor
     */
    public Webserver()
    {
        
    }
    
    /**
     * Method to initialize the Webserver object. Creates the thread pool and the ServerSocket to listen on
     * @throws IOException
     */
    public void initialize() throws IOException
    {
        this.workerThreadPool = Executors.newFixedThreadPool(Configuration.GetConfiguration().getNumThreads());
        this.listeningSocket = new ServerSocket(Configuration.GetConfiguration().getPort());
    }
    
    /**
     * Method to start operation of the Webserver object
     * @throws Exception
     */
    public void run() throws Exception
    {
        // This is the main execution loop of the Webserver
        // Since all of the processing done for the request/response mechanism is done by worker threads
        // all that needs to be done here is queuing the incoming requests
        Logger.Log(Logger.INFORMATION, String.format("Webserver listening on address %s", this.listeningSocket.getLocalSocketAddress().toString()));
        
        while (true)
        {
            // Block waiting on an incoming connection
            Socket connectionSocket = this.listeningSocket.accept();
            
            // Queue a new work item
            Logger.Log(Logger.INFORMATION, String.format("Queuing new incoming connection from remote address : %s", connectionSocket.getRemoteSocketAddress()));
            this.workerThreadPool.submit(new Worker(connectionSocket));
        }
    }
}
