Summary
=======
This project just defines several Java annotations that provide a
convenient source-level way of telling the Jumble tool about the 
relationships between your application classes and test classes.

  @JumbleIgnore   // means do not perform mutation testing on this method.
  @TestClass("com.company.ExtraTests")  // specify the class(es) that test this class. 


Jumble is a Java mutation tool for measuring the effectiveness and
coverage of JUnit test suites.

See http://jumble.sourceforge.net for an introduction to Jumble.
See http://sourceforge.net/projects/jumble for source code and downloads.


How to Compile Jumble-Annotations
=================================
If there is a file, jumble-annotations.jar, in the same directory as this README
file, then the Jumble-Annotations project is already compiled and you can start
using it.  Simply add that .jar file to your application projects.

Otherwise, you should first build jumble-annotations.jar as follows.

There is an ant (http://ant.apache.org) build script, called build.xml,
provided in the directory containing this README.

Assuming you have ant installed, run:

  ant jar

This will produce the jumble-annotations.jar file, which you then incorporate
into your application projects.

NOTE: if you have *changed* anything in this jumble-annotations project,
you should also copy the new jumble-annotations.jar from this directory into
../jumble/lib/jumble-annotations.jar, so that Jumble uses the updated annotations.
