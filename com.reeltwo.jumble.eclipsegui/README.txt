This Eclipse plugin adds popup menu commands for running Jumble.
It also provides a preferences page for setting Jumble options.

BUILDING
========
To build this plugin, build the jumble and jumble-annotation projects
first, and then copy the resulting .jar files from there into this project.

(Because of the way Jumble uses child JVMs to run the tests, it needs
a separate jumble.jar file for the child JVM anyway.  So we copy these
.jar files rather than turn them into dependent plugins.)
