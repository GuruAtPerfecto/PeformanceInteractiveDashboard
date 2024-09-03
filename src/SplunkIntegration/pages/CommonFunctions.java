package SplunkIntegration.pages;

import SplunkIntegration.splunk.ReportingCollectorFactory;
import SplunkIntegration.splunk.SplunkReportingCollector;
import com.google.gson.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


public class CommonFunctions {

    private static final String SECURITY_TOKEN = System.getProperty("PERFECTO_SECURITY_TOKEN");
    private static final String CQL_NAME = System.getProperty("CLOUD_NAME"); // TODO put your Continuous Quality
    // Lab name here
    private static final String REPORTING_SERVER_URL = "https://" + CQL_NAME + ".reporting.perfectomobile.com";

//    static String scriptKey = "PUBLIC:DBankScriptLessTransactionwiseHAR.xml";
//    static String DUT = "00008110-00090CD90161801E";
//    static String testName = "DBankScriptlessMobileBlaze";
//    static String appPath = "PUBLIC:DBank1.ipa";
//    static String backgroundAppList = "Messages,Calculator,Maps,Calendar,Safari";
//    static String networkProfile = "4g_lte_good";
//    static String reportium_job_number = "11";
//    static String reportium_job_name = "DBankScriptlessMobileBlaze";

    static JSONObject myResponse = null;
    static StringBuffer response = new StringBuffer();

    public static String localHostIp = "192.168.1.4";
    public static String localHostPort = "8888";
    public static String globalSLA = "20000";
    public static String splunkSchema = "https";
    public static String splunkHost = localHostIp;
    public static String splunkPort = "8088";
    public static String splunkToken = "35706de4-e523-4b9c-b9ef-b42fb8c69675";

    public static String initiateScriptlessTest(Map<String, String> parameters){
        String executionID = null;

        String command = "https://" + CQL_NAME + ".perfectomobile.com" + "/services/executions?operation=execute";
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!(key.equalsIgnoreCase("deviceModel") || key.equalsIgnoreCase("executeTest"))) {
                if (key.equalsIgnoreCase("scriptKey"))
                    command = command + "&" + key + "=" + value;
                else
                    command = command + "&param." + key + "=" + value;
            }
        }

//        response = APIPostRequest("https://" + cloudName + ".perfectomobile.com" + "/services/executions?operation=execute&scriptKey="
//                + scriptKey + "&securityToken="
//                + SECURITY_TOKEN + "&param.DUT=" + DUT
//                + "&param.TestName=" + testName
//                + "&param.JobName=" +  System.getProperty("reportium_job_name")
//                + "&param.JobNumber=" + System.getProperty("reportium_job_number")
//                + "&param.AppPath=" + appPath
//                + "&param.BackGroundAppNames=" + backgroundAppList
//                + "&param.NetworkProfile=" + networkProfile);

        command = command + "&param.JobName=" +  System.getProperty("reportium_job_name") + "&param.JobNumber=" + System.getProperty("reportium_job_number") + "&securityToken=" + SECURITY_TOKEN;
        System.out.println("API Command = " + command);
        response = APIPostRequest(command);

        System.out.println(response);
        myResponse = new JSONObject(response.toString());
        System.out.println(myResponse.get("executionId"));
        executionID =  myResponse.get("executionId").toString();

        //executionID = "guruswamy.bm@perfectomobile.com_DBankScriptLessTransactionwiseHAR_24-01-16_15_14_59_7182";
        return executionID;
    }

    public static void waitForExecutionToComplete(String cloudName, String executionID){
        myResponse = getScriptlessTestExecutionStatus(cloudName, executionID);
        //System.out.println(myResponse);
        while (!myResponse.get("status").toString().equalsIgnoreCase("Completed")) {
            System.out.println("Waiting for Script completion. Script Status = " + myResponse.get("status") + "   , progressPercentage = " + myResponse.get("progressPercentage"));
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            myResponse = getScriptlessTestExecutionStatus(cloudName, executionID);
        }
    }

    public static void retrieveAndSubmitHARAnalysis(SplunkReportingCollector reporting, String executionID, String deviceId) throws URISyntaxException, IOException, ParserConfigurationException, InterruptedException, SAXException {
        List<JsonObject> executions = new ArrayList<>();
        JsonObject executions1 = null;
        executions1 = retrieveTestExecutions(executionID);

        executions.add(executions1);

        JSONObject jo2 = null;
        JSONArray ja = null;

        JsonElement name = executions1.get("resources");

        //Get the respone count
        JsonArray respCount = executions1.getAsJsonArray("resources");

        ja = new JSONArray(name.toString());

        JSONObject jo = null;

        //System.out.println(respCount.size());

        //System.out.println(ja.length());

        jo2 = null;

        jo2 = ja.getJSONObject(0);

        JSONArray ja1 = jo2.getJSONArray("artifacts");

        JSONObject jo1 = null;

        //Process HAR only if the test report has Network traffic HAR file present
        if (ja1.length() != 0) {
            try {
                jo1 = ja1.getJSONObject(0);
                //System.out.println("Breaking loop");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                System.out.println("Report still Generating? Kindly check the Perfecto report");
            }

            //System.out.println("Current working directory: " + System.getProperty("user.dir"));

            String screenName = System.getProperty("user.dir") + File.separator + "HARExtract";
            JsonObject lastTestExecution = ReportiumExportUtils.retrieveTestExecutionsPagewise(0, executionID);
            JsonArray resources1 = lastTestExecution.getAsJsonArray("resources");
            JsonObject testExecution1 = resources1.get(0).getAsJsonObject();
            String lastTestName = String.valueOf(testExecution1.get("name"));

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new Date());

            String HARExtractPath = screenName + File.separator + lastTestName.replace("\"", "") + "_" + deviceId + timestamp;

            File folder1 = new File(HARExtractPath);
            folder1.mkdir();
            try {
                downloadArtifact(jo2, HARExtractPath + File.separator + "HarOutPut");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Path path = Paths.get(HARExtractPath + File.separator + "HarOutPut" + ".zip");

            try {
                unzip(HARExtractPath + File.separator + "HarOutPut" + ".zip", HARExtractPath + File.separator + "TempExtractedFolder");
            } catch (IOException e) {
                e.printStackTrace();
            }

            File folder = new File(HARExtractPath + File.separator + "TempExtractedFolder");

            File[] listOfFiles = folder.listFiles();
            String[] sortedFileList = new String[listOfFiles.length];

            sortedFileList = getHarlist(folder);

            int count = listOfFiles.length;

            try {
                if ((ja.length() - 1) == countRegularFiles(path)) {

                    System.out.println("BothAReEqual");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            ArrayList<String> transactionNames = new ArrayList<>();
            String lastTestStatus = String.valueOf(testExecution1.get("status"));
            String testId = String.valueOf(testExecution1.get("id"));
            String externalid = String.valueOf(testExecution1.get("externalId"));
            transactionNames = retriveTransactionNames(testId.replace("\"", ""), externalid.replace("\"", ""));

            for (int i = 0; i < count; i++) {

                File sourceFile = new File(HARExtractPath + File.separator + "TempExtractedFolder" + File.separator + sortedFileList[i].toString());
                String DestFilePath = HARExtractPath + File.separator + transactionNames.get(i).replace("\"", "");
                File destFile = new File(DestFilePath + ".zip");

                if (sourceFile.renameTo(destFile)) {
                    //System.out.println("File moved successfully");
                    try {
                        unzip(DestFilePath + ".zip", HARExtractPath + File.separator + transactionNames.get(i).replace("\"", ""));
                        getNetworkTimings(reporting, transactionNames.get(i).replace("\"", ""), DestFilePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Failed to move file");
                }
            }

            folder.delete();
            File tempFolder = new File(HARExtractPath + File.separator + "HarOutPut" + ".zip");
            tempFolder.delete();
        }
        else {
            System.out.println("Device = " + deviceId + " Did not generated the HAR tile to process. Skipping the HAR analysis");
        }
    }

    public static void retrieveAndDeployTransactionTimes(SplunkReportingCollector reporting, String executionId, String deviceId) throws URISyntaxException, IOException, ParserConfigurationException, InterruptedException, SAXException {

        System.out.println(reporting);
        Map<String, Double> lastTestuxTimerValues = new HashMap<>();
        ArrayList<Map<String, Double>> todayUxTimer = new ArrayList<>();
        ArrayList<Map<String, Double>> yesterdayUxTimer = new ArrayList<>();
        ArrayList<Map<String, Double>> Days_7_AgoUxTimer = new ArrayList<>();
        ArrayList<Map<String, Double>> Days_14_AgoUxTimer = new ArrayList<>();
        ArrayList<Map<String, Double>> Days_30_AgoUxTimer = new ArrayList<>();
        Map<String, Double> averageMap = new HashMap<>();

        // Get the current date and time in the system default time zone
        LocalDateTime currentDateTime = LocalDateTime.now();

        // Set the time to 12:00 AM (midnight)
        LocalDateTime midnight = currentDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);

        // Convert midnight LocalDateTime to epoch time (seconds since Unix epoch)
        long unixTimestampToday = 1000 * midnight.toEpochSecond(ZoneOffset.UTC);

        // Get Unix timestamp for yesterday
        long unixTimestampYesterday = 1000 * Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond();

        // Get Unix timestamp for X days ago (replace X with the desired number of days)
        long unixTimestamp7DaysAgo = 1000 * Instant.now().minus(7, ChronoUnit.DAYS).getEpochSecond();
        long unixTimestamp14DaysAgo = 1000 * Instant.now().minus(14, ChronoUnit.DAYS).getEpochSecond();
        long unixTimestamp30DaysAgo = 1000 * Instant.now().minus(30, ChronoUnit.DAYS).getEpochSecond();

        System.out.println("Unix timestamp for today: " + unixTimestampToday);
        System.out.println("Unix timestamp for yesterday: " + unixTimestampYesterday);
        System.out.println("Unix timestamp for " + 7 + " days ago: " + unixTimestamp7DaysAgo);
        System.out.println("Unix timestamp for " + 14 + " days ago: " + unixTimestamp14DaysAgo);
        System.out.println("Unix timestamp for " + 30 + " days ago: " + unixTimestamp30DaysAgo);

        JsonObject lastTestExecution = null;
        ArrayList<String> transactionNames = new ArrayList<>();
        String lastTestName = null;
        String lastTestStatus = null;

        // Define the email pattern
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";

        // Compile the pattern
        Pattern pattern = Pattern.compile(emailPattern);

        // Match the pattern against the beginning of the sentence
        Matcher matcher = pattern.matcher(executionId);


        // Check if the sentence starts with an email
        if (matcher.find() && matcher.start() == 0){

            lastTestExecution = ReportiumExportUtils.retrieveTestExecutionsPagewise(0, executionId);

            JsonArray resources1 = lastTestExecution.getAsJsonArray("resources");
            JsonObject testExecution1 = resources1.get(0).getAsJsonObject();
            lastTestName = String.valueOf(testExecution1.get("name"));

            lastTestStatus = String.valueOf(testExecution1.get("status"));
            String testId = String.valueOf(testExecution1.get("id"));
            String externalid = String.valueOf(testExecution1.get("externalId"));

            ArrayList<String> temp = new ArrayList<>();

            //transactionNames = retriveTransactionNames(testId.replace("\"", ""), externalid.replace("\"", ""));
            lastTestuxTimerValues = retriveUXTimer(testId.replace("\"", ""), externalid.replace("\"", ""));
            Days_30_AgoUxTimer = loadPageResponseUXValues(lastTestName, unixTimestamp30DaysAgo, deviceId);

        } else {
            //load the last test as latest execution
            Days_30_AgoUxTimer = loadPageResponseUXValues(executionId, unixTimestamp30DaysAgo, deviceId);
            lastTestuxTimerValues = Days_30_AgoUxTimer.get(0);
            lastTestName = executionId;
        }

        System.out.println("\n****** " + deviceId + "  Last Transaction Page Load Details *******");
        System.out.println("    Transaction Name " +  "\t\t\tPage Load Time" );
        for (Map.Entry<String, Double> entry : lastTestuxTimerValues.entrySet()) {
           // System.out.println("Transaction Name = " + entry.getKey() + ", Page Load Time = " + entry.getValue());
            System.out.println("        " + entry.getKey() + "\t\t\t\t\t" + entry.getValue());
            transactionNames.add(entry.getKey());
            reporting.reporting.put(entry.getKey() + "_Transaction_LastExecution", entry.getValue());
        }

        System.out.println("\n");
        todayUxTimer = loadPageResponseUXValues(lastTestName, unixTimestampToday, deviceId);
        averageMap = findAverageTransactions(transactionNames, todayUxTimer, deviceId + ": Average Value of TODAY");

        System.out.println(" \t\t\t****  " + deviceId + "  TODAY's Average Map ****");
        for (Map.Entry<String, Double> entry : averageMap.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + (Double.isNaN(entry.getValue()) ? 0 : entry.getValue()));
            reporting.reporting.put("Avg_" + entry.getKey() + "_Transaction_Today", (Double.isNaN(entry.getValue()) ? 0 : entry.getValue()));
        }

        yesterdayUxTimer = loadPageResponseUXValues(lastTestName, unixTimestampYesterday, deviceId);
        averageMap = findAverageTransactions(transactionNames, yesterdayUxTimer, deviceId + ": Average Value of YESTERDAY");

        System.out.println(" \t\t\t****  " + deviceId + "  YESTERDAY's Average Map ****");
        for (Map.Entry<String, Double> entry : averageMap.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + (Double.isNaN(entry.getValue()) ? 0 : entry.getValue()));
            reporting.reporting.put("Avg_" + entry.getKey() + "_Transaction_Yesterday", (Double.isNaN(entry.getValue()) ? 0 : entry.getValue()));
        }

        Days_7_AgoUxTimer = loadPageResponseUXValues(lastTestName, unixTimestamp7DaysAgo, deviceId);
        averageMap = findAverageTransactions(transactionNames, Days_7_AgoUxTimer,deviceId + ": Average Value of 7-DAYS-AGO");

        System.out.println(" \t\t\t****  " + deviceId + "  7-DAYS-AGO Average Map ****");
        for (Map.Entry<String, Double> entry : averageMap.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + (Double.isNaN(entry.getValue()) ? 0 : entry.getValue()));
            reporting.reporting.put("Avg_" + entry.getKey() + "_Transaction_7Days_Ago", (Double.isNaN(entry.getValue()) ? 0 : entry.getValue()));
        }

			Days_14_AgoUxTimer = loadPageResponseUXValues(lastTestName, unixTimestamp14DaysAgo, deviceId);
			averageMap = findAverageTransactions(transactionNames, Days_14_AgoUxTimer, deviceId + ": Average Value of 14-DAYS-AGO");

          System.out.println(" ****  " + deviceId + "  14-DAYS-AGO Average Map ****");
			for (Map.Entry<String, Double> entry : averageMap.entrySet()) {
				 System.out.println("Key: " + entry.getKey() + ", Value: " + (Double.isNaN(entry.getValue()) ? 0 : entry.getValue()));
               reporting.reporting.put("Avg_" + entry.getKey() + "_Transaction_14Days_Ago", (Double.isNaN(entry.getValue()) ? 0 : entry.getValue()));
			}

			//Days_30_AgoUxTimer = loadPageResponseUXValues(lastTestName, unixTimestamp30DaysAgo, deviceId);
			averageMap = findAverageTransactions(transactionNames, Days_30_AgoUxTimer, deviceId + ": Average Value of 30-DAYS-AGO");

          //System.out.println(" ****  " + deviceId + "  30-DAYS-AGO Average Map ****");
			for (Map.Entry<String, Double> entry : averageMap.entrySet()) {
				System.out.println("Key: " + entry.getKey() + ", Value: " + (Double.isNaN(entry.getValue()) ? 0 : entry.getValue()));
              reporting.reporting.put("Avg_" + entry.getKey() + "_Transaction_30Days_Ago", (Double.isNaN(entry.getValue()) ? 0 : entry.getValue()));
			}
        //System.out.println(deviceId + ": " + reporting.reporting);
    }
    public static JSONObject getScriptlessTestExecutionStatus(String cloudName, String executionId){
        response = APIPostRequest("https://" + cloudName + ".perfectomobile.com" + "/services/executions/"
                + executionId
                + "?operation=status&securityToken=" + SECURITY_TOKEN);
        myResponse = new JSONObject(response.toString());
//        System.out.println(myResponse);
//        System.out.println(myResponse.get("status"));
        return myResponse;
    }

    private static void getNetworkTimings(SplunkReportingCollector reporting, String transactionName, String networkFile)
            throws JsonSyntaxException, JsonIOException, FileNotFoundException {
        Gson gson = new Gson();

        //JsonObject jsonObject = new Gson().fromJson(new FileReader("/users/rickromanelli/Documents/QuiltDump/new/pmtl.har"), JsonObject.class);

        JsonObject jsonObject = new Gson().fromJson(new FileReader(networkFile + File.separator + "pmtl.har"), JsonObject.class);

        JsonObject log = jsonObject.getAsJsonObject("log");
        JsonArray entriesArray = log.getAsJsonArray("entries");

        // for loop
        String receiveTiming = "";
        String waitTiming = "";
        String httpstatus = "";
        String statusText = "";
        String ssl = "";
        String send = "";
        String connect = "";

        int TTFB = 0;
        int fullTTFB = 0;

        boolean TTFBFound = false;

        for (JsonElement entry : entriesArray) {

            JsonObject jSentry = entry.getAsJsonObject();
            JsonObject request = jSentry.getAsJsonObject("request");

            String url = request.get("url").getAsString();

            JsonObject timings = jSentry.getAsJsonObject("timings");
            JsonObject response = jSentry.getAsJsonObject("response");

            receiveTiming = timings.get("receive").getAsString().replace("\"", "");
            waitTiming = timings.get("wait").getAsString().replace("\"", "");
            send = timings.get("send").getAsString().replace("\"", "");
            connect = timings.get("connect").getAsString().replace("\"", "");
            ssl = timings.get("ssl").getAsString().replace("\"", "");

            httpstatus = response.get("status").getAsString().replace("\"", "");
            statusText = response.get("statusText").getAsString().replace("\"", "");

            int intStatus = Integer.parseInt(httpstatus);

            TTFB = Integer.parseInt(receiveTiming) + Integer.parseInt(waitTiming) + Integer.parseInt(send)
                    + Integer.parseInt(connect);

            if(!TTFBFound) {
                reporting.reporting.put(transactionName + "_TTFB_LastExecution", TTFB);

                reporting.reporting.put("Latency_" + transactionName + "_LastExecution", Integer.parseInt(waitTiming));
                reporting.reporting.put("HTTPStatus_" + transactionName + "_LastExecution", intStatus);
                reporting.reporting.put("statusText_" + transactionName + "_LastExecution",statusText);
                reporting.reporting.put("ssl_" + transactionName + "_LastExecution", ssl.replace("\"", ""));
                reporting.reporting.put("send_" + transactionName + "_LastExecution", Integer.parseInt(send));
                reporting.reporting.put("connect_" + transactionName + "_LastExecution", Integer.parseInt(connect));
                reporting.reporting.put("receiveTiming1_" + transactionName + "_LastExecution", Integer.parseInt(receiveTiming));

                TTFBFound = true;
            }

            fullTTFB = fullTTFB + TTFB;
        }

        reporting.reporting.put(transactionName + "_FullTTFB_LastExecution", fullTTFB);

    }

    private static JsonObject retrieveTestExecutions(String executionID)
            throws URISyntaxException, IOException, InterruptedException {
        URIBuilder uriBuilder = new URIBuilder(REPORTING_SERVER_URL + "/export/api/v1/test-executions");

        // Optional: Filter by range. In this example: retrieve test executions of the
        // past month (result may contain tests of multiple driver executions)
        /*
         * uriBuilder.addParameter("startExecutionTime[0]",
         * Long.toString(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(12)));
         * uriBuilder.addParameter("endExecutionTime[0]",
         * Long.toString(System.currentTimeMillis()));
         */

        uriBuilder.addParameter("externalId[0]", executionID);

        // Optional: Filter by a specific driver execution ID that you can obtain at
        // script execution
        // uriBuilder.addParameter("externalId[0]", "SOME_DRIVER_EXECUTION_ID");
        // uriBuilder.addParameter("tags[0]", tags);

        HttpGet getExecutions = new HttpGet(uriBuilder.build());
        addDefaultRequestHeaders(getExecutions);
        HttpClient httpClient = HttpClientBuilder.create().build();

        System.out.println(getExecutions.getURI().toString());

        JsonObject executions = null;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (int i = 0; i < 20; i++) {
            HttpResponse getExecutionsResponse = httpClient.execute(getExecutions);

            try (InputStreamReader inputStreamReader = new InputStreamReader(
                    getExecutionsResponse.getEntity().getContent())) {

                String response = IOUtils.toString(inputStreamReader);
                try {
                    executions = gson.fromJson(response, JsonObject.class);
                    JsonObject metadata = executions.getAsJsonObject("metadata");
                    String status = metadata.get("processingStatus").getAsString();
                    if (status.equalsIgnoreCase("Processing_complete")) {

                        break;

                    } else {
                        Thread.sleep(3000);
                        continue;
                    }

                } catch (JsonSyntaxException e) {
                    throw new RuntimeException("Unable to parse response: " + response);
                }
            }

        }
        //System.out.println("\nList of test executions response:\n" + gson.toJson(executions));

        return executions;
    }

    private static void addDefaultRequestHeaders(HttpRequestBase request) {

        //System.out.println(SECURITY_TOKEN);
        if (SECURITY_TOKEN == null || SECURITY_TOKEN.equals("MY_CONTINUOUS_QUALITY_LAB_SECURITY_TOKEN")) {
            throw new RuntimeException("Invalid security token '" + SECURITY_TOKEN + "'. Please set a security token");
        }
        request.addHeader("PERFECTO_AUTHORIZATION", SECURITY_TOKEN);
    }
    public static void downloadArtifact(JSONObject jo, String screenName) throws InterruptedException {

        JSONArray ja1 = jo.getJSONArray("artifacts");
        JSONObject jo1 = null;

        for(int i = 0; i < ja1.length(); i++){
            if(ja1.getJSONObject(i).getString("type").equalsIgnoreCase("NETWORK")){
                jo1 = ja1.getJSONObject(i);
            }
        }
//        Thread.sleep(5000);

        String accebilityReportUrl = jo1.getString("path");

        try {
            URL url = new URL(accebilityReportUrl);
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(screenName + ".zip");
            byte[] b = new byte[1024];
            int count;
            while ((count = in.read(b)) >= 0) {
                out.write(b, 0, count);
            }
            out.flush(); out.close(); in.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param zipFilePath
     * @param destDirectory
     * @throws IOException
     */
    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdirs();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
    }

    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[4096];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    private static String[] getHarlist(File folder) {
        File[] listOfFiles = folder.listFiles();
        long[] filetimestamp = new long[listOfFiles.length];
        String[] sortedFileList = new String[listOfFiles.length];
        HashMap<Long, String> fileMap = new HashMap<Long, String>();

        for (int i1 = 0; i1 < listOfFiles.length; i1++) {
            if (listOfFiles[i1].isFile()) {
                //System.out.println("File " + listOfFiles[i1].getName());
                String scrName = listOfFiles[i1].getName();
                String fileName = listOfFiles[i1].getName();

                int i = scrName.indexOf("_");
                scrName = scrName.substring(i);
                scrName = scrName.replaceAll("act.zip", "");
                scrName = scrName.replaceAll("_", "");
                scrName = scrName.replaceAll(".icloud", "");
                //System.out.println(scrName);
                filetimestamp[i1] = Long.parseLong(scrName);
                fileMap.put(filetimestamp[i1], fileName);

            } else if (listOfFiles[i1].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i1].getName());
            }
        }

       // System.out.println(filetimestamp);

        long temp = 0;

        for (int i = 0; i < filetimestamp.length; i++) {
            for (int j = i+1; j < filetimestamp.length; j++) {
                if(filetimestamp[i] > filetimestamp[j]) {
                    temp = filetimestamp[i];
                    filetimestamp[i] = filetimestamp[j];
                    filetimestamp[j] = temp;
                }
            }
        }

        //System.out.println(filetimestamp);

        for (int i = 0; i < filetimestamp.length; i++) {

            sortedFileList[i] = fileMap.get(filetimestamp[i]);
        }

        return sortedFileList;
    }

    private static int countRegularFiles(final Path zipFilePath) throws ZipException, IOException {

        ZipFile zipFile = new ZipFile(zipFilePath.toFile());

        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        int numRegularFiles = 0;
        while (entries.hasMoreElements()) {
            if (! entries.nextElement().isDirectory()) {
                ++numRegularFiles;
            }
        }
        return numRegularFiles;
    }

    public static void setDetails(SplunkReportingCollector reporting, String result) {

        reporting.reporting.put("testStatus", result);

        try {
            reporting.reporting.put("hostName", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        reporting.submitReporting("MySplunkIntegration");
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

    public static Map retriveUXTimer(String testId, String executionId) throws ParserConfigurationException, IOException, SAXException, InterruptedException, URISyntaxException {
        int index = 0;
        JsonArray resources3 = null;

        Map<String, Double> uxTimerValues = new HashMap<>();

        JsonObject commandsJson = ReportiumExportUtils.retrieveTestCommands(testId.replace("\"", ""));
        JsonArray resources2 = commandsJson.getAsJsonArray("resources");

        //System.out.println(resources2.size());

        for (int i = 0; i < resources2.size(); i++) {
            JsonObject testExecution2 = resources2.get(i).getAsJsonObject();
            resources3 = testExecution2.getAsJsonArray("commands");

            for (int j = 0; j < resources3.size(); j++) {
                JsonObject testExecution = resources3.get(j).getAsJsonObject();
                //System.out.println(testExecution);
                //System.out.println(testExecution.get("name"));
                if (testExecution.get("name").toString().contains("Time checkpoint")) {
                    JsonArray params = testExecution.getAsJsonArray("parameters");
                    JsonObject paramsObject = params.get(0).getAsJsonObject();
                    //System.out.println(String.valueOf(paramsObject.get("value")));

                    JsonArray params2 = testExecution.getAsJsonArray("resultData");
                    JsonObject paramsObject2 = params2.get(0).getAsJsonObject();
                    //System.out.println(String.valueOf(paramsObject2.get("value")));

                    uxTimerValues.put(String.valueOf(paramsObject.get("value")).replace("\"", ""), Double.parseDouble(String.valueOf(paramsObject2.get("value")).replace("\"", "")) * 1000);

                    //System.out.println(String.valueOf(paramsObject2.get("value")));
                }
            }
        }
        return uxTimerValues;
    }

    public static ArrayList<String> retriveTransactionNames(String testId, String executionId) throws ParserConfigurationException, IOException, SAXException, InterruptedException, URISyntaxException {
        int index = 0;
        JsonArray resources3 = null;

        ArrayList<String> transactionName = new ArrayList<>();

        Map<String, Double> uxTimerValues = new HashMap<>();

        JsonObject commandsJson = ReportiumExportUtils.retrieveTestCommands(testId.replace("\"", ""));
        JsonArray resources2 = commandsJson.getAsJsonArray("resources");

        for (int i = 0; i < resources2.size(); i++) {
            JsonObject testExecution2 = resources2.get(i).getAsJsonObject();
            resources3 = testExecution2.getAsJsonArray("commands");

        for (int j = 0; j < resources3.size(); j++) {
            JsonObject testExecution = resources3.get(j).getAsJsonObject();
            //System.out.println(testExecution);
            //System.out.println(testExecution.get("name"));
            if (testExecution.get("name").toString().contains("Time checkpoint")) {
                JsonArray params = testExecution.getAsJsonArray("parameters");
                JsonObject paramsObject = params.get(0).getAsJsonObject();
                //System.out.println(String.valueOf(paramsObject.get("value")));
                transactionName.add(String.valueOf(paramsObject.get("value")));

                JsonArray params2 = testExecution.getAsJsonArray("resultData");
                JsonObject paramsObject2 = params2.get(0).getAsJsonObject();
                //System.out.println(String.valueOf(paramsObject2.get("value")));

                uxTimerValues.put(String.valueOf(paramsObject.get("value")).replace("\"", ""), Double.parseDouble(String.valueOf(paramsObject2.get("value")).replace("\"", "")) * 1000);

                //System.out.println(String.valueOf(paramsObject2.get("value")));
            }
        }
    }
        return transactionName;
    }

    public static ArrayList<Map<String, Double>> loadPageResponseUXValues(String lastTestName, long unixTimeStampDate, String deviceId) throws URISyntaxException, IOException, ParserConfigurationException, InterruptedException, SAXException {
        String[] epochTime = new String[20];

        Boolean truncated = true;
        ArrayList<Map<String, Double>> UxTimer = new ArrayList<>();

        int page = 1;

        while (truncated) {
            JsonObject executions1 = ReportiumExportUtils.retrieveTestExecutionsPagewise(page, String.valueOf(unixTimeStampDate));
            JsonArray resources = executions1.getAsJsonArray("resources");
            JsonObject metadata = executions1.getAsJsonObject("metadata");

            truncated = Boolean.parseBoolean(String.valueOf(metadata.get("truncated")));
            page++;
            for (int i = 0; i < resources.size(); i++) {
                JsonObject testExecution = resources.get(i).getAsJsonObject();
                //System.out.println(testExecution);
                //System.out.println(testExecution.get("name"));

                String testName = String.valueOf(testExecution.get("name"));
                String testStatus = String.valueOf(testExecution.get("status"));

                if (String.valueOf(testExecution.get("name")).contains(lastTestName)) {
                    JsonArray t = testExecution.getAsJsonArray("platforms");
                   // System.out.println("deviceID = " + deviceId + "= " + t);
                    String currentTestdeviceId = String.valueOf(testExecution.getAsJsonArray("platforms").get(0).getAsJsonObject().get("deviceId")).replace("\"", "");
                    if(deviceId.contains(currentTestdeviceId)){
                        if ((testName.contains(lastTestName)) && (testStatus.contains("\"PASSED\""))) {
                            String id = String.valueOf(testExecution.get("id"));
                            String externalid = String.valueOf(testExecution.get("externalId"));
                            Double startTime = Double.parseDouble(String.valueOf(testExecution.get("startTime")));

                            Double startTimeePochTime = startTime / 1000;
                            epochTime[0] = String.valueOf(startTimeePochTime);

                            Map<String, Double> uxTimerValues = new HashMap<>();

                            UxTimer.add(retriveUXTimer(id.replace("\"", ""), externalid.replace("\"", "")));
                        }
                    }
                }

            }
        }
        return UxTimer;
    }

    public static Map<String, Double> findAverageTransactions(ArrayList<String> transactionNames, ArrayList<Map<String, Double>> Days_X_AgoUxTimer, String timeRange){
        // Calculate the average value for each key
        Map<String, Double> averageMap = new HashMap<>();
        double Avgsum = 0;

        // Create a DecimalFormat object with one decimal place
        DecimalFormat decimalFormat = new DecimalFormat("#.#");

        System.out.println("************************** " + timeRange + "*********************************");
        int trasaction_count = 0;
        for (int j=0; j < transactionNames.size(); j++) {
            Avgsum = 0;
            trasaction_count = 0;
            for (int i = 0; i < Days_X_AgoUxTimer.size(); i++) {
                System.out.println("Iteration_count = " + i + "     Transaction Name = " + transactionNames.get(j).replace("\"", "") + "    TimerValue = " + Days_X_AgoUxTimer.get(i).get(transactionNames.get(j).replace("\"", "")));
                //System.out.println(Days_X_AgoUxTimer.get(i).get(transactionNames.get(j).replace("\"", "")));
//                if (Days_X_AgoUxTimer.get(i).get(transactionNames.get(j).replace("\"", "")) != 0.0) {
//                    Avgsum = Avgsum + Days_X_AgoUxTimer.get(i).get(transactionNames.get(j).replace("\"", ""));
//                    //System.out.println(Avgsum);
//                    trasaction_count++;
//                }

                if (Days_X_AgoUxTimer.get(i).get(transactionNames.get(j).replace("\"", "")) == null) {
                    Avgsum = Avgsum + 0.0;
                    //System.out.println(Avgsum);
                    //trasaction_count++;
                } else {
                    Avgsum = Avgsum + Days_X_AgoUxTimer.get(i).get(transactionNames.get(j).replace("\"", ""));
                    //System.out.println(Avgsum);
                    trasaction_count++;
                }

            }
            String formattedValue = decimalFormat.format(Avgsum/trasaction_count);
            averageMap.put(transactionNames.get(j).replace("\"", ""), Double.parseDouble(formattedValue));
            System.out.println("Total sum = " + Avgsum + "   Total_Iteration_Count = " + trasaction_count + "    Calculated Avg Value =  " + Double.parseDouble(formattedValue) + "\n");
        }
        //System.out.println("***************************  END **************************************");

        return averageMap;
    }

    private static StringBuffer APIPostRequest(String URL) {
        StringBuffer response = new StringBuffer();
        JSONObject myResponse = null;
        try {
            java.net.URL url = new URL(URL);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/json");
            //http.setRequestProperty("Authorization", "Basic " + APIkey);
            http.setRequestProperty("Content-Length", "0");

            System.out.println(http.getResponseCode() + " " + http.getResponseMessage());

            // We need to extract the job number from the returned JSON string
            BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            http.disconnect();
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return response;
        }
    }

    private static StringBuffer APIGetRequest(String URL) {
        StringBuffer response = new StringBuffer();
        JSONObject myResponse = null;
        try {
            URL url = new URL(URL);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/json");
            //http.setRequestProperty("Authorization", "Basic " + APIkey);
            http.setRequestProperty("Content-Length", "0");

            System.out.println(http.getResponseCode() + " " + http.getResponseMessage());

            // We need to extract the job number from the returned JSON string
            BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            http.disconnect();
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return response;
        }
    }
}
