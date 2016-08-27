package org.zaproxy.zap.extension.faraday;

import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.extension.report.ReportLastScan;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.view.ZapMenuItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Jorge Gómez on 19/08/16.
 */
public class XmlExport extends ExtensionAdaptor {
    //Use this variable for run main without building extension and running zap
    //TODO Delete this variable on production environment
    private static final boolean TESTING = false;

    public static String EXTENSION_NAME = "Faraday Xml Exporter";
	public static String PREFIX = "faraday.xmlExport.";
	public static String DEFAULT_FARADAY_REPORT_PATH = System.getProperty("user.home") + "/.faraday/report";
    public static String UNPROCESSED_FARADAY_REPORT_FOLDER = "unprocessed";

    //State variables
    private String currentWorkspace;
    private String faradayReportPath;
    //TODO Check if there is a way to restore variable to false (zap settings maybe?)
    private boolean usingDefaultParameters;

    //Menu variable
    private ZapMenuItem zapMenuItem;

    //Panel variables
    private JTextField jTextField;
    private JComboBox<String> jComboBox;
    private JCheckBox jCheckBox;
    private JFileChooser folderChooser;
    private JPanel jPanel;
    final JFileChooser fc = new JFileChooser();

    //TODO Delete main on production environment
    public static void main(String[] args) throws Exception {
        new XmlExport().showExportForm();
    }

    public XmlExport() {
        super(EXTENSION_NAME);
        this.faradayReportPath = DEFAULT_FARADAY_REPORT_PATH;
        currentWorkspace = "";
        usingDefaultParameters = false;
    }

    @Override
    public void hook(ExtensionHook extensionHook) {
        super.hook(extensionHook);
        if (getView() != null) {
            extensionHook.getHookMenu().addToolsMenuItem(getMenu());
        }
    }

    private ZapMenuItem getMenu() {
        if (zapMenuItem == null) {
            this.zapMenuItem = new ZapMenuItem(PREFIX + "sendReport");
            this.zapMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (usingDefaultParameters) {
                        saveReport(faradayReportPath + "/" + currentWorkspace + "/" + UNPROCESSED_FARADAY_REPORT_FOLDER);
                    } else {
                        showExportForm();
                    }
                }
            });
        }
        return this.zapMenuItem;
    }

    public String[] getDirectories(String path) {
        File file = new File(path);
        return file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
    }

    private void showExportForm() {
        if (jPanel == null) {
            jPanel = new JPanel(new GridLayout(0, 1));
        }
        //JText field initialization
        JPanel folderPanel = new JPanel(new GridLayout(0,2));
        if (jTextField == null) {
            jTextField = new JTextField(faradayReportPath);
            jTextField.setEditable(false);
        }
        JButton chooseFolderButton = new JButton(getStringLoc("selectFaradayOutput"));
        if (folderChooser == null) {
            folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            folderChooser.setCurrentDirectory(new File(faradayReportPath));
            folderChooser.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    String selectedFolder = folderChooser.getSelectedFile().getAbsolutePath();
                    jTextField.setText(selectedFolder);
                    jComboBox.removeAllItems();
                    for (String s : getDirectories(selectedFolder)) {
                        jComboBox.addItem(s);
                    }
                }
            });
        }
        chooseFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                folderChooser.showSaveDialog(jPanel);
            }
        });
        jPanel.add(new Label(getStringLoc("selectFaradayOutput")));
        folderPanel.add(jTextField);
        folderPanel.add(chooseFolderButton);
        jPanel.add(folderPanel);

        //JComboBox initialization
        if (jComboBox == null) {
            jComboBox = new JComboBox<String>(getDirectories(faradayReportPath));
        }
        jPanel.add(new Label(getStringLoc("selectWorkspace")));
        jPanel.add(jComboBox);

        //Default checkbox initialization
        if (jCheckBox == null) {
            jCheckBox = new JCheckBox(getStringLoc("useAsDefault"));
        }
        jPanel.add(jCheckBox);

        //TODO Check if there is another way to show confirm dialog using next line
        //View.getSingleton().showConfirmDialog(jPanel, getStringLoc("sendReport"));
        int result = JOptionPane.showConfirmDialog(null, jPanel, getStringLoc("sendReport"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            currentWorkspace = (String) jComboBox.getSelectedItem();
            if (currentWorkspace == null || currentWorkspace.isEmpty()) {
                View.getSingleton().showWarningDialog(getStringLoc("invalidWorkspace"));
            }
            faradayReportPath = jTextField.getText();
            if (jCheckBox.isSelected()) {
                usingDefaultParameters = true;
            }
            saveReport(faradayReportPath + "/" + currentWorkspace + "/" + UNPROCESSED_FARADAY_REPORT_FOLDER);
        }
    }

    private void saveReport(String folderPath) {
        //TODO Search on ZAP documentation how to export report
        //TODO Show success/failure dialog
        ReportLastScan report = new ReportLastScan();
        DateFormat df = new SimpleDateFormat("YYYY-MM-DD-hh-mm-ss");
        String reportFullPath = folderPath + "/" + df.format(new Date()) + ".xml";
        System.out.println("Saving report to:" + reportFullPath);
        try {
            report.generate(reportFullPath, getModel(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getStringLoc(String str) {
        if (TESTING || Constant.messages == null) {
            return str;
        } else {
            return Constant.messages.getString(PREFIX + str);
        }
    }

    @Override
    public String getAuthor() {
        return getStringLoc("author");
    }

    @Override
    public String getDescription() {
        return getStringLoc("description");
    }

    @Override
    public URL getURL() {
        try {
            return new URL(getStringLoc("url"));
        } catch (MalformedURLException e) {
            return null;
        }
    }
}