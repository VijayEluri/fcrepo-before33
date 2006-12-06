package fedora.server.errors;

/**
 *
 * <p><b>Title:</b> ServerInitializationException.java</p>
 * <p><b>Description:</b> Signifies that an error occurred during the server's
 * initialization.</p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class ServerInitializationException
        extends InitializationException {

	private static final long serialVersionUID = 1L;
	
    /**
     * Creates a ServerInitializationException.
     *
     * @param message An informative message explaining what happened and
     *                (possibly) how to fix it.
     */
    public ServerInitializationException(String message) {
        super(message);
    }

    public ServerInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

}
