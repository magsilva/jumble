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


Contents
========
This distribution contains these subdirectories:

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


Source Code
===========
If this distribution does not contain the Java source code,
you can obtain it from the Sourceforge SVN repository.
Eg. for the latest development version of Jumble:

  svn co https://jumble.svn.sourceforge.net/svnroot/jumble/trunk trunk


Links
=====
Jumble Web Site:      http://jumble.sourceforge.net
Jumble Mailing Lists: http://sourceforge.net/mail/?group_id=193434
Jumble Forums:        https://sourceforge.net/forum/?group_id=193434
