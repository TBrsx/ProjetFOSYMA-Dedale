package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;

//This class is where we store attribute of nodes

public class MapAttribute implements Serializable {

	private static final long serialVersionUID = 3664792526110659066L;


	private String state; //open,closed
	private String occupied; //wumpus,agent
	private String claimant; //name of agent that claimed it, empty string if none
	private String collector; //name of agent that has been tasked of collecting here, empty string if none
	private Couple<Observation,Integer> treasure;


	public MapAttribute() {
		this.state = "open";
		this.claimant = "";
		this.occupied = "";
		this.treasure = new Couple<Observation, Integer>(null, 0);
		this.collector = "";
	}

	public MapAttribute(String state, String claimant, String occupied, Couple<Observation, Integer> treasure,String collector) {
		this.state = state;
		this.claimant = claimant;
		this.occupied = occupied;
		this.treasure = treasure;
		this.collector = collector;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getClaimant() {
		return claimant;
	}

	public void setClaimant(String claimant) {
		this.claimant = claimant;
	}

	public String getOccupied() {
		return occupied;
	}

	public void setOccupied(String occupied) {
		this.occupied = occupied;
	}

	public Couple<Observation, Integer> getTreasure() {
		return treasure;
	}

	public void setTreasure(Couple<Observation,Integer> treasure) {
		this.treasure = treasure;
	}

	public String getCollector() {
		return collector;
	}

	public void setCollector(String collector) {
		this.collector = collector;
	}
}