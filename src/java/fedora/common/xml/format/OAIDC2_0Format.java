/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.OAIDCNamespace;

/**
 * The OAI-DC 2.0 XML format.
 *
 * <pre>
 * Format URI        : http://www.openarchives.org/OAI/2.0/oai_dc/
 * Primary Namespace : http://www.openarchives.org/OAI/2.0/oai_dc/
 * XSD Schema URL    : http://www.openarchives.org/OAI/2.0/oai_dc.xsd
 * </pre>
 *
 * @author cwilper@cs.cornell.edu
 */
public class OAIDC2_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final OAIDC2_0Format ONLY_INSTANCE
            = new OAIDC2_0Format();

    /**
     * Constructs the instance.
     */
    private OAIDC2_0Format() {
        super("http://www.openarchives.org/OAI/2.0/oai_dc/",
              OAIDCNamespace.getInstance(),
              "http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
    }

    /**
     * Gets the only instance of this class.
     *
     * @return the instance.
     */
    public static OAIDC2_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
