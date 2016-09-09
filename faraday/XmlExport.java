package org.zaproxy.zap.extension.faraday;

import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.extension.report.ReportLastScan;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.faraday.RightClickMsgMenu;
import org.zaproxy.zap.view.PopupMenuHistoryReference.Invoker;
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
    private JPanel mainPanel;
    private JTextField reportFolderTextField;
    private JComboBox<String> workspaceComboBox;
    private JCheckBox useDefaultCheckBox;
    private JFileChooser folderChooser;
    private JButton chooseFolderButton;
    final JFileChooser fc = new JFileChooser();

    //shortcut on some components to add to report
    private RightClickMsgMenu popupMsgMenuExample;
    
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
            extensionHook.getHookMenu().addPopupMenuItem(getPopupMsgMenuExample());
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

    private JPanel getFolderPanel() {
        JPanel folderPanel = new JPanel(new GridLayout(0,2));
        if (reportFolderTextField == null) {
            reportFolderTextField = new JTextField(faradayReportPath);
            reportFolderTextField.setEditable(false);
        }
        if (chooseFolderButton == null) {
            chooseFolderButton = new JButton(getStringLoc("selectFaradayOutput"));
        }
        if (folderChooser == null) {
            folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            folderChooser.setCurrentDirectory(new File(faradayReportPath));
            folderChooser.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    String selectedFolder = folderChooser.getSelectedFile().getAbsolutePath();
                    reportFolderTextField.setText(selectedFolder);
                    workspaceComboBox.removeAllItems();
                    for (String s : getDirectories(selectedFolder)) {
                        workspaceComboBox.addItem(s);
                    }
                }
            });
        }
        chooseFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                folderChooser.showSaveDialog(mainPanel);
            }
        });
        folderPanel.add(reportFolderTextField);
        folderPanel.add(chooseFolderButton);
        return folderPanel;
    }

    private JComboBox getWorkspaceComboBox() {
        if (workspaceComboBox == null) {
            workspaceComboBox = new JComboBox<String>(getDirectories(faradayReportPath));
        }
        return workspaceComboBox;
    }

    private JCheckBox getUseDefaultCheckBox() {
        if (useDefaultCheckBox == null) {
            useDefaultCheckBox = new JCheckBox(getStringLoc("useAsDefault"));
        }
        return useDefaultCheckBox;
    }

    private void showExportForm() {
        //Main panel initialization
        if (mainPanel == null) {
            mainPanel = new JPanel(new GridLayout(0, 1));

            mainPanel.add(new Label(getStringLoc("selectFaradayOutput")));
            mainPanel.add(getFolderPanel());

            mainPanel.add(new Label(getStringLoc("selectWorkspace")));
            mainPanel.add(getWorkspaceComboBox());

            mainPanel.add(getUseDefaultCheckBox());
        }

        //TODO Check if there is another way to show confirm dialog using next line
        //View.getSingleton().showConfirmDialog(mainPanel, getStringLoc("sendReport"));
        int result = JOptionPane.showConfirmDialog(null, mainPanel, getStringLoc("sendReport"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            currentWorkspace = (String) workspaceComboBox.getSelectedItem();
            if (currentWorkspace == null || currentWorkspace.isEmpty()) {
                View.getSingleton().showWarningDialog(getStringLoc("invalidWorkspace"));
            }
            faradayReportPath = reportFolderTextField.getText();
            if (useDefaultCheckBox.isSelected()) {
                usingDefaultParameters = true;
            }
            saveReport(faradayReportPath + "/" + currentWorkspace + "/" + UNPROCESSED_FARADAY_REPORT_FOLDER);
        }
    }

    private void saveReport(String folderPath) {
        ReportLastScan report = new ReportLastScan();
        DateFormat df = new SimpleDateFormat("YYYY-MM-DD-hh-mm-ss");
        String reportFullPath = folderPath + "/" + df.format(new Date()) + ".xml";
        System.out.println("Saving report to:" + reportFullPath);
        try {
            report.generate(reportFullPath, getModel(), null);
            View.getSingleton().showMessageDialog(getStringLoc("exportSucceed"));
        } catch (Exception e) {
            e.printStackTrace();
            View.getSingleton().showWarningDialog(getStringLoc("error"));
        }
    }

    private RightClickMsgMenu getPopupMsgMenuExample() {
		if (popupMsgMenuExample  == null) {
			popupMsgMenuExample = new RightClickMsgMenu(this, 
					getStringLoc("sendReport"));
		}
		return popupMsgMenuExample;
	}
    
    protected void showAndSave() {
		if (usingDefaultParameters) {
            saveReport(faradayReportPath + "/" + currentWorkspace + "/" + UNPROCESSED_FARADAY_REPORT_FOLDER);
        } else {
            showExportForm();
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