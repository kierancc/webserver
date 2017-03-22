package webserver;

/**
 * @author Kieran Chin Cheong
 * @version 1.0
 * @since 1.0
 */
public class RequestException extends Exception
{
    // Member variables
    
    // Explicitly set serialVersionUID instead of relying on auto generation
    private static final long serialVersionUID = -2575022686802578666L;
    private Status errorCode;

    /**
     * Constructor
     * @param errorCode the HTTP response code indicating the problem encountered in the request
     * @param message A message describing the problem
     */
    public RequestException(Status errorCode, String message)
    {
        super(message);
        this.errorCode = errorCode;
    }
    
    // Simple getters
    
    /**
     * @return
     */
    public Status getErrorCode()
    {
        return this.errorCode;
    }
    
    /**
     * Custom toString() method
     * @see java.lang.Throwable#toString()
     */
    @Override
    public String toString()
    {
        return super.toString() + ", error code: " + this.errorCode;
    }
}
