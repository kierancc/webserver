package webserver;

public class RequestException extends Exception
{
    // Explicitly set serialVersionUID instead of relying on auto generation
    private static final long serialVersionUID = -2575022686802578666L;
    private Status errorCode;

    public RequestException(Status errorCode, String message)
    {
        super(message);
        this.errorCode = errorCode;
    }
    
    public Status getErrorCode()
    {
        return this.errorCode;
    }
    
    @Override
    public String toString()
    {
        return super.toString() + ", error code: " + this.errorCode;
    }
}
