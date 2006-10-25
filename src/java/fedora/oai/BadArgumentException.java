package fedora.oai;

/**
 *
 * <p><b>Title:</b> BadArgumentException.java</p>
 * <p><b>Description:</b> Signals that a request includes illegal arguments,
 * is missing required arguments, includes a repeated argument, or values for
 * arguments have an illegal syntax.</p>
 *
 * <p>This may occur while fulfilling any request.</p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class BadArgumentException
        extends OAIException {
	
	private static final long serialVersionUID = 1L;
	
    public BadArgumentException() {
        super("badArgument", null);
    }

    public BadArgumentException(String message) {
        super("badArgument", message);
    }

}