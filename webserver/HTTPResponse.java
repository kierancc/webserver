package webserver;

import java.io.DataOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.SimpleTimeZone;

/**
 * @author Kieran Chin Cheong
 * @version 1.0
 * @since 1.0
 */
public class HTTPResponse
{
    // Static methods
    
    /**
     * Static method that creates an HTTP response object that includes a message body for a valid received HTTP request
     * @param request the HTTPRequest object to build a HTTPResponse for
     * @param isKeepAliveEnabled specifies whether or not HTTP KeepAlive should be enabled for this HTTPResponse
     * @param responseNumber the number of this response in the sequence of an HTTP persistent connection
     * @return the created HTTPResponse object
     */
    public static HTTPResponse BuildHTTPResponseWithBody(HTTPRequest request, boolean isKeepAliveEnabled, int responseNumber)
    {
        try
        {
            // Create the new HTTPResponse object
            HTTPResponse response = new HTTPResponse(isKeepAliveEnabled, responseNumber);
            
            // Build the absolute path to the requested resource on the local file system
            response.localAbsolutePath = Configuration.GetConfiguration().getRootDirectory() + request.getRequestTargetLocalPath();
            File file = new File(response.localAbsolutePath);
            
            // If the requested file does not exist, immediately return a 404 Not Found response
            if (! file.exists())
            {
                return HTTPResponse.BuildHTTPResponseWithoutBody(Status.NOT_FOUND, isKeepAliveEnabled, responseNumber);
            }
            // Else if the requested file exists, but can not be read, return a 403 Forbidden response
            else if (! Files.isReadable(file.toPath()))
            {
                return HTTPResponse.BuildHTTPResponseWithoutBody(Status.FORBIDDEN, isKeepAliveEnabled, responseNumber);
            }
            // Else the file can be read, so we proceed with building the response
            else
            {
                // Determine the size of the file
                response.messageBodySize = file.length();
                
                // Determine (best effort) the type of file. This will determine the MIME type of the response
                response.mimeType = Files.probeContentType(file.toPath());
                
                // Set status to OK
                response.responseCode = Status.OK;
                
                // Populate required header fields
                response.populateRequiredHeaderFields();

                return response;
            }
        }
        catch (Exception e)
        {
            // Unhandled exception was hit during response generation
            // For simplicity's sake we will call this an internal server error
            return HTTPResponse.BuildHTTPResponseWithoutBody(Status.INTERNAL_SERVER_ERROR, isKeepAliveEnabled, responseNumber);
        }
    }
    
    /**
     * Static method that creates an HTTP response object without a message body for a valid received HTTP request
     * @param responseCode HTTP response code to use in the created HTTPRequest
     * @param isKeepAliveEnabled specifies whether or not HTTP KeepAlive should be enabled for this HTTPResponse
     * @param responseNumber the number of this response in the sequence of an HTTP persistent connection
     * @return the created HTTPResponse object
     */
    public static HTTPResponse BuildHTTPResponseWithoutBody(Status responseCode, boolean isKeepAliveEnabled, int responseNumber)
    {
        // Create the new HTTPResponse object
        HTTPResponse response = new HTTPResponse(isKeepAliveEnabled, responseNumber);
        
        // Set the response code
        response.responseCode = responseCode;
        
        // Populate required header fields
        response.populateRequiredHeaderFields();
        
        return response;
    }
    
    // Member variables
    
    private Status responseCode;
    private HashMap<String, String> headerFields = new HashMap<String, String>();
    private String localAbsolutePath;
    private long messageBodySize;
    private String mimeType;
    private boolean isKeepAliveEnabled;
    private int responseNumber;
    private int remainingResponses;
    
    /**
     * Constructor
     * @param isKeepAliveEnabled specifies if HTTPKeepAlive is enabled for this response
     * @param responseNumber the number of this response in the HTTP persistent connection
     */
    public HTTPResponse(boolean isKeepAliveEnabled, int responseNumber)
    {
        this.isKeepAliveEnabled = isKeepAliveEnabled;
        this.responseNumber = responseNumber;
        this.remainingResponses = Configuration.GetConfiguration().getHttpKeepAliveMax() - this.responseNumber;
    }
    
    /**
     * Blank constructor
     */
    public HTTPResponse()
    {
        
    }
    
    /**
     * This method sends the HTTP response in the correct format over the network to the client
     * @param stream OutputStream to the client
     * @throws ResponseException
     */
    public void Send(OutputStream stream) throws ResponseException
    {
        try
        {
            DataOutputStream ostream = new DataOutputStream(stream);
            
            // Write the start line of the response, which looks like this
            // HTTP/1.1 200 OK
            ostream.write(String.format("%s %s %s\r\n", Webserver.HTTP_VERSION, this.responseCode.toCode(), this.responseCode.toString()).getBytes(StandardCharsets.US_ASCII));
            
            // Write the headers
            for (String key : this.headerFields.keySet())
            {
                ostream.write(String.format("%s: %s\r\n", key, this.headerFields.get(key)).getBytes(StandardCharsets.US_ASCII));
            }
            
            // Write the blank line between the headers and the message body
            ostream.write("\r\n".getBytes(StandardCharsets.US_ASCII));
            
            // If the local absolute path is specified, then we should also send the contents of the file
            if (this.localAbsolutePath != null && !this.localAbsolutePath.equals(""))
            {
                byte[] fileBytes = Files.readAllBytes(Paths.get(this.localAbsolutePath));
                
                // Write the bytes to the output stream
                ostream.write(fileBytes);
                
                fileBytes = null; // Ensure that a reference to the byte array is only kept for as little time as possible
            }
            
            ostream.flush();
            
            // If HTTP KeepAlive is either not enabled or not valid for this response, close the output stream
            // otherwise, do not close it, as this would inadvertently close the connection to the client
            if (! this.isKeepAliveEnabled)
            {
                ostream.close();
            }
        }
        catch (Exception e)
        {
            // If for any reason we could not completely send the response, we will throw a ResponseException
            // to be handled by the working thread
            throw new ResponseException(String.format("Could not send response : %s", e.toString()));
        }
    }
    
    /**
     * Method used to generate HTTP response header/value pairs
     * <p>
     * Different headers are required depending on the HTTP response code
     */
    private void populateRequiredHeaderFields()
    {
        // Add header fields required for all response codes capable of being sent
        
        // Add the date header field in the expected format (GMT time zone)
        SimpleDateFormat gmtDateFormatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        gmtDateFormatter.setTimeZone(new SimpleTimeZone(0, "GMT"));
        this.headerFields.put("date", gmtDateFormatter.format(new Date()));
        
        // Add the server header field
        this.headerFields.put("server", Webserver.SERVER_VERSION);
        
        // Add the connection header field
        // If HTTP KeepAlive is enabled and this is not the last allowable response, indicate this
        // and also provide the expected "keep-alive" header values
        if (this.isKeepAliveEnabled && this.remainingResponses > 0)
        {
            this.headerFields.put("connection", "keep-alive");
            
            // If HTTP 1.1 KeepAlive is enabled, also send that header field
            this.headerFields.put("keep-alive", String.format("timeout=%d,max=%d", Configuration.GetConfiguration().getHttpKeepAliveTimeout(), this.remainingResponses));
        }
        // Otherwise indicate that the connection will be closed after the response has been sent
        else
        {
            this.headerFields.put("connection", "close");
        }
        
        // If the server is running in debug mode, include some extra information in the header fields as custom headers
        if (Configuration.GetConfiguration().isDebugMode())
        {
            // Add the thread ID of the worker thread handling the request
            this.headerFields.put("server-thread-id", String.valueOf(Thread.currentThread().getId()));
        }
        
        // Add header fields specific to each possible response code
        switch(this.responseCode)
        {
            case BAD_REQUEST:
                break;
            case CONTINUE:
                break;
            case FORBIDDEN:
                // Content-Length
                this.headerFields.put("content-length", "0");
                break;
            case HTTP_VERSION_NOT_SUPPORTED:
                break;
            case INTERNAL_SERVER_ERROR:
                break;
            case NOT_FOUND:
                // Content-Length
                this.headerFields.put("content-length", "0");
                break;
            case NOT_IMPLEMENTED:
                break;
            case NOT_MODIFIED:
                break;
            case OK:
                // Content-Type
                this.headerFields.put("content-type", this.mimeType);
                // Content-Length
                this.headerFields.put("content-length", String.valueOf(this.messageBodySize));
                // ETag
                // TODO Implement this
                break;
            case SERVICE_UNAVAILABLE:
                break;
            case UNAUTHORIZED:
                break;
            default:
                break;
        }
    }
    
    // Simple getters
    
    /**
     * @return
     */
    public Status getResponseCode()
    {
        return this.responseCode;
    }
}
