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
public class ExploCoopBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;
	
	private static final int MAX_ACCEPTED_DIVERGEANCE = 5;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;
	private int stepCount=0;

	private List<String> list_agentNames;
	
	private LinkedList<String> pathToFollow = new LinkedList<String>();
	private int divergeanceFromPath = 0;

/**
 * 
 * @param myagent
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public ExploCoopBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap,List<String> agentNames) {
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
			if (myPosition.equalsIgnoreCase(this.pathToFollow.peek())) {
				this.pathToFollow.removeFirst();
			}

			//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes + claim them.
			String nextNode=null;
			Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
			while(iter.hasNext()){
				String nodeId=iter.next().getLeft();
				boolean isNewNode=this.myMap.addNewNode(nodeId,this.myAgent.getLocalName());
				//the node may exist, but not necessarily the edge
				if (myPosition!=nodeId) {
					this.myMap.addEdge(myPosition, nodeId);
					if (nextNode==null && isNewNode) nextNode=nodeId;
				}
			}

			//3) while openNodes is not empty, continues.
			if (!this.myMap.hasOpenNode()){
				//Explo finished
				this.myAgent.addBehaviour(new EndedExploCoopBehaviour((AbstractDedaleAgent) this.myAgent,this.myMap,this.list_agentNames));
				finished=true;
				System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done, behaviour removed. Done in " + Integer.toString(this.stepCount) + " moves");
			}else{
				//4) select next move.
				//4.1 If there exist one open node directly reachable, go for it, add it to the head of the path it wanted to take
				//	 otherwise choose one from the openNode list, compute the shortestPath and go for it
				if (nextNode==null){
					//no directly accessible openNode
					//chose one, compute the path and take the first step.
					if (this.pathToFollow.isEmpty()) {
						this.pathToFollow = this.myMap.getShortestPathToClosestOpenNode(myPosition,this.myAgent.getLocalName());//getShortestPath(myPosition,this.openNodes.get(0)).get(0);
					}
					this.divergeanceFromPath = 0;
					nextNode = this.pathToFollow.peek();
					//System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"| nextNode: "+nextNode);
				}else {
					this.divergeanceFromPath++;
					if (this.divergeanceFromPath>MAX_ACCEPTED_DIVERGEANCE) {
						this.pathToFollow = new LinkedList<String>();
					}else {
						this.pathToFollow.addFirst(nextNode);
					}
					//System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNode);
				}

				//5) At each time step, the agent check if he received a graph from a teammate. 	
				// If it was written properly, this sharing action should be in a dedicated behaviour set.
				MessageTemplate msgTemplate=MessageTemplate.and(
						MessageTemplate.MatchProtocol("SHARE-TOPO"),
						MessageTemplate.MatchPerformative(ACLMessage.INFORM));
				ACLMessage msgReceived=this.myAgent.receive(msgTemplate);
				if (msgReceived!=null) {
					SerializableSimpleGraph<String, MapAttribute> sgreceived=null;
					try {
						sgreceived = (SerializableSimpleGraph<String, MapAttribute>)msgReceived.getContentObject();
					} catch (UnreadableException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					this.myMap.mergeMap(sgreceived);
				}
				this.stepCount++;
				//System.out.println(this.myAgent.getLocalName()+ "- I'm at " +myPosition + " - Going to " + nextNode);
				((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);
			}

		}
	}

	@Override
	public boolean done() {
		return finished;
	}

}
