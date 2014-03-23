package com.reeltwo.jumble.fast;


import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the corrresponding class.
 * @author Tin Pavlinic
 * @version $Revision$
 */
public class FastRunnerTest extends TestCase {
  public void testComputeTimeout() {
    assertEquals(2000, FastRunner.computeTimeout(0));
    assertEquals(12000, FastRunner.computeTimeout(1000));
  }

  public final void testWriteStats() throws Exception {
    File csv = new File("jumble_stats", "experiments.JumblerExperiment.csv");
    File matrix = new File("jumble_stats", "experiments.JumblerExperiment-binary-matrix.csv");
    assertTrue(!csv.exists() || csv.delete());
    assertTrue(!matrix.exists() || matrix.delete());

    ArrayList<String>tests = new ArrayList<String>();
    tests.add("experiments.JumblerExperimentTest");
    tests.add("experiments.JumblerExperimentSecondTest");
    FastRunner runner = new FastRunner();
    runner.setRecStat(true);
    runner.setSaveCache(false);
    runner.runJumble("experiments.JumblerExperiment", tests, null);

    // check the human-readable .csv output
    // TODO: investigate why this includes a junit.framework.TestSuite$1.warning method
    //       in addition to the expected test methods
    assertTrue(csv.exists());
    List<String> actual = Files.readAllLines(csv.toPath(), Charset.defaultCharset());
    assertEquals(14, actual.size());
    URL expectedURL = FastRunnerTest.class.getResource("expected_stats/experiments.JumblerExperiment.csv");
    List<String> expected = Files.readAllLines(Paths.get(expectedURL.toURI()), Charset.defaultCharset());
    for (int line = 1; line < Math.min(actual.size(), expected.size()); line++) {
      final String act = actual.get(line).replaceAll("\tP/[.E0-9-]*s", "\tP/0.0s").replaceAll("\tF/[.E0-9-]*s", "\tF/0.0s");
      assertEquals("Line " + line, expected.get(line), act);
    }
    assertEquals(actual.size(), expected.size());
    assertTrue(csv.delete());
    
    // check the RAISE-compatible binary matrix output file.
    assertTrue(matrix.exists());
    actual = Files.readAllLines(matrix.toPath(), Charset.defaultCharset());
    assertEquals(11, actual.size());
    expectedURL = FastRunnerTest.class.getResource("expected_stats/experiments.JumblerExperiment-binary-matrix.csv");
    expected = Files.readAllLines(Paths.get(expectedURL.toURI()), Charset.defaultCharset());
    for (int line = 1; line < Math.min(actual.size(), expected.size()); line++) {
      assertEquals("Line " + line, expected.get(line), actual.get(line));
    }
    assertEquals(actual.size(), expected.size());
    assertTrue(matrix.delete());
  }

  public static Test suite() {
    return new TestSuite(FastRunnerTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }

}
