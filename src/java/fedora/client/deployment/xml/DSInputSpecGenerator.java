/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.client.deployment.xml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fedora.client.deployment.DeploymentBuilderException;
import fedora.client.deployment.data.ServiceDeploymentTemplate;
import fedora.client.deployment.data.DSInputRule;

import fedora.common.Constants;

/**
 * @author Sandy Payette
 */
public class DSInputSpecGenerator
        implements Constants {

    private Document document;

    public DSInputSpecGenerator(ServiceDeploymentTemplate newSDep)
            throws DeploymentBuilderException {
        createDOM();
        genDSInputSpec(newSDep);
    }

    private void createDOM() throws DeploymentBuilderException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.newDocument();

        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();
            throw new DeploymentBuilderException("DSInputSpecGenerator: error configuring parser."
                    + "Underlying exception: " + pce.getMessage());
        }
    }

    private void genDSInputSpec(ServiceDeploymentTemplate newSDef) {
        DSInputRule[] rules = newSDef.getDSInputSpec();

        Element root =
                document.createElementNS(BINDING_SPEC.uri, "fbs:DSInputSpec");
        root.setAttributeNS(XMLNS.uri, "xmlns:fbs", BINDING_SPEC.uri);
        String bmlabel =
                newSDef.getbObjLabel() == null ? "" : newSDef.getbObjLabel();
        root.setAttribute("label",
                          ("Datastream Input Specification for " + bmlabel));
        String sDefPID =
                newSDef.getSDefContractPID() == null ? "" : newSDef
                        .getSDefContractPID();
        
        // FIXME: this bdefPID attribute may not be necessary any more
        root.setAttribute("bDefPID", sDefPID);
        document.appendChild(root);

        for (DSInputRule element : rules) {
            Element dsInput =
                    document.createElementNS(BINDING_SPEC.uri, "fbs:DSInput");
            String bindKeyName =
                    element.bindingKeyName == null ? ""
                            : element.bindingKeyName;
            String mime =
                    element.bindingMIMEType == null ? ""
                            : element.bindingMIMEType;
            String min =
                    element.minNumBindings == null ? ""
                            : element.minNumBindings;
            String max =
                    element.maxNumBindings == null ? ""
                            : element.maxNumBindings;
            String order = element.ordinality == null ? "" : element.ordinality;
            String label =
                    element.bindingLabel == null ? "" : element.bindingLabel;
            String instr =
                    element.bindingInstruction == null ? ""
                            : element.bindingInstruction;
            dsInput.setAttribute("wsdlMsgPartName", bindKeyName.trim());
            dsInput.setAttribute("DSMin", min.trim());
            dsInput.setAttribute("DSMax", max.trim());
            dsInput.setAttribute("DSOrdinality", order.trim());
            Element dsLabel =
                    document.createElementNS(BINDING_SPEC.uri,
                                             "fbs:DSInputLabel");
            dsLabel.appendChild(document.createTextNode(label));
            Element dsInstr =
                    document.createElementNS(BINDING_SPEC.uri,
                                             "fbs:DSInputInstruction");
            dsInstr.appendChild(document.createTextNode(instr));
            dsInput.appendChild(dsLabel);
            // rlw - bugfix #182
            // DSInputParms can contain multiple mime types separate by commas.
            // If more than one mimetype exists, split each mimetype out into a separate fbs:DSMIME element
            String[] mimetypes = mime.split(",");
            for (String element2 : mimetypes) {
                Element dsMIME =
                        document
                                .createElementNS(BINDING_SPEC.uri, "fbs:DSMIME");
                dsMIME.appendChild(document.createTextNode(element2));
                dsInput.appendChild(dsMIME);
            }
            dsInput.appendChild(dsInstr);
            root.appendChild(dsInput);
        }
    }

    public Element getRootElement() {
        return document.getDocumentElement();
    }

    public Document getDocument() {
        return document;
    }

    public void printDSInputSpec() {
        try {
            String str =
                    new XMLWriter(new DOMResult(document)).getXMLAsString();
            System.out.println(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}