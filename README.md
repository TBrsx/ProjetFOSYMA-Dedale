Our take/solution on the Dedale project, done during the "FOSYMA" teaching at Sorbonne University. We worked on the "Collect maximum amount of treasure" goal.

See below original readme taken from Dedale Project :

# Welcome to the Dedale project.

This project is developed at Sorbonne University, Paris, France. It is used in both research and teaching activities. Considering the later, during the FoSyMa course (from the French “Fondement des Systèmes Multi-Agents”) as a practical application of Multi-Agents Systems (MAS). 
It allows Master's students to obtain a first-hand experience of some of the nice (and sometimes difficult) characteristics that comes with agents and distributed systems :
 - distribution and asynchronism (system and communication),
 - autonomy, decision and coordination in uncertain and partially observable environments

**Your goal here is "simple", you have to conceive and implement the behaviours of a team of heterogeneous agents that have to explore an unknown environment and coordinate themselves to reach a given goal** :
 - To collect the maximum amount of treasure in a given time frame.
 - To hunt the Golem(s)
 - To patrol an area
 - To pick and deliver packages
 
while facing, or not, other teams.

This game is initally inspired by the famous "Hunt the Wumpus"  of [Gregory Yob](https://en.wikipedia.org/wiki/Gregory_Yob).

For more details, see Dedale's website : https://dedale.gitlab.io/

Protocole de résolution d'interlocking:
```mermaid
sequenceDiagram
A ->> B: query-if "Je te bloque?"
alt non
B -->> A: disconfirm
Note left of A: A attend un peu
else oui
B -->> A: confirm
Note over A,B: A et B calculent la distance du branchement le plus proche
A ->> B: inform "ma distance"
Note right of B: B choisi qui bouge
alt dA >= dB
B -->> A: inform "Je recule, voilà le prochain branchement"
Note over A,B: B recule, A avance
Note right of B: B est arrivé sur une des sorties
Note left of A: A est arrivé au branchement
A ->> B: inform "Je suis arrivé, tu peux repartir"
Note left of A: A bouge sur une sortie libre
Note right of B: B bouge sur le branchement
B -->> A: inform "Je pars"
Note left of A: A attends un peu et reprend son chemin
else dA < dB
B -->> A: inform "Tu recules"
A ->> B: inform "Je recule, voilà le prochai branchement"
Note over A,B: B avance, A recule
Note left of A: A est arrivé sur une des sorties
Note right of B: B est arrivé au branchement
B -->> A: inform "Je suis arrivé, tu peux repartir"
Note right of B: B bouge sur une sortie libre
Note left of A: A bouge sur le branchement
A ->> B: inform "Je pars"
Note right of B: B attends un peu et reprend son chemin
end
end
```
