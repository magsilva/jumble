Developer Instructions
======================

Steps for creating a release of Jumble (including the Eclipse update site).

1 Make sure you are using a version of Eclipse with Plugin-Development added.

2 Bump up the version numbers in the com.reeltwo.jumble.eclipsegui/plugin.xml
  and the com.reeltwo.jumble.feature/plugin.xml.  Check that ALL the version numbers
  in site.xml (in this folder) are updated correspondingly, including the path into
  the Sourceforge files release area (you will create this folder shortly).
  
3 Open site.xml in this update-site folder, and delete and re-add the feature
  (com.reeltwo.jumble.feature) as a child of the 'Jumble Test Analysis' category.
  
4 Synchronise and 'Build All' features.  This should create the content.jar, 
  artifacts.jar, and 'features' and 'plugins' directories within this
  com.reeltwo.jumble.update-site folder.
  
5 Go to the 'Files' area of the Jumble project on Sourceforge and create a new
  folder with the appropriate version number (inside the top level 'jumble' folder).
  Then copy up all the files and folders in this update-site folder into that new
  folder.  (However, copy the top-level README.txt, rather than this README!)
  
6 Also upload the jumble.jar and jumble-annotations.jar into that folder
  (add the version to the end of the *.jar names, as per the previous releases).
  Eg.  jumble_1.3.0.jar
       jumble-annotations_1.3.0.jar

7 Update the .htaccess redirection file on the Jumble web site to point to
  the new release.  See ../web/jumble.sourceforge.net.htaccess for details.
      ssh -t YOUR_USER_NAME,jumble@shell.sourceforge.net create
      cd /home/project-web/jumble/htdocs
      cat >.htaccess
      ... (paste the contents of the updated web/jumble.sourceforge.net.htaccess)...
      ... then press Control-D...
      chmod 644 .htaccess

8 Test that the Eclipse update site works with a fresh release of Eclipse.
  (Help / Install new software / Add / Location=http://jumble.sourceforge.net/update)

9 Finally, create an SVN tag for the new release:
      svn copy https://sourceforge.net/p/jumble/code/HEAD/tree/trunk/ https://sourceforge.net/p/jumble/code/HEAD/tree/tags/release_1_x_y

10 and post an announcement about the new release (key features) to:
    1. the Jumble News feed on Sourceforge.
    2. the jumble-users@lists.sourceforge.net
    3. http://slashdot.org
