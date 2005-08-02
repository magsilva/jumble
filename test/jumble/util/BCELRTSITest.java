package jumble.util;

import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

public class BCELRTSITest extends TestCase {
  public void testBCELRTSI() throws Exception {
    Collection c = BCELRTSI.getAllDerivedClasses("jumble.util.Command", "jumble.util", false);
    assertEquals(2, c.size());
    assertTrue(c.contains("jumble.util.LightOff"));
    assertTrue(c.contains("jumble.util.DoorClose"));
    
  }

  public void testBCELJarRTSI() throws Exception {
    Collection c = BCELRTSI.getAllDerivedClasses("junit.framework.TestCase", "jumble.util", true);
    assertTrue(c.contains("jumble.util.DummyTest"));
  }

  public void testGetAllClasses() {
    Collection c = BCELRTSI.getAllDerivedClasses("jumble.util.Command", false);
    assertEquals(2, c.size());
    assertTrue(c.contains("jumble.util.LightOff"));
    assertTrue(c.contains("jumble.util.DoorClose"));
  }
  
  
  public void testInstanceOf() {
    checkInstance("java.util.LinkedList", "java.util.Collection");
    checkInstance("java.util.LinkedList", "java.lang.Object");
    
    checkNonInstance("java.lang.Object", "java.util.LinkedList");
    checkNonInstance("java.util.LinkedList", "java.lang.String");
    
  }
  
  
  private void checkNonInstance(String classA, String classB) {
    JavaClass a = Repository.lookupClass(classA);
    JavaClass b = Repository.lookupClass(classB);
    assert a != null;
    assert b != null;
    assertFalse(BCELRTSI.instanceOf(a, b));
  }
  
  private void checkInstance(String classA, String classB) {
    JavaClass a = Repository.lookupClass(classA);
    JavaClass b = Repository.lookupClass(classB);
    assert a != null;
    assert b != null;
    assertTrue(BCELRTSI.instanceOf(a, b));
  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite(BCELRTSITest.class);
    return suite;
  }
  

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
