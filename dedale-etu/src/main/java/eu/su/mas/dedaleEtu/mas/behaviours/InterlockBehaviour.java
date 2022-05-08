package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.LinkedList;
import java.util.List;

public class InterlockBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = -5610039770213100761L;
	private final ExploreCoopAgent myAgent;
	private String receiver;
	private boolean isReceiver;
	private boolean verboseMode = false;

	/**
	 * The agent and receiver debates about which agent as to move backward
	 *
	 * @param ag         The agent owning this behaviour
	 * @param isReceiver Chose the state of the agent
	 */
	public InterlockBehaviour(ExploreCoopAgent ag, boolean isReceiver) {
		super(ag);
		this.myAgent = ag;
		this.receiver = ""; // get this from the datastore
		this.isReceiver = isReceiver;
	}

	private ACLMessage waitForMessage(MessageTemplate msgTemplate) {
		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		int cpt = 0;
		while (msgReceived == null && (cpt < 10)) {
			this.myAgent.doWait(250);
			msgReceived = this.myAgent.receive(msgTemplate);
			cpt++;
		}
		if (cpt >= 10) {
			log("Timeout while waiting for a message.");
			return null;
		}
		return msgReceived;
	}

	private ACLMessage setupMessage(ACLMessage msg, String content, String protocol) {
		msg.setProtocol("INTERLOCKING" + protocol);
		msg.setSender(this.myAgent.getAID());
		msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
		if (content != null) {
			msg.setContent(content);
		}
		return msg;
	}

	private void continuePath() {
		this.myAgent.doWait((int) this.getDataStore().get("waitingTime"));

		// Receive the target from the other agent
		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-TARGET"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msg = waitForMessage(msgTemplate);
		if (msg == null) {
			failExit();
			return;
		}
		String target = msg.getContent();
		this.myAgent.setTargetPosition(target);
		// Continue the path
		int cpt = 0;
		while (!this.myAgent.getCurrentPosition().equals(target) && !this.myAgent.getPathToFollow().isEmpty() && cpt <= 5) {
			if (this.myAgent.moveTo(this.myAgent.getNextPosition())) {
				this.myAgent.setNextPosition(this.myAgent.getPathToFollow().removeFirst());
				cpt = 0;
			} else {
				cpt++;
			}
			this.myAgent.doWait((int) this.getDataStore().get("waitingTime"));
		}
		msg = setupMessage(new ACLMessage(ACLMessage.INFORM), null, "-FINISHED");
		log("Remaining path: " + this.myAgent.getPathToFollow());
		this.myAgent.send(msg);
	}

	private void backtrackPath(LinkedList<String> path) {
		this.myAgent.setTargetPosition(path.getLast());
		// Send the target to the other agent
		String target = path.size() > 1 ? path.get(path.size() - 2) : this.myAgent.getCurrentPosition();
		ACLMessage msg = setupMessage(new ACLMessage(ACLMessage.INFORM), target, "-TARGET");
		this.myAgent.sendMessage(msg);

		// Backtrack to the target
		log(" begins backtracking to " + path);
		moveTo(path);
		waitForMessage(MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-FINISHED"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM)));
	}

	private void defaultInterlocking() {
		// Send the agent's path to the receiver
		ACLMessage msg = setupMessage(new ACLMessage(ACLMessage.INFORM), this.myAgent.getPathToFollow().toString(), "-SHARE_PATH");
		this.myAgent.send(msg);
		log("Sending current path to " + receiver + ": " + this.myAgent.getPathToFollow().toString());

		// Wait for the receiver's path
		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-SHARE_PATH"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgReceived = waitForMessage(msgTemplate);

		// Parsing of the received path
		if (msgReceived == null) {
			failExit();
			return;
		}
		String msgContent = msgReceived.getContent().replaceAll("[\\[\\]]", "");
		List<String> otherAgentPath = List.of(msgContent.split(", *"));
		log("Received path from " + receiver + ": " + otherAgentPath);

		// Compute the shortest backtracking path
		LinkedList<String> path = this.myAgent.getMyMap()
				.getNearestFork(this.myAgent.getNextPosition(), this.myAgent.getCurrentPosition(), otherAgentPath);
		log("Backtracking path: " + path);

		int pathSize = path.size() == 0 ? Integer.MAX_VALUE : path.size();
		// Send the backtracking path to the receiver
		String content = Integer.toString(pathSize);
		log("Sending backtracking path length to " + receiver + ": " + content);
		msg = setupMessage(new ACLMessage(ACLMessage.INFORM), content, "-PATH_SIZE");
		this.myAgent.sendMessage(msg);

		// Wait for the receiver's backtracking path
		msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-PATH_SIZE"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		msgReceived = waitForMessage(msgTemplate);
		if (msgReceived == null) {
			failExit();
			return;
		}
		int otherAgentPathSize = Integer.parseInt(msgReceived.getContent());


		// Choose which agent has to move backward
		if (otherAgentPathSize < pathSize || (otherAgentPathSize <= pathSize && isReceiver)) {
			log("I'm the one to move forward.");
			continuePath();
		} else {
			log("I'm the one to move backward.");
			backtrackPath(path);
		}
	}

	private void alreadyInterlocked() {
		// Wait for the other agent target
		ACLMessage msgReceived = waitForMessage(MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-ALREADY_INTERLOCKED"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM)));
		if (msgReceived == null) {
			failExit();
			return;
		}
		String otherAgentTarget = msgReceived.getContent();
		log("Received target from " + receiver + ": " + otherAgentTarget);

		// Get the path to the other agent target
		LinkedList<String> path = this.myAgent.getMyMap().getShortestPath(this.myAgent.getCurrentPosition(), otherAgentTarget);
		log("Backtracking path: " + path);

		// Go to the other agent target
		moveTo(path);

		// Backtrack one more time
		String nextPos = this.myAgent.getMyMap().getNextNeighboringNodes(this.myAgent.getCurrentPosition(), this.myAgent.getPathToFollow().getFirst()).get(0);
		this.myAgent.getPathToFollow().addFirst(this.myAgent.getCurrentPosition());
		this.myAgent.moveTo(nextPos);
		int cpt = 0;
		while (!this.myAgent.getCurrentPosition().equals(nextPos) && cpt < 10) {
			this.myAgent.doWait((int) this.getDataStore().get("waitingTime"));
			this.myAgent.moveTo(nextPos);
			cpt++;
		}
		if (cpt >= 10) {
			MapAttribute mapAtt = this.myAgent.getMyMap().getMapAttributeFromNodeId(nextPos);
			mapAtt.setBlocked(true);
			this.myAgent.getMyMap().addNode(nextPos, mapAtt);
			this.myAgent.getPathToFollow().clear();
			this.myAgent.setNextPosition("");
		}
		log("Backtracked one more time, final position: " + this.myAgent.getCurrentPosition());
		this.myAgent.doWait(2000);
	}

	private void moveTo(LinkedList<String> path) {
		for (String pos : path) {
			this.myAgent.getPathToFollow().addFirst(this.myAgent.getCurrentPosition());
			this.myAgent.setNextPosition(pos);
			this.myAgent.moveTo(pos);
			boolean moveSuccess = this.myAgent.getCurrentPosition().equals(pos);
			log("Next backtracking target: " + pos + ", status: " + moveSuccess);
			int cpt = 0;
			while (!moveSuccess && cpt < 5) {
				this.myAgent.doWait((int) this.getDataStore().get("waitingTime"));
				this.myAgent.moveTo(pos);
				moveSuccess = this.myAgent.getCurrentPosition().equals(pos);
				cpt++;
			}
			if (cpt >= 5) {
				MapAttribute mapAtt = this.myAgent.getMyMap().getMapAttributeFromNodeId(pos);
				mapAtt.setBlocked(true);
				this.myAgent.getMyMap().addNode(pos, mapAtt);
				this.myAgent.getPathToFollow().clear();
				this.myAgent.setNextPosition("");
				return;
			}
			this.myAgent.doWait((int) this.getDataStore().get("waitingTime"));
		}
		log("Backtracking done, final position: " + this.myAgent.getCurrentPosition() + ", target: " + this.myAgent.getTargetPosition());
	}

	private void log(String msg) {
		if (verboseMode) {
			System.out.println(this.myAgent.getLocalName() + ": " + msg + ".");
		}
	}

	private void failExit() {
		if (this.myAgent.getNbInterlockingFailed() >= 3) {
			log("Too many failed interlocking, blocking the next node: " + this.myAgent.getNextPosition());
			this.myAgent.resetNbInterlockingFailed();
			MapAttribute mapAtt = this.myAgent.getMyMap().getMapAttributeFromNodeId(this.myAgent.getNextPosition());
			mapAtt.setBlocked(true);
			this.myAgent.getMyMap().addNode(this.myAgent.getNextPosition(), mapAtt);
			this.myAgent.getPathToFollow().clear();
			this.myAgent.setNextPosition("");
		} else {
			this.myAgent.increaseNbInterlockingFailed();
			log("Failed interlocking, trying again, nb failed: " + this.myAgent.getNbInterlockingFailed());
		}
	}

	@Override
	public void action() {
		System.out.println(this.myAgent.getLocalName() + " - started behavior " + this.getBehaviourName());

		// Check if the next position is blocked
		if (this.myAgent.getMyMap().getMapAttributeFromNodeId(this.myAgent.getNextPosition()).isBlocked()) {
			log("Next position is blocked, emptying the path");
			this.myAgent.getPathToFollow().clear();
			this.myAgent.setNextPosition("");
		}

		log("Starts interlocking behavior");
		log("Current position: " + this.myAgent.getCurrentPosition() + ", next position: " + this.myAgent.getNextPosition());
		boolean needsFix = false;
		ACLMessage bandage = this.myAgent.receive(MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-START"),
				MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF)));
		if (bandage != null) {
			this.isReceiver = true;
			needsFix = true;
		}
		if (isReceiver) {
			log("I'm the receiver");
			// Receive the interlocking data
			String[] data;
			if (needsFix) {
				log("needs fix");
				data = bandage.getContent().split(",");
				this.receiver = bandage.getSender().getLocalName();
			} else {
				log("doesn't need fix");
				ACLMessage msgReceived = (ACLMessage) this.getDataStore().get("received-message");
				if (msgReceived == null) {
					return;
				}
				data = msgReceived.getContent().split(",");
				this.receiver = msgReceived.getSender().getLocalName();
			}

			// Reply to the sender
			ACLMessage msg;
			boolean isInterlocking = data[0].equals(this.myAgent.getNextPosition()) && data[1].equals(this.myAgent.getCurrentPosition());
			if (isInterlocking) {
				msg = setupMessage(new ACLMessage(ACLMessage.CONFIRM), null, "-START");
				log("I'm interlocked with " + this.receiver);
			} else {
				msg = setupMessage(new ACLMessage(ACLMessage.DISCONFIRM), null, "-START");
				log("I'm not interlocked with " + this.receiver);
			}
			this.myAgent.sendMessage(msg);
			log("Sent its reply about interlocking");
			if (!isInterlocking) {
				log("Stopped interlocking behavior");
				this.myAgent.resetNbInterlockingFailed();
				return;
			}

			// Receive the other agent's query about the current interlocking
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-CURRENT"),
					MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
			waitForMessage(msgTemplate);
			log("Received the other agent's query about the current interlocking");
			if (this.myAgent.isInterlocking()) {
				log("I'm currently interlocked with an other agent, and my target is " + this.myAgent.getTargetPosition());
				msg = setupMessage(new ACLMessage(ACLMessage.CONFIRM), null, "-CURRENT");
				this.myAgent.sendMessage(msg);
				this.myAgent.doWait(500);
				msg = setupMessage(new ACLMessage(ACLMessage.INFORM), this.myAgent.getTargetPosition(), "-CURRENT");
				this.myAgent.sendMessage(msg);
				this.myAgent.setInterlocking(false);
				log("Sent its reply about the current interlocking");
				log("Stopped interlocking behavior");
				return;
			} else {
				log("I'm not currently interlocked with an other agent");
				this.myAgent.setInterlocking(true);
				msg = setupMessage(new ACLMessage(ACLMessage.DISCONFIRM), null, "-CURRENT");
				this.myAgent.sendMessage(msg);
			}
			defaultInterlocking();
		} else {
			// Send a message to ask if the agents around are interlocked with me
			log("I'm the sender");
			List<String> otherAgents = this.myAgent.getListAgentNames();
			ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
			msg.setProtocol("INTERLOCKING-START");
			msg.setSender(this.myAgent.getAID());
			for (String name : otherAgents) {
				msg.addReceiver(new AID(name, AID.ISLOCALNAME));
			}
			msg.setContent(this.myAgent.getCurrentPosition() + "," + this.myAgent.getNextPosition());
			this.myAgent.sendMessage(msg);
			log("Sent a message to ask if the agents around are interlocked with me");

			// Wait for the answer
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-START"),
					MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
							MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)));
			ACLMessage msgReceived = waitForMessage(msgTemplate);
			if (msgReceived == null) {
				log("Nobody answered");
				log("Stopped interlocking behavior");
				this.myAgent.doWait((int) this.getDataStore().get("waitingTime"));
				failExit();
				return;
			} else if (msgReceived.getPerformative() == ACLMessage.DISCONFIRM) {
				log("No one is interlocked with me");
				log("Stopped interlocking behavior");
				this.myAgent.doWait((int) this.getDataStore().get("waitingTime"));
				this.myAgent.resetNbInterlockingFailed();
				return;
			}
			this.myAgent.setInterlocking(true);
			this.receiver = msgReceived.getSender().getLocalName();
			log("Received a positive response from " + this.receiver + " about the interlocking");

			// Send a message to ask if the receiver is interlocked with someone else
			msg = setupMessage(new ACLMessage(ACLMessage.QUERY_IF), null, "-CURRENT");
			this.myAgent.send(msg);
			log("Sent a message to ask if " + this.receiver + " is interlocked with someone else");

			// Wait for the answer
			msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-CURRENT"),
					MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
							MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)));
			msgReceived = waitForMessage(msgTemplate);
			if (msgReceived == null) {
				failExit();
				return;
			}
			if (msgReceived.getPerformative() == ACLMessage.DISCONFIRM) {
				log("No one is currently interlocked with " + this.receiver);
				log("Entered default interlocking behavior");
				defaultInterlocking();
			} else {
				log("Someone is currently interlocked with " + this.receiver);
				log("Entered already interlocked behavior");
				alreadyInterlocked();
			}
		}
		this.myAgent.setInterlocking(false);
		this.myAgent.resetNbInterlockingFailed();
		log("Interlocking behavior ended, position is " + this.myAgent.getCurrentPosition() + ", next position is " + this.myAgent.getPathToFollow());
	}
	
	public int onEnd() {
		System.out.println(this.myAgent.getLocalName() + " - ended behavior " + this.getBehaviourName());
		return 0;
	}
}