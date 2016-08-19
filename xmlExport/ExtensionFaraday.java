package org.zaproxy.zap.extension.faraday.xmlExport;

import com.sun.media.jfxmedia.logging.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.view.ZapMenuItem;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by Jorge Gómez on 19/08/16.
 */
public class ExtensionFaraday extends ExtensionAdaptor {

    private ZapMenuItem zapMenuItem;
    private ResourceBundle messages;

    public ExtensionFaraday() {
        super();
        initialize();
    }

    public ExtensionFaraday(String name) {
        super(name);
        initialize();
    }

    private void initialize() {
        System.out.println("ExtensionFaraday.initialize");
        Logger.logMsg(Logger.INFO, "ExtensionFaraday.initialize");
        this.setName("FaradayExtension");
        //this.messages = ResourceBundle.getBundle(this.getClass().getPackage().getName() + ".Messages", Constant.getLocale());
    }

    @Override
    public void hook(ExtensionHook extensionHook) {
        super.hook(extensionHook);
        System.out.println("ExtensionFaraday.hook");
        if (getView() != null) {
            extensionHook.getHookMenu().addToolsMenuItem(getMenu());
        }
    }

    private ZapMenuItem getMenu() {
        if (zapMenuItem == null) {
            zapMenuItem = new ZapMenuItem(getMessageString("ext.topmenu.faraday.download"));
            zapMenuItem.addActionListener((e) -> {
                View.getSingleton().showMessageDialog("HOLA MUNDO!");
            });
        }
        return zapMenuItem;
    }

    private String getMessageString(String key) {
        //return messages.getString(key);
        return "FARADAY!!";
    }

    @Override
    public String getAuthor() {
        return "FARADAY";
    }

    @Override
    public String getDescription() {
        return "FARADAY!";
    }

    @Override
    public boolean isCore() {
        return true;
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
