package jumble.fast;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 * A test suite which runs tests in order and inverts the result. Remembers the
 * tests that failed last time and does those first.
 * 
 * @author Tin
 */
public class JumbleTestSuite extends FlatTestSuite {
  /** Cache of previously failed tests */
  private FailedTestMap mCache;

  /** Order of tests */
  private TestOrder mOrder;

  /** Mutated class */
  private String mClass;

  /** Mutated method */
  private String mMethod;

  /** Mutation Point */
  private int mMethodRelativeMutationPoint;

  /** Should we surpress output? */
  private boolean mStopOutput = true;

  /**
   * Constructs test suite from the given order of tests.
   * 
   * @param order
   *          the order to run the tests in
   * @throws ClassNotFoundException
   *           if <CODE>order</CODE> is malformed.
   */
  public JumbleTestSuite(TestOrder order, FailedTestMap cache,
      String mutatedClass, String mutatedMethod, int mutationPoint,
      boolean stopOut) throws ClassNotFoundException {
    super();
    mCache = cache;
    mOrder = order;
    mClass = mutatedClass;
    mMethod = mutatedMethod;
    mStopOutput = stopOut;
    mMethodRelativeMutationPoint = mutationPoint;

    // Create the test suites from the order
    String[] classNames = mOrder.getTestClasses();
    for (int i = 0; i < classNames.length; i++) {
      addTestSuite(Class.forName(classNames[i]));
    }
  }

  /**
   * Runs the tests returning the result as a string. If any of the individual
   * tests fail then the run is aborted and "PASS" is returned (recall with a
   * mutation we expect the test to fail). If all tests run correctly then
   * "FAIL" is returned.
   */
  protected String run() {
    final TestResult result = new TestResult();
    Test[] tests = getOrder();
    PrintStream newOut;
    PrintStream oldOut = System.out;
    if (mStopOutput) {
      newOut = new PrintStream(new ByteArrayOutputStream());
    } else {
      newOut = oldOut;
    }
    FileWriter f;
    try {
      f = new FileWriter("jumble-debug.txt", true);
    } catch (IOException e) {
      f = null;
    }
    PrintWriter out = new PrintWriter(f);
    for (int i = 0; i < testCount(); i++) {
      TestCase t = (TestCase) tests[i];
      out.println(mClass + "." + mMethod + ":" + mMethodRelativeMutationPoint
          + " - " + t);

      System.setOut(newOut);
      t.run(result);
      System.setOut(oldOut);
      if (result.errorCount() > 0 || result.failureCount() > 0) {
        out.close();
        return "PASS: " + mClass + ":" + mMethod + ":"
            + mMethodRelativeMutationPoint + ":" + t.getName();
      }
      if (result.shouldStop()) {
        break;
      }
    }
    out.close();
    // all tests passed, this mutation is a problem, report it
    // this is made complicated because we must get the modification
    // details from a class loaded in a different name space

    return "FAIL: " + getMessage();
  }

  private String getMessage() {
    String message;
    try {
      message = (String) getClass().getClassLoader().getClass().getMethod(
          "getModification", null).invoke(getClass().getClassLoader(), null);
    } catch (IllegalAccessException e) {
      message = "!!!" + e.getMessage();
    } catch (IllegalArgumentException e) {
      message = "!!!" + e.getMessage();
    } catch (InvocationTargetException e) {
      message = "!!!" + e.getMessage();
    } catch (NoSuchMethodException e) {
      message = "!!!" + e.getMessage();
    } catch (SecurityException e) {
      message = "!!!" + e.getMessage();
    }
    if (message == null) {
      message = "none: existing tests never caused class to be loaded";
    }
    return message;
  }

  /**
   * Run the tests for the given class.
   * 
   * @param order
   *          the order in which to run the tests.
   * @param mutatedClassName
   *          the name of the class which was mutated
   * @param muatatedMethodName
   *          the name of the method which was mutated
   * @param relativeMutationPoint
   *          the mutation point location relative to the mutated method
   * @param loadLast
   *          flag indicating whether to load the cache friom a file
   * @param saveLast
   *          flag indicating whether to save the new cache to a file
   * @param useCache
   *          flag indicating whether to actually use the cache. This must be
   *          enabled if we want to save or load the cache.
   * @param surpressOutput
   *          flag whether to surpress output during the test run. Should be
   *          <CODE>true</CODE> for all Jumble runs.
   * @see TestOrder
   */
  public static String run(TestOrder order, FailedTestMap cache,
      String mutatedClassName, String mutatedMethodName,
      int relativeMutationPoint, boolean supressOutput) {
    try {
      JumbleTestSuite suite = new JumbleTestSuite(order, cache,
          mutatedClassName, mutatedMethodName, relativeMutationPoint,
          supressOutput);
      String ret = suite.run();

      return ret;
    } catch (ClassNotFoundException e) {
      return "FAIL: No test class: " + e.getMessage();
    }
  }

  /**
   * Basically separates out the tests for the current method so that they are
   * run first. Still keeps them in the same order.
   * 
   * @return array of tests in the order of timing but the ones that failed
   *         previously get run first.
   */
  private Test[] getOrder() {
    Test first = null;
    String firstTestName = null;
    Set frontTestNames = new HashSet();

    if (mCache != null) {
      firstTestName = mCache.getLastFailure(mClass, mMethod,
          mMethodRelativeMutationPoint);
      frontTestNames = mCache.getFailedTests(mClass, mMethod);
    }

    List front = new ArrayList();
    List back = new ArrayList();

    for (int i = 0; i < testCount(); i++) {
      TestCase curTest = (TestCase) testAt(mOrder.getTestIndex(i));

      if (first != null && curTest.getName().equals(firstTestName)) {
        first = curTest;
      } else if (frontTestNames.contains(curTest.getName())) {
        front.add(curTest);
      } else {
        back.add(curTest);
      }
    }

    Test[] ret = new Test[testCount()];

    int i;

    if (first == null) {
      i = 0;
    } else {
      i = 1;
      ret[0] = first;
    }

    for (int j = 0; j < front.size(); j++) {
      ret[i] = (Test) front.get(j);
      i++;
    }
    for (int j = 0; j < back.size(); j++) {
      ret[i] = (Test) back.get(j);
      i++;
    }
    return ret;
  }
}