package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Iterator;
import java.util.List;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploreMoveBehaviour;

import jade.core.behaviours.OneShotBehaviour;

public class ExploreMoveBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;
	private static final int INTERLOCKING = 0;
	private static final int SUCCESS = 1;
	private static final int NO_OPEN_NODE = 2;
	private static final int MAYBE_BLOCKED = 3;
	private int returnCode;
	private boolean wasBlocked = false;

	
	private ExploreCoopAgent myAgent;

	
	public ExploreMoveBehaviour(ExploreCoopAgent myagent) {
		super(myagent);
		this.myAgent = myagent;
	}
	public ExploreMoveBehaviour(ExploreCoopAgent myagent,boolean wasBlocked) {
		this(myagent);
		this.wasBlocked = wasBlocked;
	}

	@Override
	public void action() {
		if (this.myAgent.getMyMap() == null) {
			this.myAgent.setMyMap(new MapRepresentation());
		}

		// 0) Retrieve the current position
		String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
		// If it's a new node we add it, otherwise do nothing
		Node added = this.myAgent.getMyMap().addNewNode(myPosition, this.myAgent.getLocalName());
		if (added != null) {
			this.myAgent.addNodeOtherAgents(added);
			added = null;
			}

		if (myPosition != null) {
			// List of observable from the agent's current position
			List<Couple<String, List<Couple<Observation, Integer>>>> lobs = ((AbstractDedaleAgent) this.myAgent)
					.observe();

			try {
				this.myAgent.doWait(500);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// 1) remove the current node from openlist and add it to closedNodes + claim it
			// if it's not already claimed.
			String claimant = this.myAgent.getLocalName();
			MapAttribute newAttrib = this.myAgent.getMyMap().getMapAttributeFromNodeId(myPosition);
			if (!this.myAgent.getMyMap().getNodeClaimant(myPosition).equalsIgnoreCase("")) {
				claimant = this.myAgent.getMyMap().getNodeClaimant(myPosition);
			}
			newAttrib.setClaimant(claimant);
			newAttrib.setState("closed");
			added = this.myAgent.getMyMap().addNode(myPosition, newAttrib);
			if (added != null) {
				this.myAgent.addNodeOtherAgents(added);
				added = null;
			}
			
			// 2) get the surrounding nodes and, if not in closedNodes, add them to open
			// nodes + claim them.
			String nextNode = null;
			Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter = lobs.iterator();
			while (iter.hasNext()) {
				Couple<String,List<Couple<Observation,Integer>>> treatedObs = iter.next();
				String nodeId = treatedObs.getLeft();
				added = this.myAgent.getMyMap().addNewNode(nodeId, this.myAgent.getLocalName());
				if (added != null) {
					this.myAgent.addNodeOtherAgents(added);
				}
				// the node may exist, but not necessarily the edge
				if (myPosition != nodeId) {
					Edge addedE = this.myAgent.getMyMap().addEdge(myPosition, nodeId);
					if (addedE != null) {
						this.myAgent.addEdgeOtherAgents(addedE);
						this.myAgent.addNodeOtherAgents(addedE.getNode0());
						this.myAgent.addNodeOtherAgents(addedE.getNode1());
					}
					if (nextNode == null && added != null)
						nextNode = nodeId;
				}
				//Also have to add treasures if there are any
				for( Couple<Observation, Integer> treatedObsTreasures : treatedObs.getRight()) {
					if (treatedObsTreasures.getLeft().name().equalsIgnoreCase("Gold") || treatedObsTreasures.getLeft().name().equalsIgnoreCase("Diamond")) {
						this.myAgent.getMyMap().setTreasures(nodeId, treatedObsTreasures);
					}
				}
			}

			// 3) select next move

			// 3.1) If there exist one open node directly reachable, go for it, add the current position to
			// the head of the path the agent wanted to take
			if (nextNode != null) {
				this.myAgent.getPathToFollow().addFirst(myPosition);

				// 3.2) Otherwise if he had a destination, follow the given path
			} else if (this.myAgent.getPathToFollow().peekFirst() != null) {
				nextNode = this.myAgent.getPathToFollow().removeFirst();

				// 3.3) Otherwise choose the closest open node if there is one
			} else if (this.myAgent.getMyMap().hasOpenNode()) {
				// Compute the path and take the first step
				this.myAgent.setPathToFollow(this.myAgent.getMyMap().getShortestPathToClosestOpenNode(myPosition,
						this.myAgent.getLocalName()));
				nextNode = this.myAgent.getPathToFollow().removeFirst();

			} else {
				// 3.4) If there is no open node, the exploration *should* be complete
				//System.out.println(this.myAgent.getLocalName() + "- There is no open nodes left. I'm finished !");
				this.myAgent.setNextPosition("");
				this.myAgent.getPathToFollow().clear();
				if(this.myAgent.getCurrentPlan()==null) {
					getDataStore().put("decision-master", this.myAgent.getLocalName());
				}
				this.returnCode = NO_OPEN_NODE;
				return; //No need to do the other moves
			}
			
			//Move
			if (nextNode != null) {
				this.myAgent.setNextPosition(nextNode);
				//System.out.println(this.myAgent.getLocalName() + "- I'm at " + myPosition  +", going to " + this.myAgent.getNextPosition());
//				Iterator<Map.Entry<String, OtherAgent>> entries = this.myAgent.getOtherAgents().entrySet().iterator();
//				while (entries.hasNext()) {
//					Map.Entry<String, OtherAgent> entry = entries.next();
//					OtherAgent agent =  entry.getValue();
//					System.out.println(this.myAgent.getLocalName() + "- Nodes to share " + agent.getNodesToTransfer().toString());
//					System.out.println(this.myAgent.getLocalName() + "- Edges to share " + agent.getEdgesToTransfer().toString());
//				}
				
				if (!((AbstractDedaleAgent) this.myAgent).moveTo(this.myAgent.getNextPosition())) {
					this.myAgent.getPathToFollow().addFirst(this.myAgent.getNextPosition());
					if(this.wasBlocked) {
						this.returnCode = INTERLOCKING;
					}else {
						this.returnCode = MAYBE_BLOCKED;
					}
				}else {
					getDataStore().put("movesWithoutSharing", (int) getDataStore().get("movesWithoutSharing")+1);
					this.returnCode = SUCCESS;
				}
			}
		}
	}

	public int onEnd() {
		return this.returnCode;
	}

}
