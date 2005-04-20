package fedora.client.ingest;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import fedora.client.Administrator;
import fedora.client.APIAStubFactory;
import fedora.client.APIMStubFactory;

import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;
import fedora.server.types.gen.RepositoryInfo;
import fedora.server.types.gen.UserInfo;

/**
 * Launch a dialog for entering login information for a source repository.
 * getAPIA() and getAPIM() will return non-null if login information
 * is entered.
 *
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */

public class SourceRepoDialog
        extends JDialog {
        
    private JTextField m_serverField;
	private JTextField m_protocolField;
    private JTextField m_usernameField;
    private JPasswordField m_passwordField;
    
    private FedoraAPIA m_apia;
    private FedoraAPIM m_apim;
    
    private RepositoryInfo m_repositoryInfo;
    private UserInfo m_userInfo;
    
    private static String s_lastServer;
	private static String s_lastProtocol;
    private static String s_lastUsername;
    private static String s_lastPassword;

	private String m_protocol;
    private String m_host;
    private int m_port;
        
    public SourceRepoDialog() {
        super(JOptionPane.getFrameForComponent(Administrator.getDesktop()), "Source Repository", true);

        JPanel inputPane=new JPanel();
        inputPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(6, 6, 6, 6),
                    BorderFactory.createEtchedBorder()
                ),
                BorderFactory.createEmptyBorder(6,6,6,6)
                ));
                
        GridBagLayout gridBag=new GridBagLayout();
        inputPane.setLayout(gridBag);
        JLabel serverLabel=new JLabel("Fedora Server");
		JLabel protocolLabel=new JLabel("Protocol");
        JLabel usernameLabel=new JLabel("Username");
        JLabel passwordLabel=new JLabel("Password");
        if (s_lastServer==null) s_lastServer="hostname:portnumber";
        m_serverField=new JTextField(s_lastServer);
		if (s_lastProtocol==null) s_lastProtocol="http";
		m_protocolField=new JTextField(s_lastProtocol);
        if (s_lastUsername==null) s_lastUsername="fedoraAdmin";
        m_usernameField=new JTextField(s_lastUsername);
        if (s_lastPassword==null) s_lastPassword="";
        m_passwordField=new JPasswordField(s_lastPassword);
        addLabelValueRows(new JLabel[] {serverLabel, protocolLabel, usernameLabel, passwordLabel}, 
                new JComponent[] {m_serverField, m_protocolField, m_usernameField, m_passwordField}, 
                gridBag, inputPane);

        JButton okButton=new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                // construct apia and apim after doing some field validation
                if (m_passwordField.getPassword().length == 0 || m_usernameField.getText().equals("")) {
                    JOptionPane.showMessageDialog(Administrator.getDesktop(),
                        "Username and password must both be non-empty",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                } else {
					
                    String[] hp=m_serverField.getText().split(":");
                    if (hp.length==2) {
                        try {
                            m_protocol=m_protocolField.getText();
							m_host=hp[0];
							m_port=Integer.parseInt(hp[1]);
                            
                            // Get SOAP stubs for the source repository.
                            // NOTE! For backward compatibility with Fedora 2.0
                            // we will immediately try a describe repository
                            // request on the API-A stub to see if it works.  If it
                            // fails, we will try obtaining a stub with the OLD
                            // SOAP URL syntax.  This is because the path in the 
                            // SOAP URLs were changed in Fedora 2.1 to be more standard.
                            
                            try {
                            	//System.out.println("Getting stubs with default path...");
								m_apia=APIAStubFactory.getStub(m_protocol,
															   m_host, 
															   m_port, 
															   m_usernameField.getText(),
															   new String(m_passwordField.getPassword()));
								
								m_apim=APIMStubFactory.getStub(m_protocol,
															   m_host, 
															   m_port, 
															   m_usernameField.getText(),
															   new String(m_passwordField.getPassword()));
															   
								// try a request to see if things work ok
								m_repositoryInfo=m_apia.describeRepository();
								
							} catch (Exception e) {
								// If request on default stub fails, try the old URL path for the service
								//System.out.println("Fallback: getting stubs with OLD path...");
								m_apia=APIAStubFactory.getStubAltPath(m_protocol,
															   m_host, 
															   m_port,
															   "/fedora/access/soap", 
															   m_usernameField.getText(),
															   new String(m_passwordField.getPassword()));
								m_apim=APIMStubFactory.getStubAltPath(m_protocol,
															   m_host, 
															   m_port,
															   "/fedora/management/soap",  
															   m_usernameField.getText(),
															   new String(m_passwordField.getPassword()));
                            }

                            try {
                                m_repositoryInfo=m_apia.describeRepository();
                                m_userInfo=m_apim.describeUser(m_usernameField.getText());
                                s_lastServer=m_host + ":" + m_port;
								s_lastProtocol=m_protocol;
                                s_lastUsername=m_usernameField.getText();
                                s_lastPassword=new String(m_passwordField.getPassword());
                                dispose();
                            } catch (Exception e) {
                                // earlier repositories won't support these
                                // methods... so here we only fail if there's
                                // a connection or authentication problem
                                boolean retry=false;
                                if (e.getMessage().indexOf("Unauthorized")!=-1 || e.getMessage().indexOf("Unrecognized")!=-1) {
                                	Administrator.showErrorDialog(Administrator.getDesktop(), "Connection Error", 
                                			"Bad username or password.", e);
                                    retry=true;
                                }
                                if (e.getMessage().indexOf("java.net")!=-1) {
                                	Administrator.showErrorDialog(Administrator.getDesktop(), "Connection Error", 
                                			"Can't connect to " + m_protocol + "://" + m_host + ":" + m_port, e);
                                    retry=true;
                                }
                                if (!retry) {
                                    // the exception must have been the result
                                    // of an unimplemented method on a prior
                                    // version of fedora, which is ok.
                                    s_lastServer=m_host + ":" + m_port;
									s_lastProtocol=m_protocol;
                                    s_lastUsername=m_usernameField.getText();
                                    s_lastPassword=new String(m_passwordField.getPassword());
                                    dispose();
                                } else {
                                    e.printStackTrace();
                                }
                            }
                        } catch (NumberFormatException nfe) {
                        	Administrator.showErrorDialog(Administrator.getDesktop(), "Error", 
                        			"Server port must be numeric", nfe);
                        } catch (Exception e) {
                        	Administrator.showErrorDialog(Administrator.getDesktop(), "Error", 
                        			"Malformed host information", e);
                        }
                    } else {
                        JOptionPane.showMessageDialog(Administrator.getDesktop(),
                            "Server should be specified as host:port",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        okButton.setText("OK");
        JButton cancelButton=new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                dispose();
            }
        });
        cancelButton.setText("Cancel");
        JPanel buttonPane=new JPanel();
        buttonPane.add(okButton);
        buttonPane.add(cancelButton);
        Container contentPane=getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(inputPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        
        pack();
        setLocation(Administrator.INSTANCE.getCenteredPos(getWidth(), getHeight()));
        setVisible(true);
    }

    public FedoraAPIA getAPIA() {
        return m_apia;
    }

	public String getProtocol() {
		return m_protocol;
	}
	
    public String getHost() {
        return m_host;
    }

    public int getPort() {
        return m_port;
    }

    public FedoraAPIM getAPIM() {
        return m_apim;
    }
    
    public RepositoryInfo getRepositoryInfo() {
        return m_repositoryInfo;
    }

    public UserInfo getUserInfo() {
        return m_userInfo;
    }

    public void addLabelValueRows(JLabel[] labels, JComponent[] values,
            GridBagLayout gridBag, Container container) {
        GridBagConstraints c=new GridBagConstraints();
        c.insets=new Insets(0, 6, 6, 6);
        for (int i=0; i<labels.length; i++) {
            c.anchor=GridBagConstraints.EAST;
            c.gridwidth=GridBagConstraints.RELATIVE; //next-to-last
            c.fill=GridBagConstraints.NONE;      //reset to default
            c.weightx=0.0;                       //reset to default
            gridBag.setConstraints(labels[i], c);
            container.add(labels[i]);

            c.gridwidth=GridBagConstraints.REMAINDER;     //end row
            if (!(values[i] instanceof JComboBox)) {
                c.fill=GridBagConstraints.HORIZONTAL;
            } else {
                c.anchor=GridBagConstraints.WEST;
            }
            c.weightx=1.0;
            gridBag.setConstraints(values[i], c);
            container.add(values[i]);
        }

    }    
    
}