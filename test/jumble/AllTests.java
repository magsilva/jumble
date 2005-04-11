package jumble;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests.
 *
 * @author Sean A. Irvine
 * @version $Revision$
 */
public class AllTests extends TestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(JumbleTestSuiteTest.suite());
    suite.addTest(MutaterTest.suite());
    //suite.addTest(TimeOutTest.suite());
    return suite;
  }


  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
