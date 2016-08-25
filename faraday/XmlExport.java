package org.zaproxy.zap.extension.faraday;

import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.zaproxy.zap.view.ZapMenuItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * Created by Jorge Gómez on 19/08/16.
 */
public class XmlExport extends ExtensionAdaptor {
    //Use this variable for run main without building extension and running zap
    //TODO Delete this variable on production environment
    private static final boolean TESTING = true;

    public static String EXTENSION_NAME = "Faraday Xml Exporter";
	public static String PREFIX = "faraday.xmlExport.";
	public static String DEFAULT_FARADAY_REPORT_PATH = System.getProperty("user.home") + "/.faraday/report/";

    private String currentWorkspace;

    public static void main(String[] args) throws Exception {
        System.out.println(DEFAULT_FARADAY_REPORT_PATH);

        System.out.println(Arrays.toString(getDirectories()));
        new XmlExport().showExportForm();
    }

    private ZapMenuItem zapMenuItem;

    private JTextField jTextField;
    private JComboBox<String> jComboBox;
    private JCheckBox jCheckBox;
    private JFileChooser folderChooser;

    private String faradayReportPath;

    final JFileChooser fc = new JFileChooser();

    public XmlExport() {
        super(EXTENSION_NAME);
        this.faradayReportPath = DEFAULT_FARADAY_REPORT_PATH;
    }

    public static String[] getDirectories() {
        File file = new File(DEFAULT_FARADAY_REPORT_PATH);
        return file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
    }

    @Override
    public void hook(ExtensionHook extensionHook) {
        super.hook(extensionHook);
        if (getView() != null) {
            extensionHook.getHookMenu().addToolsMenuItem(getMenu());
        }
    }

    private ZapMenuItem getMenu() {
        System.out.println("XmlExport.getMenu");
        if (zapMenuItem == null) {
            this.zapMenuItem = new ZapMenuItem(PREFIX + "sendReport");
            this.zapMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showExportForm();
                }
            });
        }
        return this.zapMenuItem;
    }

    private void showExportForm() {
        JPanel jPanel = new JPanel(new GridLayout(0,1));

        //JText field initialization
        JPanel folderPanel = new JPanel(new GridLayout(0,2));
        if (jTextField == null) {
            jTextField = new JTextField(DEFAULT_FARADAY_REPORT_PATH);
            jTextField.setEditable(false);
        }
        JButton chooseFolderButton = new JButton(getStringLoc(PREFIX + "selectFaradayOutput"));
        if (folderChooser == null) {
            folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            folderChooser.setCurrentDirectory(new File(DEFAULT_FARADAY_REPORT_PATH));
            folderChooser.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    jTextField.setText(folderChooser.getSelectedFile().getAbsolutePath());
                }
            });
        }
        chooseFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                folderChooser.showSaveDialog(jPanel);
            }
        });
        jPanel.add(new Label(getStringLoc(PREFIX + "selectFaradayOutput")));
        folderPanel.add(jTextField);
        folderPanel.add(chooseFolderButton);
        jPanel.add(folderPanel);

        //JComboBox initialization
        if (jComboBox == null) {
            jComboBox = new JComboBox<String>(getDirectories());
        }
        jComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JComboBox jComboBox = (JComboBox)actionEvent.getSource();
                setCurrentWorkspace((String) jComboBox.getSelectedItem());
            }
        });
        jPanel.add(new Label(getStringLoc(PREFIX + "selectWorkspace")));
        jPanel.add(jComboBox);

        //Default checkbox initialization
        if (jCheckBox == null) {
            jCheckBox = new JCheckBox(getStringLoc(PREFIX + "useAsDefault"));
        }
        jPanel.add(jCheckBox);

        //View.getSingleton().getOutputPanel()
        //View.getSingleton().showMessageDialog(jPanel, getStringLoc(PREFIX + "sendReport"));
        //View.getSingleton().showConfirmDialog(jPanel, getStringLoc(PREFIX + "sendReport"));
        JOptionPane.showConfirmDialog(null, jPanel, getStringLoc(PREFIX + "sendReport"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        //jPanel.setVisible(true);
    }

    private String getStringLoc(String str) {
        if (TESTING) {
            return str;
        } else {
            return Constant.messages.getString(str);
        }
    }

    @Override
    public String getAuthor() {
        return Constant.ZAP_TEAM;
    }

    @Override
    public String getDescription() {
        return getStringLoc(PREFIX + "desc");
    }

    @Override
    public URL getURL() {
        try {
            return new URL(Constant.ZAP_EXTENSIONS_PAGE);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public void setCurrentWorkspace(String currentWorkspace) {
        this.currentWorkspace = currentWorkspace;
    }

}