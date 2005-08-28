package jumble.fast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.StringTokenizer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.bcel.util.ClassLoader;

import experiments.JumblerExperimentSecondTest;
import experiments.JumblerExperimentTest;

/**
 * Tests the corresponding class.
 * 
 * @author Tin Pavlinic
 */
public class TestOrderTest extends TestCase {
  private TestOrder mOrder;

  public void setUp() {
    Class[] classes = new Class[] { JumblerExperimentTest.class,
        JumblerExperimentSecondTest.class };

    long[] runtimes = new long[] { 300, 200, 500, 100, 400 };
    mOrder = new TestOrder(classes, runtimes);

  }

  public final void testGetTestCount() {
    assertEquals(5, mOrder.getTestCount());
  }

  public final void testGetTestIndex() {
    assertEquals(3, mOrder.getTestIndex(0));
    assertEquals(1, mOrder.getTestIndex(1));
    assertEquals(0, mOrder.getTestIndex(2));
    assertEquals(4, mOrder.getTestIndex(3));
    assertEquals(2, mOrder.getTestIndex(4));
  }

  public final void testGetTestClasses() {
    String[] classes = mOrder.getTestClasses();
    assertEquals("experiments.JumblerExperimentTest", classes[0]);
    assertEquals("experiments.JumblerExperimentSecondTest", classes[1]);
    assertEquals(2, classes.length);
  }

  public final void testGetRuntime() {
    for (int i = 0; i < mOrder.getTestCount(); i++) {
      assertEquals((i + 1) * 100, mOrder.getRuntime(i));
    }
  }

  public final void testGetTotalRuntime() {
    assertEquals(1500, mOrder.getTotalRuntime());
  }

  public final void testSavingAndLoading() throws Exception {
    // Write the object
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
        "loadAndSaveTestOrder.tmp"));
    out.writeObject(mOrder);
    out.close();
    // Read the object
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(
        "loadAndSaveTestOrder.tmp"));
    mOrder = (TestOrder) in.readObject();
    in.close();
    // delete the temporary file
    assertTrue(new File("loadAndSaveTestOrder.tmp").delete());

    // run the tests again
    testGetTestCount();
    testGetTestIndex();
    testGetTestClasses();
    testGetRuntime();
    testGetTotalRuntime();
  }

  public final void testChangeClassLoader() throws Exception {
    Object newOrder = mOrder.changeClassLoader(new ClassLoader());
    assertNotSame(mOrder.getClass(), newOrder.getClass());
  }

  public final void testToString() {
    String str = mOrder.toString();

    StringTokenizer tokens = new StringTokenizer(str);
    assertEquals("100", tokens.nextToken());
    assertEquals("200", tokens.nextToken());
    assertEquals("300", tokens.nextToken());
    assertEquals("400", tokens.nextToken());
    assertEquals("500", tokens.nextToken());
  }

  public final void testDropOrder() {
    mOrder.dropOrder();

    for (int i = 0; i < 5; i++) {
      assertEquals(i, mOrder.getTestIndex(i));
    }
    
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestOrderTest.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
