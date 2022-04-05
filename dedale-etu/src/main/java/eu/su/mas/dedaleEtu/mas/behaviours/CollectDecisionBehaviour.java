package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;

import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import jade.core.behaviours.OneShotBehaviour;

public class CollectDecisionBehaviour extends OneShotBehaviour{
	
	private static final long serialVersionUID = 3329007121557183780L;
	private static final int SHARE_THE_PLAN = 0;
	private static final int BEGIN_COLLECT = 1;
	private int returnCode;
	private boolean needsToShare = false;
	
	private ExploreCoopAgent myAgent;
	
	public CollectDecisionBehaviour(ExploreCoopAgent myagent) {
		super(myagent);
		this.myAgent = myagent;
	}
	
	public CollectDecisionBehaviour(ExploreCoopAgent myagent,boolean needsToShare) {
		this(myagent);
		this.needsToShare = true;
	}
	
	@Override
	public void action() {
		String decisionMaster = (String) this.getDataStore().get("decision-master");
		List<String> claimedNodes = this.myAgent.getMyMap().getClaimedNodes(this.myAgent.getLocalName());
		for (String n : claimedNodes) {
			MapAttribute mapAtt = this.myAgent.getMyMap().getMapAttributeFromNodeId(n);
			if (mapAtt.getTreasure().getLeft() != null) {
				mapAtt.setCollector(this.myAgent.getLocalName());
				this.myAgent.getMyMap().addNode(n, mapAtt);
			}
		}
		this.returnCode = BEGIN_COLLECT;
	}
	
	public int onEnd() {
		return this.returnCode;
	}

}
