import SplunkIntegration.splunk.HttpsTrustManager;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SplunkHECSender1 {

    private static final String SPLUNK_HEC_URL = "https://192.168.1.3:8088/services/collector";
    private static final String HEC_TOKEN = "35706de4-e523-4b9c-b9ef-b42fb8c69675";

    public static void main(String[] args) {
        String csvFilePath = "/Users/gbm/Downloads/vitals-ebfdf76d-5a07-4133-a043-a24f74a97a87-2024_06_23-19_58_10.csv";
       // String csvFilePath = "/Users/gbm/Downloads/vitals-latest.csv";

        try {
            String csvData = readCSV(csvFilePath);
            sendToSplunk(csvData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String readCSV(String filePath) throws Exception {
        StringBuilder csvData = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                csvData.append(line).append("\n");
            }
        }
        return csvData.toString();
    }

    private static void sendToSplunk(String data) throws Exception {

        String certificatesTrustStorePath = "/Users/gbm/Library/Java/JavaVirtualMachines/liberica-11.0.14/lib/security/cacerts";
        System.setProperty("javax.net.ssl.trustStore", certificatesTrustStorePath);

        HttpsTrustManager.allowAllSSL();

        URL url = new URL(SPLUNK_HEC_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Splunk " + HEC_TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");

        JSONObject event = new JSONObject();
        event.put("event", data);
        event.put("sourcetype", "csv"); // Set the custom source type here

        try (OutputStream os = conn.getOutputStream()) {
            os.write(event.toString().getBytes());
            //os.write(("{\"event\":" + event.toString().getBytes() + "}").getBytes());
            os.flush();
        }

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }

        conn.disconnect();
    }
}
