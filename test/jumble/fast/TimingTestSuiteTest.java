package jumble.fast;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;
import junit.framework.TestResult;
import experiments.TimedTests;

/**
 * Tests the corresponding class
 * 
 * @author Tin Pavlinic
 */
public class TimingTestSuiteTest extends TestCase {
  private TimingTestSuite mSuite;

  private TestResult mResult;

  protected void setUp() throws Exception {
    mSuite = new TimingTestSuite(new Class[] { TimedTests.class});
    mResult = new TestResult();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(new ByteArrayOutputStream()));
    mSuite.run(mResult);
    System.setOut(oldOut);
  }

  public final void testGetOrder() {
    TestOrder order = mSuite.getOrder();
    assertEquals(3, order.getTestCount());
    assertEquals(2, order.getTestIndex(0));
    assertEquals(0, order.getTestIndex(1));
    assertEquals(1, order.getTestIndex(2));
    //A bit dangerous
    assertEquals(11000, order.getTotalRuntime());
  }

  public final void testResult() {
    assertEquals(3, mResult.runCount());
    assertEquals(0, mResult.errorCount());
    assertEquals(0, mResult.failureCount());
  }
}
