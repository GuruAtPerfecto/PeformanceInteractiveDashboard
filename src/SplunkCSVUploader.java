import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class SplunkCSVUploader {

    private static final String SPLUNK_HEC_URL = "https://192.168.1.3:8088/services/collector";
    private static final String HEC_TOKEN = "35706de4-e523-4b9c-b9ef-b42fb8c69675";

    public static void main(String[] args) {
        String csvFile = "/Users/gbm/Downloads/vitals-latest.csv";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Skip header line
                if (line.startsWith("Timestamp")) {
                    continue;
                }

                // Create JSON payload
                Map<String, Object> event = new HashMap<>();
                event.put("event", line);
                event.put("sourcetype", "csv");

                // Convert to JSON string
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonPayload = objectMapper.writeValueAsString(event);

                // Send to Splunk
                sendToSplunk(jsonPayload);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendToSplunk(String jsonPayload) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {

//        String certificatesTrustStorePath = "/Users/gbm/Library/Java/JavaVirtualMachines/liberica-11.0.14/lib/security/cacerts";
//        System.setProperty("javax.net.ssl.trustStore", certificatesTrustStorePath);
//
//        HttpsTrustManager.allowAllSSL();
//
//        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
//            HttpPost postRequest = new HttpPost(SPLUNK_HEC_URL);
//            postRequest.addHeader("Authorization", "Splunk " + HEC_TOKEN);
//            postRequest.setEntity(new StringEntity(jsonPayload));
//
//            String response = EntityUtils.toString(httpClient.execute(postRequest).getEntity());
//            System.out.println("Response from Splunk: " + response);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, (chain, authType) -> true);
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier.INSTANCE);

        CloseableHttpClient httpClient = HttpClientBuilder.create().setSSLSocketFactory(sslsf).build();

        HttpPost postRequest = new HttpPost(SPLUNK_HEC_URL);
        postRequest.addHeader("Authorization", "Splunk " + HEC_TOKEN);
        postRequest.setEntity(new StringEntity(jsonPayload));

        String response = EntityUtils.toString(httpClient.execute(postRequest).getEntity());
        System.out.println("Response from Splunk: " + response);
    }
}
