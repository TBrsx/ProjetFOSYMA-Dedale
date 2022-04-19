package eu.su.mas.dedaleEtu.mas.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;

import eu.su.mas.dedaleEtu.mas.behaviours.BehavioursFSM;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.OtherAgent;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import jade.core.behaviours.Behaviour;


public class ExploreCoopAgent extends AbstractDedaleAgent {

	private static final long serialVersionUID = -7969469610241668140L;
	private MapRepresentation myMap;
	private List<String> listAgentNames = new ArrayList<String>();
	private String nextPosition;
	private String targetPosition;
	private LinkedList<String> pathToFollow = new LinkedList<String>();
	private boolean isInterlocking = false;
	private HashMap<String,OtherAgent> otherAgents = new HashMap<String,OtherAgent>();
	private Observation treasureType = Observation.GOLD;
	private int maxTreasureQuantity;
	private String currentPlan = "";
	private String meetingPoint = "";

	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 1) set the agent
	 * attributes 2) add the behaviours
	 * 
	 */
	protected void setup() {

		super.setup();

		// get the parameters added to the agent at creation (if any)
		final Object[] args = getArguments();

		if (args.length == 0) {
			System.err.println("Error while creating the agent, names of agent to contact expected");
			System.exit(-1);
		} else {
			int i = 2;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next
						// release.
			while (i < args.length) {
				this.getListAgentNames().add((String) args[i]);
				this.otherAgents.put((String) args[i],new OtherAgent((String) args[i]));
				i++;
			}
		}

		List<Behaviour> lb = new ArrayList<Behaviour>();

		/************************************************
		 * 
		 * ADD the behaviours of the Dummy Moving Agent
		 * 
		 ************************************************/

		lb.add(new BehavioursFSM(this));

		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */

		addBehaviour(new startMyBehaviours(this, lb));

		System.out.println("the  agent " + this.getLocalName() + " is started");

	}

	public MapRepresentation getMyMap() {
		return myMap;
	}

	public void setMyMap(MapRepresentation myMap) {
		this.myMap = myMap;
	}

	public List<String> getListAgentNames() {
		return listAgentNames;
	}

	public void setNextPosition(String nextPosition) { this.nextPosition = nextPosition; }

	public String getNextPosition() { return nextPosition; }

	public String getTargetPosition() {
		return targetPosition;
	}

	public void setTargetPosition(String targetPosition) {
		this.targetPosition = targetPosition;
	}

	public LinkedList<String> getPathToFollow() {
		return pathToFollow;
	}

	public void setPathToFollow(LinkedList<String> pathToFollow) {
		this.pathToFollow = pathToFollow;
	}

	public HashMap<String,OtherAgent> getOtherAgents() {
		return this.otherAgents;
	}

	public void setOtherAgents(HashMap<String,OtherAgent> otherAgents) {
		this.otherAgents = otherAgents;
	}
	
	//Add a node to the informations we have to transfer the next time we see another agents - do this for ALL agents in otherAgents
	public void addNodeOtherAgents(Node n) {
		
		Iterator<Map.Entry<String, OtherAgent>> entries = this.getOtherAgents().entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry<String, OtherAgent> entry = entries.next();
			OtherAgent agent =  entry.getValue();
			agent.addNode(n);
		}
	}
	
	//Add an edge the informations we have to transfer the next time we see another agents - do this for ALL agents in otherAgents
		public void addEdgeOtherAgents(Edge e) {
			
			Iterator<Map.Entry<String, OtherAgent>> entries = this.getOtherAgents().entrySet().iterator();
			while (entries.hasNext()) {
				Map.Entry<String, OtherAgent> entry = entries.next();
				OtherAgent agent =  entry.getValue();
				agent.addEdge(e);
			}
		}
		
		//Add an edge the informations we have to transfer the next time we see another agents - do this for ALL agents in otherAgents, except one given in parameter
			public void addEdgeOtherAgents(Edge e,String excludedAgentName) {
				
				Iterator<Map.Entry<String, OtherAgent>> entries = this.getOtherAgents().entrySet().iterator();
				while (entries.hasNext()) {
					Map.Entry<String, OtherAgent> entry = entries.next();
					OtherAgent agent =  entry.getValue();
					if (!agent.getName().equals(excludedAgentName)){
						agent.addEdge(e);
					}
				}
			}

			public Observation getTreasureType() {
				return treasureType;
			}

			public void setTreasureType(Observation treasureType) {
				this.treasureType = treasureType;
			}

			public int getMaxTreasureQuantity() {
				return maxTreasureQuantity;
			}

			public void setMaxTreasureQuantity(int maxTreasureQuantity) {
				this.maxTreasureQuantity = maxTreasureQuantity;
			}

			public String getCurrentPlan() {
				return currentPlan;
			}

			public void setCurrentPlan(String currentPlan) {
				this.currentPlan = currentPlan;
			}

			public String getMeetingPoint() {
				return meetingPoint;
			}

			public void setMeetingPoint(String meetingPoint) {
				this.meetingPoint = meetingPoint;
			}

	public void setInterlocking(boolean interlocking) {
		isInterlocking = interlocking;
	}

	public boolean isInterlocking() {
		return isInterlocking;
	}
}
