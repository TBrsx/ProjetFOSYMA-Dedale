package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.LinkedList;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.CollectPlan;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttributeCollect;


import jade.core.behaviours.OneShotBehaviour;

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
		ArrayList<String> allNodes = (ArrayList<String>) this.myAgent.getMyMap().getAllNodes();
		
		CollectPlan elPlan = new CollectPlan("ElPlan");
		
		for (String n : allNodes) {
			MapAttribute mapAtt = this.myAgent.getMyMap().getMapAttributeFromNodeId(n);
			if (mapAtt.getTreasure().getLeft() != null) {
				elPlan.addNode(new MapAttributeCollect(n,"",mapAtt.getClaimant(),""));
			}
		}
		this.myAgent.setCurrentPlan(elPlan);
		System.out.println(this.myAgent.getLocalName() + " - J'ai crée un plan, nommé " + this.myAgent.getCurrentPlan().getName());
		this.returnCode = PLAN_SHARING;
	}
	private void sharePlan() { //Move to the meeting point, then try to contact other agents near
		String meeting = this.myAgent.getMeetingPoint();
		//System.out.println(this.myAgent.getLocalName() + " - Je partage mon plan, nommé " + this.myAgent.getCurrentPlan().getName());
		//System.out.println(this.myAgent.getLocalName() + " - Le meeting point est " + meeting);
		
		
		if (this.myAgent.getPathToFollow().isEmpty()){
			if(this.myAgent.getCurrentPosition().equalsIgnoreCase(meeting)) { //If I am at the meeting point
				getDataStore().put("movesWithoutSharing", (int) getDataStore().get("movesWithoutSharing")+10); //Force handshake try
				this.myAgent.doWait(1000); //To make sure we don't flood the environment with messages, as this agent do nothing more than trying to handshake now

			}else {
				this.myAgent.setPathToFollow(this.myAgent.getMyMap().getShortestPath(this.myAgent.getCurrentPosition(), meeting));			
			}
		}else {
			this.myAgent.setNextPosition(this.myAgent.getPathToFollow().removeFirst());
			if (!((AbstractDedaleAgent) this.myAgent).moveTo(this.myAgent.getNextPosition())) {
				this.myAgent.getPathToFollow().addFirst(this.myAgent.getNextPosition());
				this.returnCode = INTERLOCKING;
				return;
			}
		}
		getDataStore().put("movesWithoutSharing", (int) getDataStore().get("movesWithoutSharing")+1);
		this.returnCode = PLAN_SHARING;
		
		//Stop sharing and start collecting if all agents are experts
		LinkedList<String> experts = (LinkedList<String>) this.getDataStore().get("awareOfPlan");
		if (experts.size() >= this.myAgent.getOtherAgents().size()+1) {
			//Set path to follow to reach first treasure to collect
			this.myAgent.setPathToFollow(this.myAgent.getMyMap().getShortestPathToClosestInList(this.myAgent.getCurrentPosition(), 
					this.myAgent.getCurrentPlan().getAttributedNodes(this.myAgent.getLocalName())));
			if (this.myAgent.getPathToFollow() == null) {
				this.myAgent.setPathToFollow(new LinkedList<String>());
			}
			this.myAgent.setNextPosition("");
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
			if (this.myAgent.getCurrentPlan() == null){
				this.createPlan();
			}else {
				this.sharePlan();
			}
		}else {
			if (this.myAgent.getCurrentPlan() == null){
				this.searchPlan();
			}else {
				this.myAgent.setPathToFollow(this.myAgent.getMyMap().getShortestPathToClosestInList(this.myAgent.getCurrentPosition(), 
						this.myAgent.getCurrentPlan().getAttributedNodes(this.myAgent.getLocalName())));
				if (this.myAgent.getPathToFollow() == null) {
					this.myAgent.setPathToFollow(new LinkedList<String>());
				}
				this.myAgent.setNextPosition("");
				this.returnCode = BEGIN_COLLECT;
			}
		}
	}
	
	public int onEnd() {
		return this.returnCode;
	}

}
