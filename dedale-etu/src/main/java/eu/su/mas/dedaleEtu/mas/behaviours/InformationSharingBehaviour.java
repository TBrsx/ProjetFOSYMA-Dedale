package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.CollectPlan;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.OtherAgent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;



public class InformationSharingBehaviour extends OneShotBehaviour {

	
	private static final long serialVersionUID = -2058134622078521998L;
	private ExploreCoopAgent myAgent;
	private boolean isReceiver;
	private ACLMessage msgReceived;
	
	public InformationSharingBehaviour(final ExploreCoopAgent myagent,boolean isReceiver) {
		super(myagent);
		this.myAgent = myagent;
		this.isReceiver = isReceiver;
	}
	
	//Try to receive an answer multiple times, with a pause between each try
	private ACLMessage waitForMessage(MessageTemplate msgTemplate, int timer) {
		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		int waitCounts = 0;
		while (msgReceived == null && waitCounts <5) {
			if (waitCounts == 0){
				//System.out.println(this.myAgent.getLocalName() + " - Waiting for message... ");

			}
			waitCounts++;
			this.myAgent.doWait(timer);
			msgReceived = this.myAgent.receive(msgTemplate);
		}
		return msgReceived;
	}
	
	//Wait before looking for answer
	private ACLMessage messageTimeout(MessageTemplate msgTemplate, int timer) {
		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		if (msgReceived == null) {
			this.myAgent.doWait(timer);
			msgReceived = this.myAgent.receive(msgTemplate);
		}
		if (msgReceived == null && this.isReceiver) {
			//System.out.println(this.myAgent.getLocalName() + " - Timed out while waiting in infoshare behaviour");
		}
		return msgReceived;
	}
	
	private void shareInfo(String receiver) {
		//System.out.println(this.myAgent.getLocalName() + " - Sending informations");
		
		//Basic informations like capacity and meeting point
		if(!this.myAgent.getOtherAgents().get(receiver).isAlreadyMet()) {
			this.shareMeetingPoint(receiver);
			this.myAgent.getOtherAgents().get(receiver).setAlreadyMet(true);
		}
		//Map sharing
		if(this.myAgent.getOtherAgents().get(receiver).getNodesToTransfer().size()>0) {
			this.shareMap(receiver);
		}
		if(this.myAgent.getCurrentPlan() != null && ((String) this.getDataStore().get("decision-master")).equalsIgnoreCase(this.myAgent.getLocalName())) {
			this.sharePlan(receiver);
		}
		
	}
	
	//Depending on the type of protocol we receive, start the according receiving function
	private void receiveInfo(String sender) {
		while(true) { //Do while
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.not(MessageTemplate.MatchPerformative(ACLMessage.AGREE)), 
					MessageTemplate.MatchSender(new AID(sender,AID.ISLOCALNAME)));
			ACLMessage receiveMsg = this.waitForMessage(msgTemplate, 200);
			if(receiveMsg == null) {
				//System.out.println(this.myAgent.getLocalName() + " - I've stopped waiting for informations...");
				return;
			}
			//System.out.println(this.myAgent.getLocalName() + " - Received informations");
			String protocol = receiveMsg.getProtocol();
			switch(protocol) {
			case "MEETING-POINT": //Meeting point
				this.receiveMeetingPoint(sender,receiveMsg);
				break;
			case "SHARE-TOPO" : //Map
				this.receiveMap(sender,receiveMsg);
				break;
			case "SHARE-PLAN" : //Plan
				this.receivePlan(sender,receiveMsg);
				break;
			case "INFOSHARE" : //Exit
				return;
			default :
				break;
			}
		}
	}
	
	
	private void shareMeetingPoint(String receiver) {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setProtocol("MEETING-POINT");
		msg.setSender(this.myAgent.getAID());
		msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
		String meeting = "";
		Iterator<OtherAgent> it = this.myAgent.getOtherAgents().values().iterator();
		while(meeting.isEmpty() && it.hasNext()) {
			meeting = it.next().getMeetingPoint();
		}
		if (meeting.isEmpty()) {
			meeting = this.myAgent.getCurrentPosition();
		}
		msg.setContent(meeting);
		this.myAgent.getOtherAgents().get(receiver).setMeetingPoint(meeting);
		this.myAgent.sendMessage(msg);
		this.myAgent.setMeetingPoint(meeting);
	}
	
	private void receiveMeetingPoint(String sender,ACLMessage msgReceived) {
		if (msgReceived != null) {
			this.myAgent.getOtherAgents().get(sender).setMeetingPoint(msgReceived.getContent());
			this.myAgent.setMeetingPoint(msgReceived.getContent());
		}
	}

	private void shareMap(String receiver) {
		
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setProtocol("SHARE-TOPO");
		msg.setSender(this.myAgent.getAID());
		msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
		SerializableSimpleGraph<String, MapAttribute> sg = this.myAgent.getOtherAgents().get(receiver).serializeInformations();
		try {
			msg.setContentObject(sg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.myAgent.sendMessage(msg);
		
	}
	
	private void receiveMap(String sender,ACLMessage msgReceived) {

		if (msgReceived != null && this.myAgent.getCurrentPlan() == null) {
			SerializableSimpleGraph<String, MapAttribute> sgreceived = null;
			try {
				sgreceived = (SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			this.myAgent.getMyMap().mergeMap(sgreceived,this.myAgent,msgReceived.getSender().getLocalName());
			}
		if (this.myAgent.getPathToFollow() != null) {
			this.myAgent.getPathToFollow().clear();
			this.myAgent.setNextPosition("");
		}else {
			this.myAgent.setPathToFollow(new LinkedList<String>());
		}
	}
	
	private void sharePlan(String receiver) {
		//Send the name of the plan
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.setSender(this.myAgent.getAID());
		msg.setProtocol("SHARE-PLAN");
		msg.setContent(this.myAgent.getCurrentPlan().getName());
		msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
		this.myAgent.sendMessage(msg);
		
		//Receive his answer
		
		
		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("SHARE-PLAN"),
				MessageTemplate.and(MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REFUSE),MessageTemplate.MatchPerformative(ACLMessage.AGREE)),MessageTemplate.MatchSender(new AID(receiver,AID.ISLOCALNAME))));
		ACLMessage msgReceived = this.waitForMessage(msgTemplate, 200);
		if(msgReceived == null) {
			return;
		}
		
		if(msgReceived.getPerformative() == ACLMessage.REFUSE) {
			if(msgReceived.getContent().equalsIgnoreCase("We got the same plan !")) {
				return;
			}
		}else {
			//Send the part of the plan the agent needs to know
			msg = new ACLMessage(ACLMessage.INFORM);
			msg.setProtocol("SHARE-PLAN");
			msg.setSender(this.myAgent.getAID());
			msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
			try {
				msg.setContentObject(this.myAgent.getCurrentPlan().partOfPlan(receiver));
				this.myAgent.sendMessage(msg);
				msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("SHARE-PLAN"),
						MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),MessageTemplate.MatchSender(new AID(receiver,AID.ISLOCALNAME))));
				if(this.messageTimeout(msgTemplate,3000) != null) {
					//Update list of experts
					LinkedList<String> experts = (LinkedList<String>) this.getDataStore().get("awareOfPlan");
					experts.add(receiver);
					this.getDataStore().put("awareOfPlan",experts);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void receivePlan(String sender, ACLMessage msgReceived) {
		//If we got the same plan I don't need it
		if(this.myAgent.getCurrentPlan()!=null) {
			ACLMessage msg = new ACLMessage(ACLMessage.REFUSE);
			msg.setProtocol("SHARE-PLAN");
			msg.setSender(this.myAgent.getAID());
			msg.addReceiver(msgReceived.getSender());
			if (msgReceived.getContent().equalsIgnoreCase(this.myAgent.getCurrentPlan().getName())){
				msg.setContent("We got the same plan !");
				this.myAgent.sendMessage(msg);
				return;
			}
		}
				
		ACLMessage msg = new ACLMessage(ACLMessage.AGREE);
		msg.setProtocol("SHARE-PLAN");
		msg.setSender(this.myAgent.getAID());
		msg.addReceiver(msgReceived.getSender());
		this.myAgent.sendMessage(msg);
		msg=null;
		msgReceived = null;
		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("SHARE-PLAN"),
				MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchSender(new AID(sender,AID.ISLOCALNAME))));
		//Wait for next message
		msgReceived = this.waitForMessage(msgTemplate, 200);
		if(msgReceived != null) {
			try {
				this.myAgent.setCurrentPlan((CollectPlan) msgReceived.getContentObject());
				msg = new ACLMessage(ACLMessage.CONFIRM);
				msg.setProtocol("SHARE-PLAN");
				msg.setSender(this.myAgent.getAID());
				msg.addReceiver(msgReceived.getSender());
				msg.setContent("I received your plan, everything is fine !");
				this.myAgent.sendMessage(msg);
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public void askForCapacities(String sender) {
		ArrayList<String> agentsWithUnknownCapa = new ArrayList<String>();
		Iterator<Map.Entry<String, OtherAgent>> entries = this.myAgent.getOtherAgents().entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry<String, OtherAgent> entry = entries.next();
			OtherAgent agent =  entry.getValue();
			if(!agent.isKnownCapa()) {
				agentsWithUnknownCapa.add(agent.getName());
			}
		}
		
		ACLMessage sendMsg = new ACLMessage(ACLMessage.AGREE);
		sendMsg.setProtocol("INFOSHARE");
		sendMsg.setSender(this.myAgent.getAID());
		try {
			sendMsg.setContentObject(agentsWithUnknownCapa);
		} catch (IOException e) {
			e.printStackTrace();
		}
		sendMsg.addReceiver(new AID(sender,AID.ISLOCALNAME));
		((AbstractDedaleAgent) this.myAgent).sendMessage(sendMsg);
	}
	
	public void sendCapacities(String receiver,HashMap<String,Couple<Integer,Integer>> capaAgentsToSend) {
		ACLMessage sendMsg = new ACLMessage(ACLMessage.INFORM);
		sendMsg.setProtocol("CAPACITIES");
		sendMsg.setSender(this.myAgent.getAID());
		sendMsg.addReceiver(new AID(receiver,AID.ISLOCALNAME));
		try {
			sendMsg.setContentObject(capaAgentsToSend);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.myAgent.sendMessage(sendMsg);
	}
	
	public void receiveCapacities(String sender) {
		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchSender(new AID(sender,AID.ISLOCALNAME)), MessageTemplate.MatchProtocol("CAPACITIES"));
		this.msgReceived = this.waitForMessage(msgTemplate, 200);
		if(this.msgReceived != null) {
			try {
				HashMap<String,Couple<Integer,Integer>> capaAgentsReceived = (HashMap<String, Couple<Integer, Integer>>) this.msgReceived.getContentObject();
				for (Entry<String, Couple<Integer, Integer>> entry : capaAgentsReceived.entrySet()) {
				    String key = entry.getKey();
				    Couple<Integer, Integer> value = entry.getValue();
				    this.myAgent.getOtherAgents().get(key).setCapaDiamond(value.getLeft());
				    this.myAgent.getOtherAgents().get(key).setCapaGold(value.getRight());
				    this.myAgent.getOtherAgents().get(key).setKnownCapa(true);
				}
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void action() {
		//System.out.println(this.myAgent.getLocalName() + " - started behavior " + this.getBehaviourName());

		//Reset number of moves we did without sharing
		getDataStore().put("movesWithoutSharing", 0);
		String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
		if (myPosition==null) {
			return; //exit this behaviour as I won't be able to do anything
		}
		ACLMessage sendMsg = null;
		MessageTemplate msgTemplate = null;
		
		if((!isReceiver)) { //Started this behavior as its own initiative, so I got priority on sending my stuff if someone can hear me
			//Try to contact someone
			
			sendMsg = new ACLMessage(ACLMessage.REQUEST);
			sendMsg.setSender(this.myAgent.getAID());
			sendMsg.setProtocol("INFOSHARE");
			sendMsg.setContent("I'm "+this.myAgent.getLocalName() + " at " + myPosition + " is there a friend close to me ?");
			for (String agentName: this.myAgent.getListAgentNames()) {
				sendMsg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
			}
			((AbstractDedaleAgent) this.myAgent).sendMessage(sendMsg);
			
			//Try to receive an answer, and if I do, converse. We actually do this multiple time, in case multiple agents answered.
			//Multiple time = as long as we got an "agree" performative with the protocol "INFOSHARE", 
			while(true) { //Do while, ugly
				msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INFOSHARE"),
						MessageTemplate.MatchPerformative(ACLMessage.AGREE));
				this.msgReceived = this.messageTimeout(msgTemplate, ((int) this.getDataStore().get("waitingTime"))*3);
				if (this.msgReceived == null) {
					return; //exit the while loop as no one seems to be there
					}
				
				//First message I receive contains the list of agents which my contact doesn't knows capacity of
				String receiver = this.msgReceived.getSender().getLocalName();
				HashMap<String,Couple<Integer,Integer>> capaAgentsToSend = new HashMap<String,Couple<Integer,Integer>>();
				try {
					ArrayList<String> recAgentsWithUnknownCapa = (ArrayList<String>) this.msgReceived.getContentObject() ;
					for(String agentName : recAgentsWithUnknownCapa) {
						if(!agentName.equalsIgnoreCase(this.myAgent.getLocalName())) {
							if(this.myAgent.getOtherAgents().get(agentName).isKnownCapa()) {
								OtherAgent agent = this.myAgent.getOtherAgents().get(agentName);
								capaAgentsToSend.put(agentName, new Couple<Integer,Integer>(agent.getCapaDiamond(),agent.getCapaGold()));
							}
						}else {
							List<Couple<Observation, Integer>> bp = this.myAgent.getBackPackFreeSpace();
							int diamCapa = 0;
							int goldCapa = 0;
							for(Couple<Observation,Integer> c : bp) {
								if(c.getLeft() == Observation.DIAMOND) {
									diamCapa = c.getRight();
								}
								if(c.getLeft() == Observation.GOLD) {
									goldCapa = c.getRight();
								}
							}
							capaAgentsToSend.put(this.myAgent.getLocalName(), new Couple<Integer,Integer>(diamCapa, goldCapa));
						}
					}
					this.sendCapacities(receiver, capaAgentsToSend);
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
				
				
				//Check if I have other facultative informations to communicate to it
				if(this.myAgent.getOtherAgents().get(receiver).hasInfoToShare(this.myAgent)) {
					//Share these informations
					this.shareInfo(receiver);
				}
				//Inform that I sent all I had to send (Which can be nothing) and go into receiver mode
				sendMsg.setPerformative(ACLMessage.INFORM);
				sendMsg.setProtocol("INFOSHARE");
				sendMsg.setContent("I've got nothing more for you. I'm going to send you the list of the agents I don't know about.");
				sendMsg.clearAllReceiver();
				sendMsg.addReceiver(new AID(receiver,AID.ISLOCALNAME));
				//System.out.println(this.myAgent.getLocalName() + " - I send " + sendMsg.toString());
				((AbstractDedaleAgent) this.myAgent).sendMessage(sendMsg);
				this.askForCapacities(receiver);
				this.receiveCapacities(receiver);
				this.receiveInfo(receiver);
				
			}
			
		}else { //is receiver
			
			this.msgReceived = (ACLMessage) this.getDataStore().get("received-message");
			String sender = this.msgReceived.getSender().getLocalName();
			
			//As a confirmation, tell him the agent with the capacities I don't know about
			this.askForCapacities(sender);
			this.receiveCapacities(sender);
			
			this.receiveInfo(sender);
			
			msgTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.AGREE), MessageTemplate.MatchSender(new AID(sender,AID.ISLOCALNAME)));
			this.msgReceived = this.waitForMessage(msgTemplate, 200);
			if(this.msgReceived != null) {
				HashMap<String,Couple<Integer,Integer>> capaAgentsToSend = new HashMap<String,Couple<Integer,Integer>>();
				try {
					ArrayList<String> recAgentsWithUnknownCapa = (ArrayList<String>) this.msgReceived.getContentObject() ;
					for(String agentName : recAgentsWithUnknownCapa) {
						if(!agentName.equalsIgnoreCase(this.myAgent.getLocalName())) {
							if(this.myAgent.getOtherAgents().get(agentName).isKnownCapa()) {
								OtherAgent agent = this.myAgent.getOtherAgents().get(agentName);
								capaAgentsToSend.put(agentName, new Couple<Integer,Integer>(agent.getCapaDiamond(),agent.getCapaGold()));
							}
						}else {
							List<Couple<Observation, Integer>> bp = this.myAgent.getBackPackFreeSpace();
							int diamCapa = 0;
							int goldCapa = 0;
							for(Couple<Observation,Integer> c : bp) {
								if(c.getLeft() == Observation.DIAMOND) {
									diamCapa = c.getRight();
								}
								if(c.getLeft() == Observation.GOLD) {
									goldCapa = c.getRight();
								}
							}
							capaAgentsToSend.put(this.myAgent.getLocalName(), new Couple<Integer,Integer>(diamCapa, goldCapa));
						}
					}
					this.sendCapacities(sender, capaAgentsToSend);
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			}
			
			
			//Check if I have informations to communicate to it
			if(this.myAgent.getOtherAgents().get(sender).hasInfoToShare(this.myAgent)) {
				this.shareInfo(sender);
			}
			sendMsg = new ACLMessage(ACLMessage.INFORM);
			sendMsg.setProtocol("INFOSHARE");
			sendMsg.setContent("I've got nothing for more you. Have a nice day !");
			sendMsg.clearAllReceiver();
			sendMsg.addReceiver(new AID(sender,AID.ISLOCALNAME));
			sendMsg.setSender(new AID(this.myAgent.getLocalName(),AID.ISLOCALNAME));
			//System.out.println(this.myAgent.getLocalName() + " - I send " + sendMsg.toString());
			((AbstractDedaleAgent) this.myAgent).sendMessage(sendMsg);
			
		}
		
		//System.out.println(this.myAgent.getLocalName() + " - ended behavior " + this.getBehaviourName());

	}
}