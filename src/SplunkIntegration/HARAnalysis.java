package SplunkIntegration;

import com.google.gson.*;
import SplunkIntegration.splunk.ReportingCollectorFactory;
import SplunkIntegration.splunk.SplunkReportingCollector;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class HARAnalysis {

    public static String localHostIp = "192.168.1.3";
    public static String localHostPort = "8888";

    public static String globalSLA = "20000";
    public static String splunkSchema = "https";
    public static String splunkHost = localHostIp;
    public static String splunkPort = "8088";
    public static String splunkToken = "35706de4-e523-4b9c-b9ef-b42fb8c69675";

    public static SplunkReportingCollector reporting = setSplunk();

    public static void main(String[] args) throws Exception {

        String charlesCommand = "/Applications/Charles.app/Contents/MacOS/Charles -config com.xk72.charles.config &";
        // Specify the working directory
        String workingDirectory = "/Users/gbm/Guru/Perfecto/Performance/CharlesIntegration";

        ProcessBuilder processBuilder = new ProcessBuilder(charlesCommand.split("\\s+"));
        processBuilder.directory(new File(workingDirectory));
        processBuilder.redirectErrorStream(true);

//        commandLineBuilder(processBuilder);
//
//        //Start Recording
//        processBuilder = new ProcessBuilder("curl", "-v", "-x", "http://" + localHostIp + ":" + localHostPort, "http://control.charles/recording/start");
//        Thread.sleep(5000);
//        commandLineBuilder(processBuilder);
//
//        //Stop Recording
//        processBuilder = new ProcessBuilder("curl", "-v", "-x", "http://" + localHostIp + ":" + localHostPort, "http://control.charles/recording/stop");
//        commandLineBuilder(processBuilder);
//
//        //Export session
//        processBuilder = new ProcessBuilder("curl", "-o", "session.chls", "-x", "http://" + localHostIp + ":" + localHostPort, "http://control.charles/session/download");
//        processBuilder.directory(new File(workingDirectory));
//        commandLineBuilder(processBuilder);
//
//        //Export HAR
//        processBuilder = new ProcessBuilder("curl", "--silent", "-x", "http://" + localHostIp + ":" + localHostPort, "http://control.charles/session/export-har", "-o", "myhar.har", ">", "/dev/null");
//        processBuilder.directory(new File(workingDirectory));
//        commandLineBuilder(processBuilder);

        String finalHarPath = "/Users/gbm/Guru/Perfecto/Performance/CharlesIntegration/sampleHars/myhar.har";

        finalHarPath = "/Users/gbm/Guru/Perfecto/Performance/CharlesIntegration/myhar.har";
        getNetworkTimings1(finalHarPath);

        setDetails("pass");
        //System.out.println(reporting.commitSplunk());
    }

    public static SplunkReportingCollector setSplunk() {
        SplunkReportingCollector reporting;

        reporting = ReportingCollectorFactory.createInstance(
                Long.parseLong((String) globalSLA),
                splunkSchema, splunkHost,
                splunkPort, splunkToken);

        ReportingCollectorFactory.setReporting(reporting);

        return reporting;
    }

    public static void setDetails(String result) {

        reporting.reporting.put("testStatus", result);

        try {
            reporting.reporting.put("hostName", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        reporting.submitReporting("MySplunkIntegration");

    }
    private static void commandLineBuilder(ProcessBuilder processBuilder) throws IOException {

        // Redirect the error stream to the output stream
        processBuilder.redirectErrorStream(true);

        // Start the process
        Process process = processBuilder.start();

        // Read the output of the process
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            if(line.contains("Recording Started"))
                break;
        }
    }

    private static void getNetworkTimings1(String networkFile) throws JsonSyntaxException, JsonIOException, FileNotFoundException {
        Gson gson = new Gson();

        //JsonObject jsonObject = new Gson().fromJson(new FileReader("/users/rickromanelli/Documents/QuiltDump/new/pmtl.har"), JsonObject.class);

        JsonObject jsonObject = new Gson().fromJson(new FileReader(networkFile), JsonObject.class);

        ///var/lib/jenkins/workspace/QuiltPOC/logs/
        // System.out.println(jsonObject.toString());

        JsonObject log = jsonObject.getAsJsonObject("log");
        JsonArray entriesArray = log.getAsJsonArray("entries");

        String qwilted_receiveTiming = "";
        String qwilted_waitTiming = "";
        String qwilted_httpstatus = "";
        String qwilted_statusText = "";
        String qwilted_ssl = "";
        String qwilted_send = "";
        String qwilted_connect = "";

        String receiveTiming2 = "";
        String waitTiming2 = "";
        String httpstatus2 = "";
        String statusText2 = "";
        String ssl2 = "";
        String send2 = "";
        String connect2 = "";

        int TTFB = 0;
        int index = 1;

        for (JsonElement entry : entriesArray) {

            JsonObject jSentry = entry.getAsJsonObject();
            JsonObject request = jSentry.getAsJsonObject("request");

            String url = request.get("url").getAsString();
            System.out.println(url);
           // if (url.contains("ctr-00201-na-east.opencachehub.qwilted-cds")) {
            if (url.contains("qn-01273-blr-1-04-1.opencachehub.qwilted-cds")) {
                JsonObject timings = jSentry.getAsJsonObject("timings");
                JsonObject response = jSentry.getAsJsonObject("response");
                qwilted_receiveTiming = timings.get("receive").getAsString();
                qwilted_waitTiming = timings.get("wait").getAsString();
                qwilted_send = timings.get("send").getAsString();
                qwilted_connect = timings.get("connect").getAsString();
                qwilted_ssl = timings.get("ssl").getAsString();

                qwilted_httpstatus = response.get("status").getAsString();
                qwilted_statusText = response.get("statusText").getAsString();


                int intStatus = Integer.parseInt(qwilted_httpstatus);

                TTFB = Integer.parseInt(qwilted_receiveTiming) + Integer.parseInt(qwilted_waitTiming) + Integer.parseInt(qwilted_send)
                        + Integer.parseInt(qwilted_connect);

                reporting.reporting.put("Latency_" + String.valueOf(index) + "_LastExecution", Integer.parseInt(qwilted_waitTiming));
                reporting.reporting.put("HTTPStatus_" + String.valueOf(index) + "_LastExecution", intStatus);
                reporting.reporting.put("statusText_" + String.valueOf(index) + "_LastExecution",qwilted_statusText);
                reporting.reporting.put("ssl_" + String.valueOf(index) + "_LastExecution", qwilted_ssl.replace("\"", ""));
                reporting.reporting.put("send_" + String.valueOf(index) + "_LastExecution", Integer.parseInt(qwilted_send));
                reporting.reporting.put("connect_" + String.valueOf(index) + "_LastExecution", Integer.parseInt(qwilted_connect));
                reporting.reporting.put("receiveTiming1_" + String.valueOf(index) + "_LastExecution", Integer.parseInt(qwilted_receiveTiming));
                index++;
            }

             //if (url.contains("qn-01273-blr-1-04-1.opencachehub.qwilted-cds")) {
            if (url.contains("ctr-00201-na-east.opencachehub.qwilted-cds")) {
                JsonObject timings = jSentry.getAsJsonObject("timings");
                JsonObject response = jSentry.getAsJsonObject("response");
                receiveTiming2 = timings.get("receive").getAsString();
                waitTiming2 = timings.get("wait").getAsString();
                send2 = timings.get("send").getAsString();
                connect2 = timings.get("connect").getAsString();
                ssl2 = timings.get("ssl").getAsString();

                httpstatus2 = response.get("status").getAsString();
                statusText2 = response.get("statusText").getAsString();

                int intStatus2 = Integer.parseInt(httpstatus2);

            }
        }
    }
}