package nz.ac.waikato.jumble;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * @author Lesley Payne
 */
public class Node implements Comparable<Node>, Cloneable{

	public final BitSet testsIncluded; // true = 1 = included, false = 0 = excluded
	private final BitSet mutationsCovered; // true = 1 = covered, false = 0 = uncovered
    private final int numTests; // total no. of tests in suite
    private final int numMutations; // total no. of mutations in code

    Node(int totalTests, int totalMutations)
    {
    	numTests = totalTests;
    	numMutations = totalMutations;
    	testsIncluded = new BitSet(numTests); // initialise testsIncluded, size = total no. of tests in suite. All bits are 0 initially.
    	mutationsCovered = new BitSet(numMutations); // initialise mutationsCovered, size = total no. of mutations in code. All bits are 0 initially.
    }
    
    /**
     * Alternative constructor method used as a 'deep clone' for the addtest method
     * @param totalTests
     * @param totalMutations
     * @param testInc
     * @param mutaCov
     */
    private Node(int totalTests, int totalMutations, BitSet testInc, BitSet mutaCov) {
    	numTests = totalTests;
    	numMutations = totalMutations;
    	testsIncluded = testInc; 
    	mutationsCovered = mutaCov; 
		
	}

    /**
     * Creates a new node same as the current one, but with another test added.
     * @param testnum The index number of the test to add.
     * @param mutations The bitset of mutations that the new test covers
     * @return Node identical to current Node, except with another test added.
     */
    Node addTest(int testnum, BitSet mutations)
    {
//    	//create a new node, same as the current node
//    	Node newNode;
//		try {
//			newNode = (Node) this.clone();
//		} catch (CloneNotSupportedException e) {
//			throw new RuntimeException(e);
//		}
		BitSet newTestInc = new BitSet(numTests);
		BitSet newMutaCov = new BitSet(numMutations);
		newTestInc.or(testsIncluded);
		newMutaCov.or(mutationsCovered);
		// if the test is not already added,
    	if(testsIncluded.get(testnum) == false)  
		{
    		// turn its bit to 1 (true). it is now included
    		//System.out.println("addtest " + testnum +" tests incl before " + newTestInc.toString() + "mutations was"+ newMutaCov.toString());
    		newTestInc.set(testnum);
    		newMutaCov.or(mutations);// also, add the mutations it covers to the total mutations covered
        	//System.out.println("addtest " + testnum +" tests incl after " + newTestInc.toString() + "mutations was"+ newMutaCov.toString());
		}else{
			// otherwise return failure message?
            throw new RuntimeException("testnum was " +testnum);
        }
    	    	
    	return new Node(numTests, numMutations, newTestInc, newMutaCov);
    }

    /**
     * Returns the nodes adjacent to the current node, i.e. all sets of tests same as the current node, but with one test added
     * @param mutationsMatrix the matrix of which tests cover which mutations
     * @return the nodes adjacent to the current node.
     */
    ArrayList<Node> getNeighbours(boolean[][] mutationsMatrix)
    {
        // returns all tests not yet in set as a list of nodes
    	// i.e. all nodes same as this but with another test added
    	ArrayList<Node> neighbours = new ArrayList<Node>();
    	for(int i=0; i < numTests; i++)
    	{
    		if(testsIncluded.get(i) == false) // for each test not yet included
    		{
    			// find the mutations covered by this new test
    			BitSet mutations = new BitSet(numMutations);
    			// by copying the i-th column of mutationsMatrix
    			for(int j = 0; j < numMutations; j ++) // iterate through that column
    			{
    				//System.out.println("i=" + i + " j=" + j);
    				if(mutationsMatrix[j][i]){
    					mutations.set(j); 
    				}
    			}
    			
	    		// make a new node, same as this one, but with the test added
    			Node neighbourNode = addTest(i, mutations); // get the right bitset for mutations here later
	        	// add it to the list
    			neighbours.add(neighbourNode);
    			//System.out.println(neighbourNode.toString());
    		}
    	}
    	// return all the nodes
    	return neighbours;
    }

    /**
     * The number of tests included in in this node.
     * @return number of tests included
     */
    int pastPathCost()
    {
    	return testsIncluded.cardinality();
    }

    /**
     * The number of mutations not covered by the set of tests in this node.
     * @return number of mutations not covered
     */
    int futurePathEstimate()
    {
    	//System.out.println(mutationsCovered.length());
    	//System.out.println("future path of" + this.toString() + " is " + (numMutations - mutationsCovered.cardinality()));
    	return numMutations - mutationsCovered.cardinality();
    }

	/**
     * f(x) value of the node, i.e. pastPath + futurePath
     * @return
     */
    int totalPathCost()
    {
    	return this.pastPathCost() + this.futurePathEstimate();
    }

    /**
     * Comparison method needed to implement Comparable. 
     * @param other Another node
     * @return 0 if path costs are equal; 1 if the argument node's cost is less than; -1 if the argument node's cost is more than
     */
    public int compareTo(Node other)
    {
        // based on their total path cost
    	if (Integer.compare(this.pastPathCost(), other.pastPathCost()) != 0) {
    		return Integer.compare(this.pastPathCost(), other.pastPathCost());
		}
    	else {
    		return Integer.compare(this.futurePathEstimate(), other.futurePathEstimate());
		}
    	
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((testsIncluded == null) ? 0 : testsIncluded.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (testsIncluded == null) {
			if (other.testsIncluded != null)
				return false;
		} else if (!testsIncluded.equals(other.testsIncluded))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return testsIncluded.toString();
	}

	
}
