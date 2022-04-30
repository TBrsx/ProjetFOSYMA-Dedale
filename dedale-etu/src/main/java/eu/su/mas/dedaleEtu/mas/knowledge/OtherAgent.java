package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;


import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;



//This class is the knowledge an agent has about other agents - mainly, the informations he thinks that the other agent doesn't know
//It needs to be updated each time the agent learns something new 
public class OtherAgent implements Serializable{
	
	private static final long serialVersionUID = -2214875361759942206L;
	
	private String name;
	private LinkedList<Node> nodesToTransfer;
	private LinkedList<Edge> edgesToTransfer;
	private int capaDiamond;
	private int capaGold;
	private CollectPlan currentPlan;
	private boolean alreadyMet = false;
	private boolean knownCapa = false;
	private String meetingPoint = "";
	
	public OtherAgent(String name){
		this.setName(name);
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
			MapAttribute mapAtt = new MapAttribute((String)n.getAttribute("ui.class"),
					(String)n.getAttribute("claimant"),
					(String)n.getAttribute("occupied"),
					(Couple<Observation, Integer>)n.getAttribute("treasure"),
					(String) n.getAttribute("collector"));
			sg.addNode(n.getId(),mapAtt);
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
	
	public boolean hasInfoToShare(ExploreCoopAgent myagent) {
		String planNameMYAGENT = "";
		String planNameTHIS = "";
		if(this.getCurrentPlan()!=null) {
			planNameTHIS = this.getCurrentPlan().getName();
		}
		if(myagent.getCurrentPlan()!=null) {
			planNameMYAGENT = myagent.getCurrentPlan().getName();
		}
		return (!this.edgesToTransfer.isEmpty()
				||!this.nodesToTransfer.isEmpty()
				|| planNameTHIS!= planNameMYAGENT);
	}


	public int getCapaDiamond() {
		return capaDiamond;
	}


	public void setCapaDiamond(int capaDiamond) {
		this.capaDiamond = capaDiamond;
	}


	public int getCapaGold() {
		return capaGold;
	}


	public void setCapaGold(int capaGold) {
		this.capaGold = capaGold;
	}


	public CollectPlan getCurrentPlan() {
		return currentPlan;
	}


	public void setCurrentPlan(CollectPlan currentPlan) {
		this.currentPlan = currentPlan;
	}

	public boolean isAlreadyMet() {
		return alreadyMet;
	}

	public void setAlreadyMet(boolean alreadyMet) {
		this.alreadyMet = alreadyMet;
	}

	public String getMeetingPoint() {
		return meetingPoint;
	}

	public void setMeetingPoint(String meetingPoint) {
		this.meetingPoint = meetingPoint;
	}

	public boolean isKnownCapa() {
		return knownCapa;
	}

	public void setKnownCapa(boolean knownCapa) {
		this.knownCapa = knownCapa;
	}
}
