package webserver;

public enum Status
{
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
    
    private final int value;
    
    Status(int value)
    {
        this.value = value;
    }
    
    public int toCode()
    {
        return this.value;
    }
}
