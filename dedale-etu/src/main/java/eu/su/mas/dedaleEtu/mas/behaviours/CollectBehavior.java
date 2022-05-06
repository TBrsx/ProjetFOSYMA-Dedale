package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import jade.core.behaviours.OneShotBehaviour;

public class CollectBehavior extends OneShotBehaviour{
	

	
	private static final long serialVersionUID = -3403675615819306473L;
	private ExploreCoopAgent myAgent;
	private int returnCode;
	private static final int NOT_DONE = 0;
	private static final int DONE_EXPLORE = 1;
	private static final int INTERLOCKING = 2;
	private static final int DONE = 3;
	
	//This Behavior is the one the agents use to collect the treasures
	
	public CollectBehavior(ExploreCoopAgent myagent) {
		super(myagent);
		this.myAgent = myagent;
	}

	@Override
	public void action() {
		//System.out.println(this.myAgent.getLocalName() + " - started behavior " + this.getBehaviourName());

		try {
			this.myAgent.doWait((int) this.getDataStore().get("waitingTime"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
		LinkedList<String> attributedNodes = this.myAgent.getCurrentPlan().getAttributedNodes(this.myAgent.getLocalName());
		//System.out.println(this.myAgent.getLocalName() + "Mes noeuds encore attribués sont : " + attributedNodes);
		//System.out.println(this.myAgent.getLocalName() + " - Mon chemin à suivre est " + this.myAgent.getPathToFollow());
		
		
		//Update treasures knowledge
		List<Couple<String, List<Couple<Observation, Integer>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();
		Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter = lobs.iterator();
		while (iter.hasNext()) {
			Couple<String,List<Couple<Observation,Integer>>> treatedObs = iter.next();
			String nodeId = treatedObs.getLeft();
			for( Couple<Observation, Integer> treatedObsTreasures : treatedObs.getRight()) {
				if (treatedObsTreasures.getLeft().name().equalsIgnoreCase("Gold") || treatedObsTreasures.getLeft().name().equalsIgnoreCase("Diamond")) {
					this.myAgent.getMyMap().setTreasures(nodeId, treatedObsTreasures);
				}
			}
		}
		LinkedList<String> path = this.myAgent.getPathToFollow();
		//We reached our destination, there should be a chest to open
		if (path.isEmpty()) {
			if(this.myAgent.getCurrentPlan().getAttributedNodes(this.myAgent.getLocalName()).contains( this.myAgent.getCurrentPosition())) {
				if(this.myAgent.openLock(this.myAgent.getTreasureType())) {
					int pickedQuantity = this.myAgent.pick();
					System.out.println(this.myAgent.getLocalName() + " - I picked " + Integer.toString(pickedQuantity) + " " + this.myAgent.getTreasureType());
				}else {
					System.out.println(this.myAgent.getLocalName() + " - I failed to open the lock");
				}
			}
			//Set path to next node to collect
			this.myAgent.getCurrentPlan().removeNodeWithId(myPosition);
			attributedNodes = this.myAgent.getCurrentPlan().getAttributedNodes(this.myAgent.getLocalName());
			this.myAgent.setPathToFollow( this.myAgent.getMyMap().getShortestPathToClosestInList(myPosition, attributedNodes));
		}
		
		if (this.myAgent.getPathToFollow() != null) { //If the path previously computed is null, it means there is nothing left to collect for this agent
			this.returnCode = NOT_DONE;
			this.myAgent.setNextPosition(this.myAgent.getPathToFollow().removeFirst());
			if (!((AbstractDedaleAgent) this.myAgent).moveTo(this.myAgent.getNextPosition())) {
				this.myAgent.getPathToFollow().addFirst(this.myAgent.getNextPosition());
				this.returnCode = INTERLOCKING;
			}
		} else {
			if(this.myAgent.getCurrentPlan().isComplete()) {
				this.returnCode = DONE;
			}else {
				this.returnCode = DONE_EXPLORE;
				
				//Remove the state "blocked" of nodes from my map
				for(String node : this.myAgent.getMyMap().getBlockedNodes()) {
					MapAttribute truc = this.myAgent.getMyMap().getMapAttributeFromNodeId(node);
					truc.setBlocked(false);
					this.myAgent.getMyMap().addNode(node, truc);
				}
				return;
			}
		}
	}

	public int onEnd() {
		return this.returnCode;
	}

}
