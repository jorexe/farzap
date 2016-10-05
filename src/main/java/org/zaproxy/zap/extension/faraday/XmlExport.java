package org.zaproxy.zap.extension.faraday;

import org.apache.commons.lang.StringEscapeUtils;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.extension.report.ReportLastScan;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.faraday.RightClickMsgMenu;
import org.zaproxy.zap.utils.XMLStringUtil;
import org.zaproxy.zap.view.PopupMenuHistoryReference.Invoker;
import org.zaproxy.zap.view.ZapMenuItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
    protected void generate(StringBuilder report, Model model, Map<String, List<Alert>> alertMap) throws Exception {
        report.append("<?xml version=\"1.0\"?>");
        report.append("<OWASPZAPReport version=\"").append(Constant.PROGRAM_VERSION).append("\" generated=\"").append(getCurrentDateTimeString()).append("\">\r\n");
        siteXML(report, alertMap);
        report.append("</OWASPZAPReport>");
        //System.out.println(report.toString());
        saveZapReport(report);
    }
	
	private void siteXML(StringBuilder report, Map<String, List<Alert>> alertMap) {
		String siteName = "";
		String name = "";
		boolean isSSL = true;
		String[] hostAndPort;
		for (String host : alertMap.keySet()) {
			System.out.println("host: " + host);
			siteName = alertMap.get(host).get(0).getUri(); //getCleanSiteName(host);
			name = siteName;
			System.out.println("sitename: " + siteName);
			siteName = siteName.substring(siteName.indexOf("//")+2);
			siteName = siteName.substring(0, siteName.indexOf("/"));
			System.out.println("sitename: " + siteName);
			isSSL = name.startsWith("https"); //getSiteNodeName().startWith...
			hostAndPort = siteName.split(":");
			if(hostAndPort.length <= 1){
				hostAndPort = new String[2];
				hostAndPort[0] = siteName;
				if(isSSL){
					hostAndPort[1] = "443";
				}else{
					hostAndPort[1] = "80";
				}
			}
			System.out.println("host and port: " + hostAndPort[0] + "," + hostAndPort[1]);
			name = name.substring(0, name.indexOf("/", name.indexOf(hostAndPort[0])));
			System.out.println("name: " + name);
			String siteStart = "<site name=\"" + XMLStringUtil.escapeControlChrs(name) + "\"" +
                    " host=\"" + XMLStringUtil.escapeControlChrs(hostAndPort[0])+ "\""+
                    " port=\"" + XMLStringUtil.escapeControlChrs(hostAndPort[1])+ "\""+
                    " ssl=\"" + String.valueOf(isSSL) + "\"" +
                    ">";
			System.out.println("siteStart: " + siteStart);
            StringBuilder extensionsXML = getExtensionsXML(alertMap.get(host));
            String siteEnd = "</site>";
            report.append(siteStart);
            
            report.append("<alerts>");
            int count = 0;
            for (Alert alert : alertMap.get(host)) {
            	if (count == 0) {
            		report.append("<alertitem>\r\n");
            		report.append("<pluginid>").append(alert.getPluginId()).append("</pluginid>\r\n");
            		report.append("<alert>").append(replaceEntity(alert.getAlert())).append("</alert>\r\n");
            		report.append("<name>").append(replaceEntity(alert.getAlert())).append("</name>\r\n");
            		report.append("<riskcode>").append(alert.getRisk()).append("</riskcode>\r\n");
            		report.append("<confidence>").append(alert.getConfidence()).append("</confidence>\r\n");
            		report.append("<riskdesc>").append(replaceEntity(MSG_RISK[alert.getRisk()] + " (" + MSG_CONFIDENCE[alert.getConfidence()] + ")")).append("</riskdesc>\r\n");
            		if (alert.getDescription() != null) {
            			report.append("<desc>").append(replaceEntity(paragraph(alert.getDescription()))).append("</desc>\r\n");
            		}
            		report.append("<instances>\r\n");
				}
            	
        		report.append("<instance>\r\n");
        		report.append("  <uri>").append(replaceEntity(alert.getUri())).append("</uri>\r\n");
        		if (alert.getParam().length() > 0) {
        			report.append("<param>").append(replaceEntity(alert.getParam())).append("</param>\r\n");
        		}
        		if (alert.getAttack()!= null && alert.getAttack().length() > 0) {
        			report.append("<attack>").append(replaceEntity(alert.getAttack())).append("</attack>\r\n");
        		}
        		if (alert.getEvidence() != null && alert.getEvidence().length() > 0) {
        			report.append("<evidence>").append(replaceEntity(alert.getEvidence())).append("</evidence>\r\n");
        		}
        		report.append("</instance>\r\n");
        		
        		if(count == alertMap.get(host).size()-1){
        			report.append("</instances>\r\n");
        			report.append("<count>").append(count+1).append("</count>\r\n");
        			if (alert.getSolution() != null) {
            			report.append("<solution>").append(replaceEntity(paragraph(alert.getSolution()))).append("</solution>\r\n");
            		}
            		if (alert.getOtherInfo() != null && alert.getOtherInfo().length() > 0 /*&& otherInfo*/) {
                        report.append("<otherinfo>").append(replaceEntity(paragraph(alert.getOtherInfo()))).append("</otherinfo>\r\n");
                    } 
            		if (alert.getReference() != null) {               
            			report.append("<reference>" ).append(replaceEntity(paragraph(alert.getReference()))).append("</reference>\r\n");
            		}
            		if (alert.getCweId() > 0 /*&& cweid*/) {
            			report.append("<cweid>" ).append(alert.getCweId()).append("</cweid>\r\n");
            		}
            		if (alert.getWascId() > 0 /*&& wascid*/) {
            			report.append("<wascid>" ).append(alert.getWascId()).append("</wascid>\r\n");
            		}

            		//no estoy segura si esto debe aparecer
//            		if (alert.getMessage() != null && alert.getMessage().getRequestHeader() != null && !(alert.getMessage().getRequestHeader().toString().equals(""))) {
//            			report.append("<requestheader>").append(paragraph(replaceEntity(alert.getMessage().getRequestHeader().toString()))).append("</requestheader>\r\n");
//            		}
//            		if (alert.getMessage() != null && alert.getMessage().getResponseHeader() != null && !(alert.getMessage().getResponseHeader().toString().equals(""))) {
//        			report.append("<responseheader>").append(paragraph(replaceEntity(alert.getMessage().getResponseHeader().toString()))).append("</responseheader>\r\n");
//            		}
//            		if (alert.getMessage().getRequestBody().length() > 0 /*&& requestBody*/) {
//            			report.append("<requestbody>").append(replaceEntity(alert.getMessage().getRequestBody().toString())).append("</requestbody>\r\n");
//                	}
//            		if (alert.getMessage().getResponseBody().length() > 0 /*&& responseBody*/) {
//            			report.append("<responsebody>").append(replaceEntity(alert.getMessage().getResponseBody().toString())).append("</responsebody>\r\n");
//                	}
            		report.append("</alertitem>\r\n");
        		}
        		count++;
			}
            report.append("</alerts>");
            report.append(siteEnd);
		}
    }
	
	private String replaceEntity(String text) {
		String result = null;
		if (text != null) {
			result = entityEncode(text);
		}
		return result;
	}
	
	public static String entityEncode(String text) {
		String result = text;

		if (result == null) {
			return result;
		}

		// The escapeXml function doesnt cope with some 'special' chrs

		return StringEscapeUtils.escapeXml(XMLStringUtil.escapeControlChrs(result));
	}
	private String paragraph(String text) {
		return "<p>" + text.replaceAll("\\r\\n","</p><p>").replaceAll("\\n","</p><p>") + "</p>";
	}
	
	//TODO: generate alert xml part
	private StringBuilder getExtensionsXML(List<Alert> alerts){
		return new StringBuilder();
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
	        String reportFullPath = faradayReportPath + "/" + currentWorkspace + "/" + UNPROCESSED_FARADAY_REPORT_FOLDER + "/" + df.format(new Date()) + ".xml";
			bw = new BufferedWriter(new FileWriter(reportFullPath));
			bw.write(sb.toString());
		} catch (IOException e2) {
			//logger.error(e2.getMessage(), e2);
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