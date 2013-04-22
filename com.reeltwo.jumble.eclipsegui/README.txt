This Eclipse plugin adds popup menu commands for running Jumble.
It also provides a preferences page for setting Jumble options.

BUILDING
========
To build this plugin, build the jumble and jumble-annotation projects
first, and then copy the resulting .jar files from there into this project.

(Because of the way Jumble uses child JVMs to run the tests, it needs
a separate jumble.jar file for the child JVM anyway.  So we copy these
.jar files rather than turn them into dependent plugins.)

To test the plugin from within Eclipse, right-click on this project
and use 'Run as / Eclipse Application'.

To export the plugin into an update site, edit com.reeltwo.jumble.update-site/site.xml.
In the Site Map tab, remove the feature under the 'Jumble Mutation Analysis' entry,
(right-click then Remove) and then select the 'Jumble Mutation Analysis' entry
and add use 'Add Feature' to add the com.reeltwo.jumble.feature feature that has
a version number like 1.x.y.qualifier.  Then 'Synchronize' and 'Build'.
Then the whole of the com.reeltwo.jumble.update-site folder will be a valid
Eclipse update site for Jumble.  These can be copied onto a web site or used locally.

INSTALL THE PLUGIN INTO ECLIPSE
===============================
From within Eclipse, use 'Help / Install new software / Add' to add the Jumble
update site (eg. this directory) into your Eclipse installation.

After installing the plugin, you can run Jumble on one of your
Java source code classes by selecting that class and running
the command (right-click context menu):
<pre>
  Jumble Mutation Tester / Analyze tests of this class
</pre>

Note that you should run Jumble on your application classes,
not your test classes.  For each application class, Jumble expects
to find an associated test class by following standard naming
conventions.  For example:
<pre>
  org.project.MyApp.java       // the class to be mutated.
  org.project.MyAppTest.java   // the corresponding test class.
</pre>

This should work if your test class is anywhere in the classpath
for that project, but it must be in the same package as the application
class.

Alternatively, you can use the @TestClass("foo.bar.StrangeTest")
annotation on your application class to tell Jumble the name
of your test class explicitly.  See the jumble-annotations project.
