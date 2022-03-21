package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;


import dataStructures.serializableGraph.SerializableSimpleGraph;



//This class is the knowledge an agent has about other agents - mainly, the informations he thinks that the other agent doesn't know
//It needs to be updated each time the agent learns something new 
public class OtherAgent implements Serializable{
	
	private static final long serialVersionUID = -2214875361759942206L;
	
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
	
	//Serialize informations we want to send to the other agent
	public SerializableSimpleGraph<String, MapAttribute> serializeInformations() {
		SerializableSimpleGraph<String, MapAttribute> sg = new SerializableSimpleGraph<String, MapAttribute>();
		Iterator<Node> iter = this.nodesToTransfer.iterator();
		while (iter.hasNext()) {
			Node n = iter.next();
			sg.addNode(n.getId(), new MapAttribute(n.getAttribute("ui.class").toString(), n.getAttribute("claimant").toString()));
		}
		Iterator<Edge> iterE = this.edgesToTransfer.iterator();
		while (iterE.hasNext()) {
			Edge e = iterE.next();
			Node sn = e.getSourceNode();
			Node tn = e.getTargetNode();
			sg.addEdge(e.getId(), sn.getId(), tn.getId());
		}
		//After sending the informations, we can clear
		this.edgesToTransfer.clear();
		this.nodesToTransfer.clear();
		return sg;
	}
	
	public boolean hasInfoToShare() {
		return !((this.edgesToTransfer.isEmpty())||this.nodesToTransfer.isEmpty());
	}
}
