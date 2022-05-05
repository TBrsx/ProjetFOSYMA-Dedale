package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.Optional;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;

//This class is where we store attribute of nodes

public class MapAttribute implements Serializable {

	private static final long serialVersionUID = 3664792526110659066L;


	private String state; //open,closed
	private boolean blocked; //wumpus
	private String claimant; //name of agent that claimed it, empty string if none
	private Couple<Observation,Integer> treasure;


	public MapAttribute() {
		this.state = "open";
		this.claimant = "";
		this.blocked = false;
		this.treasure = new Couple<Observation, Integer>(null, 0);
	}

	public MapAttribute(String state, String claimant, Boolean blocked, Couple<Observation, Integer> treasure) {
		this.state = state;
		this.claimant = claimant;
		this.blocked = Optional.ofNullable(blocked).orElse(false);
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

	public Couple<Observation, Integer> getTreasure() {
		return treasure;
	}

	public void setTreasure(Couple<Observation,Integer> treasure) {
		this.treasure = treasure;
	}

	public boolean isBlocked() {
		return blocked;
	}

	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}
}