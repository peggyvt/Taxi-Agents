package agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class Node implements Comparable<Node> {
	// id for readability of result purposes
	private static int idCounter = 0;
	public int id;

	// parent in the path
	public Node parent = null;

	public List<Neighbor> neighbors;
	public List<Client> clients;

	// evaluation functions
	public int f = Integer.MAX_VALUE;
	public int g = 0;
	// heuristic
	public int h; 
	// node coordinates
	public int i;
	public int j;

	Node(int i, int j) {
		this.i = i;
		this.j = j;
		this.id = idCounter++;
		this.neighbors = new ArrayList<>();
		this.clients = new ArrayList<>();
	}

	@Override
	public int compareTo(Node n) {
		return Double.compare(this.f, n.f);
	}

	
	public static class Neighbor {
		Neighbor(int weight, Node node) { //weight = g cost function 
			this.weight = weight;
			this.node = node;
		}

		public int weight;
		public Node node;
	}

	public void addNeighbor(int weight, Node node) {
		Neighbor newNeighbor = new Neighbor(weight, node);
		neighbors.add(newNeighbor);
	}

	public void removeNeighbor(int weight, Node node) {
		Neighbor newNeighbor = new Neighbor(weight, node);
		neighbors.removeIf(neighbor -> (neighbor.weight == newNeighbor.weight && neighbor.node == newNeighbor.node));
	}


	public static class Client {
		Client(String name, Node target) {
			this.name = name;
			this.target = target;
		}
		public String name;
		public Node target;
	}

	public void addClient(String name, Node target) {
		Client newClient = new Client(name, target); // the first parameter is a client id [type: string], the second one is the client's target
		clients.add(newClient);
	}

	public void removeClient(String name, Node target) {
		Client newClient = new Client(name, target);
		clients.removeIf(client -> (client.name == newClient.name && client.target == newClient.target)); 
	}


	public int calculateHeuristic(Node target) {
		// CALCULATE HEURISTIC BASED ON CHEBYSHEV DISTANCE
		this.h = Math.max(Math.abs(target.i-this.i), Math.abs(target.j-this.j));
		return this.h;
	}

	public static Node aStar(Node start, Node target) {
		PriorityQueue<Node> closedList = new PriorityQueue<>();
	    PriorityQueue<Node> openList = new PriorityQueue<>();

		start.f = start.g + start.calculateHeuristic(target);
		openList.add(start);
		for (int i=0; i<5; i++) {
			for (int j=0; j<5; j++) {
				Grid.myNodes[i][j].parent = null;
			}	
		}
		while(!openList.isEmpty()) {
			Node n = openList.peek();
			List<Node> nodes = new ArrayList<>();

			if(n == target) {
				n = openList.peek();
				while(n.parent != null){
					nodes.add(n);
					n = n.parent;
				}
				nodes.add(n);
				Collections.reverse(nodes);

				for(int i=0;i<nodes.size()-1;i++) {
					nodes.get(i).parent = nodes.get(i+1);
				}
				nodes.get(nodes.size()-1).parent = null;
				return nodes.get(0);
			}

			for(Node.Neighbor neighbor : n.neighbors) {
				Node m = neighbor.node;
				int totalWeight = n.g + neighbor.weight;

				if(!openList.contains(m) && !closedList.contains(m)) {
					m.parent = n;
					m.g = totalWeight;
					m.f = m.g + m.calculateHeuristic(target);
					openList.add(m);
				} else {
					if(totalWeight < m.g) {
						m.parent = n;
						m.g = totalWeight;
						m.f = m.g + m.calculateHeuristic(target);

						if(closedList.contains(m)) {
							closedList.remove(m);
							openList.add(m);
						}
					}
				}
			}

			openList.remove(n);
			closedList.add(n);
		}
		return null;
	}

	public static int printPath(Node target) {

		Node n = target;

		if(n==null)
			return 0;

		List<Integer> ids = new ArrayList<>();

		while(n.parent != null) {
			ids.add(n.id);
			n = n.parent;
		}
		ids.add(n.id);


		for(int id : ids) {
			System.out.print(id + " ");
		}
		System.out.println("");
		return ids.size();

	}
}