package jumble.dependency;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class AllTests extends TestSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(DependencyExtractorTest.suite());
    return suite;
  }
  
  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
