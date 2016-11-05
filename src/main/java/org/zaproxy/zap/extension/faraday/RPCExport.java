package org.zaproxy.zap.extension.faraday;

import org.apache.commons.httpclient.URI;
import org.apache.log4j.Logger;
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
import org.zaproxy.zap.extension.faraday.rpc.FaradayClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * A ZAP extension wich connects with Faraday RPC api and uploads alerts in real time.
 *
 * References:
 *   https://www.faradaysec.com/
 *   https://github.com/infobyte/faraday
 *
 * Implementation References:
 *   https://github.com/zaproxy/zaproxy/blob/b5f6c6397f42bc220372580b9ef49c97d798a9b7/src/org/zaproxy/zap/eventBus/EventConsumer.java
 *   https://github.com/zaproxy/zap-extensions/blob/45a13337dd42a776d51f7eaa0c1b03b105677f68/src/org/zaproxy/zap/extension/alertFilters/ExtensionAlertFilters.java#L113-L115
 *   https://github.com/zaproxy/zap-extensions/blob/45a13337dd42a776d51f7eaa0c1b03b105677f68/src/org/zaproxy/zap/extension/alertFilters/ExtensionAlertFilters.java#L319
 *
 *  Jorge Gómez - Julieta Sal-lari - Santiago Ramirez Ayuso
 *
 *  Instituto Tecnológico de Buenos Aires
 */
public class RPCExport extends ExtensionAdaptor implements EventConsumer{

    private static final boolean TESTING = false;

    public static String EXTENSION_NAME = "Faraday RPC Export";
    public static String PREFIX = "faraday.rpcExport.";
    public static String FARADAY_DEFAULT_RPCXML_URL = "http://localhost:9876/";

    private static FaradayClient faradayClient = new FaradayClient(FARADAY_DEFAULT_RPCXML_URL);

    private static final Logger log = Logger.getLogger(RPCExport.class);

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
        return Constant.messages.getString(PREFIX + str);
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

    @Override
    public void eventReceived(Event event) {
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
        try {

            URI alertUri = alert.getMsgUri();
            String hostId = faradayClient.createAndAddHost(alertUri.getHost());
            String interfaceId = faradayClient.createAndAddInterface(hostId, alertUri.getHost());
            String serviceId = faradayClient.createAndAddServiceToInterface(hostId, interfaceId, alertUri.getScheme(), "tcp", Arrays.asList(new String[]{Integer.toString(alertUri.getPort())}), "open");

//            TODO Delete this. Only for testing
//            log.info("Sending hostId = " + hostId);
//            log.info("Sending serviceId = " + serviceId);
//            log.info("Sending alert.getName() = " + alert.getName());
//            log.info("Sending alert.getDescription() + + alert.getReference() = " + alert.getDescription() + "\nReference: " + alert.getReference());
//            log.info("Sending Arrays.asList(new String[] {'CWE-' + alert.getCweId()}) = " + Arrays.asList(new String[] {"CWE-" + alert.getCweId()}));
//            log.info("Sending Integer.toString(alert.getRisk()) = " + Integer.toString(alert.getRisk()));
//            log.info("Sending alert.getSolution() = " + alert.getSolution());
//            log.info("Sending getHost(alert) = " + getHost(alert));
//            log.info("Sending alert.getUri() = " + alert.getUri());
//            log.info("Sending alert.getUri()  = " + alert.getUri() );
//            log.info("Sending alert.getMethod() = " + alert.getMethod());
//            log.info("Sending alert.getParam() = " + alert.getParam());
//            log.info("Sending alert.getMsgUri().getProtocolCharset() = " + alert.getMsgUri().getProtocolCharset());
//            log.info("Sending alert.getMsgUri().getPort() = " + alert.getMsgUri().getPort());
//            log.info("Sending alert.getParam() = " + alert.getParam());
//            log.info("Sending alert.getParam() = " + alert.getParam());


            //Parameters received by Faraday RPC Api based in ZAP Plugin (https://github.com/infobyte/faraday/blob/master/plugins/repo/zap/plugin.py):
            //hostId: String
            //serviceId: String
            //name: String
            //description: String. Description and reference concatenated.
            //refs: String[]. Single element with CWE ID
            //severity: String
            //website: String. Only host.
            //path: String
            //requests: String. Separated by \n
            //response: String. Empty on Faraday Zap Plugin
            //method: String
            //paramName: String. Empty on Faraday Zap Plugin
            //params: String. Comma separated values
            //query: String. Empty on Faraday Zap Plugin
            //category: String. Empty on Faraday Zap Plugin
            String vulnId = faradayClient.createAndAddVulnWebToService(hostId, serviceId,
                    alert.getName(), alert.getDescription() + "\nReferences: " + alert.getReference(),
                    Arrays.asList("CWE-" + alert.getCweId()),
                    Integer.toString(alert.getRisk()), alert.getSolution(),
                    alertUri.getHost(), alertUri.getPath(), alert.getUri(), "", alert.getMethod(),
                    alert.getParam(), alert.getParam(), alertUri.getQuery(), "");

        } catch (Exception e) {
            log.error("Could not load alert " + alert.getName() + " into Faraday.");
        }

    }

}
