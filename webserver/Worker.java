package webserver;

import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class Worker implements Runnable
{
    private Socket connectionSocket;
    private int keepAliveTimeout;
    private int keepAliveMax;
    private int requestCount;
    
    public Worker(Socket connectionSocket)
    {
        this.connectionSocket = connectionSocket;
        this.keepAliveTimeout = Configuration.GetConfiguration().getHttpKeepAliveTimeout();
        this.keepAliveMax = Configuration.GetConfiguration().isEnableHTTPKeepAlive() ? Configuration.GetConfiguration().getHttpKeepAliveMax() : 1;
        this.requestCount = 0;
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
            Logger.Log(Logger.INFORMATION, String.format("Handling HTTP request from remote address %s", this.connectionSocket.getRemoteSocketAddress().toString()));
            
            while (this.requestCount < this.keepAliveMax)
            {
                // Declare the request and response objects
                HTTPRequest request = null;
                HTTPResponse response = null;
                
                try
                {
                    // Attempt to parse the incoming HTTP request
                    // If this is not the first request handled by this worker (e.g. in the HTTP KeepAlive scenario)
                    // then set the timer to cause a timeout if no input is received from the client on this connection
                    // within the specified window
                    Timer keepAliveTimeoutTimer = new Timer();
                    
                    if (Configuration.GetConfiguration().isEnableHTTPKeepAlive() && this.requestCount > 0)
                    {
                        final long workerThreadID = Thread.currentThread().getId();
                        
                        Logger.Log(Logger.INFORMATION, String.format("Scheduling KeepAlive timeout timer for TID %d", workerThreadID));
                        
                        keepAliveTimeoutTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Logger.Log(Logger.INFORMATION, String.format("KeepAlive timeout hit for TID %d", workerThreadID));
                                
                                try
                                {
                                    connectionSocket.close();
                                }
                                catch (IOException e)
                                {
                                    Logger.Log(Logger.ERROR, String.format("Error closing connected socket due to KeepAlive timeout : %s", e.toString()));
                                }
                            }
                        }, Configuration.GetConfiguration().getHttpKeepAliveTimeout() * 1000);
                    }
                    
                    request = new HTTPRequest(this.connectionSocket.getInputStream());
                    
                    Logger.Log(Logger.INFORMATION, "Successfully parsed incoming request");
                    
                    // Input was received, so cancel the HTTP KeepAlive timeout timer if it was enabled
                    if (Configuration.GetConfiguration().isEnableHTTPKeepAlive() && this.requestCount > 0)
                    {
                        Logger.Log(Logger.INFORMATION, String.format("Cancelling KeepAlive timeout timer for TID %d", Thread.currentThread().getId()));
                        keepAliveTimeoutTimer.cancel();
                    }
                    
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
                        response = HTTPResponse.BuildHTTPResponseWithBody(request, request.isKeepAliveRequested() && Configuration.GetConfiguration().isEnableHTTPKeepAlive(), this.requestCount);
                        Logger.Log(Logger.INFORMATION, "Response built");
                    }
                }
                catch (RequestException re)
                {
                    // A problem was encountered when receiving or parsing the request. Send back an appropriate HTTP response
                    // Note that because the request could not be processed, we do not know if the client requested HTTP KeepAlive
                    // so we will default to this being false, even if support is enabled in the server configuration
                    response = HTTPResponse.BuildHTTPResponseWithoutBody(re.getErrorCode(), false, this.requestCount);
                }
                        
                // Now we try to send the response to the client
                Logger.Log(Logger.INFORMATION, "Sending response");
                response.Send(this.connectionSocket.getOutputStream());
                Logger.Log(Logger.INFORMATION, "Response sent");
                
                // Log the request/response connection line
                Logger.LogConnection(request, response, this.connectionSocket.getRemoteSocketAddress().toString(), this.connectionSocket.getLocalSocketAddress().toString());
                
                this.requestCount++;
            }
            
            // Close the connection to the client
            Logger.Log(Logger.INFORMATION, String.format("Closing connection to clienet with remote address : %s", this.connectionSocket.getRemoteSocketAddress()));
            this.connectionSocket.close();
        }
        catch (HttpKeepAliveTimeoutException kae)
        {
            // This has been logged already, do nothing
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
