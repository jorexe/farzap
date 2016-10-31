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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.parosproxy.paros.Constant;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.extension.report.ReportGenerator;
import org.parosproxy.paros.extension.report.ReportLastScan;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.view.View;
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
		try {
			Set<Alert> alerts = new TreeSet<Alert>();
			alerts.add(alert);
			this.extension.generate(new StringBuilder(500), extension.getModel(), alerts);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void performActions(Set<Alert> alerts) {
        //TODO: fix try-catch
        try {
			this.extension.generate(new StringBuilder(500), extension.getModel(), alerts);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
