package jumble.util;

import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/** Tests the RTSI class. Extremely low jumble score so this needs more work.
 * @author Tin Pavlinic
 */
public class RTSITest extends TestCase {
    public void testRTSI() throws Exception {
        Collection c = RTSI.find("jumble.util", "jumble.util.Command");
        assertEquals(2, c.size());
        assertTrue(c.contains("jumble.util.LightOff"));
        assertTrue(c.contains("jumble.util.DoorClose"));
    }
    
    public void testJarRTSI() throws Exception {
        Collection c = RTSI.find("jumble.util", "junit.framework.TestCase");

        assertTrue(c.contains("jumble.util.DummyTest"));
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(RTSITest.class);
        return suite;
      }
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
      }
}
