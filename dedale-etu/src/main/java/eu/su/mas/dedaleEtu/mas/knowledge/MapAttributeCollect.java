package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

public class MapAttributeCollect implements Serializable{
	
	private static final long serialVersionUID = 8335323692755407639L;
	private String id;
	private String goldCollector;
	private String diamondCollector;
	
	//All the useful information about a node during the collect. Mostly a deprecated class, should be merged with MapAttribute.

	
	public MapAttributeCollect(String id,String diamondCollector,String goldCollector) {
		this.diamondCollector = diamondCollector;
		this.goldCollector = goldCollector;
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


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	

}
