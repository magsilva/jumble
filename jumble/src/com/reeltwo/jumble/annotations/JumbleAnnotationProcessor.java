package com.reeltwo.jumble.annotations;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.reeltwo.jumble.mutation.Mutater;
import com.reeltwo.jumble.mutation.MutatingClassLoader;

/**
 * Class for processing Jumble annotations.
 * @author Tin Pavlinic
 * @version $Revision: $
 */
public class JumbleAnnotationProcessor {
  public List<String> getTestClassNames(String className, String classPath) throws ClassNotFoundException {
    //Load in a different class loader to prevent the initial loading of the class from affecting the jumble run
    final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
    try {
      MutatingClassLoader jumbler = new MutatingClassLoader(className, new Mutater(-1), classPath);
      Thread.currentThread().setContextClassLoader(jumbler);
      Class<?> clazz = jumbler.loadClass(className);
      TestClass testClass = clazz.getAnnotation(TestClass.class);
      List<String> testClassNames = new ArrayList<String>();
      if (testClass != null) {
        String[] testClasses = testClass.value();
        Collections.addAll(testClassNames, testClasses);
      }
      return testClassNames;
    } finally {
      Thread.currentThread().setContextClassLoader(oldLoader);
    }
  }
}
