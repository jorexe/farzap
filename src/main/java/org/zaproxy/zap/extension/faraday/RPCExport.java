package org.zaproxy.zap.extension.faraday;

import org.parosproxy.paros.Constant;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.db.DatabaseException;
import org.parosproxy.paros.db.RecordAlert;
import org.parosproxy.paros.db.TableAlert;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.model.Model;
import org.zaproxy.zap.ZAP;
import org.zaproxy.zap.eventBus.Event;
import org.zaproxy.zap.eventBus.EventConsumer;
import org.zaproxy.zap.extension.alert.AlertEventPublisher;

/**
 * Created by jorexe on 25/10/16.
 */
public class RPCExport extends ExtensionAdaptor implements EventConsumer{

    private static final boolean TESTING = false;

    public static String EXTENSION_NAME = "Faraday Xml Exporter";
    public static String PREFIX = "faraday.xmlExport.";

    private int lastAlert = -1;

    public RPCExport() {
        super(EXTENSION_NAME);
    }

    @Override
    public void init() {
        super.init();
        ZAP.getEventBus().registerConsumer(this,
                AlertEventPublisher.getPublisher().getPublisherName(),
                new String[] {AlertEventPublisher.ALERT_ADDED_EVENT});
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
    public void eventReceived(Event event) {
        System.out.println("RPCExport.eventReceived");
        TableAlert tableAlert = Model.getSingleton().getDb().getTableAlert();

        String alertId = event.getParameters().get(AlertEventPublisher.ALERT_ID);
        if (alertId != null) {
            // From 2.4.3 an alertId is included with these events, which makes life much simpler!
            try {
                handleAlert(new Alert(tableAlert.read(Integer.parseInt(alertId))));
            } catch (Exception e) {
                //log.error("Error handling alert", e);
            }
        } else {
            // Required for pre 2.4.3 versions
            RecordAlert recordAlert;
            while (true) {
                try {
                    this.lastAlert ++;
                    recordAlert = tableAlert.read(this.lastAlert);
                    if (recordAlert == null) {
                        break;
                    }
                    handleAlert(new Alert(recordAlert));

                } catch (DatabaseException e) {
                    break;
                }
            }
            // The loop will always go 1 further than necessary
            this.lastAlert--;
        }
    }

    private void handleAlert(Alert alert) {

    }
}
