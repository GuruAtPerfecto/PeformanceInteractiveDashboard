import SplunkIntegration.splunk.HttpsTrustManager;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.security.cert.X509Certificate;


public class SplunkUploader {
    public static void main(String[] args) {
        String splunkHost = "https://192.168.1.3:8000";
        String splunkToken = "35706de4-e523-4b9c-b9ef-b42fb8c69675";
        String splunkIndex = "test_index";
        String filePath = "/Users/gbm/Downloads/vitals-latest.csv";

        File file = new File(filePath);

        String certificatesTrustStorePath = "/Users/gbm/Library/Java/JavaVirtualMachines/liberica-11.0.14/lib/security/cacerts";
        System.setProperty("javax.net.ssl.trustStore", certificatesTrustStorePath);

        HttpsTrustManager.allowAllSSL();

        uploadFileToSplunk(splunkHost, splunkToken, splunkIndex, file);
    }

    public static void uploadFileToSplunk(String splunkHost, String splunkToken, String splunkIndex, File file) {
        String url = splunkHost + "/services/receivers/simple?index=" + splunkIndex + "&sourcetype=csv";

        try {
            // Trust all certificates
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }}, new java.security.SecureRandom());

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

            String certificatesTrustStorePath = "/Users/gbm/Library/Java/JavaVirtualMachines/liberica-11.0.14/lib/security/cacerts";
            System.setProperty("javax.net.ssl.trustStore", certificatesTrustStorePath);

            HttpsTrustManager.allowAllSSL();

            HttpPost uploadFile = new HttpPost(url);
            uploadFile.setHeader("Authorization", "Splunk " + splunkToken);

            FileEntity fileEntity = new FileEntity(file, ContentType.DEFAULT_BINARY);
            uploadFile.setEntity(fileEntity);

            HttpResponse response = httpClient.execute(uploadFile);
            HttpEntity responseEntity = response.getEntity();

            if (responseEntity != null) {
                String responseString = EntityUtils.toString(responseEntity);
                System.out.println("Response: " + responseString);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
