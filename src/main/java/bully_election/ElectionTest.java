package bully_election;

import java.util.Scanner;

public class ElectionTest {

	public static void main(String[] args) {
		// Getting the number of nodes that needs to be created from the user
		System.out.println("Enter the number of nodes to create for the test system:");
		Scanner in = new Scanner(System.in);
		int size = in.nextInt();
		in.close();
		
		// Create ThreadProcess for each node
		ThreadProcess[] nodes = new ThreadProcess[size];
		for (int i = 0; i < size; i++) {
			// Priority same as as ID by default, starts at 1
			nodes[i] = new ThreadProcess(new NodeInstance(i+1, i+1), size);
		}
		
		// Initial leader selection
		try {	
			ElectionProcess.initialElection(nodes);
		} catch (NullPointerException e) {
			System.out.println(e.getMessage());
		}
		
		// Start all threads in the system
		for (int i = 0; i < size; i++) {
			Thread thread = new Thread(nodes[i]);
			thread.start();
		}
	}
}
