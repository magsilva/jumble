package com.reeltwo.jumble.ui;



import com.reeltwo.jumble.fast.JumbleResult;
import com.reeltwo.jumble.fast.MutationResult;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import org.apache.bcel.util.ClassPath.ClassFile;
import org.apache.bcel.util.ClassPath;

/**
 * Prints the results of a Jumble run in <code>Emacs</code> compatible format.
 * 
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision$
 */
public class EmacsFormatListener implements JumbleListener {
  private PrintStream mStream;

  private int mCovered = 0;

  private int mMutationCount;

  private String mClassName;

  private boolean mInitialTestsPassed;

  private ClassPath mSourcePath;

  private String mBaseDir = System.getProperty("user.dir");

  public EmacsFormatListener(String classPath) {
    this(classPath, System.out);
  }

  public EmacsFormatListener(String sourcePath, PrintStream output) {
    mStream = output;
    mSourcePath = new ClassPath(sourcePath);
  }

  public void jumbleRunEnded() {
    if (mInitialTestsPassed) {
      if (mMutationCount == 0) {
        mStream.println("Score: 100");
      } else {
        mStream.println("Score: " + (mCovered) * 100 / mMutationCount);
      }
    }
   
  }

  private String findSourceName(String className) {
    String sourceName = className.replace('.', File.separatorChar) + ".java";
    // Convert inner class to the outer source file
    sourceName = sourceName.replaceAll("\\$[^.]+\\.", ".");
    try {
      // Try to resolve the class name relative to the classpath
      sourceName = mSourcePath.getPath(sourceName);
      if (sourceName.startsWith(mBaseDir)) {
        sourceName = sourceName.substring(mBaseDir.length() + 1);
      }
    } catch (IOException e) {
      // Use the sourceName that we have.
    }
    return sourceName;
  }

  public void finishedMutation(MutationResult res) {
    if (res.isFailed()) {
      String description = res.getDescription();
      description = description.substring(description.indexOf(":"));
      String sourceName = findSourceName(res.getClassName());
      mStream.println(sourceName + description);
    } else {
      mCovered++;
    }
  }

  public void jumbleRunStarted(String className, List < String > testClasses) {
    mClassName = className;
  }

  public void performedInitialTest(JumbleResult result, int mutationCount) {
    mInitialTestsPassed = result.initialTestsPassed();
    mMutationCount = mutationCount;
    String sourceName = findSourceName(mClassName);
    mStream.print("Mutating " + sourceName);

    if (result.isInterface()) {
      mStream.println(" (Interface)");
      mStream.println("Score: 100");
    } else {
      mStream.println(" (" + mMutationCount + " mutation points)");
      if (result.isMissingTestClass()) {
        mStream.println(sourceName + ":0: No test class" + result.getTestClasses());
        mStream.println("Score: 0");
      } else if (!mInitialTestsPassed) {
        mStream.println(sourceName + ":0: Test class is broken" + result.getTestClasses());
        mStream.println("Score: 0");
      }
    }
  }

  public void error(String errorMsg) {
    mStream.println("ERROR: " + errorMsg);
  }
}
