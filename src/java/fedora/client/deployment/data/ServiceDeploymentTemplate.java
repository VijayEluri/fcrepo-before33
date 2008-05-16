/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.client.deployment.data;

import java.util.Vector;

/**
 * @author Sandy Payette
 */
public class ServiceDeploymentTemplate
        extends BObjTemplate {

    // variables specific to service deployments
    private String sDefContractPID = null;

    private boolean hasBaseURL = false;

    private String serviceBaseURL = null;

    private DSInputRule[] dsInputSpec = new DSInputRule[0];

    private Vector dsBindingKeys = new Vector();

    private ServiceProfile profile = null;

    public ServiceDeploymentTemplate() {
    }

    public String getSDefContractPID() {
        return sDefContractPID;
    }

    public void setSDefContractPID(String sDefPID) {
        this.sDefContractPID = sDefPID;
    }

    public void setHasBaseURL(boolean hasBaseURL) {
        this.hasBaseURL = hasBaseURL;
    }

    public boolean getHasBaseURL() {
        return hasBaseURL;
    }

    public String getServiceBaseURL() {
        return serviceBaseURL;
    }

    public void setServiceBaseURL(String in_baseURL) {
        serviceBaseURL = in_baseURL;
    }

    public void setDSInputSpec(DSInputRule[] dsInputSpec) {
        this.dsInputSpec = dsInputSpec;
    }

    public DSInputRule[] getDSInputSpec() {
        return dsInputSpec;
    }

    public Vector getDSBindingKeys() {
        return dsBindingKeys;
    }

    public void setDSBindingKeys(Vector dsBindingKeys) {
        this.dsBindingKeys = dsBindingKeys;
    }

    public void setServiceProfile(ServiceProfile profile) {
        this.profile = profile;
    }

    public ServiceProfile getServiceProfile() {
        return profile;
    }
}