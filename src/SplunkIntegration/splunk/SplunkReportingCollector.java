package SplunkIntegration.splunk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SplunkReportingCollector extends Reporting {
	public static ArrayList<HashMap> reportCollector = new ArrayList<HashMap>();
	public HashMap<String, Object> reportFinal = new HashMap<String, Object>();
	//public static ArrayList<HashMap> reportFinalArray = new ArrayList<HashMap>();
	


	public void addReport(Object o) {
		reportCollector.add((HashMap) o);
	}

	// initializing SplunkIntegration.splunk connection values
	public SplunkReportingCollector(long sla, String splunkScheme, String splunkHost, String splunkPort,
			String splunkToken) {
		super(sla, splunkScheme, splunkHost, splunkPort, splunkToken);
	}
	
	public SplunkReportingCollector(long sla, String splunkScheme, String splunkHost, String splunkPort,
			String splunkToken, Proxy proxy) {
		super(sla, splunkScheme, splunkHost, splunkPort, splunkToken, proxy);
	}
	
	public SplunkReportingCollector(long sla, String splunkScheme, String splunkHost, String splunkPort,
			String splunkToken, String splunkChannel) {
		super(sla, splunkScheme, splunkHost, splunkPort, splunkToken, splunkChannel);
	}
	
	public SplunkReportingCollector(long sla, String splunkScheme, String splunkHost, String splunkPort,
			String splunkToken, String splunkChannel, Proxy proxy) {
		super(sla, splunkScheme, splunkHost, splunkPort, splunkToken, splunkChannel, proxy);
	}


	public String commitSplunk(SplunkReportingCollector reporting, Map<String, String> parameters) throws Exception {
		Gson gson = new GsonBuilder().enableComplexMapKeySerialization().disableHtmlEscaping().serializeNulls()
				.create();
		gson = new GsonBuilder().enableComplexMapKeySerialization().disableHtmlEscaping().serializeNulls().create();

		//System.out.println(parameters.get("deviceModel") + "_" + parameters.get("DUT") + reporting.reporting);
		reportFinal.put(parameters.get("deviceModel") + "_" + parameters.get("DUT"), reporting.reporting);

		// converts the maps to a final readable json string
		String jsonReport = gson.toJson(reportFinal);

		if (splunk.getSplunkHost() != null) {
			this.splunk.splunkFeed(jsonReport);
		}

		return jsonReport;
	}
	// merges and sorts the various maps to create the json and finally submit
	// them to SplunkIntegration.splunk
	public String commitSplunk(Map<String, String> parameters) throws Exception {
		int secondaryCount = 1;
		Gson gson = new GsonBuilder().enableComplexMapKeySerialization().disableHtmlEscaping().serializeNulls()
				.create();
				gson = new GsonBuilder().enableComplexMapKeySerialization().disableHtmlEscaping().serializeNulls().create();

		for (int i = 0; i < this.reportCollector.size(); i++) {
			String jsonCompare = gson.toJson(this.reportCollector.get(i));
			
			Object oldValue = this.reportCollector.get(i).get("PerfectoTest");

			if (i == (this.reportCollector.size() - 1)) {
//				if (!reportFinal.containsKey("Primary")) {
//					reportFinal.put("Primary", oldValue);
//				} else {
//					reportFinal.put("Secondary" + secondaryCount, oldValue);
//					secondaryCount = secondaryCount + 1;
//				}
				reportFinal.put(parameters.get("deviceModel") + "_" + parameters.get("DUT"), oldValue);

			}
//			else if (jsonCompare.contains("\"testStatus\":\"Fail\"")) {
//				reportFinal.put("Secondary" + secondaryCount, oldValue);
//				secondaryCount = secondaryCount + 1;
//			} else if (jsonCompare.contains("\"testStatus\":\"Skip\"")) {
//				reportFinal.put("Secondary" + secondaryCount, oldValue);
//				secondaryCount = secondaryCount + 1;
//
//			} else if (!reportFinal.containsKey("Primary")) {
//				reportFinal.put("Primary", oldValue);
//			} else {
//				reportFinal.put("Secondary", oldValue);
//				secondaryCount = secondaryCount + 1;
//			}
//
//			if (jsonCompare.contains("\"testStatus\":\"Skip\"")) {
//				reportFinal.put("Secondary" + secondaryCount, oldValue);
//				secondaryCount = secondaryCount + 1;
//			}
		}

		// converts the maps to a final readable json string
		String jsonReport = gson.toJson(reportFinal);
		//String jsonReport = "{\"Primary1\":{\"Steps\":[],\"hostName\":\"gbm0122\",\"statusText_DepositAmount_LastExecution\":\"\",\"Avg_StartApplication_Transaction_7Days_Ago\":450.0, \"Avg_DUMMY_Transaction_7Days_Ago\":450.0,\"HTTPStatus_DepositAmount_LastExecution\":200,\"ssl_DepositAmount_LastExecution\":\"-1\",\"Avg_StartApplication_Transaction_Yesterday\":450.0, \"Avg_DUMMY_Transaction_Today\":650.0, \"Avg_DUMMY_Transaction_Yesterday\":750.0, \"Avg_DUMMY_Transaction_7Days_Ago\":450.0, \"Avg_ATMLookup_Transaction_7Days_Ago\":425.0,\"Latency_ATMLookup_LastExecution\":102,\"statusText_LoginApp_LastExecution\":\"\",\"ATMLookup_FullTTFB_LastExecution\":168,\"connect_ATMLookup_LastExecution\":13,\"LoginApp_Transaction_LastExecution\":11111.0,\"LoginApp_FullTTFB_LastExecution\":157,\"HTTPStatus_LoginApp_LastExecution\":200,\"LoginApp_TTFB_LastExecution\":119,\"ssl_LoginApp_LastExecution\":\"-1\",\"ATMLookup_Transaction_LastExecution\":12200,\"Avg_DepositAmount_Transaction_7Days_Ago\":2600.0,\"statusText_ATMLookup_LastExecution\":\"\",\"StartApplication_Transaction_LastExecution\":13100.0,\"send_DepositAmount_LastExecution\":1,\"testStatus\":\"pass\",\"receiveTiming1_LoginApp_LastExecution\":1,\"Avg_LoginApp_Transaction_7Days_Ago\":950.0,\"receiveTiming1_DepositAmount_LastExecution\":1,\"send_ATMLookup_LastExecution\":1,\"Avg_LoginApp_Transaction_Today\":950.0,\"Avg_StartApplication_Transaction_Today\":450.0,\"Latency_LoginApp_LastExecution\":103,\"Avg_ATMLookup_Transaction_Yesterday\":400.0,\"receiveTiming1_ATMLookup_LastExecution\":1,\"connect_LoginApp_LastExecution\":14,\"connect_DepositAmount_LastExecution\":13,\"Avg_ATMLookup_Transaction_Today\":400.0,\"DepositAmount_TTFB_LastExecution\":117,\"ssl_ATMLookup_LastExecution\":\"-1\",\"Avg_LoginApp_Transaction_Yesterday\":950.0,\"ATMLookup_TTFB_LastExecution\":117,\"Avg_DepositAmount_Transaction_Yesterday\":2600.0,\"DepositAmount_Transaction_LastExecution\":14100.0,\"Avg_DepositAmount_Transaction_Today\":2600.0,\"DepositAmount_FullTTFB_LastExecution\":451,\"HTTPStatus_ATMLookup_LastExecution\":200,\"send_LoginApp_LastExecution\":1,\"StartApplication_FullTTFB_LastExecution\":0,\"Latency_DepositAmount_LastExecution\":102},\n" +
		//		"    \"Primary2\":{\"Steps\":[],\"hostName\":\"gbm0122\",\"statusText_DepositAmount_LastExecution\":\"\",\"Avg_StartApplication_Transaction_7Days_Ago\":450.0, \"Avg_DUMMY_Transaction_7Days_Ago\":450.0,\"HTTPStatus_DepositAmount_LastExecution\":200,\"ssl_DepositAmount_LastExecution\":\"-1\",\"Avg_StartApplication_Transaction_Yesterday\":450.0, \"Avg_DUMMY_Transaction_Today\":650.0, \"Avg_DUMMY_Transaction_Yesterday\":750.0, \"Avg_DUMMY_Transaction_7Days_Ago\":450.0, \"Avg_ATMLookup_Transaction_7Days_Ago\":425.0,\"Latency_ATMLookup_LastExecution\":102,\"statusText_LoginApp_LastExecution\":\"\",\"ATMLookup_FullTTFB_LastExecution\":168,\"connect_ATMLookup_LastExecution\":13,\"LoginApp_Transaction_LastExecution\":21100.0,\"LoginApp_FullTTFB_LastExecution\":157,\"HTTPStatus_LoginApp_LastExecution\":200,\"LoginApp_TTFB_LastExecution\":119,\"ssl_LoginApp_LastExecution\":\"-1\",\"ATMLookup_Transaction_LastExecution\":22000.0,\"Avg_DepositAmount_Transaction_7Days_Ago\":2600.0,\"statusText_ATMLookup_LastExecution\":\"\",\"StartApplication_Transaction_LastExecution\":23345.0,\"send_DepositAmount_LastExecution\":1,\"testStatus\":\"pass\",\"receiveTiming1_LoginApp_LastExecution\":1,\"Avg_LoginApp_Transaction_7Days_Ago\":950.0,\"receiveTiming1_DepositAmount_LastExecution\":1,\"send_ATMLookup_LastExecution\":1,\"Avg_LoginApp_Transaction_Today\":950.0,\"Avg_StartApplication_Transaction_Today\":450.0,\"Latency_LoginApp_LastExecution\":103,\"Avg_ATMLookup_Transaction_Yesterday\":400.0,\"receiveTiming1_ATMLookup_LastExecution\":1,\"connect_LoginApp_LastExecution\":14,\"connect_DepositAmount_LastExecution\":13,\"Avg_ATMLookup_Transaction_Today\":400.0,\"DepositAmount_TTFB_LastExecution\":117,\"ssl_ATMLookup_LastExecution\":\"-1\",\"Avg_LoginApp_Transaction_Yesterday\":950.0,\"ATMLookup_TTFB_LastExecution\":117,\"Avg_DepositAmount_Transaction_Yesterday\":2600.0,\"DepositAmount_Transaction_LastExecution\":24100.0,\"Avg_DepositAmount_Transaction_Today\":2600.0,\"DepositAmount_FullTTFB_LastExecution\":451,\"HTTPStatus_ATMLookup_LastExecution\":200,\"send_LoginApp_LastExecution\":1,\"StartApplication_FullTTFB_LastExecution\":0,\"Latency_DepositAmount_LastExecution\":102} }";

		// submits the values to SplunkIntegration.splunk unless the SplunkIntegration.splunk host is Null
		// setting SplunkIntegration.splunk host to null allows for the generation of the Json
		// without the need of connecting to SplunkIntegration.splunk
		if (splunk.getSplunkHost() != null) {
			this.splunk.splunkFeed(jsonReport);
		}

		// returns the json string for logging or additional tasks
		
		return jsonReport;
	}

	// stores the individual reports into the json collector
	public void submitReporting(String testName) {
		this.steps.put("Steps", this.stepCollector);
		this.reporting.put("Steps", this.stepCollector);
		Gson gson = new GsonBuilder().enableComplexMapKeySerialization().disableHtmlEscaping().serializeNulls()
				.create();


		String stepsJson = gson.toJson(this.steps);
		if (stepsJson.contains("Fail")) {
			this.reporting.put("performanceStatus", "Fail");
		} else if (stepsJson.contains("Pass")) {
			this.reporting.put("performanceStatus", "Pass");
		}

		this.reportingResults.put("PerfectoTest", this.reporting);

		addReport(this.reportingResults);
	}
}
