package org.zaproxy.zap.extension.faraday.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Scanner;

/**
 * Client that sends RPC calls via HTTP to an RPC server and retrieves responses.
 */
public class RPCClient {

    /**
     * The HTTP address of the server to communicate to.
     */
    private String serverAddress;

    /**
     * Creates a new RPCClient with the HTTP address of the server to communicate to.
     *
     * @param serverAddress The HTTP address of the server to communicate to.
     */
    public RPCClient(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    /**
     * Sends and RPC call to the configured server and retrieves its response.
     * Currently supports only {@code string} and list if {@code string} parameters.
     *
     * @param methodName The name of the RPC method to call.
     * @param params A list of {@code string} parameters to pass to the method.
     * @return The response of the method call.
     * @throws RPCException if there a failure performing the call.
     */
    public String sendMessage(String methodName, Object... params) throws RPCException {
        try {
            URL u = new URL(serverAddress);
            URLConnection uc = u.openConnection();
            HttpURLConnection connection = (HttpURLConnection) uc;
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");

            connection.setRequestProperty("Content-Type", "text/xml");


            OutputStream out = connection.getOutputStream();
            OutputStreamWriter wout = new OutputStreamWriter(out, "UTF-8");

            wout.write("<?xml version=\'1.0\'?>\r\n");
            wout.write("<methodCall>\r\n");
            wout.write(String.format("<methodName>%s</methodName>\r\n", methodName));
            wout.write("<params>\r\n");
            for (Object param : params) {
                if (param instanceof String) {
                    wout.write("<param>\r\n");
                    wout.write(String.format("<value><string>%s</string></value>\r\n", param));
                    wout.write("</param>\r\n");
                } else if (param instanceof List) {
                    wout.write("<param>\r\n");
                    wout.write("<value><array><data>\r\n");
                    for (String item : (List<String>) param) {
                        wout.write(String.format("<value><string>%s</string></value>\r\n", item));
                    }
                    wout.write("</data></array></value>\r\n");
                    wout.write("</param>\r\n");
                }
            }
            wout.write("</params>\r\n");
            wout.write("</methodCall>\r\n");

            wout.flush();
            out.close();

            InputStream in = connection.getInputStream();
            Scanner s = new Scanner(in).useDelimiter("\\A");
            String response = s.hasNext() ? s.next() : "";
            in.close();
            out.close();
            connection.disconnect();
            return response;
        }
        catch (IOException e) {
            throw new RPCException("Error performing RPC call", e);
        }
    }
}
