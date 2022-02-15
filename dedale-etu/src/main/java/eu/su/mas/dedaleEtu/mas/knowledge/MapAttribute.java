package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

//This class is where we store attribute of nodes

public class MapAttribute implements Serializable {	

	private static final long serialVersionUID = 3664792526110659066L;



	private String state; //open,closed,agent
	 private String claimant; //name of agent that claimed it, empty string if none
	 
	 public MapAttribute(String state,String claimant){
		 this.state = state;
		 this.claimant = claimant;
	 }
	 public MapAttribute(){
		 this.state = "open";
		 this.claimant = "";
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
	
}