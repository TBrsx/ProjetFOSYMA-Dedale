package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Iterator;
import java.util.List;
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
	private int returnCode = SUCCESS;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private ExploreCoopAgent myAgent;

	/**
	 * 
	 * @param myagent
	 */
	public ExploreMoveBehaviour(ExploreCoopAgent myagent) {
		super(myagent);
		this.myAgent = myagent;

	}

	@Override
	public void action() {

		if (this.myAgent.getMyMap() == null) {
			this.myAgent.setMyMap(new MapRepresentation());
		}

		// 0) Retrieve the current position
		String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
		// If it's a new node we add it, otherwise do nothing
		this.myAgent.getMyMap().addNewNode(myPosition, this.myAgent.getLocalName());

		if (myPosition != null) {
			// List of observable from the agent's current position
			List<Couple<String, List<Couple<Observation, Integer>>>> lobs = ((AbstractDedaleAgent) this.myAgent)
					.observe();

			try {
				this.myAgent.doWait(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// 1) remove the current node from openlist and add it to closedNodes + claim it
			// if it's not already claimed.
			String claimant = this.myAgent.getLocalName();
			if (!this.myAgent.getMyMap().getNodeClaimant(myPosition).equalsIgnoreCase("")) {
				claimant = this.myAgent.getMyMap().getNodeClaimant(myPosition);
			}
			this.myAgent.getMyMap().addNode(myPosition, new MapAttribute("closed", claimant));

			// 2) get the surrounding nodes and, if not in closedNodes, add them to open
			// nodes + claim them.
			String nextNode = null;
			Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter = lobs.iterator();
			while (iter.hasNext()) {
				String nodeId = iter.next().getLeft();
				boolean isNewNode = this.myAgent.getMyMap().addNewNode(nodeId, this.myAgent.getLocalName());
				// the node may exist, but not necessarily the edge
				if (myPosition != nodeId) {
					this.myAgent.getMyMap().addEdge(myPosition, nodeId);
					if (nextNode == null && isNewNode)
						nextNode = nodeId;
				}
			}

			// 3) select next move, unless there is no openNodes left.
			if(this.myAgent.getMyMap().hasOpenNode()) {
				// If there exist one open node directly reachable, go for it, add it to
				// the head of the path it wanted to take
				// otherwise choose one from the openNode list, compute the shortestPath and go
				// for it
				if (nextNode == null) {
					// no directly accessible openNode
					// chose one, compute the path and take the first step.
					nextNode = this.myAgent.getMyMap()
							.getShortestPathToClosestOpenNode(myPosition, this.myAgent.getLocalName()).get(0);
				}


				if (!((AbstractDedaleAgent) this.myAgent).moveTo(nextNode)) {
					returnCode = INTERLOCKING;
				};
			}else {
				System.out.println(this.myAgent.getLocalName() + "- There is no open nodes left. I'm finished !");
			}
		}
	}

	public int onEnd() {
		return returnCode;
	}

}
