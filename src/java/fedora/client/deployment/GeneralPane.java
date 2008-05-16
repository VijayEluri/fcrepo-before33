/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.client.deployment;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import fedora.client.Administrator;
import fedora.client.deployment.data.DCElement;

/**
 * @author Sandy Payette
 */
public class GeneralPane
        extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JInternalFrame parent;

    private JComboBox sDefPIDComboBox;

    private String sDefPID;

    protected JTextField bObjectPID;

    private JRadioButton rb_sysPID;

    private JRadioButton rb_retainPID;

    private final ButtonGroup rb_buttonGroup = new ButtonGroup();

    protected String rb_chosen;

    private JTextField bObjectLabel;

    private JTextField bObjectName;

    private JTable dcTable;

    protected DefaultTableModel dcTableModel;

    protected DCElementDialog dcDialog;

    private boolean editDCMode = false;

    protected String[] sDefOptions = new String[0];

    public GeneralPane(ServiceDeploymentBuilder parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(2, 1));
        topPanel.add(setDescriptionPanel());
        topPanel.add(setContractPanel());

        add(topPanel, BorderLayout.NORTH);
        add(setDCPanel(), BorderLayout.CENTER);
        setVisible(true);
    }

    public GeneralPane(ServiceDefinitionBuilder parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        add(setDescriptionPanel(), BorderLayout.NORTH);
        add(setDCPanel(), BorderLayout.CENTER);
        setVisible(true);
    }

    private JPanel setDescriptionPanel() {
        ActionListener rb_listen = new PIDActionListener();
        rb_sysPID = new JRadioButton("system assigned", true);
        rb_sysPID.setActionCommand("sysPID");
        rb_sysPID.addActionListener(rb_listen);
        rb_chosen = "sysPID";
        rb_retainPID = new JRadioButton("use PID", false);
        rb_retainPID.setActionCommand("retainPID");
        rb_retainPID.addActionListener(rb_listen);
        // rb_buttonGroup = new ButtonGroup();
        rb_buttonGroup.add(rb_sysPID);
        rb_buttonGroup.add(rb_retainPID);
        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new GridBagLayout());
        descriptionPanel
                .setBorder(new TitledBorder("Service Deployment Description"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0;
        gbc.gridx = 0;
        descriptionPanel.add(new JLabel("Service Object PID: "), gbc);
        gbc.gridx = 1;
        descriptionPanel.add(rb_sysPID, gbc);
        gbc.gridx = 2;
        descriptionPanel.add(rb_retainPID, gbc);
        gbc.gridx = 3;
        descriptionPanel.add(bObjectPID = new JTextField(10), gbc);
        bObjectPID
                .setToolTipText("The repository will accept test PIDs"
                        + " with the prefixes 'test:' or 'demo:' or a prefix you configured with your Fedora server."
                        + " Examples PIDs are: 'demo:1', test:50, my-behaviors:75, myprefix:200");
        bObjectPID.setEnabled(false);
        gbc.gridy = 1;
        gbc.gridx = 0;
        descriptionPanel
                .add(new JLabel("Service Object Name (1 word): "), gbc);
        gbc.gridx = 1;
        descriptionPanel.add(bObjectName = new JTextField(20), gbc);
        bObjectName
                .setToolTipText("This one-word name will be used in metadata"
                        + " within the service object (e.g., in WSDL as the service name).");
        gbc.gridy = 2;
        gbc.gridx = 0;
        descriptionPanel.add(new JLabel("Service Object Description: "), gbc);
        gbc.gridx = 1;
        descriptionPanel.add(bObjectLabel = new JTextField(20), gbc);
        bObjectLabel
                .setToolTipText("This is a free-form textual description of the"
                        + "service object.");
        gbc.gridy = 3;
        gbc.gridx = 0;
        descriptionPanel.add(new JLabel(" "), gbc);
        return descriptionPanel;
    }

    private JPanel setContractPanel() {
        JPanel contractPanel = new JPanel();
        contractPanel.setLayout(new GridBagLayout());
        contractPanel
                .setBorder(new TitledBorder("Service Definition Contract"));
        GridBagConstraints gbc2 = new GridBagConstraints();
        // gbc2.anchor = GridBagConstraints.WEST;
        gbc2.gridy = 0;
        gbc2.gridx = 0;
        contractPanel.add(new JLabel("Service: "), gbc2);

        // build dropdown of possible services by getting a full
        // list of sDefs from the server.
        Map allSDefLabels = null;
        try {
            /* FIXME: Find some other way to do this.. it depends on fType! */
            throw new UnsupportedOperationException("This operation uses obsolete field search semantics.");
        } catch (Exception e) {
            Administrator
                    .showErrorDialog(Administrator.getDesktop(),
                                     "Contact system administrator.",
                                     e.getMessage()
                                             + "\nError getting service definitions from repository!",
                                     e);
        }
        Map sDefLabels = new HashMap();
        Iterator iter = allSDefLabels.keySet().iterator();
        while (iter.hasNext()) {
            String pid = (String) iter.next();
            sDefLabels.put(pid, allSDefLabels.get(pid));
        }
        // set up the combobox
        sDefOptions = new String[sDefLabels.keySet().size() + 1];
        if (sDefOptions.length == 1) {
            sDefOptions[0] = "No Service definitions in repository!";
        } else {
            sDefOptions[0] = "[Select a Service Definition]";
        }
        iter = sDefLabels.keySet().iterator();
        int i = 1;
        while (iter.hasNext()) {
            String pid = (String) iter.next();
            String label = (String) sDefLabels.get(pid);
            sDefOptions[i++] = pid + " - " + label;
        }

        sDefPIDComboBox = new JComboBox(sDefOptions);
        sDefPIDComboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                try {
                    String[] parts =
                            ((String) sDefPIDComboBox.getSelectedItem())
                                    .split(" - ");
                    if (parts.length == 1) {
                        sDefPID = null;
                    } else {
                        sDefPID = parts[0];
                    }
                } catch (Exception e) {
                    Administrator
                            .showErrorDialog(Administrator.getDesktop(),
                                             "Error getting service definition",
                                             e.getMessage(),
                                             e);
                }
                if (sDefPID != null) {
                    MethodsPane mp =
                            (MethodsPane) ((ServiceDeploymentBuilder) parent).tabpane
                                    .getComponentAt(2);
                    mp.renderContractMethods(sDefPID);
                } else {
                    MethodsPane mp =
                            (MethodsPane) ((ServiceDeploymentBuilder) parent).tabpane
                                    .getComponentAt(2);
                    mp.clearContractMethods();
                }
            }
        });
        gbc2.gridx = 1;
        contractPanel.add(sDefPIDComboBox, gbc2);
        return contractPanel;
    }

    private JPanel setDCPanel() {
        // DC Table Panel
        dcTableModel = new DefaultTableModel();
        // Create a JTable that disallow edits (edits done via dialog box only)
        dcTable = new JTable(dcTableModel) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int vColIndex) {
                if (vColIndex == 0) {
                    return false;
                } else {
                    return true;
                }
            }
        };

        dcTable.setColumnSelectionAllowed(false);
        dcTable.setRowSelectionAllowed(true);

        dcTableModel.addColumn("Element Name");
        dcTableModel.addColumn("Value");

        dcTableModel.addRow(new Object[] {"title", ""});
        dcTableModel.addRow(new Object[] {"creator", ""});
        dcTableModel.addRow(new Object[] {"subject", ""});
        dcTableModel.addRow(new Object[] {"publisher", ""});
        dcTableModel.addRow(new Object[] {"description", ""});
        dcTableModel.addRow(new Object[] {"contributor", ""});
        dcTableModel.addRow(new Object[] {"date", ""});
        dcTableModel.addRow(new Object[] {"type", ""});
        dcTableModel.addRow(new Object[] {"format", ""});
        dcTableModel.addRow(new Object[] {"identifier", ""});
        dcTableModel.addRow(new Object[] {"source", ""});
        dcTableModel.addRow(new Object[] {"language", ""});
        dcTableModel.addRow(new Object[] {"relation", ""});
        dcTableModel.addRow(new Object[] {"coverage", ""});
        dcTableModel.addRow(new Object[] {"rights", ""});
        JScrollPane scrollpane = new JScrollPane(dcTable);
        scrollpane.getViewport().setBackground(Color.white);

        // Table Buttons Panel
        JButton jb1 = new JButton("Add");
        jb1.setMinimumSize(new Dimension(100, 30));
        jb1.setMaximumSize(new Dimension(100, 30));
        jb1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addDCElement();
            }
        });
        JButton jb2 = new JButton("Edit");
        jb2.setMinimumSize(new Dimension(100, 30));
        jb2.setMaximumSize(new Dimension(100, 30));
        jb2.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                editDCElement();
            }
        });
        JButton jb3 = new JButton("Delete");
        jb3.setMinimumSize(new Dimension(100, 30));
        jb3.setMaximumSize(new Dimension(100, 30));
        jb3.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                deleteDCElement();
            }
        });
        JPanel t_buttonPanel = new JPanel();
        // t_buttonPanel.setLayout(new GridLayout(3,1));
        t_buttonPanel.setLayout(new BoxLayout(t_buttonPanel, BoxLayout.Y_AXIS));
        t_buttonPanel.add(jb1);
        t_buttonPanel.add(jb2);
        t_buttonPanel.add(jb3);

        JPanel dcPanel = new JPanel(new BorderLayout());
        dcPanel.setBorder(new TitledBorder("Dublin Core Metadata"));
        dcPanel.add(scrollpane, BorderLayout.CENTER);
        dcPanel.add(t_buttonPanel, BorderLayout.EAST);
        return dcPanel;
    }

    public String getSDefContractPID() {
        if (parent.getClass().getName()
                .equalsIgnoreCase(ServiceDeploymentBuilder.class.getName())) {
            return sDefPID;
        }
        return null;
    }

    public String getBObjectPID() {
        return bObjectPID.getText();
    }

    public String getBObjectLabel() {
        return bObjectLabel.getText();
    }

    public String getBObjectName() {
        String s = bObjectName.getText();
        StringTokenizer st = new StringTokenizer(s, " ", false);
        String nameNoSpaces = "";
        while (st.hasMoreElements()) {
            nameNoSpaces += st.nextElement();
        }
        return nameNoSpaces;
    }

    public DCElement[] getDCElements() {
        if (dcTable.isEditing()) {
            dcTable.getCellEditor().stopCellEditing();
        }
        Vector elements = new Vector();
        int rowcount = dcTable.getModel().getRowCount();
        for (int i = 0; i < rowcount; i++) {
            DCElement dcElement = new DCElement();
            dcElement.elementName = (String) dcTable.getValueAt(i, 0);
            dcElement.elementValue = (String) dcTable.getValueAt(i, 1);
            if (dcElement.elementName != null
                    && !dcElement.elementName.trim().equals("")
                    && dcElement.elementValue != null
                    && !dcElement.elementValue.trim().equals("")) {
                elements.add(dcElement);
            }
        }
        return (DCElement[]) elements.toArray(new DCElement[0]);
    }

    public void setDCElement(String dcName, String dcValue) {
        if (editDCMode) {
            int currentRowIndex = dcTable.getSelectedRow();
            dcTable.setValueAt(dcName, currentRowIndex, 0);
            dcTable.setValueAt(dcValue, currentRowIndex, 1);
        } else {
            dcTableModel.addRow(new Object[] {dcName, dcValue});
        }
    }

    private void addDCElement() {
        dcDialog = new DCElementDialog(this, "Add DC Element", true);
    }

    private void editDCElement() {
        editDCMode = true;
        if (dcTable.isEditing()) {
            dcTable.getCellEditor().stopCellEditing();
        }
        int currentRowIndex = dcTable.getSelectedRow();
        dcDialog =
                new DCElementDialog(this,
                                    "Edit DC Element",
                                    true,
                                    (String) dcTable
                                            .getValueAt(currentRowIndex, 0),
                                    (String) dcTable
                                            .getValueAt(currentRowIndex, 1));
        editDCMode = false;
    }

    private void deleteDCElement() {
        dcTableModel.removeRow(dcTable.getSelectedRow());
    }

    protected void assertInvalidDCMsg(String msg) {
        JOptionPane.showMessageDialog(this,
                                      new String(msg),
                                      "Invalid DC Element",
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    // Action Listener for button group
    class PIDActionListener
            implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            rb_chosen = rb_buttonGroup.getSelection().getActionCommand();
            if (rb_chosen.equalsIgnoreCase("retainPID")) {
                bObjectPID.setEnabled(true);
            } else if (rb_chosen.equalsIgnoreCase("sysPID")) {
                bObjectPID.setEnabled(false);
                bObjectPID.setText("");
            }
        }
    }
}