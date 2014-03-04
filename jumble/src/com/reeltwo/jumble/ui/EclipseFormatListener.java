package com.reeltwo.jumble.ui;



import com.reeltwo.jumble.fast.MutationResult;
import java.io.PrintStream;

/**
 * Prints the results of a Jumble run in <code>Eclipse</code> console compatible format.
 * That is, each mutation failure is printed as <code>(package.File.java:LineNum)</code>.
 *
 * @author Mark Utting
 */
public class EclipseFormatListener extends JumbleScorePrinterListener {

  public EclipseFormatListener() {
    this(System.out);
  }

  public EclipseFormatListener(PrintStream output) {
    super(output);
  }

  @Override
  protected String failMessage(MutationResult res) {
    String className = res.getClassName();
    String javaName = className + ".java";
    String description = res.getDescription();
    String[] part = description.split(":");
    if (part.length == 3) {
      // part[0] is the classname.
      final String lineNum = part[1];
      final String mutDesc = part[2];
      return MUT_FAIL + "(" + javaName + ":" + lineNum + "):" + mutDesc;
    } else {
      return super.failMessage(res);
    }
  }

}
