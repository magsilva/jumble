Summary
=======
Jumble is a Java mutation tool for measuring the effectiveness and
coverage of JUnit test suites.  Jumble requires at least Java 1.5.

See http://jumble.sourceforge.net for an introduction to Jumble.
See http://sourceforge.net/projects/jumble for source code and downloads.


Details
=======
Jumble is a byte-code level mutation testing tool for Java that
inter-operates with JUnit.  Given a class file and its JUnit test
classes, Jumble performs a series of byte-code mutations on the class
file (using the BCEL library, see http://jakarta.apache.org/bcel), and 
runs the unit tests on each mutation to see if they detect the mutation.
This tells you how thoroughly your unit tests test the class.

Jumble was developed in 2003-2006 by a commercial company in New
Zealand, ReelTwo (www.reeltwo.com), and is now available as open
source under the Apache licence.  Jumble has been designed to operate in
an industrial setting with large projects.  Heuristics have been
included to speed the checking of mutations, for example, noting which
test fails for each mutation and running this first in subsequent
mutation checks.  Significant effort has been put into ensuring that it
can test code which runs in environments such as the Apache
webserver.  This requires careful attention to class path handling and
co-existence with foreign class-loaders.  At ReelTwo, Jumble is used
on a continuous basis within an agile programming environment with
approximately 400,000 lines of Java code under source control.  This
checks out project code every fifteen minutes and runs an incremental
set of unit tests and mutation tests for modified classes.


How to Compile Jumble
=====================
If there is a file, jumble.jar, in the same directory as this README
file, then Jumble is already compiled and you can start using it.
Otherwise, you should first build jumble.jar as follows.

There is an ant (http://ant.apache.org) build script, called build.xml,
provided in the directory containing this README.  The Jumble sources
are in the 'src' subdirectory, the self-tests are in the 'test' subdirectory
and the 'lib' subdirectory contains several .jar files used by Jumble.

Assuming you have ant installed, run:

  ant jar

This will produce the jumble.jar file, which you can use to run Jumble.
You may also want to run 'ant test' to run Jumble's unit and system
tests.

NOTE: the lib/bcel.jar library used by Jumble is actually a patched version
of BCEL release 5.2, which adds some extra functions to their ClassPath
object.  These patches were accepted by the BCEL project and have been
committed to their repository, but as yet there is no release that includes
them.  The patched version of BCEL is in lib/bcel.jar, and is included
into jumble.jar when you build Jumble.


How to Use Jumble
=================
See the top-level README.txt file for examples of running Jumble.  In brief, from this directory, you can run:

  javac -cp jumble.jar example/*.java
  java -jar jumble.jar --classpath=. example/Mover
