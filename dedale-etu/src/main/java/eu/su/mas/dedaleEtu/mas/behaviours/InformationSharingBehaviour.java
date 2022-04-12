package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
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
			this.shareFirst(receiver);
			this.shareMeetingPoint(receiver);
			this.myAgent.getOtherAgents().get(receiver).setAlreadyMet(true);
		}
		//Map sharing
		this.shareMap(receiver);
	}
	
	//Depending on the type of protocol we receive, start the according receiving function
	private void receiveInfo(String sender) {
		while(true) { //Do while
			MessageTemplate msgTemplate = MessageTemplate.MatchSender(new AID(sender,AID.ISLOCALNAME));
			ACLMessage receiveMsg = this.waitForMessage(msgTemplate, 600);
			if(receiveMsg == null) {
				//System.out.println(this.myAgent.getLocalName() + " - I've stopped waiting for informations...");
				return;
			}
			//System.out.println(this.myAgent.getLocalName() + " - Received informations");
			String protocol = receiveMsg.getProtocol();
			switch(protocol) {
			case "FIRST-MEETING"://Basic informations like capacity and meeting point
				this.receiveFirst(sender,receiveMsg);
				break;
			case "MEETING-POINT":
				this.receiveMeetingPoint(sender,receiveMsg);
				break;
			case "SHARE-TOPO" : //map sharing
				this.receiveMap(sender,receiveMsg);
				break;
			case "INFOSHARE" : //Exit
				return;
			default :
				break;
			}
		}
	}
	
	private void shareFirst(String receiver) {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setProtocol("FIRST-MEETING");
		msg.setSender(this.myAgent.getAID());
		msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
		List<Couple<Observation, Integer>> bp = this.myAgent.getBackPackFreeSpace();
		String capa = "";
		for (Couple<Observation,Integer> c : bp) {
			capa = capa + c.getLeft().getName() + ":" + c.getRight().toString() + ";";
		}
		msg.setContent(capa);
		this.myAgent.sendMessage(msg);
	}
	
	private void receiveFirst(String sender,ACLMessage msgReceived) {
		if (msgReceived != null) {
			String content = msgReceived.getContent();
			String[] capas = content.split(";");
			int capaGold = Integer.parseInt((capas[0].split(":"))[1]);
			int capaDiam = Integer.parseInt((capas[1].split(":"))[1]);
			this.myAgent.getOtherAgents().get(sender).setCapaGold(capaGold);
			this.myAgent.getOtherAgents().get(sender).setCapaDiamond(capaDiam);
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
		//System.out.println(this.myAgent.getLocalName() + " - I send " + msg.toString());
		this.myAgent.sendMessage(msg);
		
	}
	
	private void receiveMap(String sender,ACLMessage msgReceived) {
		if (msgReceived != null) {
			SerializableSimpleGraph<String, MapAttribute> sgreceived = null;
			try {
				sgreceived = (SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			this.myAgent.getMyMap().mergeMap(sgreceived,this.myAgent,msgReceived.getSender().getLocalName());
			}
			this.myAgent.getPathToFollow().clear();
			this.myAgent.setNextPosition(null);
	}

	@Override
	public void action() {
		//Reset number of moves we did without sharing
		getDataStore().put("movesWithoutSharing", 0);
		String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
		if (myPosition==null) {
			return; //exit this behaviour as I won't be able to do anything
		}
		ACLMessage sendMsg = null;
		ACLMessage receiveMsg = null;
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
				receiveMsg = this.messageTimeout(msgTemplate, 600);
				if (receiveMsg == null) {
					return; //exit the while loop as no one seems to be there
					}
				String receiver = receiveMsg.getSender().getLocalName();
				
				//Check if I have informations to communicate to it
				if(this.myAgent.getOtherAgents().get(receiver).hasInfoToShare()) {
					//Share these informations
					this.shareInfo(receiver);
				}
				//Inform that I sent all I had to send (Which can be nothing) and go into receiver mode
				sendMsg.setPerformative(ACLMessage.INFORM);
				sendMsg.setProtocol("INFOSHARE");
				sendMsg.setContent("I've got nothing more for you. Waiting for your informations.");
				sendMsg.clearAllReceiver();
				sendMsg.addReceiver(new AID(receiver,AID.ISLOCALNAME));
				//System.out.println(this.myAgent.getLocalName() + " - I send " + sendMsg.toString());
				((AbstractDedaleAgent) this.myAgent).sendMessage(sendMsg);
				this.receiveInfo(receiver);
			}
			
		}else { //is receiver
			this.msgReceived = (ACLMessage) this.getDataStore().get("received-message");
			String sender = this.msgReceived.getSender().getLocalName();
			sendMsg = new ACLMessage(ACLMessage.AGREE);
			sendMsg.setProtocol("INFOSHARE");
			sendMsg.setSender(this.myAgent.getAID());
			sendMsg.setContent("I'm "+this.myAgent.getLocalName() + " at " + myPosition + " and I'm ok to share informations, waiting for yours !");
			sendMsg.addReceiver(new AID(sender,AID.ISLOCALNAME));
			((AbstractDedaleAgent) this.myAgent).sendMessage(sendMsg);
			this.receiveInfo(sender);
			//Check if I have informations to communicate to it
			if(this.myAgent.getOtherAgents().get(sender).hasInfoToShare()) {
				this.shareInfo(sender);
			}
			sendMsg.setPerformative(ACLMessage.INFORM);
			sendMsg.setProtocol("INFOSHARE");
			sendMsg.setContent("I've got nothing for more you. Have a nice day !");
			sendMsg.clearAllReceiver();
			sendMsg.addReceiver(new AID(sender,AID.ISLOCALNAME));
			//System.out.println(this.myAgent.getLocalName() + " - I send " + sendMsg.toString());
			((AbstractDedaleAgent) this.myAgent).sendMessage(sendMsg);
		}
	}
}