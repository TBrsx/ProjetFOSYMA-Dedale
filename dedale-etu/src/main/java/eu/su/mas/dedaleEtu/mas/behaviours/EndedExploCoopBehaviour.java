package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;


import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


/**
 * <pre>
 * This behaviour allows an agent to explore the environment and learn the associated topological map.
 * The algorithm is a pseudo - DFS computationally consuming because its not optimised at all.
 * 
 * When all the nodes around him are visited, the agent randomly select an open node and go there to restart its dfs. 
 * This (non optimal) behaviour is done until all nodes are explored. 
 * 
 * Warning, this behaviour does not save the content of visited nodes, only the topology.
 * Warning, the sub-behaviour ShareMap periodically share the whole map
 * </pre>
 * @author hc
 *
 */
public class EndedExploCoopBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787761L;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;

	private List<String> list_agentNames;

/**
 * 
 * @param myagent
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public EndedExploCoopBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap,List<String> agentNames) {
		super(myagent);
		this.myMap=myMap;
		this.list_agentNames=agentNames;
		
	}

	@Override
	public void action() {

		if(this.myMap==null) {
			this.myMap= new MapRepresentation();
			this.myAgent.addBehaviour(new ShareMapBehaviour(this.myAgent,500,this.myMap,list_agentNames));
		}

		//0) Retrieve the current position
		String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		//System.out.println(this.myAgent.getLocalName()+ "- I'm at " +myPosition + " - This node claimant was " + this.myMap.getNodeClaimant(myPosition));

		if (myPosition!=null){
			//List of observable from the agent's current position
			List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be too quick
			 */
			try {
				this.myAgent.doWait(500);
			} catch (Exception e) {
				e.printStackTrace();
			}

			//1) remove the current node from openlist and add it to closedNodes + claim it.
			this.myMap.addNode(myPosition, new MapAttribute("closed",this.myAgent.getLocalName()));

			//2) get the surrounding nodes and, if not in closedNodes, go back to the ExploCoopBehaviour
			Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
			while(iter.hasNext()){
				String nodeId=iter.next().getLeft();
				this.myMap.addNewNode(nodeId,this.myAgent.getLocalName());
			}
			if (this.myMap.hasOpenNode()){
				System.out.println(this.myAgent.getLocalName()+" - I found something new !! ");
				this.myAgent.addBehaviour(new ExploCoopBehaviour((AbstractDedaleAgent) this.myAgent,this.myMap,this.list_agentNames));
				finished=true;
			}else {
				//Random move from the current position
				Random r= new Random();
				int moveId=1+r.nextInt(lobs.size()-1);//removing the current position from the list of target, not necessary as to stay is an action but allow quicker random move
				System.out.println(this.myAgent.getLocalName()+ "- I'm at " +myPosition + " - Going randomly to " + lobs.get(moveId).getLeft());
				((AbstractDedaleAgent)this.myAgent).moveTo(lobs.get(moveId).getLeft());
				}

		}
	}

	@Override
	public boolean done() {
		return finished;
	}

}
