package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import org.graphstream.graph.Node;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class CollectDecisionBehaviour extends OneShotBehaviour{
	
	private static final long serialVersionUID = 3329007121557183780L;
	private static final int PLAN_SHARING = 0;
	private static final int BEGIN_COLLECT = 1;
	private static final int INTERLOCKING = 2;
	private int returnCode;
	
	private ExploreCoopAgent myAgent;
	
	public CollectDecisionBehaviour(ExploreCoopAgent myagent) {
		super(myagent);
		this.myAgent = myagent;
	}
	
	private void createPlan() {
		((LinkedList<String>) this.getDataStore().get("awareOfPlan")).add(this.myAgent.getLocalName());
		((LinkedList<String>) this.getDataStore().get("awareOfPlan")).add("3rdAgent");
		((LinkedList<String>) this.getDataStore().get("awareOfPlan")).add("4thAgent");
		ArrayList<String> allNodes = (ArrayList<String>) this.myAgent.getMyMap().getAllNodes();
		this.myAgent.setCurrentPlan("ElPlan");
		
		for (String n : allNodes) {
			MapAttribute mapAtt = this.myAgent.getMyMap().getMapAttributeFromNodeId(n);
			if (mapAtt.getTreasure().getLeft() != null) {
				mapAtt.setCollector(mapAtt.getClaimant());
				Node added = this.myAgent.getMyMap().addNode(n, mapAtt);
				if (added != null) {
					this.myAgent.addNodeOtherAgents(added);
				}
			}
		}
		System.out.println(this.myAgent.getLocalName() + " - J'ai crée un plan, nommé " + this.myAgent.getCurrentPlan());
		this.returnCode = PLAN_SHARING;
	}
	private void sharePlan() {
		if (this.myAgent.getPathToFollow().isEmpty()){
			String meeting = this.myAgent.getMeetingPoint();
			if(this.myAgent.getCurrentPosition().equalsIgnoreCase(meeting)) {
				this.myAgent.setPathToFollow(this.myAgent.getMyMap().getRandomPathFrom(this.myAgent.getCurrentPosition(), 5));
			}else {
				this.myAgent.setPathToFollow(this.myAgent.getMyMap().getShortestPath(this.myAgent.getCurrentPosition(), meeting));			}
		}
		this.myAgent.setNextPosition(this.myAgent.getPathToFollow().removeFirst());
		if (!((AbstractDedaleAgent) this.myAgent).moveTo(this.myAgent.getNextPosition())) {
			this.myAgent.getPathToFollow().addFirst(this.myAgent.getNextPosition());
			this.returnCode = INTERLOCKING;
			return;
		}
		getDataStore().put("movesWithoutSharing", (int) getDataStore().get("movesWithoutSharing")+1);
		this.returnCode = PLAN_SHARING;
		
		//Stop sharing if all agents are experts
		LinkedList<String> experts = (LinkedList<String>) this.getDataStore().get("awareOfPlan");
		if (experts.size() == this.myAgent.getOtherAgents().size()+1) {
			this.returnCode = BEGIN_COLLECT;
		}
	}
	private void searchPlan() {
		//Move randomly around the meeting point, waiting for plan
		if (this.myAgent.getPathToFollow().isEmpty()){
			String meeting = this.myAgent.getOtherAgents().get((String) this.getDataStore().get("decision-master")).getMeetingPoint();
			if(this.myAgent.getCurrentPosition().equalsIgnoreCase(meeting)) {
				this.myAgent.setPathToFollow(this.myAgent.getMyMap().getRandomPathFrom(this.myAgent.getCurrentPosition(), 5));
			}else {
				this.myAgent.setPathToFollow(this.myAgent.getMyMap().getShortestPath(this.myAgent.getCurrentPosition(), meeting));
			}
		}
		this.myAgent.setNextPosition(this.myAgent.getPathToFollow().removeFirst());
		if (!((AbstractDedaleAgent) this.myAgent).moveTo(this.myAgent.getNextPosition())) {
			this.myAgent.getPathToFollow().addFirst(this.myAgent.getNextPosition());
			this.returnCode = INTERLOCKING;
			return;
		}
		getDataStore().put("movesWithoutSharing", (int) getDataStore().get("movesWithoutSharing")+1);
		this.returnCode = PLAN_SHARING;
	}
	
	@Override
	public void action() {
		this.myAgent.doWait(500);
		String decisionMaster = (String) this.getDataStore().get("decision-master");
		if(decisionMaster.equalsIgnoreCase(this.myAgent.getLocalName())){
			if (this.myAgent.getCurrentPlan().isEmpty()){
				this.createPlan();
			}else {
				this.sharePlan();
			}
		}else {
			if (this.myAgent.getCurrentPlan().isEmpty()){
				this.searchPlan();
			}else {
				this.returnCode = BEGIN_COLLECT;
			}
		}
	}
	
	public int onEnd() {
		return this.returnCode;
	}

}
