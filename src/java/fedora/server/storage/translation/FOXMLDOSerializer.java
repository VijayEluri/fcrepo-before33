package fedora.server.storage.translation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import fedora.server.Server;
import fedora.server.errors.InitializationException;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.StreamWriteException;
import fedora.server.storage.types.AuditRecord;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.Disseminator;
import fedora.server.storage.types.DSBinding;
import fedora.server.utilities.StreamUtility;

/**
 *
 * <p><b>Title:</b> FOXMLDOSerializer.java</p>
 * <p><b>Description:</b> </p>
 *
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2004 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author payette@cs.cornell.edu
 * @version $Id$
 */
public class FOXMLDOSerializer
        implements DOSerializer {

	public static final String FOXML_NS="info:fedora/def:foxml1.0";
    public static final String FEDORA_AUDIT_NS="info:fedora/def:audit";
	public static final String FEDORA_DC_NS="http://www.openarchives.org/OAI/2.0/oai_dc/";
	public static final String FEDORA_RELSOUT_NS="info:fedora/def:relation:outer";
    public static final String FOXML_PREFIX="foxml";

    public static final String FOXML_XSD_LOCATION="http://www.fedora.info/definitions/1/0/foxml1.0.xsd";
    public static final String XSI_NS="http://www.w3.org/2001/XMLSchema-instance";

    private String m_fedoraAuditPrefix="audit";
	private String m_fedoraRelsoutPrefix="fro";
    private SimpleDateFormat m_formatter=
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	// Pattern for URLs that contain the placeholder string indicating the URL is
	// based at the local repository server.  When we serialized for EXPORT, we
	// will detect this pattern and replace it with the actual host name.
	private static Pattern s_localPattern = Pattern.compile("http://local.fedora.server/");
	
	// The actual host and port of the repository server		
	private static String s_hostInfo = null; 

	// Patterns of the various ways that the local repository server address may be 
  	// encoded.  When we serialized for STORAGE, we want to replace the actual host:port
  	// of URLs to the local repository with the placeholder string "http://local.fedora.server/"
  	// to virtualize the local host:port.  This is to allow the repository host:port to 
  	// be reconfigured after an object has been stored, and be able to recreate the proper
  	// URL based on the new configuration. 	
  	private static Pattern s_localServerUrlStartWithPort; // "http://actual.hostname:8080/"
  	private static Pattern s_localServerUrlStartWithoutPort; // "http://actual.hostname/"
  	private static Pattern s_localhostUrlStartWithPort; // "http://localhost:8080/"
  	private static Pattern s_localhostUrlStartWithoutPort; // "http://localhost/"
    
  	private static String s_localServerDissemUrlStart; // "http://actual.hostname:8080/fedora/get/"

    private boolean m_onPort80=false;
    private boolean m_encodeForExport=false;

    public FOXMLDOSerializer() {
    }

    public DOSerializer getInstance() {
        return new FOXMLDOSerializer();
    }

    public void serialize(DigitalObject obj, OutputStream out, String encoding, boolean encodeForExport)
            throws ObjectIntegrityException, StreamIOException,
            UnsupportedEncodingException {
		System.out.println("Serializing using FOXMLDOSerializer...");
		m_encodeForExport=encodeForExport;
        // get the host info in a static var so search/replaces are quicker later
        if (s_hostInfo==null) {
            String fedoraHome=System.getProperty("fedora.home");
            String fedoraServerHost=null;
            String fedoraServerPort=null;
            if (fedoraHome==null || fedoraHome.equals("")) {
                // if fedora.home is undefined or empty, assume we're testing,
                // in which case the host and port will be taken from system
                // properties
                fedoraServerHost=System.getProperty("fedoraServerHost");
                fedoraServerPort=System.getProperty("fedoraServerPort");
            } else {
                try {
                    Server s=Server.getInstance(new File(fedoraHome));
                    fedoraServerHost=s.getParameter("fedoraServerHost");
                    fedoraServerPort=s.getParameter("fedoraServerPort");
					if (fedoraServerPort.equals("80")) {
					    m_onPort80=true;
					}
                } catch (InitializationException ie) {
                    // can only possibly happen during failed testing, in which
                    // case it's ok to do a System.exit
                    System.err.println("STARTUP ERROR: " + ie.getMessage());
                    System.exit(1);
                }
            }
            // set the configured host:port of the repository
			s_hostInfo="http://" + fedoraServerHost;
			if (!fedoraServerPort.equals("80")) {
				s_hostInfo=s_hostInfo + ":" + fedoraServerPort;
			}
			s_hostInfo=s_hostInfo + "/";
			
			// set the pattern for public dissemination URLs at local server
			s_localServerDissemUrlStart= s_hostInfo + "fedora/get/";
			
			// set other patterns using the configured host and port
            s_localServerUrlStartWithPort=Pattern.compile("http://"
                    + fedoraServerHost + ":" + fedoraServerPort + "/");
            s_localServerUrlStartWithoutPort=Pattern.compile("http://"
                    + fedoraServerHost + "/");
            s_localhostUrlStartWithoutPort=Pattern.compile("http://localhost/");
            s_localhostUrlStartWithPort=Pattern.compile("http://localhost:" + fedoraServerPort + "/");
            
        }
        // now do serialization stuff
        StringBuffer buf=new StringBuffer();
        appendXMLDeclaration(obj, encoding, buf);
        appendRootElementStart(obj, buf);
        appendProperties(obj, buf, encoding);
        appendAudit(obj, buf, encoding);
        appendDatastreams(obj, buf, encoding);
        appendDisseminators(obj, buf);
        appendRootElementEnd(buf);
        writeToStream(buf, out, encoding, true);
    }

    private void appendXMLDeclaration(DigitalObject obj, String encoding,
            StringBuffer buf) {
        buf.append("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n");
    }

    private void appendRootElementStart(DigitalObject obj, StringBuffer buf)
            throws ObjectIntegrityException {
        buf.append("<" + FOXML_PREFIX + ":digitalObject xmlns:" + FOXML_PREFIX + "=\""
                + StreamUtility.enc(FOXML_NS) + "\"\n");
        String indent="           ";
        // make sure XSI_NS is mapped...
        String xsiPrefix=(String) obj.getNamespaceMapping().get(XSI_NS);
        if (xsiPrefix==null) {
            xsiPrefix="fedoraxsi";
            obj.getNamespaceMapping().put(XSI_NS, "fedoraxsi"); // 99.999999999% chance this is unique
        }
        appendNamespaceDeclarations(indent,obj.getNamespaceMapping(),buf);
        // hardcode xsi:schemaLocation to definitive location for such.
        buf.append(indent + xsiPrefix + ":schemaLocation=\"" + 
        	StreamUtility.enc(FOXML_NS) + " "  + 
        	StreamUtility.enc(FOXML_XSD_LOCATION) + "\"\n");
        if (obj.getPid()==null) {
            throw new ObjectIntegrityException("Object must have a pid.");
        }
        buf.append(indent + "PID=\"" + obj.getPid() + "\" ");
		buf.append(indent + "URI=\"" + "info:fedora/" + obj.getPid()+ "\"");
        buf.append(">\n");
    }

    private void appendNamespaceDeclarations(String prepend, Map URIToPrefix,
            StringBuffer buf) {
        Iterator iter=URIToPrefix.keySet().iterator();
        while (iter.hasNext()) {
            String URI=(String) iter.next();
            String prefix=(String) URIToPrefix.get(URI);
            if (!prefix.equals("")) {
                if (URI.equals(FEDORA_AUDIT_NS)) {
                    m_fedoraAuditPrefix=prefix;
				} else if (URI.equals(FEDORA_RELSOUT_NS)) {
						m_fedoraRelsoutPrefix=prefix;
                } else if (!URI.equals(FOXML_NS)) {
                    buf.append(prepend + "xmlns:" + prefix + "=\""
                            + StreamUtility.enc(URI) + "\"\n");
                }
            }
        }
        buf.append(prepend + "xmlns:" + m_fedoraAuditPrefix + "=\""
                + FEDORA_AUDIT_NS + "\"\n");
		buf.append(prepend + "xmlns:" + m_fedoraRelsoutPrefix + "=\""
				+ FEDORA_RELSOUT_NS + "\"\n");
    }
    
	private void appendProperties(DigitalObject obj, StringBuffer buf, String encoding) 
			throws ObjectIntegrityException {
		
		String ftype = getTypeAttribute(obj);
		String state = obj.getState();
		String label = obj.getLabel();
		Date cdate = obj.getCreateDate();
		Date mdate = obj.getLastModDate();
		String cmodel = obj.getContentModelId();
		
		buf.append("  <" + FOXML_PREFIX + ":objectProperties>\n");
			
		if (ftype!=null) {
			buf.append("    <" + FOXML_PREFIX + ":property NAME=\"" + "info:fedora/def:dobj:fType" + "\"" 
			+ " VALUE=\"" + ftype + "\"/>\n");
		}
		if (state!=null) {
			buf.append("    <" + FOXML_PREFIX + ":property NAME=\"" + "info:fedora/def:dobj:state" + "\"" 
			+ " VALUE=\"" + state + "\"/>\n");
		}
		if (label!=null) {
			buf.append("    <" + FOXML_PREFIX + ":property NAME=\"" + "info:fedora/def:dobj:label" + "\""
			+ " VALUE=\"" + label + "\"/>\n"); 
		}
		if (cdate!=null) {
			buf.append("    <" + FOXML_PREFIX + ":property NAME=\"" + "info:fedora/def:dobj:cDate" + "\""
			+ " VALUE=\"" + m_formatter.format(cdate) + "\"/>\n"); 
		}
		if (mdate!=null) {
			buf.append("    <" + FOXML_PREFIX  + ":property NAME=\"" + "info:fedora/def:dobj:mDate" + "\""
			+ " VALUE=\"" + m_formatter.format(mdate) + "\"/>\n"); 
		}
		if (cmodel!=null) {
			buf.append("    <" + FOXML_PREFIX + ":property NAME=\"" + "info:fedora/def:dobj:cModel" + "\"" 
			+ " VALUE=\"" + cmodel + "\"/>\n");	
		}
		Iterator iter = obj.getExtProperties().keySet().iterator();
		while (iter.hasNext()){
			String name = (String)iter.next();
			buf.append("    <" + FOXML_PREFIX + ":extproperty NAME=\"" + name + "\""
			+ " VALUE=\"" + obj.getExtProperty(name) + "\"/>\n"); 
		}
		buf.append("  </" + FOXML_PREFIX + ":objectProperties>\n");
	}
	
	private void appendDatastreams(DigitalObject obj, StringBuffer buf, String encoding)
			throws ObjectIntegrityException, UnsupportedEncodingException, 
			StreamIOException {
		Iterator iter=obj.datastreamIdIterator();
		while (iter.hasNext()) {
			String dsid = (String) iter.next();
			if (dsid==null || dsid.equals("")) {
				throw new ObjectIntegrityException("Missing datastream ID in object: " + obj.getPid());
			}
			// AUDIT datastream is rebuilt from the latest in-memory audit trail
			// which is a separate array list in the DigitalObject class.
			// So, ignore it here.
			if (dsid.equals("AUDIT") || dsid.equals("FEDORA-AUDITTRAIL")) {
				continue;
			}
			// Given a datastream ID, get all the datastream versions.
			// Use the first version to pick up the attributes common to all versions.
			List dsList = obj.datastreams(dsid);
			for (int i=0; i<dsList.size(); i++) {
				Datastream vds = validateDatastream((Datastream) dsList.get(i));
				// insert the ds elements common to all versions.
				if (i==0) {
					buf.append("  <" + FOXML_PREFIX 
						+ ":datastream ID=\"" + vds.DatastreamID + "\""
						+ " URI=\"" + "info:fedora/" + obj.getPid() + "/" + vds.DatastreamID + "\"" 
						+ " STATE=\"" + vds.DSState + "\""
						+ " MIMETYPE=\"" + vds.DSMIME + "\""
						+ " FORMAT_URI=\"" + vds.DSFormatURI + "\""
						+ " CONTROL_GROUP=\"" + vds.DSControlGrp + "\""
						+ " VERSIONABLE=\"" + vds.DSVersionable + "\">\n");
				}
				// insert the ds version-level elements
				buf.append("    <" + FOXML_PREFIX 
					+ ":datastreamVersion ID=\"" + vds.DSVersionID + "\"" 
					+ " LABEL=\"" + StreamUtility.enc(vds.DSLabel) + "\""
					+ " CREATED=\"" + m_formatter.format(vds.DSCreateDT) + "\""
					+ " SIZE=\"" + vds.DSSize +  "\">\n");
			
				// if E or R insert ds content location as URL
				if (vds.DSControlGrp.equalsIgnoreCase("E") ||
					vds.DSControlGrp.equalsIgnoreCase("R") ) {
						buf.append("      <" + FOXML_PREFIX 
							+ ":contentLocation TYPE=\"" + "URL\""
							+ " REF=\"" 
							+ StreamUtility.enc(normalizeDSLocat(obj.getPid(), vds)) 
							+ "\"/>\n");	
				// if M insert ds content location as an internal identifier				
				} else if (vds.DSControlGrp.equalsIgnoreCase("M")) {
					buf.append("      <" + FOXML_PREFIX 
						+ ":contentLocation TYPE=\"" + "INTERNAL_ID\""
						+ " REF=\"" 
						+ StreamUtility.enc(normalizeDSLocat(obj.getPid(), vds)) 
						+ "\"/>\n");	
				// if X insert inline XML
				} else if (vds.DSControlGrp.equalsIgnoreCase("X")) {
					appendInlineXML(obj.getFedoraObjectType(), 
						(DatastreamXMLMetadata)vds, buf, encoding);
				}					
				// FUTURE: Add digest of datastream content 
				//(to be calculated in DefaultManagement).
				buf.append("      <" + FOXML_PREFIX + ":contentDigest TYPE=\"MD5\""
					+ " DIGEST=\"future: hash of content goes here\"/>\n"); 
				buf.append("    </" + FOXML_PREFIX + ":datastreamVersion>\n");
				// if it's the last version, wrap-up with closing datastream element.	
				if (i==(dsList.size() - 1)) {
					buf.append("  </" + FOXML_PREFIX + ":datastream>\n");
				}			
			}
		}
	}

	private void appendAudit(DigitalObject obj, StringBuffer buf, String encoding) 
			throws ObjectIntegrityException {
		if (obj.getAuditRecords().size()>0) {
			// Audit trail datastream re-created from audit records.
			// There is only ONE version of the audit trail datastream!
			buf.append("  <" + FOXML_PREFIX 
				+ ":datastream ID=\"" + "AUDIT" + "\"" 
				+ " URI=\"" + "info:fedora/" + obj.getPid() + "/AUDIT" + "\""
				+ " STATE=\"" + "A" + "\""
				+ " MIMETYPE=\"" + "text/xml" + "\""
				+ " FORMAT_URI=\"" + "info:fedora/format:xml:audit" + "\""
				+ " CONTROL_GROUP=\"" + "X" + "\""
				+ " VERSIONABLE=\"" + "NO" + "\">\n");
			// insert the ds version-level elements
			buf.append("    <" + FOXML_PREFIX 
				+ ":datastreamVersion ID=\"" + "AUDIT.0" + "\"" 
				+ " LABEL=\"" + "Fedora Object Audit Trail" + "\""
				+ " CREATED=\"" + m_formatter.format(obj.getCreateDate()) +  "\">\n");
			buf.append("      <" + FOXML_PREFIX + ":xmlContent>\n");
			buf.append("        <" + m_fedoraAuditPrefix + ":auditTrail xmlns:" 
						+ m_fedoraAuditPrefix + "=\"" + FEDORA_AUDIT_NS + "\">\n");
			for (int i=0; i<obj.getAuditRecords().size(); i++) {
				AuditRecord audit=(AuditRecord) obj.getAuditRecords().get(i);
				validateAudit(audit);
				buf.append("          <" + m_fedoraAuditPrefix + ":record ID=\""
						+ StreamUtility.enc(audit.id) + "\">\n");
				buf.append("            <" + m_fedoraAuditPrefix + ":process type=\""
						+ StreamUtility.enc(audit.processType) + "\"/>\n");
				buf.append("            <" + m_fedoraAuditPrefix + ":action>"
						+ StreamUtility.enc(audit.action)
						+ "</" + m_fedoraAuditPrefix + ":action>\n");
				buf.append("            <" + m_fedoraAuditPrefix + ":componentID>"
						+ StreamUtility.enc(audit.componentID)
						+ "</" + m_fedoraAuditPrefix + ":componentID>\n");
				buf.append("            <" + m_fedoraAuditPrefix + ":responsibility>"
						+ StreamUtility.enc(audit.responsibility)
						+ "</" + m_fedoraAuditPrefix + ":responsibility>\n");
				buf.append("            <" + m_fedoraAuditPrefix + ":date>"
						+ m_formatter.format(audit.date)
						+ "</" + m_fedoraAuditPrefix + ":date>\n");
				buf.append("            <" + m_fedoraAuditPrefix + ":justification>"
						+ StreamUtility.enc(audit.justification)
						+ "</" + m_fedoraAuditPrefix + ":justification>\n");
				buf.append("          </" + m_fedoraAuditPrefix + ":record>\n");
			}
			buf.append("        </" + m_fedoraAuditPrefix + ":auditTrail" + ">\n");
			buf.append("      </" + FOXML_PREFIX + ":xmlContent>\n");
			// FUTURE: Add digest of datastream content (calc in DefaultManagement).
			buf.append("      <" + FOXML_PREFIX + ":contentDigest TYPE=\"MD5\">"
				+ "future: hash of content goes here" 
				+ "</" + FOXML_PREFIX + ":contentDigest>\n");
			buf.append("    </" + FOXML_PREFIX + ":datastreamVersion>\n");				
			buf.append("  </" + FOXML_PREFIX + ":datastream>\n");
		}
	}

	private void appendInlineXML(int fedoraObjectType, DatastreamXMLMetadata ds, 
		StringBuffer buf, String encoding)
		throws ObjectIntegrityException, UnsupportedEncodingException, StreamIOException {
			
		buf.append("        <" + FOXML_PREFIX + ":xmlContent>\n");
        if ( fedoraObjectType==DigitalObject.FEDORA_BMECH_OBJECT &&
             (ds.DatastreamID.equals("SERVICE-PROFILE") || 
			  ds.DatastreamID.equals("WSDL")) ) {
	            // If WSDL or SERVICE-PROFILE datastream (in BMech) 
	            // make sure that any embedded URLs are encoded 
	            // appropriately for either EXPORT or STORE.
	            buf.append(normalizeDSInlineXML(ds));
        } else {
            appendXMLStream(ds.getContentStream(), buf, encoding);
        }
        buf.append("        </" + FOXML_PREFIX + ":xmlContent>\n");
    }

    private void appendXMLStream(InputStream in, StringBuffer buf, String encoding)
            throws ObjectIntegrityException, UnsupportedEncodingException,
            StreamIOException {
        if (in==null) {
            throw new ObjectIntegrityException("Object's inline xml "
                    + "stream cannot be null.");
        }
        try {
            byte[] byteBuf = new byte[4096];
            int len;
            while ( ( len = in.read( byteBuf ) ) != -1 ) {
                buf.append(new String(byteBuf, 0, len, encoding));
            }
        } catch (UnsupportedEncodingException uee) {
            throw uee;
        } catch (IOException ioe) {
            throw new StreamIOException("Error reading from inline xml datastream.");
        } finally {
            try {
                in.close();
            } catch (IOException closeProb) {
                throw new StreamIOException("Error closing read stream.");
            }
        }
    }

    private void appendDisseminators(DigitalObject obj, StringBuffer buf)
            throws ObjectIntegrityException {

        Iterator dissIdIter=obj.disseminatorIdIterator();
        while (dissIdIter.hasNext()) {
            String did=(String) dissIdIter.next();
            Iterator dissIter=obj.disseminators(did).iterator();
            List dissList = obj.disseminators(did);
            
            for (int i=0; i<dissList.size(); i++) {
                Disseminator vdiss = 
                	validateDisseminator((Disseminator) obj.disseminators(did).get(i));
                                
				// If dissVersionable is null or missing, default to YES.
				if (vdiss.dissVersionable==null || vdiss.dissVersionable.equals("")) {
					vdiss.dissVersionable="YES";
				}                
				// insert the disseminator elements common to all versions.
				if (i==0) {
					buf.append("  <" + FOXML_PREFIX + ":disseminator ID=\"" + did
							+ "\" BDEF_CONTRACT_PID=\"" + vdiss.bDefID 
							+ "\" STATE=\"" + vdiss.dissState 
							+ "\" VERSIONABLE=\"" + vdiss.dissVersionable +"\">\n");
				}
				// insert the disseminator version-level elements
				String dissLabelString="";
				if (vdiss.dissLabel!=null && !vdiss.dissLabel.equals("")) {
					dissLabelString=" LABEL=\"" + StreamUtility.enc(vdiss.dissLabel) + "\"";
				}
				buf.append("    <" + FOXML_PREFIX 
					+ ":disseminatorVersion ID=\"" + vdiss.dissVersionID + "\"" 
					+ dissLabelString
					+ " BMECH_SERVICE_PID=\"" + vdiss.bMechID + "\""
					+ " CREATED=\"" + m_formatter.format(vdiss.dissCreateDT) +  "\">\n");
				
				// datastream bindings...	
				DSBinding[] bindings = vdiss.dsBindMap.dsBindings;
				buf.append("      <" + FOXML_PREFIX + ":serviceInputMap>\n");
				for (int j=0; j<bindings.length; j++){
					String bindLabelString="";
					if (bindings[j].bindLabel!=null && !bindings[j].bindLabel.equals("")) {
						bindLabelString=" LABEL=\"" 
						+ StreamUtility.enc(bindings[j].bindLabel) + "\"";
					}				
	                buf.append("        <" + FOXML_PREFIX + ":datastreamBinding KEY=\""
	                        + bindings[j].bindKeyName + "\""
							+ bindLabelString
	                        + " ORDER=\"" + bindings[j].seqNo + "\""
							+ " DATASTREAM_ID=\"" + bindings[j].datastreamID + "\"/>\n");
				}
				buf.append("      </" + FOXML_PREFIX + ":serviceInputMap>\n");
				buf.append("    </" + FOXML_PREFIX + ":disseminatorVersion>\n");
            }
			buf.append("  </" + FOXML_PREFIX + ":disseminator>\n");
        }
    }

    private void appendRootElementEnd(StringBuffer buf) {
        buf.append("</" + FOXML_PREFIX + ":digitalObject>");
    }

	private Datastream validateDatastream(Datastream ds) throws ObjectIntegrityException {
		// check on some essentials
		if (ds.DatastreamID==null || ds.DatastreamID.equals("")) {
			throw new ObjectIntegrityException("Datastream must have an id.");
		}
		if (ds.DSState==null || ds.DSState.equals("")) {
			throw new ObjectIntegrityException("Datastream must have a state indicator.");
		}
		if (ds.DSVersionID==null || ds.DSVersionID.equals("")) {
			throw new ObjectIntegrityException("Datastream must have a version id.");
		}
		if (!ds.DSControlGrp.equalsIgnoreCase("E") &&
			!ds.DSControlGrp.equalsIgnoreCase("R") &&
			!ds.DSControlGrp.equalsIgnoreCase("M") &&
			!ds.DSControlGrp.equalsIgnoreCase("X")) {
			throw new ObjectIntegrityException("Datastream control group must be E,R,M or X.");
		}
		if (ds.DSCreateDT==null) {
			throw new ObjectIntegrityException("Datastream must have a create date.");
		}
		if (!ds.DSControlGrp.equalsIgnoreCase("X") && 
			(ds.DSLocation==null || ds.DSLocation.equals(""))) {
			throw new ObjectIntegrityException("Content datastream must have a location.");
		}
		if ((ds.DSMIME==null || ds.DSVersionID.equals("")) && ds.DSControlGrp.equalsIgnoreCase("X")) {
			ds.DSMIME="text/xml";
		}
		if (ds.DSInfoType==null || ds.DSInfoType.equals("")
				|| ds.DSInfoType.equalsIgnoreCase("OTHER") ) {
			ds.DSInfoType="UNSPECIFIED";
		}
		if ( ds.DSLabel==null && ds.DSLabel.equals("") ) {
			ds.DSLabel = "Datastream known as: " + ds.DatastreamURI;
		}
		if (ds.DSFormatURI==null) {
			ds.DSFormatURI="";
		}		
		// Until future when we implement selective versioning,
		// set default to YES.
		if (ds.DSVersionable==null || ds.DSVersionable.equals("")) {
			ds.DSVersionable="YES";
		}
		// For METS backward compatibility:
		// If we have a METS MDClass value, preserve MDClass and MDType in a format URI
		if (ds.DSControlGrp.equalsIgnoreCase("X")) {
			if ( ((DatastreamXMLMetadata)ds).DSMDClass !=0 ) {
				String mdClassName = "";
				String mdType=ds.DSInfoType;
				String otherType="";
				if (((DatastreamXMLMetadata)ds).DSMDClass==1) {mdClassName = "techMD";
				} else if (((DatastreamXMLMetadata)ds).DSMDClass==2) {mdClassName = "sourceMD";
				} else if (((DatastreamXMLMetadata)ds).DSMDClass==3) {mdClassName = "rightsMD";
				} else if (((DatastreamXMLMetadata)ds).DSMDClass==4) {mdClassName = "digiprovMD";
				} else if (((DatastreamXMLMetadata)ds).DSMDClass==5) {mdClassName = "descMD";}			
				if ( !mdType.equals("MARC") && !mdType.equals("EAD")
						&& !mdType.equals("DC") && !mdType.equals("NISOIMG")
						&& !mdType.equals("LC-AV") && !mdType.equals("VRA")
						&& !mdType.equals("TEIHDR") && !mdType.equals("DDI")
						&& !mdType.equals("FGDC") ) {
					mdType="OTHER";
					otherType=ds.DSInfoType;
				}
				ds.DSFormatURI = 
					"info:fedora/format:xml:mets:" 
					+ mdClassName + ":" + mdType + ":" + otherType;
			}
		}
		return ds;
	}

	private Disseminator validateDisseminator(Disseminator dissVersion) throws ObjectIntegrityException {
		if (dissVersion.dissVersionID==null || dissVersion.dissVersionID.equals("")) {
			throw new ObjectIntegrityException("Object's disseminator must have a version id.");
		}
		if (dissVersion.dissState==null || dissVersion.dissState.equals("")) {
			throw new ObjectIntegrityException("Object's disseminator must have a state.");
		}
		if (dissVersion.bDefID==null || dissVersion.bDefID.equals("")) {
			throw new ObjectIntegrityException("Object's disseminator must have a bdef id.");
		}
		if (dissVersion.bMechID==null || dissVersion.bMechID.equals("")) {
			throw new ObjectIntegrityException("Object's disseminator must have a bdef id.");
		}
		if (dissVersion.dissCreateDT==null) {
			throw new ObjectIntegrityException("Object's disseminator must have a create date.");
		}
		// Until future when we implement selective versioning,
		// set default to YES.
		if (dissVersion.dissVersionable==null || dissVersion.dissVersionable.equals("")) {
			dissVersion.dissVersionable="YES";
		}
		return dissVersion;
	}
	
	private void validateAudit(AuditRecord audit) throws ObjectIntegrityException {
		if (audit.id==null) {
			throw new ObjectIntegrityException("Audit record must have id.");
		}
		if (audit.date==null) {
			throw new ObjectIntegrityException("Audit record must have date.");
		}
		if (audit.processType==null) {
			throw new ObjectIntegrityException("Audit record must have processType.");
		}
		if (audit.action==null) {
			throw new ObjectIntegrityException("Audit record must have action.");
		}
		if (audit.componentID==null) {
			audit.componentID = ""; // for backwards compatibility, no error on null
			// throw new ObjectIntegrityException("Audit record must have componentID.");
		}
		if (audit.responsibility==null) {
			throw new ObjectIntegrityException("Audit record must have responsibility.");
		}
		if (audit.justification==null) {
			throw new ObjectIntegrityException("Audit record must have justification.");
		}
	}
	private String normalizeDSLocat(String PID, Datastream ds) {
		// SERIALIZE FOR EXPORT: Ensure that ds location is appropriate for export (public usage)
		if (m_encodeForExport){
			String publicLoc=ds.DSLocation;
			if (ds.DSControlGrp.equals("E") || ds.DSControlGrp.equals("R")){
				// make sure ACTUAL host:port is on ds location for localized content URLs
				if (ds.DSLocation!=null && 
					ds.DSLocation.startsWith("http://local.fedora.server/")) {
					// That's our cue.. make it a proper URL with the server's host:port
					publicLoc=s_hostInfo + ds.DSLocation.substring(27);
				}
				return publicLoc;
			} else if (ds.DSControlGrp.equals("M")) {
				// make sure internal ids are converted to public dissemination URLs
				publicLoc=s_localServerDissemUrlStart 
						+ PID 
						+ "/fedora-system:3/getItem/"
						+ m_formatter.format(ds.DSCreateDT)
						+ "?itemID=" + ds.DatastreamID;
				return publicLoc;
			} else {
				return publicLoc;
			}
		}
		// SERIALIZE FOR INTERNAL STORAGE (or for GetObjectXML requests): 
		// Ensure that ds location contains the internal storage identifiers
		else {
			String newLoc=ds.DSLocation;
			if (ds.DSControlGrp.equals("E") || ds.DSControlGrp.equals("R")) {
				// When ds location makes reference to the LOCAL machine and port
				// (i.e., the one that the repository is running on), then we want to put 
				// a "localizer" string in the ds location.  This is to prevent breakage if the 
				// repository host:port is reconfigured after an object has been ingested.
				newLoc=s_localServerUrlStartWithPort.matcher(ds.DSLocation).replaceAll("http://local.fedora.server/");
				newLoc=s_localhostUrlStartWithPort.matcher(ds.DSLocation).replaceAll("http://local.fedora.server/");
			  	if (m_onPort80) {
					newLoc=s_localServerUrlStartWithoutPort.matcher(ds.DSLocation).replaceAll("http://local.fedora.server/");
					newLoc=s_localhostUrlStartWithoutPort.matcher(ds.DSLocation).replaceAll("http://local.fedora.server/");
			  	}
			  	return newLoc;
			} else if (ds.DSControlGrp.equals("M")) {
				// make sure ds location is an internal identifier (PID+DSID+DSVersionID)
				if (!ds.DSLocation.startsWith(PID)) {
					newLoc = PID + "+" + ds.DatastreamID + "+" + ds.DSVersionID;
				}
				return newLoc;
			} else {
				return newLoc;
			}
		}
	}
	
	private String normalizeDSInlineXML(DatastreamXMLMetadata ds) {
		String xml = null;
		try {
			xml = new String(ds.xmlContent, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			// wont happen, java always supports UTF-8
		}
		if (m_encodeForExport) {
			// Make appropriate for EXPORT:
			// detect any "localized" placeholders ("local.fedora.server")
			// and replace with host:port of the local server.
			xml=s_localPattern.matcher(xml).replaceAll(s_hostInfo);
		} else {
			// Make appropriate for INTERNAL STORE (and for GetObjectXML):
			// detect host:port pattern that is the local server and
			// "localize" URLs with the internal placeholder "local.fedora.server"
			xml=s_localServerUrlStartWithPort.matcher(xml).replaceAll(
					"http://local.fedora.server/");
			xml=s_localhostUrlStartWithPort.matcher(xml).replaceAll(
					"http://local.fedora.server/");
			if (m_onPort80) {
				xml=s_localServerUrlStartWithoutPort.matcher(xml).replaceAll(
						"http://local.fedora.server/");
				xml=s_localhostUrlStartWithoutPort.matcher(xml).replaceAll(
						"http://local.fedora.server/");
			}
		}
		return xml;
		
	}
	
	private String getTypeAttribute(DigitalObject obj)
			throws ObjectIntegrityException {
		int t=obj.getFedoraObjectType();
		if (t==DigitalObject.FEDORA_BDEF_OBJECT) {
			return "FedoraBDefObject";
		} else if (t==DigitalObject.FEDORA_BMECH_OBJECT) {
			return "FedoraBMechObject";
		} else if (t==DigitalObject.FEDORA_OBJECT) {
			return "FedoraObject";
		} else {
			throw new ObjectIntegrityException("Object must have a FedoraObjectType.");
		}
	}
	
    private void writeToStream(StringBuffer buf, OutputStream out,
            String encoding, boolean closeWhenFinished)
            throws StreamIOException, UnsupportedEncodingException {
        try {
            out.write(buf.toString().getBytes(encoding));
            out.flush();
        } catch (IOException ioe) {
            throw new StreamWriteException("Problem serializing to FOXML: "
                    + ioe.getMessage());
        } finally {
            if (closeWhenFinished) {
                try {
                    out.close();
                } catch (IOException ioe2) {
                    throw new StreamWriteException("Problem closing stream after "
                            + " serializing to FOXML: " + ioe2.getMessage());
                }
            }
        }
    }
 }