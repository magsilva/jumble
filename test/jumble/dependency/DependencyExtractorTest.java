/*
 * Created on May 1, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jumble.dependency;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;

import jumble.util.JavaRunner;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the DependencyExtractor
 * 
 * @author Tin Pavlinic
 */
public class DependencyExtractorTest extends TestCase {
  private DependencyExtractor extractor;

  public void setUp() {
    extractor = new DependencyExtractor();
  }

  public void testDT1() {
    Collection classes = extractor.getAllDependencies("jumble.dependency.DT1", true);
    assertEquals("experiments.JumblerExperiment", classes.iterator().next());
    assertEquals(1, classes.size());
  }

  public void testDT2() {
    extractor.setClassName("jumble.dependency.DT2");
    Collection classes = extractor.getAllDependencies("jumble.dependency.DT2", true);
    assertEquals(3, classes.size());

    assertTrue(classes.contains("experiments.JumblerExperiment"));
    assertTrue(classes.contains("jumble.dependency.DT1"));
    assertTrue(classes.contains("jumble.dependency.DT3"));
  }

   public void testDT3() {
     Collection classes = extractor.getAllDependencies("jumble.dependency.DT3", true);
     assertEquals(2, classes.size());

     assertTrue(classes.contains("experiments.JumblerExperiment"));
     assertTrue(classes.contains("jumble.dependency.DT1"));
   }
   
   public void testDT4() {
     Collection classes = extractor.getAllDependencies("jumble.dependency.DT4", true);
     assertEquals(2, classes.size());

     assertTrue(classes.contains("experiments.JumblerExperiment"));
     assertTrue(classes.contains("jumble.dependency.DT1"));
   }

   public void testDT5() {
     Collection classes = extractor.getAllDependencies("jumble.dependency.DT5", true);
     assertEquals(2, classes.size());

     assertTrue(classes.contains("experiments.JumblerExperiment"));
     assertTrue(classes.contains("jumble.dependency.DT1"));
   }
   
   public void testDT6() {
     Collection classes = extractor.getAllDependencies("jumble.dependency.DT6", true);
     assertEquals(2, classes.size());

     assertTrue(classes.contains("experiments.JumblerExperiment"));
     assertTrue(classes.contains("jumble.dependency.DT1"));
   }
   
   public void testDT7() {
     Collection classes = extractor.getAllDependencies("jumble.dependency.DT7", true);
     assertEquals(2, classes.size());

     assertTrue(classes.contains("experiments.JumblerExperiment"));
     assertTrue(classes.contains("jumble.dependency.DT1"));
   }
   
   public void testDT8() {
     Collection classes = extractor.getAllDependencies("jumble.dependency.DT8", true);
     assertEquals(2, classes.size());

     assertTrue(classes.contains("experiments.JumblerExperiment"));
     assertTrue(classes.contains("jumble.dependency.DT1"));
   }
  public void testNotFiltered() {
    // huge amount of dependencies - this is more of a regression thing
    // than an actual test - I have no idea what the value should be
    // may vary with different JRE's (although hasn't done so far)
    Collection classes = extractor.getAllDependencies("jumble.dependency.DT1", false);
    assertTrue(1000 < classes.size());
  }

  public void testSilly() {
    extractor.setClassName("[[C");
    Collection classes = extractor.getAllDependencies("[[C", true);
    assertEquals(0, classes.size());
  }
  
  public void testMainNormal() throws Exception {
    JavaRunner runner = new JavaRunner("jumble.dependency.DependencyExtractor");
    runner.setArguments(new String[] {"jumble.dependency.DT2"});
    
    Process p = runner.start();
    
    BufferedReader in = new BufferedReader
      (new InputStreamReader(p.getInputStream()));
    BufferedReader err = new BufferedReader
      (new InputStreamReader(p.getInputStream()));

    assertEquals("Dependencies for jumble.dependency.DT2", in.readLine().trim());
    assertEquals("", in.readLine().trim());
    assertEquals("experiments.JumblerExperiment", in.readLine().trim());
    assertEquals("jumble.dependency.DT3", in.readLine().trim());
    assertEquals("jumble.dependency.DT1", in.readLine().trim());
    assertEquals(null, in.readLine());
    assertEquals(null, err.readLine());
    p.destroy();
  }
  
  public void testMainIgnore() throws Exception {
    JavaRunner runner = new JavaRunner("jumble.dependency.DependencyExtractor");
    runner.setArguments(new String[] {"experiments.JumblerExperimentTest", 
        "-i", "junit,java"});
    
    Process p = runner.start();

    BufferedReader in = new BufferedReader
      (new InputStreamReader(p.getInputStream()));
    BufferedReader err = new BufferedReader
      (new InputStreamReader(p.getInputStream()));

    assertEquals("Dependencies for experiments.JumblerExperimentTest", in.readLine().trim());
    assertEquals("", in.readLine().trim());
    assertEquals("experiments.JumblerExperiment", in.readLine().trim());
    assertEquals(null, in.readLine());
    assertEquals(null, err.readLine());
    p.destroy();
  }
  public static Test suite() {
    TestSuite suite = new TestSuite(DependencyExtractorTest.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
  
  public void testIsPrimitive() {
    assertTrue(DependencyExtractor.isPrimitiveArray("[[C"));
    assertFalse(DependencyExtractor.isPrimitiveArray
        ("[java.util.LinkedList"));
  }
}
