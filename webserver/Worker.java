package webserver;

import java.io.*;
import java.net.Socket;

public class Worker implements Runnable
{
    private Socket connectionSocket;
    
    public Worker(Socket connectionSocket)
    {
        this.connectionSocket = connectionSocket;
    }

    @Override
    public void run()
    {
        // This is the main execution path of the Worker
        // We have accepted a connection from a client, we will attempt to parse the provided input into a valid HTTP request that we are equipped to handle
        // If the incoming HTTP request is valid, and we can handle it, we then create a valid HTTP response and send it back to the client
        // If the HTTP 1.1 KeepAlive feature is enabled, the connection will remain open for a defined window of time. If no new request is received
        // within that window, the connection is then closed. If HTTP 1.1 KeepAlive is not enabled, the connection is immediately closed.
        // Any errors detected in this process will cause a response to be returned to the client including the appropriate HTTP status code
        try
        {
            // Declare the request and response objects
            HTTPRequest request = null;
            HTTPResponse response = null;
            
            try
            {
                Logger.Log(Logger.INFORMATION, String.format("Incoming request from remote address %s", this.connectionSocket.getRemoteSocketAddress().toString()));
                
                // Attempt to parse the incoming HTTP request
                request = new HTTPRequest(this.connectionSocket.getInputStream());
                
                Logger.Log(Logger.INFORMATION, "Successfully parsed incoming request");
                
                // If no exceptions were encountered, the request was valid so we will now attempt to process it
                // If output caching is enabled, check our cache to see if there is a valid cached response that can be used
                if (Configuration.GetConfiguration().isEnableContentCaching())
                {
                    // TODO Implement this
                    response = null;
                }
                else // No cache hit, so we must build the response
                {
                    Logger.Log(Logger.INFORMATION, "Building response");
                    response = HTTPResponse.BuildHTTPResponseWithBody(request);
                    Logger.Log(Logger.INFORMATION, "Response built");
                }
            }
            catch (RequestException re)
            {
                // A problem was encountered when receiving or parsing the request. Send back an appropriate HTTP response
                response = HTTPResponse.BuildHTTPResponseWithoutBody(re.getErrorCode());
            }
                    
            // Now we try to send the response to the client
            Logger.Log(Logger.INFORMATION, "Sending response");
            response.Send(this.connectionSocket.getOutputStream());
            Logger.Log(Logger.INFORMATION, "Response sent");
            
            // Log the request/response connection line
            Logger.LogConnection(request, response, this.connectionSocket.getRemoteSocketAddress().toString(), this.connectionSocket.getLocalSocketAddress().toString());
            
            // Close the connection
            // TODO change this behaviour for KeepAlive
            this.connectionSocket.close();
            Logger.Log(Logger.INFORMATION, String.format("Connection closed to remote address %s", this.connectionSocket.getRemoteSocketAddress().toString()));
        }
        catch (ResponseException re)
        {
            // TODO Figure out what happens here...
            Logger.Log(Logger.ERROR, String.format("Error responding to request, caught ResponseException : %s", re.toString()));
        }
        catch (Exception e)
        {
            // We have hit an unhandled exception, log this
            Logger.Log(Logger.ERROR, String.format("Error responding to request, unhandled exception : %s", e.toString()));
        }
    }
}
