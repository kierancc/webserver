package webserver;

/**
 * @author Kieran Chin Cheong
 * @version 1.0
 * @since 1.0
 */
public class ResponseException extends Exception
{
    // Member variables
    
    // Explicitly set serialVersionUID instead of relying on auto generation
    private static final long serialVersionUID = -763015345955560065L;

    /**
     * Constructor
     * @param message A message describing the problem encountered with the response
     */
    public ResponseException(String message)
    {
        super(message);
    }
}
