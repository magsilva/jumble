package com.reeltwo.jumble.eclipsegui;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.reeltwo.jumble.eclipsegui"; //$NON-NLS-1$

  // The shared instance
  private static Activator plugin;

  /**
   * The constructor
   */
  public Activator() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
   * )
   */
  public void start(BundleContext context) throws Exception {
    super.start(context);
    System.out.println("Activator.start " + PLUGIN_ID);
    plugin = this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
   * )
   */
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    System.out.println("Activator.stop " + PLUGIN_ID);
    super.stop(context);
  }

  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static Activator getDefault() {
    return plugin;
  }

  /**
   * Returns an image descriptor for the image file at the given plug-in
   * relative path
   * 
   * @param path
   *          the path
   * @return the image descriptor
   */
  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

  /**
   * Gets the installation location of the plugin.
   * 
   * @return The File Location of this plugin
   */
  public File getPluginFolder() throws IOException {
    URL url = getBundle().getEntry("/");
    url = FileLocator.toFileURL(url);
    System.out.println("DEBUG: getPluginFolder => " + url);
    return new File(url.getPath());
  }
}
