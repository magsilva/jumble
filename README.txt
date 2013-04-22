Welcome to Jumble
=================

Do you want to know how good your JUnit tests are?

Jumble will tell you: from 0% (worthless) to 100% (angelic!).

Jumble is a class level mutation testing tool that works in
conjunction with JUnit.  The purpose of mutation testing is to provide
a measure of the effectiveness of test cases.  A single mutation is
performed on the code to be tested, the corresponding test cases are
then executed.  If the modified code fails the tests, then this
increases confidence in the tests.  Conversely, if the modified code
passes the tests this indicates a testing deficiency.


How to Use Jumble (Command Line Usage)
======================================
An example of a simple application of Jumble to the class example/Mover is:

  java -jar jumble_binary_*.jar --classpath=. example/Mover

This assumes that class Mover is in the 'example' package, which is relative
to the current directory ('.') and that its JUnit tests are in example/MoverTest.

NOTE: Jumble works on .class files, so you must use javac to create
example/Mover.class and example/MoverTest.class before you run Jumble:

  javac -cp jumble_binary_*.jar example/*.java

Jumble starts by running the unit tests (in MoverTest.class) on the
unmodified Mover class to check that they all pass, and to measure the
time taken by each test.  Then it will mutate Mover and run the tests
again to see if they detect the mutation.  It continues this process until
all mutations of Mover have been tried.  The output might look like this:

  Mutating example.Mover
  Tests: example.MoverTest
  Mutation points = 10, unit test time limit 2.03s
  ......M FAIL: example.Mover:27: - -> +
  ..M FAIL: example.Mover:30: + -> -

  Score: 80%

This says that Jumble has tried 10 different mutants of Mover and the
unit tests (in MoverTest) correctly detected the changed behaviour in
8/10 cases (indicated by a '.'), but failed to detect the change in
the other 2/10 cases.  Overall, 80% of the mutations were detected by
the unit tests, which is not too bad, but could be improved.


Let's analyze these results a little.
The first failed test is on line 27 of Mover.java, which is
      x -= speed / SLOWER;
The Jumble message is complaining that mutating the '-' operator
to '+' was not detected by the unit tests.  This shows that our test
value of speed=2 is not a good choice, because SLOWER==5 and 2
divided by 5 is zero, so it makes no difference whether we add or
subtract the speed.  This tells us that we should choose a better
test value for speed in the "left" and "right" cases, such as 5.
If we improved our testLeft() and testRight() test cases by using
5 rather than 2, then all mutations would be detected and the 
Jumble score would rise to 100%.

This example also shows that Jumble does not (yet) ensure statement
coverage.  That is, even 100% Jumble coverage does not mean that all
statements of Mover.java have been executed by the tests.  
For example, Jumble has not complained that the prettyString() method
is never tested, or that the 'throw new RuntimeException()' branch
of move() has not been tested.  If we rerun Jumble with the "-r" option
(mutate Return values), then it will mutate the return statements of
all non-void methods, and will detect that the prettyString() result
is not used.

  $ java -jar jumble_binary_*.jar -r --classpath="." example/Mover
  Mutating example.Mover
  Tests: example.MoverTest
  Mutation points = 12, unit test time limit 2.01s
  ......M FAIL: example.Mover:27: - -> +
  ..M FAIL: example.Mover:30: + -> -
  .M FAIL: example.Mover:44: changed return value (areturn)

  Score: 75%

This tells us that we need to test the prettyString() method.
However, the lack of testing of the throw case of move() is still
not detected.  This illustrates why it can be useful to use code 
coverage tools (such as statement coverage) in addition to Jumble.


More Advanced Usage
===================
The next example shows a more complex command-line usage of Jumble to test
a hypothetical class called Bar, which has two sets of JUnit tests, called
BarTest1 and BarTest2.  Since these names do not follow the usual Jumble
naming convention for JUnit class (which would be BarTest.java), we must
tell Jumble which JUnit test files to use, by listing them on the command
line after the main class Bar.  We assume that all three of these classes
are in a Java package called 'app', but that application source files are
in the 'src' directory, whereas the JUnit files are in the 'test' directory.

  java -jar jumble_binary_*.jar --classpath="src;test;." app.Bar app.BarTest1 app.BarTest2

So the files involved in this usage are organised as follows:

  ./jumble_binary_*.jar
  ./src/app/Bar.java        (not used by Jumble)
  ./src/app/Bar.class       (the class file that will be mutated)
  ./test/app/BarTest1.java  (not used by Jumble)
  ./test/app/BarTest1.class (the first set of JUnit tests for Bar)
  ./test/app/BarTest2.java  (not used by Jumble)
  ./test/app/BarTest2.class (the second set of JUnit tests for Bar)


Jumble accepts many other command line options, which allow you to
further customize its behaviour.  Use the '--help' option to see
a brief summary of all the options that it accepts.

  java -jar jumble_binary_*.jar --help


Jumble Annotations
==================
The jumble-annotations*.jar file contains several Java annotations
that provide a convenient source-level way of telling the Jumble tool
about the relationships between your application classes and test classes.

  @JumbleIgnore   // means do not perform mutation testing on this method.

  @TestClass("a.b.ExtraTests")  // specify the class that tests this class.
  @TestClass({"a.Test1","a.b.ExtraTests"})  // specify multiple test classes.

To use these annotations you will need to add the jumble-annotations*.jar
to your application project, and then import the annotation in your Java
source code.  For example:

  import com.reeltwo.jumble.annotations.TestClass;
  
  @TestClass("my.CalculatorTests")
  public class Calculator { ... }


Eclipse Plugin for Jumble
=========================
To install the Jumble plugin into Eclipse, start Eclipse then use:

   'Help / Install new software / Add'
   
to add the following Jumble update site into your Eclipse installation.

   http://jumble.sourceforge.net

After installing the plugin, you can run Jumble on one of your
Java source code classes by selecting that class and running
the command (in the right-click context menu):

  'Jumble Mutation Tester / Analyze tests of this class'

Note that you should run Jumble on your application classes,
not your test classes. 


Source Code
===========
If this distribution does not contain the Java source code,
you can obtain it from the Sourceforge SVN repository.
Eg. for the latest development version of Jumble:

  svn co https://jumble.svn.sourceforge.net/svnroot/jumble/trunk trunk

The source distribution contains these subdirectories:

* jumble-annotations
      This defines a Java 1.5+ annotation (@TestClass), which
      can be used to specify the connection between your main
      Java class and its unit tests.  The use of this annotation
      is optional, but is useful for allowing the Eclipse plugin
      to support more flexible naming conventions for unit test files.

      If you do use it within your own projects, you will need to add
      jumble-annotations.jar to the classpath of those projects.
      This is why these two annotations classes are provided as a 
      separate .jar file, to make it easy to add them to other
      projects with very low space overhead (less than 2Kb).

* jumble
      Source code for Jumble.  Go here to build jumble.jar, 
      which contains the command line interface for Jumble.

* com.reeltwo.jumble.eclipsegui
* com.reeltwo.jumble.feature
* com.reeltwo.jumble.update-site
      A simple plugin (and its update-site) for running Jumble within Eclipse.
      This must be built from within Eclipse.

* antplugin
      An ant plug for running Jumble in ant. It can run jumble on classes 
      in a given directory and output results to files for every mutated class.

See the README.txt file in each subdirectory for information
on how to build, install and use each package.
Make sure you build them in the order shown above.


Links
=====
Jumble Web Site:           http://jumble.sourceforge.net
Jumble Mailing Lists:      http://sourceforge.net/mail/?group_id=193434
Jumble Forums:             https://sourceforge.net/forum/?group_id=193434
Mutation Testing Overview: http://en.wikipedia.org/wiki/Mutation_testing
