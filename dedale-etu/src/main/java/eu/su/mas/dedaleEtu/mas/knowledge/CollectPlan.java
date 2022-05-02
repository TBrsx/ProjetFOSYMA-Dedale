package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

public class CollectPlan implements Serializable{
	
	private static final long serialVersionUID = -8140672521231830913L;
	private String name;
	private HashMap<String,MapAttributeCollect> nodes = new HashMap<String,MapAttributeCollect>();
	private LinkedList<String> diamondCollectors = new LinkedList<String>();
	private LinkedList<String> goldCollectors = new LinkedList<String>();

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
			if(added.getExplorer().isEmpty()) {
				added.setExplorer(this.nodes.get(added.getId()).getExplorer());
			}
		}
		this.nodes.put(added.getId(),added);
	}
	public void removeNodeWithId(String id) {
		this.nodes.remove(id);
	}
	
	public LinkedList<String> getAttributedNodes(String askName) {
		LinkedList<String> computedList = new LinkedList<String>();
		for (MapAttributeCollect n : this.nodes.values()) {
			
			if(n.getDiamondCollector().equalsIgnoreCase(askName) || n.getGoldCollector().equalsIgnoreCase(askName)||n.getExplorer().equalsIgnoreCase(askName)) {
				computedList.add(n.getId());
			}
		}
		return computedList;
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
		for(MapAttributeCollect n : this.nodes.values()) {
			if (!n.getDiamondCollector().isEmpty()){
				diamond = diamond.concat(n.getId() +  " : " + n.getDiamondCollector() + ", ");
			}
			if (!n.getGoldCollector().isEmpty()){
				gold = gold.concat(n.getId() +  " : " + n.getGoldCollector() + ", ");
			}
			if (!n.getExplorer().isEmpty()) {
				explorer = explorer.concat(n.getId() +  " : " + n.getExplorer() + ", ");
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

}
