package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import jade.core.behaviours.OneShotBehaviour;

public class JobDoneBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = -5610039770213140761L;
	
	private ExploreCoopAgent myAgent;

	public JobDoneBehaviour(ExploreCoopAgent ag) {
		super(ag);
		this.myAgent=ag;
		
	}

	@Override
	public void action() {
		while(true) {
			System.out.println(this.myAgent.getLocalName() + " - Job Done !");
			this.myAgent.doWait(3000);
			this.myAgent.moveTo(this.myAgent.getMyMap().getRandomPathFrom(this.myAgent.getCurrentPosition(), 1).get(0));
		}
		
	}

}
