package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * The agent periodically share its map. It blindly tries to send all its graph
 * to its friend(s) If it was written properly, this sharing action would NOT be
 * in a ticker behaviour and only a subgraph would be shared.
 * 
 * @author hc
 *
 */
public class ShareMapBehaviour extends OneShotBehaviour {

	private ExploreCoopAgent myAgent;
	private List<String> receivers;

	/**
	 * The agent periodically share its map. It blindly tries to send all its graph
	 * to its friend(s) If it was written properly, this sharing action would NOT be
	 * in a ticker behaviour and only a subgraph would be shared.
	 * 
	 * @param ag         the agent
	 * @param receivers 
	 * @param period    the periodicity of the behaviour (in ms)
	 * @param mymap     (the map to share)
	 * @param receivers the list of agents to send the map to
	 */
	public ShareMapBehaviour(ExploreCoopAgent ag, List<String> receivers) {
		super(ag);
		this.myAgent = ag;
		this.receivers = receivers;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -568863390879327961L;

	@Override
	public void action() {
		// The agent blindly send all its graph to its surrounding
		// to illustrate how to share its knowledge (the topology currently) with the
		// the others agents.

		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setProtocol("SHARE-TOPO");
		msg.setSender(this.myAgent.getAID());
		for (String agentName : receivers) {
			msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
		}

		SerializableSimpleGraph<String, MapAttribute> sg = this.myAgent.getMyMap().getSerializableGraph();
		try {
			msg.setContentObject(sg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		((AbstractDedaleAgent) this.myAgent).sendMessage(msg);

	}

}
