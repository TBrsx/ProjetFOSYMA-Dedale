package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.LinkedList;

public class CollectPlan implements Serializable{
	
	private static final long serialVersionUID = -8140672521231830913L;
	private String name;
	private LinkedList<MapAttributeCollect> nodes = new LinkedList<MapAttributeCollect>();

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
		this.nodes.add(added);
	}
	public void removeNodeWithId(String id) {
		for (int i=0;i<this.nodes.size();i++) {
			System.out.println(this.nodes.get(i).getId());
			if (this.nodes.get(i).getId().equalsIgnoreCase(id)){
				this.nodes.remove(i);
				i = this.nodes.size()+1;
			}
		}
	}
	
	public LinkedList<String> getAttributedNodes(String askName) {
		LinkedList<String> computedList = new LinkedList<String>();
		for (MapAttributeCollect n : this.nodes) {
			
			if(n.getDiamondCollector().equalsIgnoreCase(askName) || n.getGoldCollector().equalsIgnoreCase(askName)||n.getExplorer().equalsIgnoreCase(askName)) {
				computedList.add(n.getId());
			}
		}
		return computedList;
	}

	public LinkedList<MapAttributeCollect> getNodes() {
		return this.nodes;
	}

	public void setNodes(LinkedList<MapAttributeCollect> nodes) {
		this.nodes = nodes;
	}
}
