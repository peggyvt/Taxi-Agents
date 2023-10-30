package agents;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import agents.Node.Client;

public class Grid extends Agent {

	public static int tries=0;
	private int maxTries=100;
	private String[] agentArray;
	private int numberOfAgents;
	private static int x = 5;
	private static int y = 5;
	public static String[][] locations = new String[5][5];
	static Node[][] myNodes = new Node[5][5]; // ALL GRID NODES WITH NEIGHBOURS
	private Boolean[] rightOfWay = {true, true, true};
	int[] distanceToDestination = new int[3];
	public static Client[] clients = new Client[3];
	public static Boolean[][] isAllowed = new Boolean[3][4];
	public static Boolean[][] isInterested = new Boolean[3][4];
	public static int counter1 = 0;
	public static int counter2 = 0;
	int id = 0;

	public void setup() {

		try {Thread.sleep(50);} catch (InterruptedException ie) {} // important
		// search the registry for agents
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("agent");
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			numberOfAgents = result.length;
			agentArray = new String[numberOfAgents];
			for (int i = 0; i < numberOfAgents; ++i) {
				agentArray[i] = result[i].getName().getLocalName();
			}
		}
		catch (FIPAException fe) {fe.printStackTrace();}
		Arrays.sort(agentArray);
		System.out.println("Found:");
		for (int i = 0; i < numberOfAgents; ++i) System.out.println(agentArray[i]);
		findStartingLocations();

		addBehaviour(new CyclicBehaviour(this) {
			public void action() {

				try {Thread.sleep(50);} catch (InterruptedException ie) {}
				
				if(tries<maxTries) {
					printGrid();
					String[] messageQueue = new String[3];
					for (String agentName: agentArray) { 
						ACLMessage message = new ACLMessage(ACLMessage.INFORM);
						//refer to receiver by local name
						message.addReceiver(new AID(agentName, AID.ISLOCALNAME));
						message.setContent("Pick a Direction");
						send(message);
						ACLMessage msg = null;

						msg = blockingReceive();
						
						String senderName = msg.getSender().getLocalName();
						int id = Integer.parseInt(senderName.
								replaceAll("[\\D]", "")); // AGENT-37 -> 37
						
						String[] array = msg.getContent().split(",");

						// each agent's decision -- next move (up, down, etc)
						String decision = array[0];
						
						// in case there are no clients left, terminate the program 
						if (decision == "There are no clients left") {
							terminateProgram();
						}
						 
						// length of each distance, from node to target 
						int length = Integer.parseInt(array[1]);
						
						// inserting each agent's next move and length to destination
						messageQueue[id-1] = decision;
						distanceToDestination[id-1] = length;
					}
					// checking for conflicts here 
					checkConflicts(messageQueue); // in case there are agents who want to go to the same cell
					checkSecondTypeOfConflict(messageQueue); // in case there are agents who do not serve a client and they all go in a cell who only has one client
					
					for (int i = 0; i < numberOfAgents; i++) {
						
						if(rightOfWay[i] == false) {
							continue;
						}
						
						if (messageQueue[i].contentEquals("Client target and current node are the same staying here")) {
							System.out.println(messageQueue[i]);
							makeMove(messageQueue[i], i+1);
						} else if (messageQueue[i].contentEquals("There are no clients left")) {
							System.out.println("There are no clients left");
						} else {
							int a = i+1;
							System.out.println("Agent-" + a + " moving towards: " + messageQueue[i]);
							makeMove(messageQueue[i], i+1);
						}
					}
					
					for (int i=0; i < numberOfAgents; i++) {
						rightOfWay[i] = true;
					}
					
					tries++;
				} else {
					terminateProgram();
				}
			}

			private void checkSecondTypeOfConflict(String[] messageQueue) {
				
				// SECOND TYPE OF CONFLICT
				// check if there are agents who do not serve a client and they all go in a cell who only has one client
				
				for (int i=0; i < 4; i++) { 
					if (isInterested[0][i] == isInterested[1][i] && isInterested[1][i]== true) {
						counter2 += 1; // total number of conflicts
						if (isInterested[0][i] == isInterested[2][i]) {
							// in case all three of them have conflict
							System.out.println("\n THERE WAS CONFLICT.");
							int min = minDestination();
							for (int ii=0; ii < numberOfAgents; ii++) {
								if(ii == min) {
									continue;
								}
								isAllowed[ii][i] = false;
								ACLMessage message = new ACLMessage(ACLMessage.INFORM);
								// refer to receiver by local name
								int k = i+1;
								String agentName = "Agent-" + Integer.toString(k);
								message.addReceiver(new AID(agentName, AID.ISLOCALNAME));
								message.setContent("Pick a Direction");
								send(message);
								ACLMessage msg = null;

								msg = blockingReceive();
								
								
								String[] array = msg.getContent().split(",");

								// each agent's decision -- next move (up, down, etc)
								String decision = array[0];
								 
								// length of each distance, from node to target 
								int length = Integer.parseInt(array[1]);
								
								// inserting each agent's next move and length to destination
								messageQueue[ii] = decision;
								distanceToDestination[ii] = length;
								isInterested[ii][i] = false;
							}
						} else {
							// in case Agent-1 & Agent-2 have conflict
							secondConflictSolution(messageQueue, 0, 1, i);
							System.out.println("\n THERE WAS CONFLICT.");
						}
					} else if (isInterested[1][i] == isInterested[2][i]&& isInterested[2][i]== true) {
						counter2 += 1; // total number of conflicts
						// in case Agent-2 & Agent-3 have conflict
						secondConflictSolution(messageQueue, 1, 2, i);
						System.out.println("\n THERE WAS CONFLICT.");
					} else if (isInterested[0][i] == isInterested[2][i] && isInterested[2][i]== true) {
						counter2 += 1; // total number of conflicts
						System.out.println("\n THERE WAS CONFLICT.");
						// in case Agent-1 & Agent-3 have conflict
						secondConflictSolution(messageQueue, 0, 2, i);
					}
				}
			}

			private void secondConflictSolution(String[] messageQueue, int i, int j, int cell) {

				if (distanceToDestination[i] > distanceToDestination[j]) {
					isAllowed[i][cell] = false;
					
					ACLMessage message = new ACLMessage(ACLMessage.INFORM);
					// refer to receiver by local name
					int k = i + 1;
					String agentName = "Agent-" + Integer.toString(k);
					message.addReceiver(new AID(agentName, AID.ISLOCALNAME));
					message.setContent("Pick a Direction");
					send(message);
					ACLMessage msg = null;

					msg = blockingReceive();
					
					String[] array = msg.getContent().split(",");

					// each agent's decision -- next move (up, down, etc)
					String decision = array[0];
					 
					// length of each distance, from node to target 
					int length = Integer.parseInt(array[1]);
					
					// inserting each agent's next move and length to destination
					messageQueue[i] = decision;
					distanceToDestination[i] = length;
					isInterested[i][cell] = false;
				} else {
					isAllowed[j][cell] = false;
					
					ACLMessage message = new ACLMessage(ACLMessage.INFORM);
					// refer to receiver by local name
					int k = j+1;
					String agentName = "Agent-" + Integer.toString(k);
					message.addReceiver(new AID(agentName, AID.ISLOCALNAME));
					message.setContent("Pick a Direction");
					send(message);
					ACLMessage msg = null;

					msg = blockingReceive();
					
					String[] array = msg.getContent().split(",");

					// each agent's decision -- next move (up, down, etc)
					String decision = array[0];
					 
					// length of each distance, from node to target 
					int length = Integer.parseInt(array[1]);
					
					// inserting each agent's next move and length to destination
					messageQueue[j] = decision;
					distanceToDestination[j] = length;
					isInterested[i][cell]= false;
				}
				
			}

			private void terminateProgram() {
				try {Thread.sleep(50);} catch (InterruptedException ie) {}
				for (String agentNames: agentArray) {
					ACLMessage messageFinal = new ACLMessage(ACLMessage.INFORM);
					messageFinal.addReceiver(new AID(agentNames, AID.ISLOCALNAME));
					messageFinal.setContent("End");
					send(messageFinal);
				}
				System.out.println(getLocalName()+" terminating");
				// terminate
				doDelete();
			}

			private void checkConflicts(String[] messageQueue) {
				
				int[][] coordinates = new int[][] {search("A"), search("B"), search("C")};
				
				for (int i=0; i < numberOfAgents; i++) {
					if (messageQueue[i].contentEquals("up")) {
						coordinates[i][0] = coordinates[i][0] - 1;
					} else if (messageQueue[i].contentEquals("down")) {
						coordinates[i][0] = coordinates[i][0] + 1;
					} else if (messageQueue[i].contentEquals("left")) {
						coordinates[i][1] = coordinates[i][1] - 1;
					} else if (messageQueue[i].contentEquals("right")) {
						coordinates[i][1] = coordinates[i][1] + 1;
					}
				}
				
				// FIRST TYPE OF CONFLICT
				// check conflicts based on their next move
				
				if (coordinates[0][0] == coordinates[1][0] && coordinates[0][1] == coordinates[1][1] 
						&& !isOnRGBY(coordinates[0][0], coordinates[0][1])) {
					counter1 += 1; // total number of conflicts
					// found conflict between Agent-1 & Agent-2 
					if ((coordinates[1][0] == coordinates[2][0] && coordinates[1][1] == coordinates[2][1])) {
						// solve conflict for all of them
						int min = minDestination();
						
						for (int i=0; i < numberOfAgents; i++) {
							if (i == min) {
								continue;
							}
							rightOfWay[i] = false;
						}
					} else {
						// solve conflict for Agent-1 & Agent-2 only
						if (distanceToDestination[0] > distanceToDestination[1]) {
							/* if Agent-1's distance to their destination is bigger than Agent-2's,
							 * then Agent-2 should go first (smaller distance so the client benefits)*/ 
							rightOfWay[0] = false;
							if (reCheckConflicts(coordinates)) {
								rightOfWay[0] = true;
								rightOfWay[1] = false;
							}
						} else {
							rightOfWay[1] = false;
							if (reCheckConflicts(coordinates)) {
								rightOfWay[1] = true;
								rightOfWay[0] = false;
							}
						}
					}
				} else if (coordinates[1][0] == coordinates[2][0] && coordinates[1][1] == coordinates[2][1] 
						&& !isOnRGBY(coordinates[1][0], coordinates[1][1])) {
					counter1 += 1; // total number of conflicts
					// found conflict between Agent-2 & Agent-3
					if (coordinates[0][0] == coordinates[1][0] && coordinates[0][1] == coordinates[1][1]) {
						// solve conflict for all of them
						int min = minDestination();
						
						for (int i=0; i < numberOfAgents; i++) {
							if (i == min) {
								continue;
							}
							rightOfWay[i] = false;
						}
					} else {
						// solve conflict for Agent-2 & Agent-3 only
						if (distanceToDestination[1] > distanceToDestination[2]) {
							/* if Agent-2's distance to their destination is bigger than Agent-3's,
							 * then Agent-3 should go first (smaller distance so the client benefits)*/ 
							rightOfWay[1] = false;
							if (reCheckConflicts(coordinates)) {
								rightOfWay[1] = true;
								rightOfWay[2] = false;
							}
						} else {
							rightOfWay[2] = false;
							if (reCheckConflicts(coordinates)) {
								rightOfWay[2] = true;
								rightOfWay[1] = false;
							}
						}
					}
				} else if (coordinates[0][0] == coordinates[2][0] && coordinates[0][1] == coordinates[2][1] 
						&& !isOnRGBY(coordinates[0][0], coordinates[0][1])) {
					counter1 += 1; // total number of conflicts
					// found conflict between Agent-1 & Agent-3
					if (coordinates[0][0] == coordinates[1][0] && coordinates[0][1] == coordinates[1][1]) {
						// solve conflict for all of them
						int min = minDestination();
						
						for (int i=0; i < numberOfAgents; i++) {
							if (i == min) {
								continue;
							}
							rightOfWay[i] = false;
						}
					} else {
						// solve conflict for Agent-1 & Agent-3 only
						if (distanceToDestination[0] > distanceToDestination[2]) {
							/* if Agent-1's distance to their destination is bigger than Agent-3's,
							 * then Agent-3 should go first (smaller distance so the client benefits)*/ 
							rightOfWay[0] = false;
							if (reCheckConflicts(coordinates)) {
								rightOfWay[0] = true;
								rightOfWay[2] = false;
							}
						} else {
							rightOfWay[2] = false;
							if (reCheckConflicts(coordinates)) {
								rightOfWay[2] = true;
								rightOfWay[0] = false;
							}
						}
					}
				}
			}

			private boolean reCheckConflicts(int[][] coordinates) {
				int a = 0;
				for (int i=0; i < numberOfAgents; i++) {
					if (rightOfWay[i] == false) {
						a = i;
					}
				}
				
				if(a == 0) {
					coordinates[a] = search("A");
				} else if(a == 1) {
					coordinates[a] = search("B");
				} else if(a == 2) {
					coordinates[a] = search("C");
				} 
				
				if (coordinates[0][0] == coordinates[1][0] && coordinates[0][1] == coordinates[1][1] 
						&& !isOnRGBY(coordinates[0][0], coordinates[0][1])) {
					return true;
				} else if (coordinates[1][0] == coordinates[2][0] && coordinates[1][1] == coordinates[2][1] 
						&& !isOnRGBY(coordinates[1][0], coordinates[1][1])) {
					return true;
				} else if (coordinates[0][0] == coordinates[2][0] && coordinates[0][1] == coordinates[2][1] 
						&& !isOnRGBY(coordinates[0][0], coordinates[0][1])) {
					return true;
				}
				return false;
			}

			private boolean isOnRGBY(int i, int j) {
				
				if((i == 0 && j == 0) || (i == 0 && j == 4) || (i == 4 && j == 0) || (i == 4 && j == 3)) {
					return true;
				}
				
				return false;
			}

			private int minDestination() {
				
				int min = 0;
				
				for (int i=0; i < numberOfAgents; i++) {
					if (distanceToDestination[min] > distanceToDestination[i]) {
						min = i;
					}
				}
				
				return min;
			}
		});
	}

	private void findStartingLocations() {

		for (int i=0; i<x; i++) {
			for (int j=0; j<y; j++) {
				locations[i][j] = "X";
			}
		}

		locations[0][0] = "0"; //R
		locations[4][0] = "0"; //Y
		locations[4][3] = "0"; //B
		locations[0][4] = "0"; //G

		// SPAWN AGENT A
		// nextInt is normally exclusive of the top value, so add 1 to make it inclusive
		int x1 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
		int y1 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
		
		while ((x1 == 0 && y1 == 0) || (x1 == 0 && y1 == 4) 
				|| (x1 == 4 && y1 == 0) || (x1 == 4 && y1 == 3)) {
			x1 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
			y1 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
		}
		
		locations[x1][y1] = "A";

		// SPAWN AGENT B
		int x2 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
		int y2 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
		
		while ((x2 == 0 && y2 == 0) || (x2 == 0 && y2 == 4) 
				|| (x2 == 4 && y2 == 0) || (x2 == 4 && y2 == 3) || (x2 == x1 && y2 == y1)) {
			x2 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
			y2 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
		}
		
		locations[x2][y2] = "B";

		// SPAWN AGENT C
		int x3 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
		int y3 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
		
		while ((x3 == 0 && y3 == 0) || (x3 == 0 && y3 == 4) 
				|| (x3 == 4 && y3 == 0) || (x3 == 4 && y3 == 3) || (x3 == x2 && y3 == y2) || (x3 == x1 && y3 == y1)) {
			x3 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
			y3 = ThreadLocalRandom.current().nextInt(0, 4 + 1);
		}
		
		locations[x3][y3] = "C";

		
		// CREATE OUR GRID NODES
		for (int i=0; i<x; i++) {
			for (int j=0; j<y; j++) {
				myNodes[i][j] = new Node(i, j);
			}
		}

		// CREATE NEIGHBOURS FOR OUR NODES
		for (int i=0; i<x; i++) {
			for (int j=0; j<x; j++) {
				if (i-1>=0) {
					myNodes[i][j].addNeighbor(1, myNodes[i-1][j]);
				}
				if (i+1<=4) {
					myNodes[i][j].addNeighbor(1, myNodes[i+1][j]);
				}
				if (j-1>=0) {
					myNodes[i][j].addNeighbor(1, myNodes[i][j-1]);
				}
				if (j+1<=4) {
					myNodes[i][j].addNeighbor(1, myNodes[i][j+1]);
				}
			}
		}

		// REMOVE NEIGHBOURS THAT HAVE VALUE 1 INSTEAD OF 100 (conflict)
		myNodes[0][1].removeNeighbor(1, myNodes[0][2]);
		myNodes[1][1].removeNeighbor(1, myNodes[1][2]);
		myNodes[0][2].removeNeighbor(1, myNodes[0][1]);
		myNodes[1][2].removeNeighbor(1, myNodes[1][1]);

		myNodes[3][0].removeNeighbor(1, myNodes[3][1]);
		myNodes[4][0].removeNeighbor(1, myNodes[4][1]);
		myNodes[3][1].removeNeighbor(1, myNodes[3][0]);
		myNodes[4][1].removeNeighbor(1, myNodes[4][0]);

		myNodes[3][2].removeNeighbor(1, myNodes[3][3]);
		myNodes[4][2].removeNeighbor(1, myNodes[4][3]);
		myNodes[3][3].removeNeighbor(1, myNodes[3][2]);
		myNodes[4][3].removeNeighbor(1, myNodes[4][2]);

		// FIND DEAD ENDS (-100 value)
		myNodes[0][1].addNeighbor(100, myNodes[0][2]);
		myNodes[1][1].addNeighbor(100, myNodes[1][2]);
		myNodes[0][2].addNeighbor(100, myNodes[0][1]);
		myNodes[1][2].addNeighbor(100, myNodes[1][1]);

		myNodes[3][0].addNeighbor(100, myNodes[3][1]);
		myNodes[4][0].addNeighbor(100, myNodes[4][1]);
		myNodes[3][1].addNeighbor(100, myNodes[3][0]);
		myNodes[4][1].addNeighbor(100, myNodes[4][0]);

		myNodes[3][2].addNeighbor(100, myNodes[3][3]);
		myNodes[4][2].addNeighbor(100, myNodes[4][3]);
		myNodes[3][3].addNeighbor(100, myNodes[3][2]);
		myNodes[4][3].addNeighbor(100, myNodes[4][2]);

		// SPAWN CLIENT
		for(int i=0;i<10;i++) {
			spawnClient();
		}
		
		for (int i=0; i < numberOfAgents; i++) {
			for(int j=0; j < 4; j++) {
				isAllowed[i][j] = true;
				isInterested[i][j] = false;
			}
		}
	}

	private void spawnClient() {

		int r = ThreadLocalRandom.current().nextInt(0, 3 + 1);

		if(r == 0) {
			locations[0][0] = Integer.toString(Integer.parseInt(locations[0][0]) + 1); //R
			// the first parameter of addClient is a client id [type: String], 
			// the second parameter is the client's target [type: Node]
			myNodes[0][0].addClient("Client-" + id++, chooseClientTarget()); 
		} else if(r == 1) {
			locations[4][0] = Integer.toString(Integer.parseInt(locations[4][0]) + 1); //Y
			myNodes[4][0].addClient("Client-" + id++, chooseClientTarget());
		} else if(r == 2) {
			locations[4][3] = Integer.toString(Integer.parseInt(locations[4][3]) + 1); //B
			myNodes[4][3].addClient("Client-" + id++, chooseClientTarget());
		} else if(r == 3) {
			locations[0][4] = Integer.toString(Integer.parseInt(locations[0][4]) + 1); //G
			myNodes[0][4].addClient("Client-" + id++, chooseClientTarget());
		}

	}

	private Node chooseClientTarget() {

		int t = ThreadLocalRandom.current().nextInt(0, 3 + 1);
		int x = 0, y = 0;

		if (t == 0) {
			x = 0;
			y = 0;
		} else if(t == 1) {
			x = 4;
			y = 0;
		} else if(t == 2) {
			x = 4;
			y = 3;
		} else if(t == 3) {
			x = 0;
			y = 4;
		}

		Node target = myNodes[x][y];

		return target;
	}

	private void printGrid() {
		for (int i=0; i<x; i++) {
			for (int j=0; j<y; j++) {
				System.out.print("\t" + locations[i][j]);
			}
			System.out.println();
		}
	}

	private void makeMove(String content, int i) { 

		int[] agentCoordinates = search("A");
		String agentName = "A"; 
		
		if(i == 2) {
			agentCoordinates = Grid.search("B");
			agentName = "B";
		} else if(i == 3) {
			agentCoordinates = Grid.search("C");
			agentName = "C";
		} 
		

		// agent is going to move, so we have to change his grid position (no longer there)
		if((agentCoordinates[0] == 0 && agentCoordinates[1] == 0) || (agentCoordinates[0] == 0 && agentCoordinates[1] == 4) 
				|| (agentCoordinates[0] == 4 && agentCoordinates[1] == 0) || (agentCoordinates[0] == 4 && agentCoordinates[1] == 3)) {
			locations[agentCoordinates[0]][agentCoordinates[1]] = locations[agentCoordinates[0]][agentCoordinates[1]].replace(agentName, "");
		} else if(locations[agentCoordinates[0]][agentCoordinates[1]].contentEquals(agentName)) {
			// in case there is only one agent in the cell and he is going to leave
			locations[agentCoordinates[0]][agentCoordinates[1]] = "X";
		} else { // in case there are two agents in a cell 
			locations[agentCoordinates[0]][agentCoordinates[1]] = locations[agentCoordinates[0]][agentCoordinates[1]].replace(agentName, "");
		}


		switch (content) {
		case "up":
			agentCoordinates[0] = agentCoordinates[0] - 1;
			changeCoordinates(agentCoordinates, agentName);

			System.out.println("The agent went UP");
			break;

		case "down":
			agentCoordinates[0] = agentCoordinates[0] + 1;
			changeCoordinates(agentCoordinates, agentName);

			System.out.println("The agent went DOWN");
			break;

		case "left":
			agentCoordinates[1] = agentCoordinates[1] - 1;
			changeCoordinates(agentCoordinates, agentName);

			System.out.println("The agent went LEFT");
			break;

		case "right": 
			agentCoordinates[1] = agentCoordinates[1] + 1;
			changeCoordinates(agentCoordinates, agentName);

			System.out.println("The agent went RIGHT");
			break;
			
		case "Client target and current node are the same staying here":
			locations[agentCoordinates[0]][agentCoordinates[1]] = agentName + locations[agentCoordinates[0]][agentCoordinates[1]];
			break;
			
		case "There are no available nodes left":
			System.out.println("The agent stayed in place -- There are no available nodes left.");
			break;
			
		default: 
			if(clients[i-1] == null) {
				changeCoordinates(agentCoordinates, agentName);
			}
			System.out.println("The agent stayed in place.");
			break;
		}
	}

	public static int[] search(String AgentName) {

		int coordinates[] = new int[2];
		int k = 0;

		for (int i=0; i<x; i++) {
			for (int j=0; j<y; j++) {
				if (locations[i][j].contains(AgentName)) {
					coordinates[0] = i;
					coordinates[1] = j;
					k=1;
					break;
				}
			}
			if (k==1) {
				break;
			}	
		}

		return coordinates;
	}

	private void changeCoordinates(int[] coordinates, String agentName) {
		if (locations[coordinates[0]][coordinates[1]].contentEquals("X") 
				|| locations[coordinates[0]][coordinates[1]].contentEquals("A")
				|| locations[coordinates[0]][coordinates[1]].contentEquals("B")
				|| locations[coordinates[0]][coordinates[1]].contentEquals("C")) {
			if (!locations[coordinates[0]][coordinates[1]].contentEquals("X")) {
				locations[coordinates[0]][coordinates[1]] = agentName + locations[coordinates[0]][coordinates[1]];
			} else {
				locations[coordinates[0]][coordinates[1]] = agentName;
			}
				
		} else { // if agent steps into r, g, b, y
			
			int a = 0;
			
			if(agentName.contentEquals("B")) {
				a = 1;
			} else if(agentName.contentEquals("C")) {
				a = 2;
			}  
			
			String num = locations[coordinates[0]][coordinates[1]].replaceAll("[A-Z]", "");
			String letter = locations[coordinates[0]][coordinates[1]].replaceAll("[0-9]", "");
			
			if (Integer.parseInt(num) > 0) {
				// reduce number of customers on block because agent picked them up
				
				locations[coordinates[0]][coordinates[1]] = letter + Integer.toString((Integer.parseInt(num) - 1));
				locations[coordinates[0]][coordinates[1]] = agentName + locations[coordinates[0]][coordinates[1]];
				
				// calls a star in order to find the quickest client, and get their name and target
				if (clients[a] == null) {
					Client chosenClient = chooseClientWithClosestDestination(coordinates);
					myNodes[coordinates[0]][coordinates[1]].removeClient(chosenClient.name, chosenClient.target); 
					clients[a] = chosenClient;
				}
			} else if (Integer.parseInt(num) == 0) {
				if (!myNodes[coordinates[0]][coordinates[1]].clients.isEmpty()) {
					clients[a] = myNodes[coordinates[0]][coordinates[1]].clients.get(0);
					myNodes[coordinates[0]][coordinates[1]].removeClient(myNodes[coordinates[0]][coordinates[1]].clients.get(0).name, myNodes[coordinates[0]][coordinates[1]].clients.get(0).target);
				}
				locations[coordinates[0]][coordinates[1]] = agentName + locations[coordinates[0]][coordinates[1]];
			}
		}
	}

	private Client chooseClientWithClosestDestination(int[] coordinates) {

		Node client1;
		Client chosenClient = null; 
		int min = 10000;

		for (Node.Client client : myNodes[coordinates[0]][coordinates[1]].clients) {
			client1 = Node.aStar(myNodes[coordinates[0]][coordinates[1]], client.target);
			if (min > Node.printPath(client1)) {
				min = Node.printPath(client1);
				chosenClient = client;
			}
		}

		return chosenClient;

	}

	public String[][] getLocations() {
		return locations;
	}

	public void setLocations(String[][] locations1) {
		locations = locations1;
	}

	public static Node[][] getMyNodes() {
		return myNodes;
	}

	public static void setMyNodes(Node[][] myNodes1) {
		myNodes = myNodes1;
	}
}