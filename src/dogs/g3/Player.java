package dogs.g3;

import java.util.*;

import dogs.sim.Directive;
import dogs.sim.Directive.Instruction;
import dogs.sim.Dictionary;
import dogs.sim.Dog;
import dogs.sim.Owner;
import dogs.sim.ParkLocation;
import dogs.sim.SimPrinter;


public class Player extends dogs.sim.Player {
	private List<Owner> otherOwners;
	private List<Dog> myDogs;
	private Integer round;
	private Owner myOwner;
	private double listeningProbability = 0.2;
	
    /**
     * Player constructor
     *
     * @param rounds      	   number of rounds
     * @param numDogsPerOwner  number of dogs per owner
     * @param numOwners	  	   number of owners
     * @param seed        	   random seed
     * @param simPrinter  	   simulation printer
     *
     */
	public Player(Integer rounds, Integer numDogsPerOwner, Integer numOwners, Integer seed, Random random, SimPrinter simPrinter) {
		super(rounds, numDogsPerOwner, numOwners, seed, random, simPrinter);
	}

    /**
     * Choose command/directive for next round
     *
     * @param round        current round
     * @param myOwner      my owner
     * @param otherOwners  all other owners in the park
     * @return             a directive for the owner's next move
     *
     */
    public Directive chooseDirective(Integer round, Owner myOwner, List<Owner> otherOwners) {
		this.round = round;
		this.myDogs = myOwner.getDogs();
		this.otherOwners = otherOwners;
		this.myOwner = myOwner;

		sortOwners();

    	Directive directive = new Directive();
    	
    	if(round <= 151) {
    		directive.instruction = Instruction.MOVE;
    		directive.parkLocation = new ParkLocation(myOwner.getLocation().getRow() + 2, myOwner.getLocation().getColumn() + 2);
    		return directive;
		}
		else if (round <= 271) {
			directive.instruction = Instruction.MOVE;
    		directive.parkLocation = new ParkLocation(myOwner.getLocation().getRow(), myOwner.getLocation().getColumn() + 2);
    		return directive;
		}
    	

		List<Dog> waitingDogs = getWaitingDogs(myOwner, otherOwners);

		List<Dog> notOwnedByMe = new LinkedList<Dog>();

		for (Dog dog: waitingDogs) {
			if (dog.getOwner() != this.myOwner) {
				notOwnedByMe.add(dog);
			}
		}


		List<Dog> sortedDogs = sortDogs(notOwnedByMe);
		directive.dogToPlayWith = sortedDogs.get(0);
		directive.instruction = Instruction.MOVE;
		Owner throwOwner = this.otherOwners.get(0);
		
		double ballRow = throwOwner.getLocation().getRow();
		double ballColumn = throwOwner.getLocation().getColumn();
		if(ballRow < 0.0)
			ballRow = 0.0;
		if(ballRow > ParkLocation.PARK_SIZE - 1)
			ballRow = ParkLocation.PARK_SIZE - 1;
		if(ballColumn < 0.0)
			ballColumn = 0.0;
		if(ballColumn > ParkLocation.PARK_SIZE - 1)
			ballColumn = ParkLocation.PARK_SIZE - 1;
		directive.parkLocation = new ParkLocation(ballRow, ballColumn);

		return directive;
	}
    
    private List<String> getOtherOwnersSignals(List<Owner> otherOwners) {
    	List<String> otherOwnersSignals = new ArrayList<>();
    	for(Owner otherOwner : otherOwners)
    		if(!otherOwner.getCurrentSignal().equals("_"))
    			otherOwnersSignals.add(otherOwner.getCurrentSignal());
    	return otherOwnersSignals;
    }
    
    private List<Dog> getWaitingDogs(Owner myOwner, List<Owner> otherOwners) {
    	List<Dog> waitingDogs = new ArrayList<>();
    	for(Dog dog : myOwner.getDogs()) {
    		if(dog.isWaitingForItsOwner())
    			waitingDogs.add(dog);
    	}
    	for(Owner otherOwner : otherOwners) {
    		for(Dog dog : otherOwner.getDogs()) {
    			if(dog.isWaitingForOwner(myOwner))
    				waitingDogs.add(dog);
    		}
    	}
    	return waitingDogs;
	}

	/* SORTING */
	private List<Dog> sortDogs(List<Dog> dogList){
		Collections.sort(dogList, new Comparator<Dog>() {
			@Override
			public int compare(Dog u1, Dog u2) {
			  return compareDogs(u1, u2);
			}
		  });
		return dogList;
	}

	private int compareDogs(Dog u1, Dog u2){
		return u2.getExerciseTimeRemaining().compareTo(u1.getExerciseTimeRemaining());
	}

	private void sortOwners(){
		Collections.sort(this.otherOwners, new Comparator<Owner>() {
			@Override
			public int compare(Owner u1, Owner u2) {
			  return compareOwners(u1, u2);
			}
		  });
	}

	private int compareOwners(Owner u1, Owner u2){
		Double distanceToOwner1 = Math.pow(u1.getLocation().getRow() - this.myOwner.getLocation().getRow(), 2);
		distanceToOwner1 += Math.pow(u1.getLocation().getColumn() - this.myOwner.getLocation().getColumn(), 2);

		Double distanceToOwner2 = Math.pow(u2.getLocation().getRow() - this.myOwner.getLocation().getRow(), 2);
		distanceToOwner2 += Math.pow(u2.getLocation().getColumn() - this.myOwner.getLocation().getColumn(), 2);

		return distanceToOwner2.compareTo(distanceToOwner1);
	}

}