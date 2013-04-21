package com.reeltwo.jumble.eclipsegui.handlers;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.reeltwo.jumble.eclipsegui.Activator;
import com.reeltwo.jumble.eclipsegui.preferences.PreferenceConstants;

/**
 * This handles Jumble commands from a selected class in the package explorer.
 *
 * This handler extends AbstractHandler, an IHandler base class.
 *
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 *
 * @author Mark.Utting
 * @author Tin Pavlinic
 */
public class JumbleClass extends AbstractHandler {

  private static final String LAUNCH_NAME = "Run Jumble";

  private static final String PATH_SEPARATOR = System.getProperty("path.separator");

  /**
   * This console output stream shows setup messages about starting to run Jumble.
   * It does not display the actual Jumble output - that goes to the launch configuration console.
   */
  protected MessageConsoleStream mOut;

  /**
   * The constructor.
   */
  public JumbleClass() {
    MessageConsole myConsole = findConsole("Jumble Console");
    mOut = myConsole.newMessageStream();
    // mOut.println("Created JumbleClass handler...");
  }

  /**
   * Decides if the current selection is a valid Java source file that can be Jumbled.
   *
   * TODO: find out how to use the parameter to find out if the current selection is a valid Java file.
   * @param evalContext does not seem to be useful, so we use the current workbench selection instead.
   */
  @Override
  public void setEnabled(Object evalContext) {
    ICompilationUnit icu = getSelectedJavaFile();
    super.setBaseEnabled(icu != null);
  }

  /**
   * Try to guess which Java file the user is referring to.
   *
   * There must be nicer ways of doing this.
   *
   * @return the compilation unit for the Java file to Jumble, or null if there is none,
   *         or if it looks like a test file (*Test.java).
   */
  protected ICompilationUnit getSelectedJavaFile() {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    IWorkbenchPage page = window == null ? null : window.getActivePage();
    IWorkbenchPart part = page == null ? null : page.getActivePart();
    ICompilationUnit icu = null;
    if (part != null && "Package Explorer".equals(part.getTitle())) {
      // look at the current selection in the Package Explorer
      ISelection selection = window.getSelectionService().getSelection("org.eclipse.jdt.ui.PackageExplorer");
      if (selection instanceof IStructuredSelection) {
        IStructuredSelection ssel = (IStructuredSelection) selection;
        Object obj = ssel.getFirstElement();
        if (obj instanceof ICompilationUnit) {
          icu = (ICompilationUnit) obj;
          //System.out.println("DEBUG: PackageExplorer ICU path=" + icu.getPath());
        }
      }
    } else {
      // look at the current editor and its file
      IEditorPart editor = page.getActiveEditor();
      if (editor != null && editor.getEditorInput() instanceof IFileEditorInput) {
        IFileEditorInput f = (IFileEditorInput) editor.getEditorInput();
        icu = JavaCore.createCompilationUnitFrom(f.getFile());
        //System.out.println("DEBUG: Editor ICU path=" + icu.getPath());
      }
    }
    if (icu != null && isJavaSource(icu)) {
      return icu;
    } else {
      return null;
    }
  }

  /**
   *
   * @param icu compilation unit
   * @return true if the underlying file is a Java source file, not containing tests.
   */
  private boolean isJavaSource(ICompilationUnit icu) {
    return "java".equals(icu.getPath().getFileExtension()) && !icu.getPath().toPortableString().endsWith("Test.java");
  }

  /**
   * the command has been executed, so extract the needed information from the
   * application context.
   */
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ICompilationUnit cu = getSelectedJavaFile();
    if (cu == null) {
      mOut.println("No current class selected to run Jumble on.");
      return null;
    }
//    IEditorPart editor = HandlerUtil.getActiveEditor(event);
//    try {
//      System.out.println("DEBUG: targeteditor=" + editor
//          + " for file=" + editor.getEditorInput().getAdapter(IFile.class)
//          + " selJavaFile=" + getClassName(cu));
//    } catch (JavaModelException e1) {
//      // TODO Auto-generated catch block
//      e1.printStackTrace();
//    }
    runJumble(cu);
    return null;
  }

  protected void runJumble(ICompilationUnit cu) {
    final IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
    final String pluginLocation;
    try {
      pluginLocation = Activator.getDefault().getPluginFolder().getAbsolutePath();
      mOut.println("Starting Jumble with plugin folder " + pluginLocation);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    IPath jumbleJarPath = new Path(pluginLocation + "/jumble.jar");

    try {
      // Get launch manager
      ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
      ILaunchConfigurationType type = manager
          .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);

      // Delete previous configuration
      ILaunchConfiguration[] configurations = manager.getLaunchConfigurations(type);
      for (ILaunchConfiguration configuration : configurations) {
        if (configuration.getName().equals(LAUNCH_NAME)) {
          configuration.delete();
          break;
        }
      }
      ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, LAUNCH_NAME);

      // Use the default JRE
      workingCopy.setAttribute(
          IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
          JavaRuntime.getDefaultVMInstall().getInstallLocation().toString());

      // Use the specified JVM arguments
      String extraArgs = prefs.getString(PreferenceConstants.P_ARGS);

      // Set up command line arguments
      String className = getClassName(cu);
      mOut.println("Mutating file: " + cu.getPath().toPortableString()); // or print className?

      // Set up class paths
      workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);
      IRuntimeClasspathEntry jumbleJarEntry = JavaRuntime.newArchiveRuntimeClasspathEntry(jumbleJarPath);
      String jar = jumbleJarEntry.getMemento();
      workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, Collections.singletonList(jar));
      final String classPath = getClasspathString(cu.getJavaProject());

      boolean verbose = prefs.getBoolean(PreferenceConstants.P_VERBOSE);
      boolean returnVals = prefs.getBoolean(PreferenceConstants.P_RETURNS);
      boolean increments = prefs.getBoolean(PreferenceConstants.P_INCREMENTS);
      boolean inlineConstants = prefs.getBoolean(PreferenceConstants.P_INLINE_CONSTANTS);
      boolean constantPoolConstants = prefs.getBoolean(PreferenceConstants.P_CONSTANT_POOL_CONSTANTS);
      boolean stringConstants = prefs.getBoolean(PreferenceConstants.P_STRING_CONSTANTS);
      boolean switchStatements = prefs.getBoolean(PreferenceConstants.P_SWITCH);
      boolean mutateAssignments = prefs.getBoolean(PreferenceConstants.P_STORES);
      workingCopy.setAttribute(
          IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
          "com.reeltwo.jumble.Jumble");
      String args0 = "-p com.reeltwo.jumble.ui.EclipseFormatListener " // so we get clickable links.
          + (returnVals ? "-r " : "")
          + (inlineConstants ? "-k " : "")
          + (increments ? "-i " : "")
          + (verbose ? "-v " : "")
          + (constantPoolConstants ? "-w " : "")
          + (stringConstants ? "-S " : "")
          + (switchStatements ? "-j " : "")
          + (mutateAssignments ? "-X " : "");
      String args1 = "--classpath \"" + classPath + "\" ";
      String args2 = extraArgs + " " + className;
      String argsSummary = args0 + "--classpath ... " + args2;
      String argsReal = args0 + args1 + args2;
      workingCopy.setAttribute(
          IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
          argsReal);
      mOut.println("classpath.length=" + classPath.length() + " args.length=" + argsReal.length());
      mOut.println("--classpath=" + classPath);
      mOut.println("Launching... with args: " + argsSummary);

      // Now run...
      ILaunchConfiguration configuration = workingCopy.doSave();
      DebugUITools.launch(configuration, ILaunchManager.RUN_MODE);
      mOut.println("Done launch.");
    } catch (Exception ex) {
      mOut.println(ex.getLocalizedMessage());
      StackTraceElement[] trace = ex.getStackTrace();
      for (StackTraceElement elem : trace) {
        mOut.println(elem.toString());
      }
    }
  }

  /**
   * Constructs the classpath for the given project, removing all duplicates.
   * Only the first occurrence of each path is retained.
   *
   * @param curProject
   * @return A system-dependent classpath.
   * @throws JavaModelException
   */
  String getClasspathString(IJavaProject curProject) throws JavaModelException {
    Set<String> paths = new LinkedHashSet<>(100); // must preserve the order.
    buildClasspath(curProject.getJavaProject(), paths);

    // now concatenate all the paths, in their original order.
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for (String cp : paths) {
      sb.append(sep);
      sep = PATH_SEPARATOR;
      sb.append(cp);
    }
    return sb.toString();
  }

  private void buildClasspath(IJavaProject curProject, Set<String> paths)
      throws JavaModelException {
    IClasspathEntry[] entries = curProject.getResolvedClasspath(true);
    IWorkspaceRoot root = curProject.getProject().getWorkspace().getRoot();
    for (IClasspathEntry entry : entries) {
      IPath path = entry.getPath();
      IResource res = root.findMember(path);
      if (res == null) {
        paths.add(path.toOSString());
      } else {
        if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
          IPath outputLocn = curProject.getOutputLocation();
          IResource resource = root.findMember(outputLocn);
          if (resource == null) {
            mOut.println("WARNING: cannot find source code location for fragment: "
                + entry + " outputLocn="+ outputLocn);
          } else {
            IPath outputLocation = resource.getLocation();
            paths.add(outputLocation.toOSString());
          }
        } else if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
          buildClasspath(JavaCore.create((IProject) res), paths);
        } else {
          paths.add(res.getLocation().toOSString());
        }
      }
    }
  }

  /**
   * Extracts the full class name of the given compilation unit.
   *
   * @param cu
   * @return
   * @throws JavaModelException
   */
  protected String getClassName(ICompilationUnit cu) throws JavaModelException {
    IPackageDeclaration[] packages = cu.getPackageDeclarations();
    final String packageName;
    final String className;

    if (packages == null || packages.length == 0) {
      packageName = null;
    } else if (packages.length == 1) {
      packageName = packages[0].getElementName();
    } else {
      packageName = packages[0].getElementName();
      mOut.println("Error: too many packages: ");
      for (IPackageDeclaration package1 : packages) {
        mOut.println(package1.getElementName());
      }
    }
    final String rawClassName = cu.getElementName().substring(0,
        cu.getElementName().lastIndexOf('.'));
    if (packageName == null) {
      className = rawClassName;
    } else {
      className = packageName + "." + rawClassName;
    }
    return className;
  }

  private MessageConsole findConsole(String name) {
    ConsolePlugin plugin = ConsolePlugin.getDefault();
    IConsoleManager conMan = plugin.getConsoleManager();
    IConsole[] existing = conMan.getConsoles();
    for (IConsole element : existing) {
      if (name.equals(element.getName())) {
        return (MessageConsole) element;
      }
    }
    //no console found, so create a new one
    MessageConsole myConsole = new MessageConsole(name, null);
    conMan.addConsoles(new IConsole[]{myConsole});
    return myConsole;
 }
}
