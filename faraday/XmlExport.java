package org.zaproxy.zap.extension.faraday;

import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.view.ZapMenuItem;

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

    public static String EXTENSION_NAME = "Faraday Xml Exporter";
	public static String PREFIX = "faraday.xmlExport.";
	public static String DEFAULT_FARADAY_REPORT_PATH = System.getProperty("user.home") + "/.faraday/report/";

    public static void main(String[] args) throws Exception {
        System.out.println(DEFAULT_FARADAY_REPORT_PATH);
        File file = new File(DEFAULT_FARADAY_REPORT_PATH);
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
        System.out.println(Arrays.toString(directories));
    }

    private ZapMenuItem zapMenuItem;

    private String faradayReportPath; 

    public XmlExport() {
        super(EXTENSION_NAME);
        this.faradayReportPath = DEFAULT_FARADAY_REPORT_PATH;
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
                	View.getSingleton().showMessageDialog(Constant.messages.getString(PREFIX + "testString"));
                    //View.getSingleton().showMessageDialog("HOLA MUNDO!");
                }
            });
        }
        return this.zapMenuItem;
    }

    @Override
    public String getAuthor() {
        return Constant.ZAP_TEAM;
    }

    @Override
    public String getDescription() {
        return Constant.messages.getString(PREFIX + "desc");
    }

    @Override
    public URL getURL() {
        try {
            return new URL(Constant.ZAP_EXTENSIONS_PAGE);
        } catch (MalformedURLException e) {
            return null;
        }
    }

}