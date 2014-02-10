package nz.ac.waikato.jumble;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
/**
 * @author Lesley Payne
 */
public class AStar {

	private static boolean[][] mutationsMatrix; // array mapping tests to the mutations they cover
    private static int numTests; // total no. of tests in suite
    private static int numMutations; // total no. of mutations in code
	private static PriorityQueue<Node> openSet; // nodes yet to be explored
	private static HashSet<Node> closedSet; // the set of nodes that have already been explored
	static ArrayList<String> testNames; // the names of the tests
	static ArrayList<String> mutationNameStrings; // the names of the mutations
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		//long start = System.currentTimeMillis();
		
		// initialise priority queue and hashset
		openSet = new PriorityQueue<Node>();
		closedSet = new HashSet<Node>();
		
		// list of test names
		testNames = new ArrayList<String>();
		mutationNameStrings = new ArrayList<String>();
		// get file names
		String mappingFile = args[0];
		String matrixFile = args[1];
		
		// read MAPPING FILE, store test names		
		BufferedReader br1 = null;
		String line = "";	 
		try
		{	 
			br1 = new BufferedReader(new FileReader(mappingFile));
			br1.readLine(); // first line is just "Tests mapping:" so ignore that
			// read through the tests
			line = br1.readLine();
			while (line != null && !line.isEmpty())
			{	 
				testNames.add(line); 
				numTests ++;
				line = br1.readLine();
			}
			// skip through "Tests cover zero mutations" and the tests listed under it
			while ((line = br1.readLine()) != null) {
				if (line.contains("Mutations mapping:")) 
					break;				
			}
			// read through the mutation names
			while ((line = br1.readLine()) != null) {
				mutationNameStrings.add(line);
				numMutations ++;				
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br1 != null) {
				try {
					br1.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// read MATRIX file, put tests<->mutations data into mutations matrix
		mutationsMatrix = new boolean[numMutations][numTests];
		BufferedReader br2 = null;
		line = "";	 
		try
		{	 
			int rowNum = 0;
			br2 = new BufferedReader(new FileReader(matrixFile));
			for (int i = 0; i < numMutations; i++)
			{	 
			    if ((line = br2.readLine()) != null)
			    {
					// use space as separator?
					String[] row = line.split(" ");
					// store row[] as a row of booleans in mutationsMatrix
					for (int i1 = 0; i1 < row.length -1; i1++) // - 1 so that we ignore the last column
					{
						mutationsMatrix[rowNum][i1] = row[i1].equals("1");
					}
					rowNum++;
			    }    
			}
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br2 != null) {
				try {
					br2.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	 
//		// debug output 
//		for (int i = 0; i < mutationsMatrix.length; i++) {
//			System.out.println(Arrays.toString(mutationsMatrix[i]));
//		}
//		System.out.println("Done reading in files");
		
		
		Node shortestPath = findMinimalSet();
		if (shortestPath == null) // if no path is found
		{
			// output an error message: there is no path covering all mutations
			System.err.println("Error: no set of tests could be found that covers all mutations.");
		}
		else // otherwise a path has been found
		{
			System.out.println(shortestPath.toString());
			System.out.println("Minimised test set: (" + shortestPath.pastPathCost() + " tests)");
			for (int i = 0; i < numTests; i++) {
				if (shortestPath.testsIncluded.get(i) == true) {
					System.out.println(testNames.get(i));
				}
			}
		}
		
		//long end = System.currentTimeMillis();;
	    //System.out.println((end - start) + " ms");
		
	}

	/**
	 * The A* algorithm to find the minimal set of tests
	 * @return the minimal set of tests that covers all mutations
	 */
	static Node findMinimalSet()
	{
		Node currentNode = null;
		// add the empty node to the open set
		Node startNode = new Node(numTests, numMutations);
		openSet.add(startNode);
		
		// loop until the open set is empty
		while(!openSet.isEmpty())
		{
			// set the currentNode to be the node in the open set with the lowest cost
			currentNode = openSet.peek();			
			// if the current node covers all mutations
			if(currentNode.futurePathEstimate() == 0)
			{		
				return currentNode; // we have reached the end! return this node
			}			
			else
			{
				// remove current Node from the open set, add it to the closed set
				closedSet.add(openSet.remove());
				// for each neighbour of current 
				ArrayList<Node> neighbourNodes = currentNode.getNeighbours(mutationsMatrix);
				for(Node neighbour: neighbourNodes)
				{
					// check that it's not already in the closed set
					if(closedSet.contains(neighbour) == false)
					{
						// add to openset
						openSet.add(neighbour);
					}
				}
			}
		}
		// if open set becomes empty, i.e. no path is found
		// note: it should be impossible to get here, since there will always be a path that covers all mutations
		System.out.println("Reached end of A* algorithm; no path found. Final bitset is '" + currentNode.toString() + "' and future path is " + currentNode.futurePathEstimate());
		return null;
	}
}
