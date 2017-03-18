package webserver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.SimpleTimeZone;

public class HTTPResponse
{
    public static HTTPResponse BuildHTTPResponseWithBody(HTTPRequest request)
    {
        try
        {
            HTTPResponse response = new HTTPResponse();
            
            // Build the absolute path the requested resource on the local file system
            response.localAbsolutePath = Configuration.GetConfiguration().getRootDirectory() + request.getRequestTargetLocalPath();
            File file = new File(response.localAbsolutePath);
            
            // If the requested file does not exist, immediately return a 404 Not Found response
            if (! file.exists())
            {
                return HTTPResponse.BuildHTTPResponseWithoutBody(Status.NOT_FOUND);
            }
            // Else if the requested file exists, but can not be read, return a 403 Forbidden response
            else if (! file.canRead())
            {
                return HTTPResponse.BuildHTTPResponseWithoutBody(Status.FORBIDDEN);
            }
            // Else the file can be read, so we proceed with building the response
            else
            {
                // Determine the size of the file
                response.messageBodySize = file.length();
                
                // Determine (best effort) the type of file. This will determine the MIME type
                response.mimeType = Files.probeContentType(file.toPath());
                
                // Set status to OK
                response.responseCode = Status.OK;
                
                // Generate the header fields
                response.populateRequiredHeaderFields();

                return response;
            }
        }
        catch (Exception e)
        {
            // Unhandled exception hit during response generation
            // For simplicity's sake we will call this an internal server error
            return HTTPResponse.BuildHTTPResponseWithoutBody(Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    public static HTTPResponse BuildHTTPResponseWithoutBody(Status responseCode)
    {
        HTTPResponse response = new HTTPResponse();
        response.responseCode = responseCode;
        response.populateRequiredHeaderFields();
        
        return response;
    }
    
    private Status responseCode;
    private HashMap<String, String> headerFields = new HashMap<String, String>();
    private String localAbsolutePath;
    private long messageBodySize;
    private String mimeType;
    
    public HTTPResponse()
    {
        
    }
    
    public void Send(OutputStream stream) throws Exception
    {
        try(DataOutputStream ostream = new DataOutputStream(stream))
        {
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
            }
            
            ostream.flush();
        }
    }
    
    private void populateRequiredHeaderFields()
    {
        // Add header fields required for all response codes capable of being sent
        
        // Add the date header field
        SimpleDateFormat gmtDateFormatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        gmtDateFormatter.setTimeZone(new SimpleTimeZone(0, "GMT"));
        this.headerFields.put("date", gmtDateFormatter.format(new Date()));
        
        // Add the server header field
        this.headerFields.put("server", Webserver.SERVER_VERSION);
        
        // Add the connection header field
        if (Configuration.GetConfiguration().isEnableHTTPKeepAlive())
        {
            this.headerFields.put("connection", "Keep-Alive");
            
            // If HTTP 1.1 KeepAlive is enabled, also send that header field   
            this.headerFields.put("keep-alive", "timeout=" + Configuration.GetConfiguration().getHttpKeepAliveTimeout() + ", max=" + Configuration.GetConfiguration().getHttpKeepAliveMax());
        }
        else
        {
            this.headerFields.put("connection", "close");
        }
        
        // If the server is running in debug mode, include some extra information in the header fields
        if (Configuration.GetConfiguration().isDebugMode())
        {
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
            break;
        case HTTP_VERSION_NOT_SUPPORTED:
            break;
        case INTERNAL_SERVER_ERROR:
            break;
        case NOT_FOUND:
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
}
