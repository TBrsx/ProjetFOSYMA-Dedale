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
	
	//Suffisantes, ou faut modifier aussi ?
	
	public void addNode(Node n) {
		
	}
	
	public void addEdge(Node n) {
		
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
