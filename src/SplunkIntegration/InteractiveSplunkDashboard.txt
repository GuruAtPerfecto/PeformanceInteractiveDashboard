package SplunkIntegration;

import SplunkIntegration.pages.CommonFunctions;
import SplunkIntegration.splunk.SplunkReportingCollector;
import org.testng.ITestContext;
import org.testng.annotations.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;



public class InteractiveSplunkDashboard {

  public SplunkReportingCollector reporting;

  private Map<String, String> parameters;
  private static final String CLOUD_NAME = System.getProperty("CLOUD_NAME"); // TODO put your Continuous Quality

  @Test
  //@Parameters({"cloudName", "scriptKey", "DUT", "testName", "appPath", "backgroundAppList", "networkProfile"})
  public void updateSplunkDashboard() throws InterruptedException, URISyntaxException, IOException, ParserConfigurationException, SAXException {
    //String executionId = parameters.get("e_Id");
    String executionId = null;
    if (parameters.get("executeTest").contains("true")) {
      executionId = CommonFunctions.initiateScriptlessTest(parameters);
      CommonFunctions.waitForExecutionToComplete(CLOUD_NAME, executionId);
      Thread.sleep(20000);
      CommonFunctions.retrieveAndDeployTransactionTimes(this.reporting, executionId, parameters.get("DUT"));
    } else {
      CommonFunctions.retrieveAndDeployTransactionTimes(this.reporting, parameters.get("testName"), parameters.get("DUT"));
    }
    //CommonFunctions.retrieveAndSubmitHARAnalysis(this.reporting, executionId, parameters.get("DUT"));
  }

  @BeforeTest
  public void beforeTest(ITestContext context) {
    parameters = context.getCurrentXmlTest().getAllParameters();
  }

  @AfterTest
  public void afterTest() throws Exception {
  }

  @BeforeClass
  public void beforeClass() {
    reporting = CommonFunctions.setSplunk();
    CommonFunctions.setDetails(this.reporting, "pass");
  }

  @AfterClass
  public void afterClass() throws Exception {
    System.out.println(this.reporting.commitSplunk(this.reporting, parameters));
  }
}
