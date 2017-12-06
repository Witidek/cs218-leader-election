package bully_election;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;

public class ThreadProcess implements Runnable {
	// Node info
	private NodeInstance node;
	private int totalNodes;
	
	// Message once
	private static boolean doneMessaging[];
	
	// This node's regular socket
	private ServerSocket socket;
	
	// For automation random events
	private Random r;

	public NodeInstance getProcess() {
		return node;
	}

	public static boolean isDoneMessaging(int index) {
		return ThreadProcess.doneMessaging[index];
	}

	public static void setDoneMessaging(boolean flag, int index) {
		ThreadProcess.doneMessaging[index] = flag;
	}
	
	public ThreadProcess(NodeInstance node, int totalNodes) {
		this.node = node;
		this.totalNodes = totalNodes;
		this.r = new Random();
		ThreadProcess.doneMessaging = new boolean[totalNodes];
		for (int i = 0; i < totalNodes; i++) {
			ThreadProcess.doneMessaging[i] = false;
		}
	}

	synchronized private void recovery() {
		while (ElectionProcess.isCurrentElection()) {
			// Wait for current election to finish before recovering
		}
		
		// Come back online and find leader
		try {
			System.out.println("Node[" + this.node.getNodeID() + "] -> Attempting to recover from crash");
			this.node.setIsDown(false);
			
			// Disable leader pings while we talk to leader
			ElectionProcess.pingLock.lock();
			ElectionProcess.setAllowPing(false);
			
			// Ask the leader about its ID and priority
			Socket outgoing = new Socket(InetAddress.getLocalHost(), 12345);
			Scanner scan = new Scanner(outgoing.getInputStream());
			PrintWriter out = new PrintWriter(outgoing.getOutputStream(), true);
			System.out.println("Node[" + this.node.getNodeID() + "]: Who is the leader?");
			out.println("Who is the leader?");
			out.flush();

			// Read reply for leader ID
			String nodeID = scan.nextLine();
			// Read reply for leader priority
			String nodePriority = scan.nextLine();
			
			// If this priority is higher than leader, bully leader and become leader
			if (this.node.getNodePriority() > Integer.parseInt(nodePriority)) {
				out.println("Resign");
				out.flush();
				System.out.println("Node[" + this.node.getNodeID() + "] -> Bullying Node[" + nodeID + "]");
				
				// Get confirmation for current leader resigning and become leader
				String resignStatus = scan.nextLine();
				if (resignStatus.equals("Successfully Resigned")) {
					this.node.setIsLeader(true);
					System.out.println("Node[" + this.node.getNodeID() + "] -> Became new leader by bullying");
					System.out.println("NEW LEADER: Node[" + this.node.getNodeID() + "]");
				}
			} else {
				// Current leader is higher priority, end conversation and start regular node socket
				out.println("Don't Resign");
				out.flush();
				this.socket = new ServerSocket(10000 + this.node.getNodeID());
				
				ElectionProcess.setAllowPing(true);
			}

			ElectionProcess.pingLock.unlock();
			scan.close();
			outgoing.close();

		} catch (IOException e) {
			// Tried to recover when leader crashed, before election!
			ElectionProcess.pingLock.unlock();
			ElectionProcess.setAllowPing(true);
			System.out.println("Node[" + this.node.getNodeID() + "] -> Found no leader while recovering");
			
			// Open regular node socket to prepare for election
			try {
				this.socket = new ServerSocket(10000 + this.node.getNodeID());
			}catch (IOException ex) {
				//
			}
		}
	}

	synchronized private void pingLeader() {
		try {
			// Wait for lock
			ElectionProcess.pingLock.lock();
			
			// Ping leader if pings allowed
			if (ElectionProcess.isAllowPing()) {
				System.out.println("Node[" + this.node.getNodeID() + "]: Are you alive?");
				Socket outgoing = new Socket(InetAddress.getLocalHost(), 12345);
				outgoing.close();
			}
		} catch (Exception ex) {
			// Connection failed, leader offline, trigger election
			ElectionProcess.setAllowPing(false);
			ElectionProcess.setCurrentElection(true);
			ElectionProcess.setElectionDetector(this.node);

			System.out.println("Node[" + this.node.getNodeID() + "]: Leader is down");
			System.out.println("Node[" + this.node.getNodeID() + "] -> Initiating election");
		} finally {
			ElectionProcess.pingLock.unlock();
		}
	}

	private void simulateWork() {
		// Generate random number 0-100
		int luck = r.nextInt(100);
		
		// Crash 5 out of 100 times 
		if (luck > 50 && luck % 10 == 1) {
			// Close socket
			if (socket != null && !socket.isClosed()) {
				try {
					socket.close();
					this.node.setIsDown(true);
					System.out.println("Node[" + this.node.getNodeID() + "] -> Crashed");
					
					// Recover after a while
					Thread.sleep(10000);
					recovery();

				} catch (Exception e) {
					System.out.println("Non-leader failed to crash, what?");
					System.out.println(e.getMessage());
				}
			}
		}else {
			// Simulate work by waiting based on random number
			try {
				Thread.sleep((luck + 1) * 100);
			} catch (InterruptedException e) {
				System.out.println("Error Executing Thread:" + node.getNodeID());
				System.out.println(e.getMessage());
			}
		}
	}

	synchronized private boolean sendMessage() {
		try {
			// Wait for our turn to message
			ElectionProcess.electionLock.lock();
			// Election is on, haven't messaged yet, higher then detector, not isDown
			if (ElectionProcess.isCurrentElection() && !ThreadProcess.isDoneMessaging(this.node.getNodeID() - 1)
					&& this.node.getNodePriority() >= ElectionProcess.getElectionDetector().getNodePriority()
					&& !this.node.isDown()) {
				
				// Message all higher nodes
				for (int i = this.node.getNodeID() + 1; i <= this.totalNodes; i++) {
					try {
						Socket electionMessage = new Socket(InetAddress.getLocalHost(), 10000 + i);
						
						// Got response
						System.out.println("Node[" + this.node.getNodeID() + "] -> Node[" + i + "] Responded to election message successfully");
						electionMessage.close();
						
						// Release lock and return early
						ThreadProcess.setDoneMessaging(true, this.node.getNodeID() - 1);
						ElectionProcess.electionLock.unlock();
						return true;
					} catch (IOException ex) {
						
						// No response
						System.out.println("Node[" + this.node.getNodeID() + "] -> Node[" + i + "] Did not respond to election message");
					} catch (Exception e) {
						System.out.println("Exception in sendMessage()");
						System.out.println(e.getMessage());
					}
				}
				// Done messaging for this node
				ThreadProcess.setDoneMessaging(true, this.node.getNodeID() - 1);
				ElectionProcess.electionLock.unlock();
				return false;
			}else {
				throw new Exception();
			}
		}catch (Exception e) {
			// Unlock and do not become leader on exception
			ElectionProcess.electionLock.unlock();
			return true;
		}
	}

	

	synchronized private void doLeaderWork() {
		try {
			// Open leader socket and allow pings
			Socket incoming = null;
			ServerSocket s = new ServerSocket(12345);
			ElectionProcess.setAllowPing(true);
			
			// Set a number of connects to accept before crashing
			int crashTimer = this.r.nextInt(5) + 5;

			for (int i = 0; i < crashTimer; i++) {
				// Get a connection
				incoming = s.accept();
				
				// Reply to ping
				if (ElectionProcess.isAllowPing()) {
					System.out.println("Node[" + this.node.getNodeID() + "]: Yes");
				}
				
				Scanner scan = new Scanner(incoming.getInputStream());
				PrintWriter out = new PrintWriter(incoming.getOutputStream(), true);
				
				// Receive messages from recovered node
				while (scan.hasNextLine()) {
					String line = scan.nextLine();
					if (line.equals("Who is the leader?")) {
						// Reply with leader ID and priority
						System.out.println("Node[" + this.node.getNodeID() + "]: " + this.node.getNodeID() + " is the leader");
						out.println(this.node.getNodeID());
						out.flush();
						out.println(this.node.getNodePriority());
						out.flush();
					} else if (line.equals("Resign")) {
						// Getting bullied, resign as leader
						this.node.setIsLeader(false);
						out.println("Successfully Resigned");
						out.flush();
						incoming.close();
						s.close();
						System.out.println("Node[" + this.node.getNodeID() + "] -> Successfully resigned");
						scan.close();
						return;
					} else if (line.equals("Don't Resign")) {
						break;
					}
				}
				scan.close();
			}

			// Leader crashes now, not leader, set isDown, close sockets
			this.node.setIsLeader(false);
			this.node.setIsDown(true);
			
			try {
				System.out.println("Node[" + this.node.getNodeID() + "] -> Crashed");
				incoming.close();
				s.close();
				socket.close();
				
				// Wait a while before recovering
				Thread.sleep(15000);
				recovery();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

		} catch (IOException e) {
			System.out.println("Failed to open leader socket or accept on leader socket");
			System.out.println(e.getMessage());
		}
	}

	public void run() {
		// Set socket for node with port 10000 + nodeID
		try {
			int nodeID = this.node.getNodeID();
			socket = new ServerSocket(10000 + nodeID);
		} catch (IOException e) {
			System.out.println("Failed to create socket on first run");
			System.out.println(e.getMessage());
		}
		
		// Main logic loop
		while (true) {
			if (this.node.isLeader()) {
				// Do leader stuff
				doLeaderWork();
			}else {
				// Do non-leader stuff
				while (true) {
					// Simulate work to wait some time or crash
					simulateWork();
					
					// Ping leader
					pingLeader();
					
					// Do election if needed
					if (ElectionProcess.isCurrentElection()) {
						
						// No higher node, set self as leader
						if (!sendMessage()) {
							this.node.setIsLeader(true);
							ElectionProcess.setCurrentElection(false);
							System.out.println("NEW LEADER: Node[" + this.node.getNodeID() + "]");

							// Reset doneMessaging[]
							for (int i = 0; i < totalNodes; i++) {
								ThreadProcess.setDoneMessaging(false, i);
							}

							// Break out of non-leader loop to get to leader part
							break;
						}
					}
				}
			}
		}
	}
}
