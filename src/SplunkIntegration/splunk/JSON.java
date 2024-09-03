package SplunkIntegration.splunk;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileReader;

public class JSON
{
	public static void main(String[] args) throws Exception
	{
		Gson gson = new Gson();
		
		// 1. JSON file to Java object
		//Object object = gson.fromJson(new FileReader("/Users/rickromanelli/Documents/QuiltDump/new/pmtl.har"), Object.class);

		JsonObject jsonObject = new Gson().fromJson(new FileReader("/Users/rickromanelli/Documents/QuiltDump/new/pmtl.har"), JsonObject.class);

		System.out.println(jsonObject.toString());
		
		JsonObject log = jsonObject.getAsJsonObject("log");
		JsonArray entriesArray = log.getAsJsonArray("entries");
		System.out.println(entriesArray);
		
		//for loop 
		String receiveTiming = ""; 
		String waitTiming = "";
		
		for (JsonElement entry:entriesArray) {
			
			JsonObject jSentry = entry.getAsJsonObject();
			JsonObject request = jSentry.getAsJsonObject("request");
			
			String url = request.get("url").getAsString();
			
			if (url.contains("ctr-00201-na-east.opencachehub.qwilted-cds")) {
				JsonObject timings = jSentry.getAsJsonObject("timings");
				receiveTiming = timings.get("receive").getAsString();
				waitTiming = timings.get("wait").getAsString();
				
				break;
			}
		}
		System.out.println(receiveTiming + " " + waitTiming);
		
		
			}
}
