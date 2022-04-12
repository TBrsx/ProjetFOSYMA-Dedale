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

public class ShareAndCollectBehaviour extends OneShotBehaviour{
	

	
	private static final long serialVersionUID = -3403675615819306473L;
	private ExploreCoopAgent myAgent;
	private int returnCode;
	private static final int NOT_DONE = 0;
	private static final int DONE = 1;
	private static final int INTERLOCKING = 2;
	
	
	public ShareAndCollectBehaviour(ExploreCoopAgent myagent) {
		super(myagent);
		this.myAgent = myagent;
	}

	@Override
	public void action() {
		try {
			this.myAgent.doWait(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
		MapAttribute newAttrib = this.myAgent.getMyMap().getMapAttributeFromNodeId(myPosition);
		newAttrib.setCollector("");
		this.myAgent.getMyMap().addNode(myPosition, newAttrib);
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
			if(this.myAgent.openLock(this.myAgent.getTreasureType())) {
				this.myAgent.pick();
			}
		}
		//Are we done ? TODO : Change this
		this.myAgent.setPathToFollow( this.myAgent.getMyMap().getShortestPathToClosestToCollect(myPosition, this.myAgent.getLocalName()));
		if(this.myAgent.getPathToFollow() != null) {
				this.returnCode = NOT_DONE;
				this.myAgent.setNextPosition(this.myAgent.getPathToFollow().removeFirst());
				if (!((AbstractDedaleAgent) this.myAgent).moveTo(this.myAgent.getNextPosition())) {
					this.myAgent.getPathToFollow().addFirst(this.myAgent.getNextPosition());
					this.returnCode = INTERLOCKING;
				}
		}else {
			this.returnCode = DONE;
			return;
		}
	}

	public int onEnd() {
		return this.returnCode;
	}

}
