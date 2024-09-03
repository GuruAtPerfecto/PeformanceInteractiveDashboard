import SplunkIntegration.splunk.HttpsTrustManager;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SplunkHECSender {

    private static final String SPLUNK_HEC_URL = "https://192.168.1.3:8088/services/collector";
    private static final String HEC_TOKEN = "35706de4-e523-4b9c-b9ef-b42fb8c69675";

    public static void main(String[] args) {
        String csvFilePath = "/Users/gbm/Downloads/vitals-ebfdf76d-5a07-4133-a043-a24f74a97a87-2024_06_23-19_58_10.csv";
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

        JSONObject event = new JSONObject();
        event.put("event", data);
        event.put("sourcetype", "csv"); // Set the custom source type here

        URL obj;
        obj = new URL(SPLUNK_HEC_URL);

        String value = "";
        try {
            HttpURLConnection con = null;

            HttpsTrustManager.allowAllSSL();

            con = (HttpURLConnection) obj.openConnection();

            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            con.setRequestProperty("Authorization", "Splunk " + HEC_TOKEN);

            con.setRequestMethod("POST");

//            OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
//            out.write("{\"event\":" + value + "}");
//            out.close();

            try (OutputStream os = con.getOutputStream()) {
            os.write(event.toString().getBytes());
            os.flush();
        }

            new InputStreamReader(con.getInputStream());

            int responseCode = con.getResponseCode();
            String response = "";
            System.out.println(con.getResponseMessage());

            if (responseCode > HttpURLConnection.HTTP_OK) {
                //handleError(con);
            } else {
                response = getStream(con);
            }

            System.out.println("\nSending 'GET' request to URL : " + obj.toURI());
            System.out.println("Response Code : " + responseCode);
            System.out.println("Response message: " + response.toString());
        } finally {

        }
    }

    private static String getStream(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, UTF_8);
        BufferedReader bufferReader = new BufferedReader(inputStreamReader);
        String response = "";
        try {
            StringBuilder builder = new StringBuilder();
            String outputString;
            while ((outputString = bufferReader.readLine()) != null) {
                if (builder.length() != 0) {
                    builder.append("\n");
                }
                builder.append(outputString);
            }
            response = builder.toString();
        } finally {
            bufferReader.close();
        }
        return response;
    }
}
