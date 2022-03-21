package eu.su.mas.dedaleEtu.mas.behaviours;

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
		this.isReceiver = isReceiver;
	}
	
	private ACLMessage waitForMessage(MessageTemplate msgTemplate, int timer) {
		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		while (msgReceived == null) {
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
		return msgReceived;
	}
	
	private void shareInfo(String receiver) {
		//Currently it's easy we can only share maps
		this.shareMap(receiver);
	}
	
	//Depending on the type of protocol we receive, start the according receiving function
	private void receiveInfo(String sender) {
		
		MessageTemplate msgTemplate = MessageTemplate.MatchSender(new AID(sender,AID.ISLOCALNAME));
		ACLMessage treatedMsg = this.waitForMessage(msgTemplate, 2000);
		String protocol = treatedMsg.getProtocol();
		switch(protocol) {
		case "SHARE-TOPOLOGY" : //map sharing
			this.receiveMap(sender,treatedMsg);
			break;
		default :
			return; //exit function, the "INFOSHARE" protocol falls into this case as it means "no more info to share, your turn"
		}
	}

	private void shareMap(String receiver) {
		
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
		ACLMessage sendMsg = null;
		ACLMessage receiveMsg = null;
		MessageTemplate msgTemplate = null;
		
		if((!isReceiver)) { //Started this behavior as its own initiative, so I got priority on sending my stuff if someone can hear me
			sendMsg = new ACLMessage(ACLMessage.REQUEST);
			sendMsg.setSender(this.myAgent.getAID());
			sendMsg.setProtocol("HANDSHAKE");
			sendMsg.setContent("I'm "+this.myAgent.getLocalName() + " at " + myPosition + " is there a friend close to me ?");
			for (String agentName: this.myAgent.getListAgentNames()) {
				sendMsg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
			}
			
			((AbstractDedaleAgent) this.myAgent).sendMessage(sendMsg);
			
			msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INFOSHARE"),
					MessageTemplate.MatchPerformative(ACLMessage.AGREE));
			receiveMsg = this.messageTimeout(msgTemplate, 2000);
			if (receiveMsg == null) {
				return; //exit behaviour
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
				((AbstractDedaleAgent) this.myAgent).sendMessage(sendMsg);
				this.receiveInfo(receiver);
				
			}else {
				//Inform that I have nothing and go into receiver mode
				sendMsg.setPerformative(ACLMessage.INFORM);
				sendMsg.setProtocol("INFOSHARE");
				sendMsg.setContent("I've got nothing for you. Waiting for your informations.");
				sendMsg.clearAllReceiver();
				sendMsg.addReceiver(new AID(receiver,AID.ISLOCALNAME));
				((AbstractDedaleAgent) this.myAgent).sendMessage(sendMsg);
				this.receiveInfo(receiver);
				//After that, behaviour is over
			}
			
		}else { //is receiver
			this.msgReceived = (ACLMessage) this.getDataStore().get("received-message");
			String sender = this.msgReceived.getSender().getLocalName();
			sendMsg = new ACLMessage(ACLMessage.AGREE);
			sendMsg.setProtocol("INFOSAHRE");
			sendMsg.setSender(this.myAgent.getAID());
			sendMsg.setContent("I'm "+this.myAgent.getLocalName() + " at " + myPosition + " and I'm ok to share informations, waiting for yours !");
			sendMsg.addReceiver(new AID(sender,AID.ISLOCALNAME));
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
			((AbstractDedaleAgent) this.myAgent).sendMessage(sendMsg);
		}
	}
}