package webserver;

import java.io.*;
import java.net.SocketException;
import java.util.HashMap;
import java.util.regex.*;

public class HTTPRequest
{
    private static final String[] SUPPORTED_REQUEST_METHODS = {"GET"};
    private static final String REQUEST_START_LINE_PATTERN = "^(.+)\\s(.+)\\s(.+)$";
    private static final String HEADER_LINE_PATTERN = "^(.+?):\\s*(.+)\\s*$";
    private String requestMethod;
    private String requestTarget;
    private String httpVersion;
    private HashMap<String, String> headerFields = new HashMap<String, String>();
    private boolean keepAliveRequested;
    
    public HTTPRequest(InputStream stream) throws Exception
    {
        // Attempt to parse the incoming request, throw any encountered exceptions so that
        // an appropriate error code can be returned to the client

        try
        {
            // NOTE: This reader should not be closed; this will cause the connection to the client to be closed
            // before any response can be sent back to the client.  Instead, the connection should be closed
            // after the response is sent, if HTTP KeepAlive is not enabled.  If it is enabled, the connection will be managed
            // and closed when appropriate
            // TODO KeepAlive
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            
            // Read and parse the first line of the request.  This should be in form of something like
            // GET /index.html HTTP/1.1
            // Anything else is invalid
            // Note that since we are using the readLine() function of our reader, any CR and LF characters will be removed
            // This is OK because since a line was read, then they were there in the request.  So as long as the preceding
            // line conforms to the HTTP specification there is no problem
            String line;
            
            // This line will block until input is received.  In the case of a persistent connection that does not receive
            // another request from the client, the thread will block there until timed out, at which point the socket will be closed.
            // This isn't really an error since it is expected that this could happen due to the design of the client
            try
            {
                line = reader.readLine();
            }
            catch (SocketException se)
            {
                Logger.Log(Logger.INFORMATION, "Connection socket closed due to timeout or by peer");
                throw new HttpKeepAliveTimeoutException();
            }
            
            // For debug purposes, log a line if control reaches here indicating that the above did not happen
            Logger.Log(Logger.INFORMATION, String.format("HTTP Request received, start-line : %s", line));
            
            Pattern compiledStartLinePattern = Pattern.compile(REQUEST_START_LINE_PATTERN);
            Matcher startLineMatcher = compiledStartLinePattern.matcher(line);
            
            if (startLineMatcher.find())
            {
                this.requestMethod = startLineMatcher.group(1);
                this.requestTarget = startLineMatcher.group(2);
                this.httpVersion = startLineMatcher.group(3);
            }
            else
            {
                throw new RequestException(Status.BAD_REQUEST, "Invalid start line");
            }
            
            // Ensure that the request method provided is supported
            // If it is not supported, we need to send back the appropriate response
            if (! this.isMethodSupported(this.requestMethod))
            {
                throw new RequestException(Status.NOT_IMPLEMENTED, "Method " + this.requestMethod + " not implemented");
            }
            
            // Check that a specific resource was requested, otherwise apply the default document name if only a directory was requested
            if (this.requestTarget.equals("/"))
            {
                Logger.Log(Logger.INFORMATION, "Specific document not requested, applying default document");
                this.requestTarget += Configuration.GetConfiguration().getDefaultDocument();
            }
            
            // Next we read and parse any provided request headers. These have a form like
            // field-name:[optional white space]field-value[optional white space]
            // The end of the list of headers is denoted by a blank line
            // Note that the only required header is "host", so if we don't find it by the time
            // the blank line is reached, this is an invalid request
            Pattern compiledHeaderLinePattern = Pattern.compile(HEADER_LINE_PATTERN);
            Matcher headerLineMatcher;
            line = reader.readLine();
            
            while (! "".equals(line))
            {
                // In this loop if a null line is ever encountered, this is an unexpected end of request
                if (line == null)
                {
                    throw new RequestException(Status.BAD_REQUEST, "Unexpected end of request");
                }
                
                headerLineMatcher = compiledHeaderLinePattern.matcher(line);
                
                // Add the header key/value pair
                if (headerLineMatcher.find())
                {
                    // For simplicity's sake we will convert all field names to lower case
                    String fieldName = headerLineMatcher.group(1).toLowerCase();
                    String fieldValue = headerLineMatcher.group(2);
                    
                    // Handle special case where if a duplicate file name is encountered, it should be appended to the previous, seperated by a comma
                    if (this.headerFields.containsKey(fieldName))
                    {
                        String newValue = this.headerFields.get(fieldName) + "," + fieldValue;
                        this.headerFields.replace(fieldName, newValue);
                    }
                    else
                    {
                        this.headerFields.put(fieldName, fieldValue);
                    }
                }
                // If the line does not match, it is incorrectly formatted
                else
                {
                    throw new RequestException(Status.BAD_REQUEST, "Incorrectly formatted request header field/value pair");
                }
                
                // Read the next line of the request
                line = reader.readLine();
            }
            
            // At this point we have finished processing the provided headers.  So we need to check that the host header was provided
            if (! this.headerFields.containsKey("host"))
            {
                throw new RequestException(Status.BAD_REQUEST, "Missing host header");
            }
            
            // Check if the client sent a "connection: keep-alive" header.  If it did, and KeepAlive is enabled, save this information
            // for later usage by the server
            if (this.headerFields.containsKey("connection") && this.headerFields.get("connection").toLowerCase().equals("keep-alive"))
            {
                this.keepAliveRequested = true;
            }
            else
            {
                this.keepAliveRequested = false;
            }
            
            // If control has reached this point, the request received is valid.  A message body could follow, but it has no meaning
            // in the context of a GET request.   
            // However, we must ensure that all of the provided input is read, even if we do not need it.  This is necessary if KeepAlive is enabled
            // to ensure that the next time we read from this stream, we have fresh input.
            // According to the HTTP specification, a GET request that provides no content-length header has no message body
            // However, if for some reason a content-length was provided, simply read that number of bytes
            int messageBodyLength = this.getMessageBodyLength();
            
            for (int i = 0; i < messageBodyLength; i++)
            {
                reader.read(); // read one
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public String getRequestTargetLocalPath()
    {
        return this.requestTarget.replace('/', '\\');
    }
    
    public String getRequestMethod()
    {
        return this.requestMethod;
    }
    
    public String getRequestTarget()
    {
        return this.requestTarget;
    }
    
    public String getUserAgent()
    {
        if (this.headerFields.containsKey("user-agent"))
        {
            return this.headerFields.get("user-agent");
        }
        else
        {
            return "";
        }
    }
    
    public boolean isKeepAliveRequested()
    {
        return this.keepAliveRequested;
    }
    
    private boolean isMethodSupported(String method)
    {
        for (int i = 0; i < SUPPORTED_REQUEST_METHODS.length; i++)
        {
            if(SUPPORTED_REQUEST_METHODS[i].toUpperCase().equals(method.toUpperCase()))
            {
                return true;
            }
        }
        
        return false;
    }
    
    private int getMessageBodyLength()
    {
        if (this.headerFields.containsKey("content-length"))
        {
            return Integer.parseInt(this.headerFields.get("content-length"));
        }
        else
        {
            return 0;
        }
    }
}
