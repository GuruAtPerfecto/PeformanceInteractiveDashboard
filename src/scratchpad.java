import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class scratchpad {

    public static void main(String[] args) {
        // Sample data (this would be your ArrayList<Map<String, Double>>)
        List<Map<String, Double>> list = new ArrayList<>();

        // Creating 3 elements with sample data
        for (int i = 0; i < 3; i++) {
            Map<String, Double> map = new HashMap<>();
            map.put("More", 700.0);
            map.put("LogOut", 1100.0);
            map.put("testStartTime_ePoch", 1.725216810097E12 + i * 1E11); // Slightly different epoch values for demonstration
            map.put("loginApp", 20300.0);
            map.put("PageRefresh", 8300.0);
            map.put("My Requests", 2900.0);
            map.put("EnterPassword", 8800.0);
            map.put("Settings", 1100.0);
            map.put("OpenPerfInstance", 0.0);
            list.add(map);
        }

        // JSON object to hold the final structure
        JSONObject jsonObject = new JSONObject();

        // Loop through each map in the list
        for (Map<String, Double> map : list) {
            Double testStartTime_ePoch = map.get("testStartTime_ePoch");
            JSONObject innerObject = new JSONObject();

            // Iterate through the map to populate the inner JSON object
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                if (!entry.getKey().equals("testStartTime_ePoch")) {
                    innerObject.put(entry.getKey(), entry.getValue());
                }
            }

            // Place the inner JSON object under the testStartTime_ePoch key
            jsonObject.put(testStartTime_ePoch.toString(), innerObject);
        }

        // Now jsonObject holds the JSON structure you can traverse later
        // Example: you can print it or access elements as needed
        System.out.println(jsonObject.toString(4)); // Pretty print with indentation of 4 spaces
    }
}
