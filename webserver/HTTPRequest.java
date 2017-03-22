package webserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kieran Chin Cheong
 * @version 1.0
 * @since 1.0
 */
public class HTTPRequest
{
    // Static variables
    private static final String[] SUPPORTED_REQUEST_METHODS = {"GET"};
    private static final String REQUEST_START_LINE_PATTERN = "^(.+)\\s(.+)\\s(.+)$";
    private static final String HEADER_LINE_PATTERN = "^(.+?):\\s*(.+)\\s*$";
    
    // Member variables
    private String requestMethod;
    private String requestTarget;
    private String httpVersion;
    private HashMap<String, String> headerFields = new HashMap<String, String>();
    private boolean keepAliveRequested;
    
    /**
     * Reads and parses the request made by an accepted client
     * @param stream InputStream object from accepted client connection
     * @throws RequestException
     * @throws HttpKeepAliveTimeoutException
     */
    public HTTPRequest(InputStream stream) throws RequestException, HttpKeepAliveTimeoutException
    {
        // Attempt to parse the incoming request, throw any encountered exceptions so that
        // an appropriate error code can be returned to the client
        try
        {
            // NOTE: This reader should not be closed; this will cause the connection to the client to be closed
            // before any response can be sent back to the client.  Instead, the connection should be closed
            // after the response is sent, if HTTP KeepAlive is not enabled.  If it is enabled, the connection will be managed
            // and closed when appropriate
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            
            // Read and parse the first line of the request.  This should be in form of something like
            // GET /index.html HTTP/1.1
            // Anything else is invalid
            // Note that since we are using the readLine() function of our reader, any CR and LF characters will be removed
            // This is OK because since a line was read, then they were there in the request.  So as long as the preceding
            // line conforms to the HTTP specification there is no problem
            String line;
            
            try
            {
                // This line will block until input is received.  In the case of a KeepAlive connection that does not receive
                // another request from the client, the thread will block there until timed out, at which point the socket will 
                // be closed (by another thread)
                // This isn't really an error since it is expected that this can happen depending on the design of the client
                // but it needs to be handled
                line = reader.readLine();
            }
            catch (SocketException se)
            {
                // The connection was closed, either by the timeout timer, or by the peer itself
                Logger.Log(Logger.INFORMATION, "Connection socket closed due to timeout or by peer");
                throw new HttpKeepAliveTimeoutException();
            }
            
            Logger.Log(Logger.INFORMATION, String.format("HTTP Request received, start-line : %s", line));
            
            // Validate the received start-line
            Pattern compiledStartLinePattern = Pattern.compile(REQUEST_START_LINE_PATTERN);
            Matcher startLineMatcher = compiledStartLinePattern.matcher(line);
            
            // The start-line is valid
            if (startLineMatcher.find())
            {
                this.requestMethod = startLineMatcher.group(1);
                this.requestTarget = startLineMatcher.group(2);
                this.httpVersion = startLineMatcher.group(3);
            }
            // The start-line is not valid
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
            
            // Loop while we have not read in an empty line
            while (! "".equals(line))
            {
                // If a null line is ever encountered within this loop, this is an unexpected end of request
                if (line == null)
                {
                    throw new RequestException(Status.BAD_REQUEST, "Unexpected end of request");
                }
                
                // Attempt to match the read line as a valid request header
                headerLineMatcher = compiledHeaderLinePattern.matcher(line);
                
                // Add the header key/value pair
                if (headerLineMatcher.find())
                {
                    // For simplicity's sake we will convert all field names to lower case
                    // The HTTP spec states that they are case-insensitive, so this will not cause any issues
                    String fieldName = headerLineMatcher.group(1).toLowerCase();
                    String fieldValue = headerLineMatcher.group(2);
                    
                    // Handle the special case where if a duplicate file name is encountered, it should be appended to the previous, separated by a comma
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
            // If it was not provided, this is a bad request
            if (! this.headerFields.containsKey("host"))
            {
                throw new RequestException(Status.BAD_REQUEST, "Missing host header");
            }
            
            // Check if the client sent a "connection: keep-alive" header.  If it did, and KeepAlive is enabled on the server, save this information
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
            // to ensure that the next time we read from this stream, we are really reading the new request.
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
            // Here we failed to read from the socket for an unexpected reason
            // TODO Figure out how to prevent this from causing a response to be sent
            Logger.Log(Logger.ERROR, String.format("Error reading request : %s", e.toString()));
        }
    }
    
    /**
     * Returns the requested target on the default filesystem using the appropriate character to separate files
     * @return a string representing the path to the requested target specifc to the default file system
     */
    public String getRequestTargetLocalPath()
    {
        return this.requestTarget.replace('/', File.separatorChar);
    }
    
    /**
     * Returns the user-agent as provided by the client
     * @return user-agent of client
     */
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
    
    /**
     * Returns whether or not HTTP KeepAlive was requested by the client
     * @return true if requested, false otherwise
     */
    public boolean isKeepAliveRequested()
    {
        return this.keepAliveRequested;
    }
    
    /**
     * Method used to determine if a HTTP request method is supported by this server
     * @param method requsted by client
     * @return true if supported, false otherwise
     */
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
    
    /**
     * Method used to determine the length of the message body as reported by the client
     * <p>
     * If no content-length header was provided, simply return 0
     * @return length of message body in bytes
     */
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
    
    // Simple getters
    
    /**
     * @return
     */
    public String getRequestMethod()
    {
        return this.requestMethod;
    }
    
    /**
     * @return
     */
    public String getRequestTarget()
    {
        return this.requestTarget;
    }
    
    /**
     * @return
     */
    public String getHttpVersion()
    {
        return this.httpVersion;
    }
}
