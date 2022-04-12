package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import org.graphstream.graph.Node;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class CollectDecisionBehaviour extends OneShotBehaviour{
	
	private static final long serialVersionUID = 3329007121557183780L;
	private static final int SHARE_THE_PLAN = 0;
	private static final int BEGIN_COLLECT = 1;
	private static final int INTERLOCKING = 2;
	private int returnCode;
	
	private ExploreCoopAgent myAgent;
	
	public CollectDecisionBehaviour(ExploreCoopAgent myagent) {
		super(myagent);
		this.myAgent = myagent;
	}
	
	private void createPlan() {
		((LinkedList<String>) this.getDataStore().get("awareOfPlan")).add(this.myAgent.getLocalName());
		((LinkedList<String>) this.getDataStore().get("awareOfPlan")).add("3rdAgent");
		((LinkedList<String>) this.getDataStore().get("awareOfPlan")).add("4thAgent");
		ArrayList<String> allNodes = (ArrayList<String>) this.myAgent.getMyMap().getAllNodes();
		this.myAgent.setCurrentPlan("ElPlan");
		
		for (String n : allNodes) {
			MapAttribute mapAtt = this.myAgent.getMyMap().getMapAttributeFromNodeId(n);
			if (mapAtt.getTreasure().getLeft() != null) {
				mapAtt.setCollector(mapAtt.getClaimant());
				Node added = this.myAgent.getMyMap().addNode(n, mapAtt);
				if (added != null) {
					this.myAgent.addNodeOtherAgents(added);
				}
			}
		}
		this.returnCode = SHARE_THE_PLAN;
	}
	private void sharePlan() {
	//Check if someone wants my plan
	ACLMessage msg = null;
	ACLMessage receivedMsg = null;	
	msg = new ACLMessage(ACLMessage.REQUEST);
	msg.setSender(this.myAgent.getAID());
	msg.setProtocol("PLANSHARE");
	msg.setContent(this.myAgent.getCurrentPlan());
	for (String agentName: this.myAgent.getListAgentNames()) {
		//TODO : ne pas envoyer aux experts
		msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
	}
	this.myAgent.sendMessage(msg);
	msg = null;
	boolean exitLoop = true;
	while(exitLoop) { //Do while, ugly
		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("PLANSHARE"),
				MessageTemplate.MatchPerformative(ACLMessage.AGREE));
		this.myAgent.doWait(1000);
		receivedMsg = this.myAgent.receive(msgTemplate);
		if (receivedMsg == null) {
			exitLoop = false; //exit the while loop as no one seems to want my plan
			}else {
				LinkedList<String> experts = (LinkedList<String>) this.getDataStore().get("awareOfPlan");
				String receiver = receivedMsg.getSender().getLocalName();
				experts.add(receiver);
				this.getDataStore().put("awareOfPlan",experts);
				msg = new ACLMessage(ACLMessage.INFORM);
				msg.setProtocol("PLANSHARE");
				msg.setSender(this.myAgent.getAID());
				msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
				SerializableSimpleGraph<String, MapAttribute> sg = this.myAgent.getMyMap().getSerializableGraph();
				try {
					msg.setContentObject(sg);
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.myAgent.sendMessage(msg);
				}
		
	}
		if (this.myAgent.getPathToFollow().isEmpty()){
			String meeting = this.myAgent.getMeetingPoint();
			if(this.myAgent.getCurrentPosition().equalsIgnoreCase(meeting)) {
				this.myAgent.setPathToFollow(this.myAgent.getMyMap().getRandomPathFrom(this.myAgent.getCurrentPosition(), 5));
			}else {
				this.myAgent.setPathToFollow(this.myAgent.getMyMap().getShortestPath(this.myAgent.getCurrentPosition(), meeting));			}
		}
		this.myAgent.setNextPosition(this.myAgent.getPathToFollow().removeFirst());
		if (!((AbstractDedaleAgent) this.myAgent).moveTo(this.myAgent.getNextPosition())) {
			this.myAgent.getPathToFollow().addFirst(this.myAgent.getNextPosition());
			this.returnCode = INTERLOCKING;
		}
		this.returnCode = SHARE_THE_PLAN;
		
		LinkedList<String> experts = (LinkedList<String>) this.getDataStore().get("awareOfPlan");
		if (experts.size() == this.myAgent.getOtherAgents().size()+1) {
			this.returnCode = BEGIN_COLLECT;
		}
	}
	private void searchPlan() {
		//Try to receive a plan
		ACLMessage msg = null;
		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("PLANSHARE"), MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		//If we got a proposition to share a plan
		if (msgReceived != null) {
			this.myAgent.setCurrentPlan(msgReceived.getContent());
			msg = new ACLMessage(ACLMessage.AGREE);
			msg.setProtocol("PLANSHARE");
			msg.setSender(this.myAgent.getAID());
			msg.addReceiver(msgReceived.getSender());
			this.myAgent.sendMessage(msg);
			msg=null;
			msgReceived = null;
			msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("PLANSHARE"), MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			//Wait for next message, no timeout, bad
			while(msgReceived == null) {
				msgReceived = this.myAgent.receive(msgTemplate);
			}
			//Replace my map, it is the plan I will now follow
			SerializableSimpleGraph<String, MapAttribute> sgreceived = null;
			try {
				sgreceived = (SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			this.myAgent.getMyMap().replaceMap(sgreceived);
			this.myAgent.getPathToFollow().clear();
			this.myAgent.setNextPosition(null);
			this.returnCode = BEGIN_COLLECT;
			return;
		}
		if (this.myAgent.getPathToFollow().isEmpty()){
			String meeting = this.myAgent.getOtherAgents().get((String) this.getDataStore().get("decision-master")).getMeetingPoint();
			if(this.myAgent.getCurrentPosition().equalsIgnoreCase(meeting)) {
				this.myAgent.setPathToFollow(this.myAgent.getMyMap().getRandomPathFrom(this.myAgent.getCurrentPosition(), 5));
			}else {
				this.myAgent.setPathToFollow(this.myAgent.getMyMap().getShortestPath(this.myAgent.getCurrentPosition(), meeting));
			}
		}
		this.myAgent.setNextPosition(this.myAgent.getPathToFollow().removeFirst());
		if (!((AbstractDedaleAgent) this.myAgent).moveTo(this.myAgent.getNextPosition())) {
			this.myAgent.getPathToFollow().addFirst(this.myAgent.getNextPosition());
			this.returnCode = INTERLOCKING;
		}
		this.returnCode = SHARE_THE_PLAN;
	}
	
	@Override
	public void action() {
		this.myAgent.doWait(500);
		String decisionMaster = (String) this.getDataStore().get("decision-master");
		if(decisionMaster.equalsIgnoreCase(this.myAgent.getLocalName())){
			if (this.myAgent.getCurrentPlan().isEmpty()){
				this.createPlan();
			}else {
				this.sharePlan();
			}
		}else {
			if (this.myAgent.getCurrentPlan().isEmpty()){
				this.searchPlan();
			}else {
				this.returnCode = BEGIN_COLLECT;
			}
		}
	}
	
	public int onEnd() {
		return this.returnCode;
	}

}
