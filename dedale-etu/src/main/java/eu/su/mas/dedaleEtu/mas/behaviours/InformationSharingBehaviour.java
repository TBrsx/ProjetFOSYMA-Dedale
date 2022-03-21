package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


public class InformationSharingBehaviour extends OneShotBehaviour {

	/**
	 * 
	 */
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
			waitCounts++;
			this.myAgent.doWait(timer);
			System.out.println(this.myAgent.getLocalName() + " - Waiting for message... ");
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
			System.out.println(this.myAgent.getLocalName() + " - Timed out while waiting in infoshare behaviour");
		}
		return msgReceived;
	}
	
	private void shareInfo(String receiver) {
		System.out.println(this.myAgent.getLocalName() + " - Sending informations");
		//Currently it's easy we can only share maps
		this.shareMap(receiver);
	}
	
	//Depending on the type of protocol we receive, start the according receiving function
	private void receiveInfo(String sender) {
		MessageTemplate msgTemplate = MessageTemplate.MatchSender(new AID(sender,AID.ISLOCALNAME));
		ACLMessage receiveMsg = this.waitForMessage(msgTemplate, 600);
		if(receiveMsg == null) {
			System.out.println(this.myAgent.getLocalName() + " - I've stopped waiting for informations...");
			return;
		}
		System.out.println(this.myAgent.getLocalName() + " - Received informations");
		String protocol = receiveMsg.getProtocol();
		switch(protocol) {
		case "SHARE-TOPO" : //map sharing
			this.receiveMap(sender,receiveMsg);
			break;
		default :
			return; //exit function, the "INFOSHARE" protocol falls into this case as it means "no more info to share, your turn"
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
		((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
		
	}
	
	private void receiveMap(String sender,ACLMessage msgReceived) {
		if (msgReceived != null) {
			SerializableSimpleGraph<String, MapAttribute> sgreceived = null;
			try {
				sgreceived = (SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			this.myAgent.getMyMap().mergeMap(sgreceived);
			}
	}

	@Override
	public void action() {
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
				receiveMsg = this.messageTimeout(msgTemplate, 1500);
				if (receiveMsg == null) {
					return; //exit the while loop as no one seems to be there
					}
				String receiver = receiveMsg.getSender().getLocalName();
				
				//Check if I have informations to communicate to it
				if(this.myAgent.getOtherAgents().get(receiver).hasInfoToShare()) {
					this.shareInfo(receiver);
					//Inform that I sent all I had to send and go into receiver mode
					sendMsg.setPerformative(ACLMessage.INFORM);
					sendMsg.setProtocol("INFOSHARE");
					sendMsg.setContent("I've got nothing more for you. Waiting for your informations.");
					sendMsg.clearAllReceiver();
					sendMsg.addReceiver(new AID(receiver,AID.ISLOCALNAME));
					//System.out.println(this.myAgent.getLocalName() + " - I send " + sendMsg.toString());
					((AbstractDedaleAgent) this.myAgent).sendMessage(sendMsg);
					this.receiveInfo(receiver);
					
				}else {
					//Inform that I have nothing and go into receiver mode
					sendMsg.setPerformative(ACLMessage.INFORM);
					sendMsg.setProtocol("INFOSHARE");
					sendMsg.setContent("I've got nothing for you. Waiting for your informations.");
					sendMsg.clearAllReceiver();
					sendMsg.addReceiver(new AID(receiver,AID.ISLOCALNAME));
					//System.out.println(this.myAgent.getLocalName() + " - I send " + sendMsg.toString());
					((AbstractDedaleAgent) this.myAgent).sendMessage(sendMsg);
					this.receiveInfo(receiver);
				}
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