package org.zaproxy.zap.extension.faraday;

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
 * Created by jorexe on 25/10/16.
 */
public class RPCExport extends ExtensionAdaptor implements EventConsumer{

    private static final boolean TESTING = false;

    public static String EXTENSION_NAME = "Faraday RPC Export";
    public static String PREFIX = "faraday.rpcExport.";

    private static FaradayClient faradayClient = new FaradayClient("http://localhost:9876/");

    private static final Logger log = Logger.getLogger(RPCExport.class);

    private int lastAlert = -1;

    public RPCExport() {
        super(EXTENSION_NAME);
    }

    @Override
    public void init() {
        log.info("Init RPC Export");
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
        log.info("RPCExport.eventReceived");
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
//        log.info("Alert: alert.getAttack() : " + alert.getAttack());
//        log.info("Alert: alert.getDescription() : " + alert.getDescription());
//        log.info("Alert: alert.getEvidence() : " + alert.getEvidence());
//        log.info("Alert: alert.getMethod() : " + alert.getMethod());
//        log.info("Alert: alert.getName() : " + alert.getName());
//        log.info("Alert: alert.getOtherInfo() : " + alert.getOtherInfo());
//        log.info("Alert: alert.getParam() : " + alert.getParam());
//        log.info("Alert: alert.getPostData() : " + alert.getPostData());
//        log.info("Alert: alert.getReference() : " + alert.getReference());
//        log.info("Alert: alert.getSolution() : " + alert.getSolution());
//        log.info("Alert: alert.getUri() : " + alert.getUri());
//        log.info("Alert: alert.getUrlParamXML() : " + alert.getUrlParamXML());

        String hostId = faradayClient.createAndAddHost(getHost(alert));
        String interfaceId = faradayClient.createAndAddInterface(hostId, getHost(alert));
        String serviceId = faradayClient.createAndAddServiceToInterface(hostId, interfaceId, "http", "tcp", Arrays.asList(new String[]{"8081"}), "open");
        String vulnId = faradayClient.createAndAddVulnToService(hostId, serviceId, alert.getName(), alert.getDescription(),
                Arrays.asList(new String[] {alert.getReference()}), getFaradayRisk(alert.getRisk()), alert.getSolution());

//        TODO Maybe
//        String vulnId = faradayClient.createAndAddVulnWebToService(hostId, serviceId, alert.getName(), alert.getDescription(),
//                Arrays.asList(new String[] {alert.getReference()}), getFaradayRisk(alert.getRisk()), alert.getSolution(),
//                getHost(alert), alert.getUri(), alert.getUri() /*\n separated string*/, alert.getParam()/*, separated string*/);
        log.info("Added vuln " + vulnId);


    }

    private String getHost(Alert alert) {
        try {
            return alert.getMsgUri().getHost();
        } catch (Exception e) {

        }
        try {
            return new URL(alert.getUri()).getHost();
        } catch (MalformedURLException e) {
            return "";
        }
    }

    private String getFaradayRisk(int risk) {
        switch (risk) {
            case Alert.RISK_HIGH:
                return "high";
            case Alert.RISK_INFO:
                return "info";
            case Alert.RISK_LOW:
                return "low";
            case Alert.RISK_MEDIUM:
                return "med";
        }
        return "undefined";
    }

//    Python ZAP RPC METHODS
//    h_id = self.createAndAddHost(site.ip)
//    i_id = self.createAndAddInterface(h_id,site.ip,ipv4_address=site.ip,hostname_resolution=host)
//    s_id = self.createAndAddServiceToInterface(h_id,i_id,"http","tcp",ports=[site.port],status='open')
//    n_id = self.createAndAddNoteToService(h_id, s_id, "website", "")
//    n2_id = self.createAndAddNoteToNote(h_id, s_id, n_id, site.host, "")
//
//            for item in site.items:
//    v_id = self.createAndAddVulnWebToService(h_id,s_id,item.name,item.desc,website=site.host,severity=item.severity,path=item.items[0]['uri'],params=item.items[0]['params'],request=item.requests,ref=item.ref,resolution=item.resolution)
}
