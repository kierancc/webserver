package webserver;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Kieran Chin Cheong
 * @version 1.0
 * @since 1.0
 */
public class Worker implements Runnable
{
    // Member variables
    
    private Socket connectionSocket;
    private int keepAliveTimeout;
    private int keepAliveMax;
    private int requestCount;
    
    /**
     * Constructor
     * @param connectionSocket the Socket object representing a connection to a client
     */
    public Worker(Socket connectionSocket)
    {
        this.connectionSocket = connectionSocket;
        this.keepAliveTimeout = Configuration.GetConfiguration().getHttpKeepAliveTimeout();
        
        // Set this value to 1 if HTTP KeepAlive is not enabled, this will ensure only one request will be served in the lifetime of the connection
        this.keepAliveMax = Configuration.GetConfiguration().isEnableHTTPKeepAlive() ? Configuration.GetConfiguration().getHttpKeepAliveMax() : 1;
        this.requestCount = 0;
    }

    /**
     * This method is the main execution path of the Worker object
     * <p>
     * Here it will read and parse the incoming request, and attempt to create and send the appropriate response
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        // This is the main execution path of the Worker
        // We have accepted a connection from a client, we will attempt to parse the provided input into a valid HTTP request that we are equipped to handle
        // If the incoming HTTP request is valid, and we can handle it, we then create a valid HTTP response and send it back to the client
        // If the HTTP 1.1 KeepAlive feature is enabled, the connection will remain open for a defined window of time. If no new request is received
        // within that window, the connection is then closed. If HTTP 1.1 KeepAlive is not enabled, the connection is immediately closed.
        // Any errors detected in this process that necessitate a response to be returned to the client will be created including the appropriate HTTP status code
        try
        {
            Logger.Log(Logger.INFORMATION, String.format("Handling HTTP request from remote address %s", this.connectionSocket.getRemoteSocketAddress().toString()));
            
            // Potentially loop while more requests may be served by the connection
            while (this.requestCount < this.keepAliveMax)
            {
                // Declare the request and response objects
                HTTPRequest request = null;
                HTTPResponse response = null;
                
                // Declare the KeepAlive timer so that it is in scope to be cancelled if necessary
                Timer keepAliveTimeoutTimer = new Timer();
                
                try
                {
                    // Attempt to parse the incoming HTTP request
                    // If this is not the first request handled by this worker (e.g. in the HTTP KeepAlive scenario)
                    // then set the timer to cause a timeout if no input is received from the client on this connection
                    // within the specified window                  
                    if (Configuration.GetConfiguration().isEnableHTTPKeepAlive() && this.requestCount > 0)
                    {
                        final long workerThreadID = Thread.currentThread().getId();
                        
                        Logger.Log(Logger.INFORMATION, String.format("Scheduling KeepAlive timeout timer for TID %d", workerThreadID));
                        
                        // For simplicitly's sake use an anonymous inner class to close the connection socket since this is a simple operation
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
                        }, this.keepAliveTimeout * 1000);
                    }
                    
                    // Attempt to read and parse the request
                    request = HTTPRequest.BuildHTTPRequestFromInput(this.connectionSocket.getInputStream());
                    
                    Logger.Log(Logger.INFORMATION, "Successfully parsed incoming request");
                    
                    // Valid input was received, so cancel the HTTP KeepAlive timeout timer if it was enabled
                    if (Configuration.GetConfiguration().isEnableHTTPKeepAlive() && this.requestCount > 0)
                    {
                        Logger.Log(Logger.INFORMATION, String.format("Cancelling KeepAlive timeout timer for TID %d", Thread.currentThread().getId()));
                        keepAliveTimeoutTimer.cancel();
                        keepAliveTimeoutTimer.purge();
                    }

                    // Attempt to build a response to the request
                    Logger.Log(Logger.INFORMATION, "Building response");
                    response = HTTPResponse.BuildHTTPResponseWithBody(request, request.isKeepAliveRequested() && Configuration.GetConfiguration().isEnableHTTPKeepAlive(), this.requestCount);
                    Logger.Log(Logger.INFORMATION, "Response built");

                }
                catch (RequestException re)
                {
                    // A problem was encountered when receiving or parsing the request. Send back an appropriate HTTP response
                    // Note that because the request could not be processed, we do not know if the client requested HTTP KeepAlive
                    // so we will default to this being false, even if support is enabled in the server configuration
                    response = HTTPResponse.BuildHTTPResponseWithoutBody(re.getErrorCode(), false, this.requestCount);
                }
                catch (SocketException se)
                {
                    // If we have encountered a SocketException that was not converted to a HttpKeepAliveTimeoutException earlier
                    // then the connection was unexpectedly closed by the peer.  Here we should just make sure to clean up our state
                    // and then return, since the connection has been closed
                    Logger.Log(Logger.WARNING, "Warning : Connection unexpectedly closed by peer");
                    keepAliveTimeoutTimer.cancel();
                    keepAliveTimeoutTimer.purge();
                    return;
                }
                        
                // Now we try to send the response to the client
                Logger.Log(Logger.INFORMATION, "Sending response");
                response.Send(this.connectionSocket.getOutputStream());
                Logger.Log(Logger.INFORMATION, "Response sent");
                
                // Log the request/response connection line
                Logger.LogConnection(request, response, this.connectionSocket.getRemoteSocketAddress().toString(), this.connectionSocket.getLocalSocketAddress().toString());
                
                // Increment the request counter
                this.requestCount++;
            }
            
            // We have reached the maximum number of requests that can be served for this connection
            // Close the connection to the client
            Logger.Log(Logger.INFORMATION, String.format("Closing connection to clienet with remote address : %s", this.connectionSocket.getRemoteSocketAddress()));
            this.connectionSocket.close();
        }
        catch (IOException ioe)
        {
            // We could not read from or write to the socket
            Logger.Log(Logger.ERROR, String.format("Error reading from or writing to socket %s", ioe.toString()));
        }
        catch (HttpKeepAliveTimeoutException kae)
        {
            // This has been logged already, do nothing
        }
        catch (ResponseException re)
        {
            Logger.Log(Logger.ERROR, String.format("Error responding to request, caught ResponseException : %s", re.toString()));
        }
        catch (Exception e)
        {
            // We have hit an unhandled exception, log this
            Logger.Log(Logger.ERROR, String.format("Error responding to request, unhandled exception : %s", e.toString()));
        }
        finally
        {
            try
            {
                // Ensure that in all cases when a Worker exits it attempts to close the connection socket
                this.connectionSocket.close();
            }
            catch (IOException e)
            {
                Logger.Log(Logger.WARNING, String.format("Warning : exiting Worker could not close connection : %s", e.toString()));
            }
        }
    }
}
