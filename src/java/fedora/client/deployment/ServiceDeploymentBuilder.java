/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.client.deployment;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.InputStream;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fedora.client.Administrator;
import fedora.client.deployment.data.ServiceDeploymentTemplate;
import fedora.client.deployment.data.DCElement;
import fedora.client.deployment.data.DSInputRule;
import fedora.client.deployment.data.Datastream;
import fedora.client.deployment.data.Method;
import fedora.client.deployment.data.MethodParm;
import fedora.client.deployment.data.MethodProperties;
import fedora.client.deployment.data.ServiceSoftware;
import fedora.client.deployment.xml.ServiceDeploymentMETSSerializer;
import fedora.client.deployment.xml.DCGenerator;
import fedora.client.deployment.xml.DSInputSpecGenerator;
import fedora.client.deployment.xml.MethodMapGenerator;
import fedora.client.deployment.xml.ServiceProfileSerializer;
import fedora.client.deployment.xml.WSDLGenerator;
import fedora.client.utility.ingest.AutoIngestor;

import static fedora.common.Constants.METS_EXT1_1;

/**
 * @author Sandy Payette
 */
public class ServiceDeploymentBuilder
        extends JInternalFrame {

    private static final long serialVersionUID = 1L;

    protected JTabbedPane tabpane;

    protected ServiceDeploymentTemplate newSDep;

    private File s_lastDir = null;

    private String currentTabName;

    private int currentTabIndex;

    public static void main(String[] args) {
        try {
            if (args.length == 5) {
                JFrame frame = new JFrame("ServiceDeploymentBuilder Test");
                String protocol = args[0];
                String host = args[1];
                int port = new Integer(args[2]).intValue();
                String user = args[3];
                String pass = args[4];
                File dir = null;
                frame.addWindowListener(new WindowAdapter() {

                    public void windowClosing(WindowEvent e) {
                        System.exit(0);
                    }
                });
                frame.getContentPane().add(new ServiceDeploymentBuilder(protocol,
                                                            host,
                                                            port,
                                                            user,
                                                            pass,
                                                            dir),
                                           BorderLayout.CENTER);
                frame.setSize(700, 500);
                frame.setVisible(true);
            } else {
                System.out
                        .println("ServiceDeploymentBuilder main method requires 5 arguments.");
                System.out
                        .println("Usage: ServiceDeploymentBuilder protocol host port user pass");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public ServiceDeploymentBuilder(String protocol,
                        String host,
                        int port,
                        String user,
                        String pass,
                        File dir) {
        super("Service Deployment Builder");
        s_lastDir = dir;
        setClosable(true);
        setMaximizable(true);
        setSize(700, 500);
        getContentPane().setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        newSDep = new ServiceDeploymentTemplate();

        tabpane = new JTabbedPane();
        tabpane.setBackground(Color.GRAY);
        tabpane.addTab("General", createGeneralPane());
        tabpane.addTab("Service Profile", createProfilePane());
        tabpane.addTab("Service Methods", createMethodsPane());
        tabpane.addTab("Datastream Input", createDSInputPane());
        tabpane.addTab("Documentation", createDocPane());

        // General Buttons Panel
        JButton save = new JButton("Save");
        save.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        JButton ingest = new JButton("Ingest");
        ingest.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ingest();
            }
        });
        JButton help = new JButton("Help");
        help.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showHelp();
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        JPanel gbuttonPanel = new JPanel();
        gbuttonPanel.setBackground(Color.WHITE);
        gbuttonPanel.add(save);
        gbuttonPanel.add(ingest);
        gbuttonPanel.add(help);
        gbuttonPanel.add(cancel);

        getContentPane().add(tabpane, BorderLayout.CENTER);
        getContentPane().add(gbuttonPanel, BorderLayout.SOUTH);
        setListeners();
        setVisible(true);
    }

    private void setListeners() {
        // set up listener for JTabbedPane object
        tabpane.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                // everytime a tab changes, update the sdef template object in
                // memory
                updateTemplate();
                currentTabIndex = tabpane.getSelectedIndex();
                currentTabName = tabpane.getTitleAt(currentTabIndex);
                // pre-populate the DatastreamInputPane with valid datastream
                // input parms that were defined in the MethodsPane
                if (currentTabIndex == 3) {
                    DatastreamInputPane dsip =
                            (DatastreamInputPane) tabpane.getComponentAt(3);
                    dsip.renderDSBindingKeys(newSDep.getDSBindingKeys());
                }
            }
        });
    }

    public ServiceDeploymentTemplate getTemplate() {
        return newSDep;
    }

    public void save() {
        ServiceDeploymentMETSSerializer mets = savePanelInfo();
        File file = null;
        if (mets != null) {
            JFileChooser chooser = new JFileChooser(s_lastDir);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            XMLFileChooserFilter filter = new XMLFileChooserFilter();
            chooser.setFileFilter(filter);
            if (chooser.showSaveDialog(tabpane) == JFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();
                s_lastDir = file.getParentFile(); // remember the dir for next
                // time
                String ext = filter.getExtension(file);
                if (ext == null || !ext.equalsIgnoreCase("xml")) {
                    file = new File((file.getPath() + ".xml"));
                }
                try {
                    mets.writeMETSFile(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    assertTabPaneMsg(("ServiceDeploymentBuilder: Error saving METS file for deployment: " + e
                                             .getMessage()),
                                     "ServiceDeploymentBuilder");
                }
            } else {
                assertTabPaneMsg("ServiceDeploymentBuilder: You did not specify a file to Save.",
                                 "ServiceDeploymentBuilder");
            }
        }
    }

    public void ingest() {
        InputStream in = null;
        String pid = null;
        ServiceDeploymentMETSSerializer mets = savePanelInfo();
        if (mets != null) {
            try {
                in = mets.writeMETSStream();
            } catch (Exception e) {
                e.printStackTrace();
                assertTabPaneMsg(("ServiceDeploymentBuilder: Error saving METS to stream: " + e
                                         .getMessage()),
                                 "ServiceDeploymentBuilder");
            }
            try {
                AutoIngestor ingestor =
                        new AutoIngestor(Administrator.APIA, Administrator.APIM);
                pid =
                        ingestor
                                .ingestAndCommit(in,
                                                 METS_EXT1_1.uri,
                                                 "ingest deployment object via ServiceDeploymentBuilder tool");
            } catch (Exception e) {
                e.printStackTrace();
                assertTabPaneMsg(("ServiceDeploymentBuilder: error ingesting deployment object: " + e
                                         .getMessage()),
                                 null);
            }
            assertTabPaneMsg(("New PID = " + pid), "Successful Ingest");
        }
    }

    public void showHelp() {
        if (currentTabIndex == 0) {
            showGeneralHelp();
        } else if (currentTabIndex == 1) {
            showProfileHelp();
        } else if (currentTabIndex == 2) {
            showMethodsHelp();
        } else if (currentTabIndex == 3) {
            showDatastreamsHelp();
        } else if (currentTabIndex == 4) {
            showDocumentsHelp();
        }
    }

    public void cancel() {
        setVisible(false);
        dispose();
    }

    protected void updateTemplate() {
        Component[] tabs = tabpane.getComponents();
        for (Component element : tabs) {
            if (element.getName().equalsIgnoreCase("GeneralTab")) {
                GeneralPane gp = (GeneralPane) element;
                if (gp.rb_chosen.equalsIgnoreCase("retainPID")) {
                    newSDep.setbObjPID(gp.getBObjectPID());
                } else {
                    newSDep.setbObjPID(null);
                }
                newSDep.setSDefContractPID(gp.getSDefContractPID());
                newSDep.setbObjLabel(gp.getBObjectLabel());
                newSDep.setbObjName(gp.getBObjectName());
                newSDep.setDCRecord(gp.getDCElements());
            } else if (element.getName().equalsIgnoreCase("ProfileTab")) {
                // set the datastream input rules
                ServiceProfilePane spp = (ServiceProfilePane) element;
                newSDep.setServiceProfile(spp.getServiceProfile());
            } else if (element.getName().equalsIgnoreCase("MethodsTab")) {
                MethodsPane mp = (MethodsPane) element;
                newSDep.setHasBaseURL(mp.hasBaseURL());
                if (mp.hasBaseURL()) {
                    String baseURL = mp.getBaseURL();
                    if (baseURL.endsWith("/")) {
                        newSDep.setServiceBaseURL(baseURL);
                    } else {
                        newSDep.setServiceBaseURL(baseURL + "/");
                    }
                } else {
                    newSDep.setServiceBaseURL("LOCAL");
                }
                HashMap mmap = mp.getMethodMap();
                Method[] methods = mp.getMethods();
                newSDep.setMethodsHashMap(mmap);
                newSDep.setMethods(methods);

                // we need to update the deployment template object with the latest
                // datastream binding keys that are defined as method parms
                Vector<String> dsBindingKeys = new Vector<String>();
                for (Method element2 : methods) {
                    MethodProperties props = element2.methodProperties;
                    if (props != null) {
                        for (int j = 0; j < props.dsBindingKeys.length; j++) {
                            if (!dsBindingKeys.contains(props.dsBindingKeys[j])) {
                                dsBindingKeys.add(props.dsBindingKeys[j]);
                            }
                        }
                    }
                }
                newSDep.setDSBindingKeys(dsBindingKeys);
            } else if (element.getName().equalsIgnoreCase("DSInputTab")) {
                // set the datastream input rules
                DatastreamInputPane dsp = (DatastreamInputPane) element;
                newSDep.setDSInputSpec(dsp.getDSInputRules());
            } else if (element.getName().equalsIgnoreCase("DocumentsTab")) {
                DocumentsPane docp = (DocumentsPane) element;
                newSDep.setDocDatastreams(docp.getDocDatastreams());
            }
        }
        return;
    }

    protected boolean validateTemplate() {
        Component[] tabs = tabpane.getComponents();
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i].getName().equalsIgnoreCase("GeneralTab")) {
                if (!validGeneralTab((GeneralPane) tabs[i])) {
                    return false;
                }
            } else if (tabs[i].getName().equalsIgnoreCase("ProfileTab")) {
                if (!validProfileTab((ServiceProfilePane) tabs[i])) {
                    return false;
                }
            } else if (tabs[i].getName().equalsIgnoreCase("MethodsTab")) {
                if (!validMethodsTab((MethodsPane) tabs[i])) {
                    return false;
                }
            } else if (tabs[i].getName().equalsIgnoreCase("DSInputTab")) {
                if (!validDSInputTab((DatastreamInputPane) tabs[i])) {
                    return false;
                }
            } else if (tabs[i].getName().equalsIgnoreCase("DocumentsTab")) {
                if (!validDocsTab((DocumentsPane) tabs[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    public ServiceDeploymentMETSSerializer savePanelInfo() {
        updateTemplate();
        ServiceDeploymentMETSSerializer mets = null;
        if (validateTemplate()) {
            DCGenerator dcg = null;
            DSInputSpecGenerator dsg = null;
            MethodMapGenerator mmg = null;
            WSDLGenerator wsdlg = null;
            ServiceProfileSerializer spg = null;
            try {
                dcg = new DCGenerator(newSDep);
                // dcg.printDC();
            } catch (Exception e) {
                e.printStackTrace();
                assertTabPaneMsg("ServiceDeploymentBuilder: error serializing dc record",
                                 null);
            }
            try {
                spg = new ServiceProfileSerializer(newSDep);
            } catch (Exception e) {
                e.printStackTrace();
                assertTabPaneMsg("ServiceDeploymentBuilder: error serializing service profile",
                                 null);
            }
            try {
                dsg = new DSInputSpecGenerator(newSDep);
                // dsg.printDSInputSpec();
            } catch (Exception e) {
                e.printStackTrace();
                assertTabPaneMsg("ServiceDeploymentBuilder: error serializing ds input spec",
                                 null);
            }
            try {
                mmg = new MethodMapGenerator(newSDep);
                // mmg.printMethodMap();
            } catch (Exception e) {
                e.printStackTrace();
                assertTabPaneMsg("ServiceDeploymentBuilder: error serializing method map",
                                 null);
            }
            try {
                wsdlg = new WSDLGenerator(newSDep);
                // wsdlg.printWSDL();
            } catch (Exception e) {
                e.printStackTrace();
                assertTabPaneMsg("ServiceDeploymentBuilder: error serializing wsdl", null);
            }

            try {
                mets =
                        new ServiceDeploymentMETSSerializer(newSDep,
                                                dcg.getRootElement(),
                                                spg.getRootElement(),
                                                dsg.getRootElement(),
                                                mmg.getRootElement(),
                                                wsdlg.getRootElement());
            } catch (Exception e) {
                e.printStackTrace();
                assertTabPaneMsg("ServiceDeploymentBuilder: error in creating METS.",
                                 null);
            }
            // mets.printMETS();
            // return mets;
        }
        return mets;
    }

    private JComponent createGeneralPane() {
        GeneralPane gpane = new GeneralPane(this);
        gpane.setName("GeneralTab");
        return gpane;
        // return new JLabel("Insert general stuff here.");
    }

    private JComponent createProfilePane() {
        ServiceProfilePane profpane = new ServiceProfilePane(this);
        profpane.setName("ProfileTab");
        return profpane;
    }

    private JComponent createMethodsPane() {
        MethodsPane mpane = new MethodsPane(this);
        mpane.setName("MethodsTab");
        return mpane;
    }

    private JComponent createDSInputPane() {
        DatastreamInputPane dspane = new DatastreamInputPane(this);
        dspane.setName("DSInputTab");
        return dspane;
    }

    private JComponent createDocPane() {
        DocumentsPane docpane = new DocumentsPane();
        docpane.setName("DocumentsTab");
        return docpane;
    }

    private boolean validGeneralTab(GeneralPane gp) {
        if (gp.rb_chosen.equalsIgnoreCase("retainPID")
                && (gp.getBObjectPID() == null || gp.getBObjectPID().trim()
                        .equals(""))) {
            assertTabPaneMsg("The test PID value is missing on General Tab.",
                             gp.getName());
            return false;
        } else if (gp.getSDefContractPID() == null
                || gp.getSDefContractPID().trim().equals("")) {
            assertTabPaneMsg("SDefPID is missing on General Tab.", gp.getName());
            return false;
        } else if (gp.getBObjectLabel() == null
                || gp.getBObjectLabel().trim().equals("")) {
            assertTabPaneMsg("Service Object Description is missing on General Tab.",
                             gp.getName());
            return false;
        } else if (gp.getBObjectName() == null
                || gp.getBObjectName().trim().equals("")) {
            assertTabPaneMsg("Service Object Name (1-word) is missing on General Tab.",
                             gp.getName());
            return false;
        } else if (gp.getDCElements().length <= 0) {
            assertTabPaneMsg("You must enter at least one DC element on General Tab.",
                             gp.getName());
            return false;
        }
        return true;
    }

    private boolean validProfileTab(ServiceProfilePane spp) {
        if (spp.getServiceName() == null) {
            assertTabPaneMsg(new String("You must enter a Service name"
                    + " in the Service Profile Tab"), spp.getName());
            return false;
        } else if (spp.getMsgProtocol() == null) {
            assertTabPaneMsg(new String("You must enter the messaging protocol for"
                                     + " this service in the Service Profile Tab"),
                             spp.getName());
            return false;
        } else if (spp.getOutputMIMETypes().length == 0) {
            assertTabPaneMsg(new String("You must enter at least one output MIME type"
                                     + " for this service in the Service Profile Tab"),
                             spp.getName());
            return false;
        }
        return true;
    }

    private boolean validMethodsTab(MethodsPane mp) {
        if (mp.hasBaseURL()
                && (mp.getBaseURL() == null || mp.getBaseURL().trim()
                        .equals(""))) {
            assertTabPaneMsg("The Base URL is missing on Service Methods Tab.",
                             mp.getName());
            return false;
        } else if (mp.getMethods().length <= 0) {
            assertTabPaneMsg("You must enter at least one method definition in Service Methods Tab.",
                             mp.getName());
            return false;
        } else {
            Method[] methods = mp.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].methodProperties == null) {
                    assertTabPaneMsg(new String("You must enter properties for the method "
                                             + methods[i].methodName)
                                             + " in the Service Methods Tab",
                                     mp.getName());
                    return false;
                } else if (!methods[i].methodProperties.wasValidated) {
                    assertTabPaneMsg(new String("You must enter valid properties for the method "
                                             + methods[i].methodName
                                             + " in the Service Methods Tab"),
                                     mp.getName());
                    return false;
                }
            }
            return true;
        }
    }

    private boolean validDSInputTab(DatastreamInputPane dsp) {
        DSInputRule[] rules = dsp.getDSInputRules();
        Vector bindkeys = newSDep.getDSBindingKeys();
        if (bindkeys.size() != rules.length) {
            assertTabPaneMsg(new String("You have not completed entry of the Datastream"
                                     + " input binding rules"
                                     + " in the Datastream Input Tab"),
                             dsp.getName());
            return false;
        }
        for (DSInputRule element : rules) {
            if (element.bindingKeyName == null) {
                assertTabPaneMsg(new String("A Datastream parm name is missing"
                                         + " from column 1 of the table in the Datastream Input Tab"),
                                 dsp.getName());
                return false;
            } else if (element.bindingMIMEType == null
                    || element.bindingMIMEType.trim().equalsIgnoreCase("")) {
                assertTabPaneMsg(new String("You must enter MIMEType for"
                        + " datastream input parm " + element.bindingKeyName
                        + " in the Datastream Input Tab"), dsp.getName());
                return false;
            } else if (element.minNumBindings == null) {
                assertTabPaneMsg(new String("You must enter Min Occurs for"
                        + " datastream input parm " + element.bindingKeyName
                        + " on Datastream Input Tab."), dsp.getName());
                return false;
            } else if (element.maxNumBindings == null) {
                assertTabPaneMsg(new String("You must enter Max Occurs for"
                        + " datastream input parm " + element.bindingKeyName
                        + " on Datastream Input Tab."), dsp.getName());
                return false;
            } else if (element.ordinality == null) {
                assertTabPaneMsg(new String("You must enter Order Matters for"
                        + " datastream input parm " + element.bindingKeyName
                        + " on Datastream Input Tab."), dsp.getName());
                return false;
            } else if (element.bindingLabel == null) {
                assertTabPaneMsg(new String("You must enter Pretty Label for"
                        + " datastream input parm " + element.bindingKeyName
                        + " on Datastream Input Tab."), dsp.getName());
                return false;
            }
        }
        return true;
    }

    private boolean validDocsTab(DocumentsPane docp) {
        Datastream[] docs = docp.getDocDatastreams();
        if (docs.length < 1) {
            assertTabPaneMsg(new String("You must enter at least one document"
                                     + " that describes the service in the Documents Tab."),
                             docp.getName());
            return false;
        }

        for (Datastream element : docs) {
            if (element.dsLabel == null) {
                assertTabPaneMsg(new String("You must enter a Label for all documents"
                                         + "listed on the Documents Tab."),
                                 docp.getName());
                return false;
            } else if (element.dsMIMEType == null) {
                assertTabPaneMsg(new String("You must enter a MIME type for all documents"
                                         + "listed on the Documents Tab."),
                                 docp.getName());
                return false;
            }
        }
        return true;
    }

    private void showGeneralHelp() {
        JTextArea helptxt = new JTextArea();
        helptxt.setLineWrap(true);
        helptxt.setWrapStyleWord(true);
        helptxt.setBounds(0, 0, 550, 20);
        helptxt
                .append("There are three sections to the General Tab that"
                        + " must be completed:\n\n"
                        + " Object Description:\n"
                        + " >>> Service Object PID: either select the button for the"
                        + " repository system to generate one, or enter your own"
                        + " with the prefix 'test:' or 'demo:'\n\n"
                        + " >>> Service Object Name:  enter a single word to name the object."
                        + " This name is used in various places within inline metadata that"
                        + " is generated by the tool.\n\n"
                        + " >>> Service Object Label: enter a meaningful label for theobject.\n\n"
                        + " \n"
                        + " Service Contract:\n"
                        + " >>> Service Definition PID: enter the PID of the Service Definition"
                        + " Object that the Service Deployment is fullfilling\n\n"
                        + " \n" + " Dublin Core Metadata:\n"
                        + ">>> Enter at least one DC element to describe"
                        + " the Service Deployment Object.");

        JOptionPane.showMessageDialog(this,
                                      helptxt,
                                      "Help for General Tab",
                                      JOptionPane.OK_OPTION);
    }

    private void showMethodsHelp() {
        JTextArea helptxt = new JTextArea();
        helptxt.setLineWrap(true);
        helptxt.setWrapStyleWord(true);
        helptxt.setBounds(0, 0, 550, 20);
        helptxt
                .append("Service Address:\n There are three types of service bindings that can"
                        + " be set up in a Service Deployment object:\n\n"
                        + " 1. Base URL (Service with a Base URL): You are mapping the"
                        + " Service Deployment object to a service that has a"
                        + " single base URL that all of the service methods are relative to."
                        + " The service will be used to transform or refactor Datastream content.\n\n"
                        + " 2. No Base URL (Multi-Server Service): You are mapping the"
                        + " Service Deployment object to a service whose methods do not have a"
                        + " common base URL.  Instead, different methods may run on different"
                        + " servers.  However, from the Fedora perspective these methods"
                        + " may be aggregated together in a single Service Deployment object"
                        + " to fulfill a Service contract. The service methods will be used to"
                        + " transform or refactor Datastream content.\n\n"
                        + " 2. Fedora Built-in Datastream Resolver: You are NOT mapping the"
                        + " Service Deployment object to a service. Instead, this Service Deployment"
                        + " object will partake of default capabilities of the Fedora repository"
                        + " server.  You can use this option if you simply want to make an association"
                        + " between methods of a behavor contract and Datastreams in the object."
                        + " So, for example, you want the Service contract methods to just return"
                        + " specific Datastreams in the object without transforming or refactoring"
                        + " those datastreams via a service.  This option is really just specifying"
                        + " a MethodName-to-Datastream binding relationship.\n\n\n"
                        + " Service Method Definitions:\n Here are the definitions of the specific methods"
                        + " that are runnable by the service.  A list of methods are automatically "
                        + " listed in the table.  These were obtained by looking up the abstract methods"
                        + " defined by the Service Definition Contract that you specified in the 'General Tab.'"
                        + " Use the 'Properties' button to the right of the table to enter specific service"
                        + " binding information for each method.");

        JOptionPane.showMessageDialog(this,
                                      helptxt,
                                      "Help for Service Methods Tab",
                                      JOptionPane.OK_OPTION);
    }

    private void showDatastreamsHelp() {
        JTextArea helptxt = new JTextArea();
        helptxt.setLineWrap(true);
        helptxt.setWrapStyleWord(true);
        helptxt.setBounds(0, 0, 550, 20);
        helptxt.append("insert datastream Input help\n\n");
        helptxt.append("\n\n");
        helptxt.append("\n\n");

        JOptionPane.showMessageDialog(this,
                                      helptxt,
                                      "Help for Datastream Input Tab",
                                      JOptionPane.OK_OPTION);
    }

    private void showDocumentsHelp() {
        JTextArea helptxt = new JTextArea();
        helptxt.setLineWrap(true);
        helptxt.setWrapStyleWord(true);
        helptxt.setBounds(0, 0, 550, 20);
        helptxt.append("insert documents help\n\n");
        helptxt.append("\n\n");
        helptxt.append("\n\n");

        JOptionPane.showMessageDialog(this,
                                      helptxt,
                                      "Help for Documents Tab",
                                      JOptionPane.OK_OPTION);
    }

    private void showProfileHelp() {
        JTextArea helptxt = new JTextArea();
        helptxt.setLineWrap(true);
        helptxt.setWrapStyleWord(true);
        helptxt.setBounds(0, 0, 550, 20);
        helptxt
                .append("Use the Service Profile to enter technical information about"
                        + " the service being mapped to this Service Deployment object.\n\n");

        JOptionPane.showMessageDialog(this,
                                      helptxt,
                                      "Help for Service Profile Tab",
                                      JOptionPane.OK_OPTION);
    }

    private void assertTabPaneMsg(String msg, String tabpane) {
        JOptionPane.showMessageDialog(this, new String(msg), new String(tabpane
                + " Message"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void print() {
        System.out.println("FROM GENERAL TAB===============================");
        System.out.println("sDefPID: " + newSDep.getSDefContractPID());
        System.out.println("sSepLabel: " + newSDep.getbObjLabel());
        System.out.println("DCRecord: ");
        DCElement[] dcrecord = newSDep.getDCRecord();
        for (DCElement element : dcrecord) {
            System.out.println(">>> " + element.elementName + "="
                    + element.elementValue);
        }
        System.out.println("FROM PROFILE TAB===============================");
        System.out.println("serviceName: "
                + newSDep.getServiceProfile().serviceName);
        System.out.println("serviceLabel: "
                + newSDep.getServiceProfile().serviceLabel);
        System.out.println("serviceTestURL: "
                + newSDep.getServiceProfile().serviceTestURL);
        System.out.println("Input MIME: ");
        String[] inputMIME = newSDep.getServiceProfile().inputMIMETypes;
        for (String element : inputMIME) {
            System.out.println(">>> " + element);
        }
        System.out.println("Input MIME: ");
        String[] outputMIME = newSDep.getServiceProfile().outputMIMETypes;
        for (String element : outputMIME) {
            System.out.println(">>> " + element);
        }
        System.out.println("SW Depend: ");
        ServiceSoftware[] sw = newSDep.getServiceProfile().software;
        for (ServiceSoftware element : sw) {
            System.out.println(">>> " + element.swName + "," + element.swType
                    + "," + element.swVersion + "," + element.swLicenceType
                    + ",");
        }
        System.out.println("FROM METHODS TAB===============================");
        System.out.println("hasBaseURL: " + newSDep.getHasBaseURL());
        System.out.println("serviceBaseURL: " + newSDep.getServiceBaseURL());
        System.out.println("methods: ");
        HashMap m2 = newSDep.getMethodsHashMap();
        Collection methods = m2.values();
        Iterator it_methods = methods.iterator();
        while (it_methods.hasNext()) {
            Method method = (Method) it_methods.next();
            System.out.println("  method name: " + method.methodName + "\n"
                    + "  method desc: " + method.methodLabel + "\n"
                    + "  method URL: " + method.methodProperties.methodFullURL
                    + "\n" + "  method protocol"
                    + method.methodProperties.protocolType + "\n");
            System.out.println("  method parms:");
            int parmcnt = method.methodProperties.methodParms.length;
            for (int i = 0; i < parmcnt; i++) {
                MethodParm mp = method.methodProperties.methodParms[i];
                System.out.println(">>>parmName: " + mp.parmName + "\n"
                        + ">>>parmType: " + mp.parmType + "\n"
                        + ">>>parmLabel: " + mp.parmLabel + "\n"
                        + ">>>parmDefaultValue: " + mp.parmDefaultValue + "\n"
                        + ">>>parmPassBy: " + mp.parmPassBy + "\n"
                        + ">>>parmRequired: " + mp.parmRequired + "\n"
                        + ">>>parmDomainValues: " + mp.parmDomainValues + "\n");
            }
        }
        System.out.println("FROM DSINPUT TAB===============================");
        DSInputRule[] rules = newSDep.getDSInputSpec();
        for (DSInputRule element : rules) {
            System.out.println(">>>name= " + element.bindingKeyName + "\n"
                    + ">>>mime= " + element.bindingMIMEType + "\n" + ">>>min= "
                    + element.minNumBindings + "\n" + ">>>max= "
                    + element.maxNumBindings + "\n" + ">>>order= "
                    + element.ordinality + "\n" + ">>>label= "
                    + element.bindingLabel + "\n" + ">>>instruct= "
                    + element.bindingInstruction + "\n");
        }
    }
}