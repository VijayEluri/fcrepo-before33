/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */
package fedora.server.messaging;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import fedora.common.Constants;
import fedora.server.Server;
import fedora.server.errors.MessagingException;
import fedora.server.management.Management;

/**
 * The default, JMS implementation of Messaging.
 *
 * @author Edwin Shin
 * @since 3.0
 * @version $Id$
 */
public class MessagingImpl implements Messaging {
    /** Logger for this class. */
    private static Logger LOG = 
            Logger.getLogger(MessagingImpl.class.getName());
    
    private Map<String, List<String>> mdMap;
    private JMSManager jmsMgr;
    private String fedoraBaseUrl;
    private final static String serverVersion = Server.VERSION_MAJOR + Server.VERSION_MINOR;
    private final static String messageFormat = Constants.ATOM_APIM1_0.uri;
    
    /**
     * Required JNDI Properties:
     * <ul>
     *   <li>{@link javax.naming.Context#INITIAL_CONTEXT_FACTORY INITIAL_CONTEXT_FACTORY}</li>
     *   <li>{@link javax.naming.Context#PROVIDER_URL PROVIDER_URL}</li>
     * </ul>
     * 
     * Optional JNDI Properties:
     * <ul>
     *   <li>{@link JMSManager#CONNECTION_FACTORY_NAME CONNECTION_FACTORY_NAME}</li>
     * </ul>
     * 
     * @param fedoraBaseUrl e.g. http://localhost:8080/fedora
     * @param mdMap a <code>Map</code> of {@link Messaging#MessageType} to 
     * Destinations.
     * @param jndiProps the JNDI configuration properties.
     * @throws MessagingException
     */
    public MessagingImpl(String fedoraBaseUrl, Map<String, List<String>> mdMap, Properties jndiProps) throws MessagingException {
        this(fedoraBaseUrl, mdMap, new JMSManager(jndiProps));
    }
    
    public MessagingImpl(String fedoraBaseUrl, Map<String, List<String>> mdMap, JMSManager jmsMgr) {
        this.fedoraBaseUrl = fedoraBaseUrl;
        this.mdMap = mdMap;
        this.jmsMgr = jmsMgr;
    }
    
    public void send(String destName, FedoraMessage message)
            throws MessagingException {
        jmsMgr.send(destName, message.toString());
    }
    
    /**
     * Send a message to each of the destinations configured for each 
     * {@link Messaging#MessageType}. Currently, only 
     * {@link FedoraMethod}s that represent 
     * {@link fedora.server.Management} methods are supported.
     * {@inheritDoc}
     */
    public void send(FedoraMethod method) throws MessagingException {
        if (Management.class == method.getMethod().getDeclaringClass()) {

            APIMMessage message = new AtomAPIMMessage(method, fedoraBaseUrl, serverVersion, messageFormat);
            
            String methodName = method.getName();
            if (methodName.startsWith("ingest") 
                    || methodName.startsWith("add")
                    || methodName.startsWith("modify")
                    || methodName.startsWith("purge")
                    || methodName.startsWith("set")) {
                for (String destName : mdMap.get(MessageType.apimUpdate.toString())) {
                    send(destName, message);
                }
            } else {
                for (String destName : mdMap.get(MessageType.apimAccess.toString())) {
                    send(destName, message);
                }
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Silently dropping non-Management method: " + method.getName());
            }
        }
    }

    public void close() throws MessagingException {
        if (jmsMgr != null) {
            jmsMgr.close();
        }
    }
}
