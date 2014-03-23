package com.reeltwo.jumble.ui;

import java.io.PrintStream;
import java.util.List;

import com.reeltwo.jumble.fast.JumbleResult;
import com.reeltwo.jumble.fast.MutationResult;

/**
 * Prints the results of a Jumble run to a <code>PrintStream</code>, this will
 * usually be <code>System.out</code>.
 *
 * @author Tin Pavlinic
 * @version $Revision$
 */
public class JumbleScorePrinterListener implements JumbleListener {

  private static final int DOTS_PER_LINE = 50;

  private final PrintStream mStream;

  private int mCovered = 0;

  private int mMutationCount;

  private String mClassName;

  private List<String> mTestNames;

  private boolean mInitialTestsPassed;

  private int mDotCount = 0;

  private long mStartTime = 0;

  public JumbleScorePrinterListener() {
    this(System.out);
  }

  public JumbleScorePrinterListener(PrintStream output) {
    mStream = output;
  }

  @Override
  public void jumbleRunEnded() {
    if (mInitialTestsPassed) {
      getStream().println();
      printResultsForNormalRun();
    }
  }

  @Override
  public void finishedMutation(MutationResult res) {
    if (res.isPassed()) {
      getStream().print(".");
      mCovered++;
      newDot();
    } else if (res.isTimedOut()) {
      getStream().print("T");
      mCovered++;
      newDot();
    } else {
      getStream().println(failMessage(res));
      mDotCount = 0;
    }
  }

  protected static final String MUT_FAIL = "M FAIL: ";

  protected String failMessage(MutationResult res) {
    return MUT_FAIL + res.getDescription();
  }

  private void newDot() {
    mDotCount++;
    if (mDotCount == DOTS_PER_LINE) {
      mDotCount = 0;
      getStream().println();
    }
  }

  @Override
  public void jumbleRunStarted(String className, List<String> testClassNames) {
    mMutationCount = 0;
    mCovered = 0;
    mClassName = className;
    mTestNames = testClassNames;
    mStartTime = System.currentTimeMillis();
  }

  @Override
  public void performedInitialTest(JumbleResult result, int mutationCount) {
    mInitialTestsPassed = result.initialTestsPassed();
    mMutationCount = mutationCount;
    getStream().println("Mutating " + mClassName);

    if (result.isInterface()) {
      printResultsForInterface();
      return;
    }

    getStream().print("Tests:");
    for (String mTestName : mTestNames) {
      getStream().print(" " + mTestName);
    }
    getStream().println();

    if (result.isMissingTestClass()) {
      getStream().println("Score: 0% (NO TEST CLASS)");
      getStream().println("Mutation points = " + mMutationCount);
      return;
    }

    if (!mInitialTestsPassed) {
      getStream().println("Score: 0% (TEST CLASS IS BROKEN)");
      getStream().println("Mutation points = " + mMutationCount);
      return;
    }

    getStream().print("Mutation points = " + mMutationCount);
    getStream().println(", unit test time limit " + (double) result.getTimeoutLength() / 1000 + "s");
  }

  protected void printResultsForNormalRun() {
    getStream().println("Jumbling took " + (double) (System.currentTimeMillis() - mStartTime) / 1000 + "s");
    if (mMutationCount == 0) {
      getStream().println("Score: 100% (NO MUTATIONS POSSIBLE)");
    } else {
      getStream().println("Score: " + (mCovered) * 100 / mMutationCount + "%");
    }
  }

  protected void printResultsForInterface() {
    getStream().println("Score: 100% (INTERFACE)");
    getStream().println("Mutation points = 0");
  }

  @Override
  public void error(String errorMsg) {
    getStream().println("ERROR: " + errorMsg);
  }

  public PrintStream getStream() {
    return mStream;
  }
}
