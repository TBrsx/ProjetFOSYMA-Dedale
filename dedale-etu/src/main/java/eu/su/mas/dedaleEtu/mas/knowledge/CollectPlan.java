package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import eu.su.mas.dedale.env.Observation;


public class CollectPlan implements Serializable{
	
	private static final long serialVersionUID = -8140672521231830913L;
	private String name;
	private HashMap<String,MapAttributeCollect> nodes = new HashMap<String,MapAttributeCollect>();
	private LinkedList<String> diamondCollectors = new LinkedList<String>();
	private LinkedList<String> goldCollectors = new LinkedList<String>();
	private LinkedList<String> nodesToExplore = new LinkedList<String>();
	private HashMap<String,Double> fillingRatioDiamond;
	private HashMap<String,Double> fillingRatioGold;
	private HashMap<String,Integer> spaceRemainingDiamond;
	private HashMap<String,Integer> spaceRemainingGold;
	private boolean isComplete = false;
	private int nodesInPlan = 0;
	private int version = 0;

	public CollectPlan(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void addNode(MapAttributeCollect added) {
		if(this.nodes.get(added.getId())!= null) {
			if(added.getDiamondCollector().isEmpty()) {
				added.setDiamondCollector(this.nodes.get(added.getId()).getDiamondCollector());
			}
			if(added.getGoldCollector().isEmpty()) {
				added.setGoldCollector(this.nodes.get(added.getId()).getGoldCollector());
			}
		}
		this.nodes.put(added.getId(),added);
		this.nodesInPlan++;
	}
	public void removeNodeWithId(String id) {
		this.nodes.remove(id);
		this.nodesInPlan--;
	}
	
	public CollectPlan partOfPlan(String agent) {
		CollectPlan partOfPlan = new CollectPlan(this.name);
		partOfPlan.setNodesInPlan(this.nodesInPlan);
		for (MapAttributeCollect n : this.nodes.values()) {
			if(n.getDiamondCollector().equalsIgnoreCase(agent) || n.getGoldCollector().equalsIgnoreCase(agent)) {
				partOfPlan.addNode(n);
			}
		}
		partOfPlan.setNodesToExplore(this.nodesToExplore);
		partOfPlan.setComplete(isComplete);
		return partOfPlan;
	}
	
	public LinkedList<String> getAttributedNodes(String askName) {
		LinkedList<String> computedList = new LinkedList<String>();
		for (MapAttributeCollect n : this.nodes.values()) {
			
			if(n.getDiamondCollector().equalsIgnoreCase(askName) || n.getGoldCollector().equalsIgnoreCase(askName)) {
				computedList.add(n.getId());
			}
		}
		return computedList;
	}
	
	public void saveRatios(HashMap<String,Double> fillingRatioDiamond,HashMap<String,Double> fillingRatioGold,HashMap<String,Integer> spaceRemainingDiamond,HashMap<String,Integer> spaceRemainingGold) {
		this.fillingRatioDiamond = fillingRatioDiamond;
		this.fillingRatioGold = fillingRatioGold;
		this.spaceRemainingDiamond = spaceRemainingDiamond;
		this.spaceRemainingGold = spaceRemainingGold;
	}
	
	public void adaptPlan(MapRepresentation myMap,LinkedList<String> agentsAlreadyCollecting,CollectPlan oldPlan) {
		HashMap<String,MapAttributeCollect> nodesOldPlan = oldPlan.nodes;
		Map.Entry<String, Integer> dN = null;
		Map.Entry<String, Integer> gN = null;
		for (String nodeId : this.nodes.keySet()) {
			String collectorD = nodesOldPlan.get(nodeId).getDiamondCollector();
			String collectorG = nodesOldPlan.get(nodeId).getGoldCollector();
			if(!(collectorD != null && agentsAlreadyCollecting.contains(collectorD))) {
				if(!(collectorG != null && agentsAlreadyCollecting.contains(collectorG))) {
					//We have to attribute this node to an agent
					if(myMap.getMapAttributeFromNodeId(nodeId).getTreasure().getLeft()==Observation.DIAMOND) {
						dN = Map.entry(nodeId, myMap.getMapAttributeFromNodeId(nodeId).getTreasure().getRight());
						//Find the agent with the lowest ratio and the largest available space in case of a tie
						Map.Entry<String, Double> lowestRatio = Map.entry("", 101.0);
						int biggestSpaceRemaining = 0;
						for(String agent : this.spaceRemainingDiamond.keySet()) {
							if (dN.getValue() < this.spaceRemainingDiamond.get(agent)){
								if (fillingRatioDiamond.get(agent)<lowestRatio.getValue() || 
										(fillingRatioDiamond.get(agent) == lowestRatio.getValue() && this.spaceRemainingDiamond.get(agent) > biggestSpaceRemaining)){
									lowestRatio = Map.entry(agent, this.fillingRatioDiamond.get(agent));
									biggestSpaceRemaining = this.spaceRemainingDiamond.get(agent);
								}
							}
						}
						if(lowestRatio.getValue()<101) { //We did find an agent eligible
							String chosenAgent = lowestRatio.getKey();
							this.removeNodeWithId(nodeId);
							this.addNode(new MapAttributeCollect(dN.getKey(),chosenAgent,""));
							//Compute the remaining space the ratio and if it is now at 100%, remove this agent from the list
							int oldSpaceRemaining = this.spaceRemainingDiamond.get(chosenAgent);
							this.spaceRemainingDiamond.put(chosenAgent, (int) this.spaceRemainingDiamond.get(chosenAgent)- dN.getValue());
							Double newRatio = ((double) (this.fillingRatioDiamond.get(chosenAgent)*this.spaceRemainingDiamond.get(chosenAgent)/(float)oldSpaceRemaining));
							if(newRatio>=100.0) {
								this.spaceRemainingDiamond.remove(chosenAgent);
							}else {
								fillingRatioDiamond.put(chosenAgent, newRatio);
							}
						}
					}else if(myMap.getMapAttributeFromNodeId(nodeId).getTreasure().getLeft()==Observation.GOLD) {
						gN = Map.entry(nodeId, myMap.getMapAttributeFromNodeId(nodeId).getTreasure().getRight());
						//Find the agent with the lowest ratio and the largest available space in case of a tie
						Map.Entry<String, Double> lowestRatio = Map.entry("", 101.0);
						int biggestSpaceRemaining = 0;
						for(String agent : this.spaceRemainingGold.keySet()) {
							if (dN.getValue() < this.spaceRemainingGold.get(agent)){
								if (fillingRatioGold.get(agent)<lowestRatio.getValue() || 
										(fillingRatioGold.get(agent) == lowestRatio.getValue() && this.spaceRemainingGold.get(agent) > biggestSpaceRemaining)){
									lowestRatio = Map.entry(agent, this.fillingRatioGold.get(agent));
									biggestSpaceRemaining = this.spaceRemainingGold.get(agent);
								}
							}
						}
						if(lowestRatio.getValue()<101) { //We did find an agent eligible
							String chosenAgent = lowestRatio.getKey();
							this.removeNodeWithId(nodeId);
							this.addNode(new MapAttributeCollect(gN.getKey(),"",chosenAgent));
							//Compute the remaining space the ratio and if it is now at 100%, remove this agent from the list
							int oldSpaceRemaining = this.spaceRemainingGold.get(chosenAgent);
							this.spaceRemainingGold.put(chosenAgent, (int) this.spaceRemainingGold.get(chosenAgent)- gN.getValue());
							Double newRatio = ((double) (this.fillingRatioGold.get(chosenAgent)*this.spaceRemainingGold.get(chosenAgent)/(float)oldSpaceRemaining));
							if(newRatio>=100.0) {
								this.spaceRemainingGold.remove(chosenAgent);
							}else {
								fillingRatioGold.put(chosenAgent, newRatio);
							}
						}
					}
					
				}
			}
			
		}
	}

	public HashMap<String,MapAttributeCollect> getNodes() {
		return this.nodes;
	}

	public void setNodes(HashMap<String, MapAttributeCollect> nodes) {
		this.nodes = nodes;
	}

	@Override
	public String toString() {
		String returnedString = "";
		returnedString = returnedString.concat(getName() + "\n");
		String diamond = "Diamond :\n";
		String gold = "Gold :\n";
		String explorer = "Explorer :\n";
		explorer = explorer.concat(this.nodesToExplore.toString());
		for(MapAttributeCollect n : this.nodes.values()) {
			if (!n.getDiamondCollector().isEmpty()){
				diamond = diamond.concat(n.getId() +  " : " + n.getDiamondCollector() + ", ");
			}
			if (!n.getGoldCollector().isEmpty()){
				gold = gold.concat(n.getId() +  " : " + n.getGoldCollector() + ", ");
			}
		}
		return returnedString.concat(gold + "\n" + diamond + "\n" + explorer);
	}

	public LinkedList<String> getDiamondCollectors() {
		return diamondCollectors;
	}

	public void setDiamondCollectors(LinkedList<String> diamondCollectors) {
		this.diamondCollectors = diamondCollectors;
	}

	public LinkedList<String> getGoldCollectors() {
		return goldCollectors;
	}

	public void setGoldCollectors(LinkedList<String> goldCollectors) {
		this.goldCollectors = goldCollectors;
	}

	public int getNodesInPlan() {
		return nodesInPlan;
	}

	public void setNodesInPlan(int nodesInPlan) {
		this.nodesInPlan = nodesInPlan;
	}

	public boolean isComplete() {
		return isComplete;
	}

	public void setComplete(boolean isComplete) {
		this.isComplete = isComplete;
	}

	public LinkedList<String> getNodesToExplore() {
		return nodesToExplore;
	}

	public void setNodesToExplore(LinkedList<String> nodesToExplore) {
		this.nodesToExplore = nodesToExplore;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

}
