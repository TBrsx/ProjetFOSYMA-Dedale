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
			for (int i=0;i<10;i++) {
				this.myAgent.doWait(1000);
				while(!this.myAgent.moveTo(this.myAgent.getMyMap().getRandomPathFrom(this.myAgent.getCurrentPosition(), 1).get(1)));
			}
			
		}
		
	}

}
