package org.zaproxy.zap.extension.faraday;

import org.apache.commons.lang.StringEscapeUtils;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.extension.Extension;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.extension.ExtensionLoader;
import org.parosproxy.paros.extension.report.ReportLastScan;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.model.SiteMap;
import org.parosproxy.paros.model.SiteNode;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.XmlReporterExtension;
import org.zaproxy.zap.extension.alert.AlertParam;
import org.zaproxy.zap.utils.XMLStringUtil;
import org.zaproxy.zap.view.ScanPanel;
import org.zaproxy.zap.view.ZapMenuItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Created by Jorge Gómez on 19/08/16.
 */
public class XmlExport extends ExtensionAdaptor {
    //Use this variable for run main without building extension and running zap
    //TODO Delete this variable on production environment
    private static final boolean TESTING = false;
    
    //parse report
	public static final String[] MSG_RISK = {"Informational", "Low", "Medium", "High"};
    public static final String[] MSG_CONFIDENCE = {"False Positive", "Low", "Medium", "High", "Confirmed"};
	private static final SimpleDateFormat staticDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

    public static String EXTENSION_NAME = "Faraday Xml Exporter";
	public static String PREFIX = "faraday.xmlExport.";
	public static String DEFAULT_FARADAY_REPORT_PATH = System.getProperty("user.home") + "/.faraday/report";

    //State variables
    private String currentWorkspace;
    private String faradayReportPath;
    //TODO Check if there is a way to restore variable to false (zap settings maybe?)
    private boolean usingDefaultParameters;

    private AlertParam alertParam = null;
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
                        saveReport(faradayReportPath + "/" + currentWorkspace);
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
            saveReport(faradayReportPath + "/" + currentWorkspace);
        }
    }

    private void saveReport(String folderPath) {
        ReportLastScan report = new ReportLastScan();
        DateFormat df = new SimpleDateFormat("YYYY-MM-DD-hh-mm-ss");
        String reportFullPath = folderPath + "/" + df.format(new Date()) + ".xml";
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
    
    public static String getCurrentDateTimeString() {
		Date dateTime = new Date(System.currentTimeMillis());
		return getDateTimeString(dateTime);

	}
    
    public static String getDateTimeString(Date dateTime) {
		// ZAP: fix unsafe call to DateFormats
		synchronized (staticDateFormat) {
			return staticDateFormat.format(dateTime);
		}
	}
    protected void generate(StringBuilder report, Model model, Set<Alert> alerts) throws Exception {
        report.append("<?xml version=\"1.0\"?>");
        report.append("<OWASPZAPReport version=\"").append(Constant.PROGRAM_VERSION).append("\" generated=\"").append(getCurrentDateTimeString()).append("\">\r\n");
        siteXML(report, alerts);
        report.append("</OWASPZAPReport>");
        saveZapReport(report);
    }
   
    
    private String appendHeader(StringBuilder report, String uri){
    	String siteName = "";
		String name = "";
		boolean isSSL = true;
		String[] hostAndPort;
		URL url = null;
		try {
			url = new URL(uri);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			return "";
		}
        String host = url.getHost();
		siteName = url.toString();
		name = siteName.substring(0, siteName.indexOf("/",siteName.indexOf(host)));
		
		isSSL = name.startsWith("https");
		
		siteName = siteName.substring(siteName.indexOf("//")+2);
		siteName = siteName.substring(siteName.indexOf(host), siteName.indexOf("/"));
		hostAndPort = siteName.split(":");
		if(hostAndPort.length <= 1){
			hostAndPort = new String[2];
			//hostAndPort[0] = host;
			if(isSSL){
				hostAndPort[1] = "443";
			}else{
				hostAndPort[1] = "80";
			}
		}
		hostAndPort[0] = host;

		String siteStart = "<site name=\"" + XMLStringUtil.escapeControlChrs(name) + "\"" +
                " host=\"" + XMLStringUtil.escapeControlChrs(hostAndPort[0])+ "\""+
                " port=\"" + XMLStringUtil.escapeControlChrs(hostAndPort[1])+ "\""+
                " ssl=\"" + String.valueOf(isSSL) + "\"" +
                ">";
        report.append(siteStart);
        return host;
    }
    
    private void siteXML(StringBuilder report, Set<Alert> alerts) {
    	String host = appendHeader(report, alerts.iterator().next().getUri());
            
		SiteMap siteMap = Model.getSingleton().getSession().getSiteTree();
        SiteNode root = (SiteNode) siteMap.getRoot();
        int siteNumber = root.getChildCount();
        for (int i = 0; i < siteNumber; i++) {
            SiteNode site = (SiteNode) root.getChildAt(i);
            String sitename = ScanPanel.cleanSiteName(site, true);
            String[] hostPort = sitename.split(":");
            if(hostPort[0].equals(host)){
            	StringBuilder extensionsXML = getExtensionsXML(site, alerts);
            	report.append(extensionsXML);
            }
        }
    }
	
    public StringBuilder getExtensionsXML(SiteNode site, Set<Alert> alerts) {
        StringBuilder extensionXml = new StringBuilder();
        ExtensionLoader loader = Control.getSingleton().getExtensionLoader();
        int extensionCount = loader.getExtensionCount();
        for(int i=0; i<extensionCount; i++) {
            Extension extension = loader.getExtension(i);
            if(extension instanceof XmlReporterExtension) {
                extensionXml.append(getMineXml(site, alerts));
            }
        }
        return extensionXml;
    }
    
    private AlertParam getAlertParam() {
		if (alertParam == null) {
			alertParam = new AlertParam();
		}
		return alertParam;
	}
    
    private String alertFingerprint(Alert alert) {
    	return alert.getPluginId() + "/" + alert.getName() + "/" + alert.getRisk() + "/" + alert.getConfidence();
    }
    
    public String getMineXml(SiteNode site, Set<Alert> alertsSelected) {
        StringBuilder xml = new StringBuilder();
        xml.append("<alerts>");
        List<Alert> alerts = site.getAlerts();
        SortedSet<String> handledAlerts = new TreeSet<String>(); 

        for (Alert alert : alertsSelected) {
            if (alert.getConfidence() != Alert.CONFIDENCE_FALSE_POSITIVE) {
            	if (this.getAlertParam().isMergeRelatedIssues()) {
            		String fingerprint = alertFingerprint(alert);
	            	if (handledAlerts.add(fingerprint)) {
	            		// Its a new one
	            		// Build up the full set of details
	            		StringBuilder sb = new StringBuilder();
	            		sb.append("  <instances>\n");
	            		int count = 0;
	            		for (int j=0; j < alerts.size(); j++) {
	            			// Deliberately include i!
	            			Alert alert2 = alerts.get(j);
	            			if (fingerprint.equals(alertFingerprint(alert2))) {
	            				if (this.getAlertParam().getMaximumInstances() == 0 ||
	            						count < this.getAlertParam().getMaximumInstances()) {
		            				sb.append("  <instance>\n");
		            				sb.append(alert2.getUrlParamXML());
		            				sb.append("  </instance>\n");
	            				}
	            				count ++;
	            			}
	            		}
	            		sb.append("  </instances>\n");
	            		sb.append("  <count>");
	            		sb.append(count);
	            		sb.append("</count>\n");
	            		xml.append(alert.toPluginXML(sb.toString()));
	            	}
            	} else {
                    String urlParamXML = alert.getUrlParamXML();
                    xml.append(alert.toPluginXML(urlParamXML));
            	}
            }
        }
        xml.append("</alerts>");
        xml.append("</site>");
        return xml.toString();
    }
    
	public static String entityEncode(String text) {
		String result = text;

		if (result == null) {
			return result;
		}

		// The escapeXml function doesnt cope with some 'special' chrs

		return StringEscapeUtils.escapeXml(XMLStringUtil.escapeControlChrs(result));
	}
    
    private void saveZapReport(StringBuilder sb){
    	if(!usingDefaultParameters){
	    	if (mainPanel == null) {
	            mainPanel = new JPanel(new GridLayout(0, 1));
	
	            mainPanel.add(new Label(getStringLoc("selectFaradayOutput")));
	            mainPanel.add(getFolderPanel());
	
	            mainPanel.add(new Label(getStringLoc("selectWorkspace")));
	            mainPanel.add(getWorkspaceComboBox());
	
	            mainPanel.add(getUseDefaultCheckBox());
	        }
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
	        }
    	}
    	BufferedWriter bw = null;
		try {
			DateFormat df = new SimpleDateFormat("YYYY-MM-DD-hh-mm-ss");
	        String reportFullPath = faradayReportPath + "/" + df.format(new Date()) + ".xml";
			System.out.println("path:"+reportFullPath);
	        bw = new BufferedWriter(new FileWriter(reportFullPath));
			bw.write(sb.toString());
		} catch (IOException e2) {
			//could not generate report
		} finally {
			try {
				if (bw != null) {
					bw.close();
				}
			} catch (IOException ex) {
			}
		}
    }
}