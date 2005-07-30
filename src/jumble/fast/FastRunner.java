package jumble.fast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import jumble.Mutater;
import jumble.Mutation;
import jumble.util.IOThread;
import jumble.util.JavaRunner;
import jumble.util.Utils;
import junit.framework.TestResult;

/**
 * A runner for the <CODE>FastJumbler</CODE>. Runs the FastJumbler in a new
 * JVM and detects timeouts. Consists mostly of static methods.
 * 
 * @author Tin Pavlinic
 *  
 */
public class FastRunner {
  /**
   * Main method.
   * 
   * @param args
   *          command line arguments. format is
   * 
   * <PRE>
   * 
   * java jumble.fast.FastRunner [OPTIONS] [CLASS] [TESTS]
   * 
   * CLASS the fully-qualified name of the class to mutate.
   * 
   * TESTS a list of test names to run on this class
   * 
   * OPTIONS -r Mutate return values. -k Mutate inline constants. -i Mutate
   * increments. -x Exclude specified methods. -h Display this help message.
   * -o Name of the class responsible for producing output.
   * 
   * </PRE>
   */
  public static void main(String[] args) throws Exception {
    try {
      //Process arguments
      boolean help = Utils.getFlag('h', args);
      String excludes = Utils.getOption('x', args);
      boolean constants = Utils.getFlag('k', args);
      boolean returns = Utils.getFlag('r', args);
      boolean increments = Utils.getFlag('i', args);
      boolean finishedTests = false;
      String outputClass = Utils.getOption('o', args);
      

      String className;
      List testList;
      Set excludeMethods = new HashSet();
      
      StringTokenizer tokens = new StringTokenizer(excludes, ",");
      
      while (tokens.hasMoreTokens()) {
        excludeMethods.add(tokens.nextToken());
      }
      
      if (help) {
        printUsage();
        return;
      }

      className = Utils.getNextArgument(args);
      testList = new ArrayList();

      //We need at least one test
      testList.add(Utils.getNextArgument(args));

      while (!finishedTests) {
        try {
          testList.add(Utils.getNextArgument(args));
        } catch (NoSuchElementException e) {
          finishedTests = true;
        }
      }
      
      JumbleResult res = runJumble(className, testList, 
          excludeMethods, constants, returns, increments);
      JumbleResultPrinter printer = getPrinter(outputClass);
      printer.printResult(res);
      

    } catch (Exception e) {
      printUsage();
      System.out.println();
      e.printStackTrace(System.out);
    }
  }

  /**
   * Returns a result printer instance as specified by 
   * <CODE>className</CODE>. The printer is constructed with 
   * <CODE>System.out</CODE> as the argument. If this is not allowed, then
   * the no arguments constructor is ibvoked.
   * @param className name of result printer class.
   * @return a <CODE>JumbleResultPrinter</CODE> instance.
   */
  private static JumbleResultPrinter getPrinter(String className) {
    try {
      Class clazz = Class.forName(className);
      
      try {
        Constructor c = clazz.getConstructor(new Class[] {PrintStream.class});
        return (JumbleResultPrinter)c.newInstance(new Object[] {System.out});
      } catch (Exception e) {
        try {
          Constructor c = clazz.getConstructor(new Class[0]);
          return (JumbleResultPrinter)c.newInstance(new Object[0]);
        } catch (Exception ex) {
          System.err.println("Invalid output class. Exception: ");
          e.printStackTrace();
          return new SeanResultPrinter(System.out);
        }
      }
    } catch (ClassNotFoundException e) {
      return new SeanResultPrinter(System.out);
    }
  }
  
  /**
   * Prints command line usage help for this class.
   */
  private static void printUsage() {
    System.out.println("Usage:");
    System.out.println("java jumble.fast.FastRunner [OPTIONS] [CLASS] [TESTS]");
    System.out.println();

    System.out
        .println("CLASS the fully-qualified name of the class to mutate.");
    System.out.println();
    System.out.println("TESTS a test suite file containing the tests.");
    System.out.println();
    System.out.println("OPTIONS");
    System.out.println("         -r Mutate return values.");
    System.out.println("         -k Mutate inline constants.");
    System.out.println("         -i Mutate increments.");
    System.out.println("         -x Exclude specified methods. ");
    System.out.println("         -h Display this help message.");
  }

  /**
   * A function which computes the timeout for given that the original test took
   * <CODE>runtime</CODE>
   * 
   * @param runtime
   *          the original runtime
   * @return the computed timeout
   */
  public static long computeTimeout(long runtime) {
    return runtime * 10 + 2000;
  }

  /**
   * Performs a Jumble run on the specified class with the specified tests.
   * 
   * @param className
   *          the name of the class to Jumble
   * @param testClassNames
   *          the names of the associated test classes
   * @param excludeMethods
   *          a <CODE>Set</CODE> of strings containing the names of the
   *          methods which are excluded from the jumble run.
   * @param inlineConstants
   *          flag indicating whether to mutate inline constants
   * @param returnVals
   *          flag indicating whether to mutate return values.
   * @param increments
   *          flag indicating whether to mutate increment instructions.
   * @return the results of the Jumble run
   * @throws Exception
   *           if something goes wrong
   * @see JumbleResult
   */
  public static JumbleResult runJumble(final String className,
      final List testClassNames, final Set excludeMethods,
      final boolean inlineConstants, final boolean returnVals,
      final boolean increments) throws Exception {

    Class[] testClasses = new Class[testClassNames.size()];
    final TestResult initialResult;
    final TimingTestSuite timingSuite;
    final TestOrder order;
    //Unique name for this test suite
    final String fileName = 
      "testSuite" + System.currentTimeMillis() + ".dat";
    
    for (int i = 0; i < testClasses.length; i++) {
      testClasses[i] = Class.forName((String) testClassNames.get(i));
    }
    initialResult = new TestResult();
    timingSuite = new TimingTestSuite(testClasses);
    timingSuite.run(initialResult);
    order = timingSuite.getOrder();

    //Now, if the tests failed, can return straight away
    if (!initialResult.wasSuccessful()) {
      return new JumbleResult() {
        public String getClassName() {
          return className;
        }

        public Mutation[] getAllMutations() {
          return null;
        }
        
        public String[] getTestClasses() {
          return (String[])testClassNames.toArray(new String[0]);
        }
        public long getTimeoutLength() {
          return 0;
        }

        public TestResult getInitialTestResult() {
          return initialResult;
        }

        public Mutation[] getCovered() {
          return null;
        }

        public Mutation[] getMissed() {
          return null;
        }

        public Mutation[] getTimeouts() {
          return null;
        }
      };
    }
    
    //Store the timing stuff in a temporary file
    ObjectOutputStream oos = new ObjectOutputStream
      (new FileOutputStream(fileName));
    oos.writeObject(order);
    oos.close();
    
    //compute the timeout
    long timeLeft = order.getTotalRuntime();
    
    //Get the number of mutation points from the Jumbler
    final Mutater m = new Mutater(0);
    m.setIgnoredMethods(excludeMethods);
    m.setMutateIncrements(increments);
    m.setMutateInlineConstants(inlineConstants);
    m.setMutateReturnValues(returnVals);
    
    final int mutationCount = m.countMutationPoints(className);
    
    final JavaRunner runner = new JavaRunner("jumble.fast.FastJumbler");
    Process childProcess = null;
    IOThread iot = null;
    
    final Mutation[] allMutations = new Mutation[mutationCount];
    
    for (int currentMutation = 0; currentMutation < mutationCount; currentMutation++) {
      
      
      //If no process is running, start a new one
      if (childProcess == null) {
        ArrayList args = new ArrayList();
        
        //class name
        args.add(className);
        
        //mutation point
        args.add(String.valueOf(currentMutation));
        
        //test suite filename
        args.add(fileName);
        
        //exclude methods
        if (!excludeMethods.isEmpty()) {
          StringBuffer ex = new StringBuffer();
          ex.append("-x ");
          Iterator it = excludeMethods.iterator();
          
          for (int i = 0; i < excludeMethods.size(); i++) {
            if (i == 0) {
              ex.append(it.next());
            } else {
              ex.append("," + it.next());
            }
          }
          args.add(ex.toString());
        }
        
        //inline constants
        if (inlineConstants) {
          args.add("-k");
        }
        
        //return values
        if(returnVals) {
          args.add("-r");
        }
        
        //increments
        if (increments) {
          args.add("-i");
        }
        
        //start process
        runner.setArguments((String[])args.toArray(new String[0]));
        childProcess = runner.start();
        iot = new IOThread(childProcess.getInputStream());
        iot.start();
        
        //read the "START" to let us know the JVM has started
        //we don't want to time this.
        while (true) {
          String str = iot.getNext();
          //System.out.println(str);
          if (str == null) {
            Thread.sleep(10);
          } else if (str.equals("START")) {
            break;
          } else {
            throw new RuntimeException("jumble.fast.FastJumbler returned "
                + str + " instead of START");
            
          }
        }
      }
      
      long before = System.currentTimeMillis();
      long after = before;
      long timeout = computeTimeout(timeLeft);
      
      //Run until we time out
      while (true) {
        String out = iot.getNext();
        
        if (out == null) {
          if (after - before > timeout) {
            allMutations[currentMutation] = 
              new Mutation("TIMEOUT", className, currentMutation);
            childProcess.destroy();
            childProcess = null;
            break;
          } else {
            Thread.sleep(50);
            after = System.currentTimeMillis();
          }
        } else {
          try {
            //We have output so go to the next loop iteration
            allMutations[currentMutation] = 
              new Mutation(out, className, currentMutation);
          } catch (RuntimeException e) {
            throw e;
          }
          break;
        }
      }
      
    }
    
    JumbleResult ret = new JumbleResult() {
      public String getClassName() {
        return className;
      }
      
      public TestResult getInitialTestResult() {
        return initialResult;
      }
      
      public Mutation[] getAllMutations() {
        return allMutations;
      }
      
      public Mutation[] getCovered() {
        return filter(Mutation.PASS);
      }
      
      public Mutation[] getTimeouts() {
        return filter(Mutation.TIMEOUT);
      }

      public Mutation[] getMissed() {
        return filter(Mutation.FAIL);
      }
      
      public long getTimeoutLength() {
        return computeTimeout(order.getTotalRuntime());
      }
      
      public String[] getTestClasses() {
        return (String[])testClassNames.toArray(new String[0]);
      }
      
      private Mutation[] filter(int mutationType) {
        Mutation[] all = getAllMutations();
        ArrayList ret = new ArrayList();
        
        for (int i = 0; i < all.length; i++) {
          if (all[i].getStatus() == mutationType) {
            ret.add(all[i]);
          }
        }
        
        return (Mutation[])ret.toArray(new Mutation[ret.size()]);
      }
      
    };
    
    //finally, delete the test suite file
    if(!new File(fileName).delete()) {
      System.err.println("Error: could not delete temporary file");
    }
    return ret;
  }
}
