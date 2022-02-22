package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

import dataStructures.tuple.Couple;

//This class is where we store attribute of nodes

public class MapAttribute implements Serializable {

	private static final long serialVersionUID = 3664792526110659066L;


	private String state; //open,closed
	private String occupied; //wumpus,agent
	private String claimant; //name of agent that claimed it, empty string if none
	private Couple<String, Integer> treasure;


	public MapAttribute() {
		this.state = "open";
		this.claimant = "";
		this.occupied = "";
		this.treasure = new Couple("", 0);
	}

	public MapAttribute(String state, String claimant) {
		this();
		this.state = state;
		this.claimant = claimant;
	}

	public MapAttribute(String state, String claimant, String occupied, Couple<String, Integer> treasure) {
		this(state, claimant);
		this.occupied = occupied;
		this.treasure = treasure;
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

	public Couple<String, Integer> getTreasure() {
		return treasure;
	}

	public void setTreasure(Couple<String, Integer> treasure) {
		this.treasure = treasure;
	}
}