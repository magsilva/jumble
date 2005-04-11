package experiments;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JumblerExperimentTest extends TestCase {
    private JumblerExperiment exp = new JumblerExperiment();

    public void testAdd() {
	assertEquals(3,exp.add(2,1));
	//assertEquals(1, exp.add(1,2));
	
    }
    
    public static Test suite() {
	TestSuite suite = new TestSuite(JumblerExperimentTest.class);
	return suite;
    }

    public static void main(String[] args) {
	junit.textui.TestRunner.run(suite());
  }
}