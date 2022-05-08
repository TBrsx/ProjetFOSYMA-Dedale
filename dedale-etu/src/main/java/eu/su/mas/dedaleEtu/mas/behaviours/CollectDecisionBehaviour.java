package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.CollectPlan;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttributeCollect;
import eu.su.mas.dedaleEtu.mas.knowledge.OtherAgent;
import jade.core.behaviours.OneShotBehaviour;

public class CollectDecisionBehaviour extends OneShotBehaviour{
	
	private static final long serialVersionUID = 3329007121557183780L;
	private static final int PLAN_SHARING = 0;
	private static final int BEGIN_COLLECT = 1;
	private static final int INTERLOCKING = 2;
	private int returnCode;
	private int totalDiamond = 0;
	private int totalGold = 0;
	
	private ExploreCoopAgent myAgent;
	
	public CollectDecisionBehaviour(ExploreCoopAgent myagent) {
		super(myagent);
		this.myAgent = myagent;
	}
	
	private void moveToMeeting() {
		String meeting = this.myAgent.getMeetingPoint();
		if (this.myAgent.getPathToFollow() == null){
			this.myAgent.setPathToFollow(new LinkedList<String>());
		}
		if (this.myAgent.getPathToFollow().isEmpty()){
			this.myAgent.setPathToFollow(this.myAgent.getMyMap().getRandomPathFrom(this.myAgent.getCurrentPosition(), 3));
			if(this.myAgent.getCurrentPosition().equalsIgnoreCase(meeting)) { //If I am at the meeting point
				getDataStore().put("movesWithoutSharing", (int) getDataStore().get("movesWithoutSharing")+1);
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
	}
	
	private void createPlan() {
		//If I don't know every backpack sizes, I can't create a plan !
		boolean knowsEveryCapacity = true;
		//Map.entry are immutable and easier to sort on, that's why we chose this and not hashmap
		HashMap<String, Integer> agentsDiamondCapacity = new HashMap<String, Integer>();
		HashMap<String, Integer> agentsGoldCapacity = new HashMap<String, Integer>();
		
		Iterator<Map.Entry<String, OtherAgent>> entries = this.myAgent.getOtherAgents().entrySet().iterator();
		while (knowsEveryCapacity && entries.hasNext()) {
			Map.Entry<String, OtherAgent> entry = entries.next();
			OtherAgent agent =  entry.getValue();
			if(!agent.isKnownCapa()) {
				knowsEveryCapacity = false;
			}
		}
		
		//If I do know them, I CAN make a plan !
		if(knowsEveryCapacity) {
			
			//Inits
			((LinkedList<String>) this.getDataStore().get("awareOfPlan")).add(this.myAgent.getLocalName());
			ArrayList<String> allNodes = (ArrayList<String>) this.myAgent.getMyMap().getAllNodes();
			
			LinkedList<Map.Entry<String, Integer>> diamondNodes = new LinkedList<Map.Entry<String, Integer>>();
			LinkedList<Map.Entry<String, Integer>> goldNodes = new LinkedList<Map.Entry<String, Integer>>();
			LinkedList<String> toExploreNodes = new LinkedList<String>();
			CollectPlan elPlan = new CollectPlan("ElPlan_"+"1");
			elPlan.setVersion(1);
			elPlan.setNodesInPlan(allNodes.size());
			if(!this.myAgent.getMyMap().hasOpenNode()) {
				elPlan.setComplete(true);
			}
			Map.Entry<String, Integer> bestCollectorD = null;
			Map.Entry<String, Integer> bestCollectorG = null;
			
			//Get info on nodes to collect or explore
			for (String n : allNodes) {
				MapAttribute mapAtt = this.myAgent.getMyMap().getMapAttributeFromNodeId(n);
				if (mapAtt.getTreasure().getLeft() != null) {
					if(mapAtt.getTreasure().getLeft() == Observation.DIAMOND) {
						diamondNodes.add(Map.entry(n,mapAtt.getTreasure().getRight()));
						totalDiamond += mapAtt.getTreasure().getRight();
					}else if(mapAtt.getTreasure().getLeft() == Observation.GOLD) {
						goldNodes.add(Map.entry(n,mapAtt.getTreasure().getRight()));
						totalGold += mapAtt.getTreasure().getRight();
					}
				}
				if (mapAtt.getState().equalsIgnoreCase("open")) {
					toExploreNodes.add(n);
				}
			}
			
			Observation priority = totalDiamond > totalGold ? Observation.DIAMOND : Observation.GOLD;
			
			//Add this agent own capacities, and put it in its best role (gold or diamond collector, depending on the size of its backpack)
			List<Couple<Observation, Integer>> bp = this.myAgent.getBackPackFreeSpace();
			int diamCapa = 0;
			int goldCapa = 0;
			for(Couple<Observation,Integer> c : bp) {
				if(c.getLeft() == Observation.DIAMOND) {
					diamCapa = c.getRight();
				}
				if(c.getLeft() == Observation.GOLD) {
					goldCapa = c.getRight();
				}
			}
			bestCollectorD = Map.entry(this.myAgent.getLocalName(),diamCapa);
			bestCollectorG = Map.entry(this.myAgent.getLocalName(),goldCapa);
			if(diamCapa>goldCapa || (priority == Observation.DIAMOND && diamCapa == goldCapa)) {
				agentsDiamondCapacity.put(this.myAgent.getLocalName(),diamCapa);
			}else{
				agentsGoldCapacity.put(this.myAgent.getLocalName(),goldCapa);
			}
			
			
			//Add other agents capacities, and put them in their best role
			entries = this.myAgent.getOtherAgents().entrySet().iterator();
			while (knowsEveryCapacity && entries.hasNext()) {
				Map.Entry<String, OtherAgent> entry = entries.next();
				OtherAgent agent =  entry.getValue();
				diamCapa = agent.getCapaDiamond();
				goldCapa = agent.getCapaGold();
				if (diamCapa > bestCollectorD.getValue()){
					bestCollectorD = Map.entry(agent.getName(),diamCapa);
				}
				if (goldCapa > bestCollectorG.getValue()){
					bestCollectorG = Map.entry(agent.getName(),goldCapa);
				}
				if(diamCapa>goldCapa || (priority == Observation.DIAMOND && diamCapa == goldCapa)) {
					agentsDiamondCapacity.put(agent.getName(),diamCapa);
				}else{
					agentsGoldCapacity.put(agent.getName(),goldCapa);
				}
			}
			
			if(agentsDiamondCapacity.isEmpty()) {
				agentsDiamondCapacity.put(bestCollectorD.getKey(),bestCollectorD.getValue());
			}
			if(agentsGoldCapacity.isEmpty()) {
				agentsGoldCapacity.put(bestCollectorG.getKey(),bestCollectorG.getValue());
			}
			
			if(elPlan.isComplete()) {
				if(totalDiamond < 1) {
					for(String agent : agentsDiamondCapacity.keySet()) {
						agentsGoldCapacity.put(agent, this.myAgent.getOtherAgents().get(agent).getCapaGold());
					}
				}
				if(totalGold < 1) {
					for(String agent : agentsGoldCapacity.keySet()) {
						agentsDiamondCapacity.put(agent, this.myAgent.getOtherAgents().get(agent).getCapaDiamond());
					}
				}
			}
			
			HashMap<String,Double> fillingRatioDiamond = new HashMap<String,Double>();
			HashMap<String,Double> fillingRatioGold = new HashMap<String,Double>();
			HashMap<String,Integer> spaceRemaining = new HashMap<String,Integer>();
			
			for(String agent : agentsDiamondCapacity.keySet()) {
				fillingRatioDiamond.put(agent, 0.0);
				spaceRemaining.put(agent, agentsDiamondCapacity.get(agent));
				elPlan.getDiamondCollectors().add(agent);
			}
			
			for(String agent : agentsGoldCapacity.keySet()) {
				fillingRatioGold.put(agent, 0.0);
				spaceRemaining.put(agent, agentsGoldCapacity.get(agent));
				elPlan.getGoldCollectors().add(agent);
			}
			
			//Sort on amount to collect
			Collections.sort(diamondNodes, (o1,o2) -> o1.getValue().compareTo(o2.getValue()));
			Collections.sort(goldNodes, (o1,o2) -> o1.getValue().compareTo(o2.getValue()));
			
			
			
			//===What follows is O(n*k) where n is the number of nodes, k the number of agents. A bit expensive, but still manageable.
			
			for(Map.Entry<String, Integer> dN : diamondNodes) {
				//Find the agent with the lowest ratio and the largest available space in case of a tie
				Map.Entry<String, Double> lowestRatio = Map.entry("", 101.0);
				int biggestSpaceRemaining = 0;
				for(String agent : agentsDiamondCapacity.keySet()) {
					if (dN.getValue() < spaceRemaining.get(agent)){
						if (fillingRatioDiamond.get(agent)<lowestRatio.getValue() || 
								(fillingRatioDiamond.get(agent) == lowestRatio.getValue() && spaceRemaining.get(agent) > biggestSpaceRemaining)){
							lowestRatio = Map.entry(agent, fillingRatioDiamond.get(agent));
							biggestSpaceRemaining = spaceRemaining.get(agent);
						}
					}
				}
				if(lowestRatio.getValue()<101) { //We did find an agent eligible
					String chosenAgent = lowestRatio.getKey();
					elPlan.addNode(new MapAttributeCollect(dN.getKey(),chosenAgent,""));
					//Compute the remaining space the ratio and if it is now at 100%, remove this agent from the list
					spaceRemaining.put(chosenAgent, spaceRemaining.get(chosenAgent)- dN.getValue());
					Double newRatio = ((double) (agentsDiamondCapacity.get(chosenAgent)-spaceRemaining.get(chosenAgent)))/agentsDiamondCapacity.get(chosenAgent)*100;
					if(newRatio>=100.0) {
						agentsDiamondCapacity.remove(chosenAgent);
					}else {
						fillingRatioDiamond.put(chosenAgent, newRatio);
					}
				}
			}
			
			for(Map.Entry<String, Integer> gN : goldNodes) {
				//Find the agent with the lowest ratio and the largest available space in case of a tie
				Map.Entry<String, Double> lowestRatio = Map.entry("", 101.0);
				int biggestSpaceRemaining = 0;
				for(String agent : agentsGoldCapacity.keySet()) {
					if (gN.getValue() < spaceRemaining.get(agent)){
						if (fillingRatioGold.get(agent)<lowestRatio.getValue() || 
								(fillingRatioGold.get(agent) == lowestRatio.getValue() && spaceRemaining.get(agent) > biggestSpaceRemaining)){
							lowestRatio = Map.entry(agent, fillingRatioGold.get(agent));
							biggestSpaceRemaining = spaceRemaining.get(agent);
						}
					}
				}
				if(lowestRatio.getValue()<101) { //We did find an agent eligible
					String chosenAgent = lowestRatio.getKey();
					elPlan.addNode(new MapAttributeCollect(gN.getKey(),"",chosenAgent));
					//Compute the remaining space the ratio and if it is now at 100%, remove this agent from the list
					spaceRemaining.put(chosenAgent, spaceRemaining.get(chosenAgent)- gN.getValue());
					Double newRatio = ((double) (agentsGoldCapacity.get(chosenAgent)-spaceRemaining.get(chosenAgent)))/agentsGoldCapacity.get(chosenAgent)*100;
					if(newRatio>=100.0) {
						agentsGoldCapacity.remove(chosenAgent);
					}else {
						fillingRatioGold.put(chosenAgent, newRatio);
					}
				}
			}
		
			
			//===
			elPlan.saveRatios(fillingRatioDiamond, fillingRatioGold, agentsDiamondCapacity, agentsGoldCapacity);
			elPlan.setNodesToExplore(toExploreNodes);
			this.myAgent.setCurrentPlan(elPlan);
			
			
			System.out.println(this.myAgent.getLocalName() + " - I created a plan, named " + this.myAgent.getCurrentPlan().getName());
			System.out.println(elPlan);
			this.returnCode = PLAN_SHARING;
			return;
		}else {
			this.moveToMeeting();
		}
		
		this.returnCode = PLAN_SHARING;
		return;
	}
	
	private void adaptPlan() {
		if(!this.myAgent.getMyMap().hasOpenNode()) {
			this.myAgent.getCurrentPlan().setComplete(true);
		}
		this.myAgent.getCurrentPlan().adaptPlan(this.myAgent.getMyMap(), (LinkedList<String>) this.getDataStore().get("awareOfPlan"),this.myAgent.getCurrentPlan());
		LinkedList<String> experts = new LinkedList<String>();
		experts.add(this.myAgent.getLocalName());
		getDataStore().put("awareOfPlan",experts);
		this.myAgent.getCurrentPlan().setVersion(this.myAgent.getCurrentPlan().getVersion()+1);
		String truncatedName = this.myAgent.getCurrentPlan().getName().replaceAll("[0-9]", "");
		this.myAgent.getCurrentPlan().setName(truncatedName+this.myAgent.getCurrentPlan().getVersion()+1);
		
		System.out.println(this.myAgent.getLocalName() + " - I created a plan, named " + this.myAgent.getCurrentPlan().getName());
	}

	private void sharePlan() { //Move to/around the meeting point
		
		//Stop sharing and start collecting if all agents are experts
		LinkedList<String> experts = (LinkedList<String>) this.getDataStore().get("awareOfPlan");
		if (experts.size() >= this.myAgent.getOtherAgents().size()+1) {
			startCollect();
		}else {
			//System.out.println(experts);
			this.returnCode = PLAN_SHARING;
			this.moveToMeeting();
			return;
		}
	}
	private void searchPlan() {
		//Move randomly around the meeting point, waiting for plan
		this.returnCode = PLAN_SHARING;
		this.moveToMeeting();
		return;
	}
	
	private void startCollect() {
		
		if (this.myAgent.getCurrentPlan().getAttributedNodes(this.myAgent.getLocalName()).isEmpty()) { //Useful if the agent had nothing to collect and so has nowhere to go
			this.getDataStore().put("skipInCollect", true);
			return;
		}else {
			//Set path to follow to reach first treasure to collect
			this.myAgent.setPathToFollow(this.myAgent.getMyMap().getShortestPathToClosestInList(this.myAgent.getCurrentPosition(), 
					this.myAgent.getCurrentPlan().getAttributedNodes(this.myAgent.getLocalName())));
			if (this.myAgent.getPathToFollow() == null) {
				this.myAgent.setPathToFollow(new LinkedList<String>());
			}
			this.myAgent.setNextPosition("");
			String firstNodeName = this.myAgent.getCurrentPlan().getAttributedNodes(this.myAgent.getLocalName()).getFirst();
			if(this.myAgent.getMyMap().getMapAttributeFromNodeId(firstNodeName).getTreasure().getLeft()==Observation.DIAMOND) {
				this.myAgent.setTreasureType(Observation.DIAMOND);
			}
			if(this.myAgent.getMyMap().getMapAttributeFromNodeId(firstNodeName).getTreasure().getLeft()==Observation.GOLD) {
				this.myAgent.setTreasureType(Observation.GOLD);
			}
			this.returnCode = BEGIN_COLLECT;

			return;
		}
	}
	
	@Override
	public void action() {
		System.out.println(this.myAgent.getLocalName() + " - started behavior " + this.getBehaviourName());
		this.myAgent.doWait((int) this.getDataStore().get("waitingTime"));
		String decisionMaster = (String) this.getDataStore().get("decision-master");
		if(decisionMaster.equalsIgnoreCase(this.myAgent.getLocalName())){
			if ((Boolean) this.getDataStore().get("CreateNewPlan")){
				this.getDataStore().put("CreateNewPlan", false);
				if(this.myAgent.getCurrentPlan() == null) {
					this.createPlan();
					return;
				}else {
					this.adaptPlan();
					return;
				}
			}else {
				this.sharePlan();
				return;
			}
		}else {
			if (this.myAgent.getCurrentPlan() == null) {
				this.searchPlan();
				return;
			}else {
				System.out.println(this.myAgent.getLocalName() + " - I received a plan, named " + this.myAgent.getCurrentPlan().getName());
				this.startCollect();
				return;
			}
		}
	}
	
	public int onEnd() {
		System.out.println(this.myAgent.getLocalName() + " - ended behavior " + this.getBehaviourName() + " return code " + this.returnCode + " moves " + (int) getDataStore().get("movesWithoutSharing"));
		return this.returnCode;

	}

}
