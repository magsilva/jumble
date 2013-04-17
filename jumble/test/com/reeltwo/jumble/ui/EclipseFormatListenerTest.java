package com.reeltwo.jumble.ui;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.reeltwo.jumble.fast.InterfaceResult;
import com.reeltwo.jumble.fast.MutationResult;

/**
 * Tests the Eclipse output format class.
 *
 * @author Mark Utting
 */
public class EclipseFormatListenerTest extends JumbleScorePrinterListenerTest {

  @Override
  protected JumbleListener getListener(PrintStream ps) {
    return new EclipseFormatListener(ps);
  }

  @Override
  protected String expectedFailureMessage(String cName, int lineNum, String mDesc) {
    String javaName = cName + ".java";
    return "M FAIL: (" + javaName + ":" + lineNum + "): " + mDesc;
  }

  public static Test suite() {
    return new TestSuite(EclipseFormatListenerTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
