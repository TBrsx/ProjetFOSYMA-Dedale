package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

public class MapAttributeCollect implements Serializable{
	
	private static final long serialVersionUID = 8335323692755407639L;
	private String id;
	private String diamondCollector; //Name of agent that is tasked to collecting diamond on this node
	private String goldCollector; //Name of agent that is tasked to collecting gold on this node
	private String explorer; //Name of agent that has been tasked to look for changes on this node
	
	public MapAttributeCollect(String id,String diamondCollector,String goldCollector, String explorer) {
		this.diamondCollector = diamondCollector;
		this.goldCollector = goldCollector;
		this.explorer = explorer;
		this.id = id;
	}

	public String getDiamondCollector() {
		return diamondCollector;
	}

	public void setDiamondCollector(String diamondCollector) {
		this.diamondCollector = diamondCollector;
	}

	public String getGoldCollector() {
		return goldCollector;
	}

	public void setGoldCollector(String goldCollector) {
		this.goldCollector = goldCollector;
	}

	public String getExplorer() {
		return explorer;
	}

	public void setExplorer(String explorer) {
		this.explorer = explorer;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	

}
