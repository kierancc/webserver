package webserver;

/**
 * @author Kieran Chin Cheong
 * @version 1.0
 * @since 1.0
 */
public enum Status
{
    // Enum values
    
    CONTINUE(100),
    OK(200),
    NOT_MODIFIED(304),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    INTERNAL_SERVER_ERROR(500),
    NOT_IMPLEMENTED(501),
    SERVICE_UNAVAILABLE(503),
    HTTP_VERSION_NOT_SUPPORTED(505);
    
    // Member variables
    
    private final int value;
    
    /**
     * Constructor
     * @param value response code
     */
    Status(int value)
    {
        this.value = value;
    }
    
    /**
     * Returns the response code associated with this status
     * @return response code
     */
    public int toCode()
    {
        return this.value;
    }
}
