package eu.su.mas.dedaleEtu.mas.knowledge;

import java.util.LinkedList;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;



//This class is the knowledge an agent has about other agents - mainly, the informations he thinks that the other agent doesn't know
//It needs to be updated each time the agent learns something new 
public class OtherAgent {
	private String name;
	private String lastKnownPosition;
	private LinkedList<Node> nodesToTransfer;
	private LinkedList<Edge> edgesToTransfer;
	
	public OtherAgent(String name){
		this.setName(name);
		this.setLastKnownPosition(null);
		this.setNodesToTransfer(new LinkedList<Node>());
		this.setEdgesToTransfer(new LinkedList<Edge>());
	}
	public OtherAgent(String name,String position){
		this.setName(name);
		this.setLastKnownPosition(position);
		this.setNodesToTransfer(new LinkedList<Node>());
		this.setEdgesToTransfer(new LinkedList<Edge>());
	}
	
	
	//Add a node to the nodesToTransfer list, updating it if it already exists
	public void addNode(Node n) {
		this.nodesToTransfer.remove(n);
		this.nodesToTransfer.add(n);
	}
	
	//Add an edge to the edgesToTransfer list, updating it if it already exists
	public void addEdge(Edge e) {
		this.edgesToTransfer.remove(e);
		this.edgesToTransfer.add(e);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLastKnownPosition() {
		return lastKnownPosition;
	}

	public void setLastKnownPosition(String lastKnownPosition) {
		this.lastKnownPosition = lastKnownPosition;
	}

	public LinkedList<Node> getNodesToTransfer() {
		return nodesToTransfer;
	}

	public void setNodesToTransfer(LinkedList<Node> nodesToTransfer) {
		this.nodesToTransfer = nodesToTransfer;
	}

	public LinkedList<Edge> getEdgesToTransfer() {
		return edgesToTransfer;
	}

	public void setEdgesToTransfer(LinkedList<Edge> edgesToTransfer) {
		this.edgesToTransfer = edgesToTransfer;
	}
}
