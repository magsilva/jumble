package jumble.ui;

import java.io.PrintStream;

import jumble.fast.JumbleResult;
import jumble.fast.MutationResult;


/**
 * Class outputting jumble results in Sean's original jumble format
 * 
 * @author Tin Pavlinic
 * @version $Revision$
 */
public class SeanResultPrinter extends AbstractResultPrinter {
  /**
   * Constructor.
   * 
   * @param p
   *          the output stream to print to.
   */
  public SeanResultPrinter(PrintStream p) {
    super(p);
  }

  /**
   * Displays the result.
   * 
   * @param res
   *          the Jumble result to print
   * @throws Exception if something goes wrong
   */
  public void printResult(JumbleResult res) throws Exception {
    PrintStream out = getStream();

    out.println("Mutating " + res.getClassName());
    
    if (res.isInterface()) {
      out.println("Score: 100 (INTERFACE)");
      return;
    }
    
    String[] testClasses = res.getTestClasses();
    out.print("Tests: ");
    for (int i = 0; i < testClasses.length; i++) {
      out.print((i == 0 ? "" : ", ") + testClasses[i]);
    }
    out.println();

    if (res.isMissingTestClass()) {
      out.println("Score: 0 (NO TEST CLASS)");
      out.println("Mutation points = " + res.getNumberOfMutations());
      return;
    }
    
    if (!res.initialTestsPassed()) {
      out.println("Score: 0 (TEST CLASS IS BROKEN)");
      out.println("Mutation points = " + res.getNumberOfMutations());
      return;
    }
    
    out.print("Mutation points = " + res.getNumberOfMutations());
    out.println(", unit test time limit " + (double) res.getTimeoutLength()
        / 1000 + "s");

    for (int i = 0; i < res.getAllMutations().length; i++) {
      MutationResult currentMutation = res.getAllMutations()[i];
      if (currentMutation.isPassed()) {
        out.print(".");
      } else if (currentMutation.isTimedOut()) {
        out.print("T");
      } else {
        out.println("M " + currentMutation.getDescription());
      }
    }
    out.println();
    
    if (res.getAllMutations().length == 0) {
      out.println("Score: 100 (NO MUTATIONS POSSIBLE)");
    } else {
      out.println("Score: " + (res.getCovered().length + res.getTimeouts().length)
          * 100 / res.getNumberOfMutations());
    }
  }
}