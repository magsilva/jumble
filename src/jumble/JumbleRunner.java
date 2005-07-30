package jumble;

import java.util.HashSet;

import jumble.util.Utils;

/** Class implementing a java version of jumble.sh.
 *  @author Tin Pavlinic
 *  @version 1.0
 */
public class JumbleRunner {
    public static void main(String [] args) throws Exception {
        try {
            //Process arguments
            String excludes = Utils.getOption('x', args);
            if(excludes.equals(""))
                excludes = Utils.getOption("exclude", args);
            
        	boolean constants = Utils.getFlag('k',args) || Utils.getFlag("inlineconstants", args);
        	boolean returns = Utils.getFlag('r', args) || Utils.getFlag("returns", args);
        	boolean increments = Utils.getFlag('i', args) || Utils.getFlag("increments", args);
        	String className = Utils.getNextArgument(args);
            String testName = Utils.getNextArgument(args);
            int startingPoint = Integer.parseInt(Utils.getNextArgument(args));
            
            final HashSet ignore = new HashSet();
            
            if (!excludes.equals("")) {
        	    final String[] ig = excludes.split(",");
        	    for (int i = 0; i < ig.length; i++) {
        		ignore.add(ig[i]);
        	    }
        	} else {
        	    ignore.add("main");
        	    ignore.add("integrity");
        	}
            
            //First count the number of possible mutaton points
            Mutater m = new Mutater(0); // run in count mode only, param ignored
    	    m.setIgnoredMethods(ignore);
    	    m.setMutateInlineConstants(constants);
    	    m.setMutateReturnValues(returns);
    	    m.setMutateIncrements(increments);
    	    
    	    
    	    int count = m.countMutationPoints(className);
    	    
    	    if(startingPoint >= count)
    	        throw new RuntimeException("StartingPoint must be smaller than the "
    	                + "total number of mutation points");
    	    //now run the tests for every mutation
    	    for(int i = startingPoint; i < count; i++) {
    	        m = new Mutater(i);
    	        m.setIgnoredMethods(ignore);
    	        m.setMutateInlineConstants(constants);
    	        m.setMutateReturnValues(returns);
    	        m.setMutateIncrements(increments);
    	        
    	        final ClassLoader loader = new Jumbler(className.replace('/', '.'), m);
    	        final Class clazz = loader.loadClass("jumble.JumbleTestSuite");
    	        System.out.println(clazz.getMethod("run", new Class[] { String.class }).invoke(null, new Object[] { testName.replace('/', '.') }));
    	    }
            
        } catch(Exception e) {
            StackTraceElement [] st = e.getStackTrace();
            System.out.print(e.getMessage() + " ");
            for(int i = 0; i < st.length; i++) {
                System.out.print(st[i] + " ");
            }
            System.out.println("Usage: java jumble.JumbleRunner [Options] [Class] [TestClass] [StartingPoint]");
        }
    }
}
