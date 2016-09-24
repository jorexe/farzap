/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.zaproxy.zap.extension.faraday;

import java.awt.Component;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.parosproxy.paros.Constant;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.extension.report.ReportGenerator;
import org.parosproxy.paros.model.Model;
import org.zaproxy.zap.utils.XMLStringUtil;


public class RightClickMsgMenu extends PopupMenuItemAlert {

	private static final long serialVersionUID = 1L;
	private XmlExport extension;

    public RightClickMsgMenu(XmlExport ext, String label) {
        super(label, true);
        /*
         * This is how you can pass in your extension, which you may well need to use
         * when you actually do anything of use.
         */
        this.extension = ext;
    }
	
	@Override
	protected void performAction(Alert alert) {
		// TODO Auto-generated method stub
		System.out.println("on perform single alert");
		try {
			Map<String, List<Alert>> alertMap = new HashMap<String, List<Alert>>();
			List<Alert> alerts = new ArrayList();
			alerts.add(alert);
			//TODO: check
        	alertMap.put(alert.getUrlParamXML(), alerts);
			generate(new StringBuilder(500), extension.getModel(), alertMap);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void performActions(Set<Alert> alerts) {
        System.out.println("perform set");
        //TODO: fix try-catch
        try {
			generate(new StringBuilder(500), extension.getModel(), getAlertsByHost(alerts));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	private Map<String, List<Alert>> getAlertsByHost(Set<Alert> alerts) {
	    Map<String, List<Alert>> alertsByHost = new HashMap<String, List<Alert>>();
	    for (Alert alert : alerts) {
	        URL url = null;
	        try {
	            url = new URL(alert.getUri());
	            String host = url.getHost();
	            if (alertsByHost.get(host) == null) {
	                alertsByHost.put(host, new ArrayList<Alert>());
	            }
	            alertsByHost.get(host).add(alert);
	        } catch (MalformedURLException e) {
	            System.err.println("Skipping malformed URL: "+alert.getUri());
	            e.printStackTrace();
	        }
	    }
	    return alertsByHost;
	}
	
	public void generate(StringBuilder report, Model model, Map<String, List<Alert>> alertMap) throws Exception {
        report.append("<?xml version=\"1.0\"?>");
        report.append("<OWASPZAPReport version=\"").append(Constant.PROGRAM_VERSION).append("\" generated=\"").append(ReportGenerator.getCurrentDateTimeString()).append("\">\r\n");
        siteXML(report, alertMap);
        report.append("</OWASPZAPReport>");
        System.out.println(report.toString());
    }
	
	//TODO: clear comments --> implement methods
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
			isSSL = siteName.startsWith("https"); //getSiteNodeName().startWith...
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
            report.append(extensionsXML);
            report.append(siteEnd);
            
		}
    }
	
	//TODO: generate alert xml part
	private StringBuilder getExtensionsXML(List<Alert> alerts){
		return new StringBuilder();
	}
	
	@Override
	public boolean isSafe() {
		return true;
	}
	
	@Override
	public boolean isEnableForComponent(Component invoker) {
		if (super.isEnableForComponent(invoker)){
			setEnabled(true);
			return true;
		}
		return false;
	}
}
