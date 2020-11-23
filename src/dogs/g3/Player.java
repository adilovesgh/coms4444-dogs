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

		sortDogs();
		sortOwners();

    	Directive directive = new Directive();
    	
    	if(round <= 151) {
    		directive.instruction = Instruction.MOVE;
    		directive.parkLocation = new ParkLocation(myOwner.getLocation().getRow() + 2, myOwner.getLocation().getColumn() + 2);
    		return directive;
    	}
    	
    	List<Instruction> instructions = new ArrayList<>(Arrays.asList(Instruction.values()));
    	instructions.remove(Instruction.EXIT_PARK);

    	int instructionIndex = random.nextInt(instructions.size());
    	Instruction chosenInstruction = instructions.get(instructionIndex);
    	directive.instruction = chosenInstruction;
    	    	
    	double randomDistance, randomAngle;    	
    	switch(chosenInstruction) {
    	case THROW_BALL:
			List<Dog> waitingDogs = getWaitingDogs(myOwner, otherOwners);
			if(waitingDogs.size() > 0)
				directive.dogToPlayWith = waitingDogs.get(random.nextInt(waitingDogs.size()));
			else {
				directive.instruction = Instruction.NOTHING;
				break;
			}
    		
			randomDistance = 40.0;
			randomAngle = Math.toRadians(random.nextDouble() * 360);
			double ballRow = myOwner.getLocation().getRow() + randomDistance * Math.sin(randomAngle);
			double ballColumn = myOwner.getLocation().getColumn() + randomDistance * Math.cos(randomAngle);
			if(ballRow < 0.0)
				ballRow = 0.0;
			if(ballRow > ParkLocation.PARK_SIZE - 1)
				ballRow = ParkLocation.PARK_SIZE - 1;
			if(ballColumn < 0.0)
				ballColumn = 0.0;
			if(ballColumn > ParkLocation.PARK_SIZE - 1)
				ballColumn = ParkLocation.PARK_SIZE - 1;
			directive.parkLocation = new ParkLocation(ballRow, ballColumn);

			break;
    	case MOVE:
			randomDistance = random.nextDouble() * 5.0;
			randomAngle = Math.toRadians(random.nextDouble() * 360);
			double newOwnerRow = myOwner.getLocation().getRow() + randomDistance * Math.sin(randomAngle);
			double newOwnerColumn = myOwner.getLocation().getColumn() + randomDistance * Math.cos(randomAngle);
			if(newOwnerRow < 0.0)
				newOwnerRow = 0.0;
			if(newOwnerRow > ParkLocation.PARK_SIZE - 1)
				newOwnerRow = ParkLocation.PARK_SIZE - 1;
			if(newOwnerColumn < 0.0)
				newOwnerColumn = 0.0;
			if(newOwnerColumn > ParkLocation.PARK_SIZE - 1)
				newOwnerColumn = ParkLocation.PARK_SIZE - 1;
			directive.parkLocation = new ParkLocation(newOwnerRow, newOwnerColumn);			
    		break;
    	case CALL_SIGNAL:
    		List<String> otherOwnersPrevRoundWords = getOtherOwnersSignals(otherOwners);
    		
    		List<String> words = Dictionary.words;
        	words.remove("_");
        	if(random.nextInt(10) < 10 * listeningProbability && !otherOwnersPrevRoundWords.isEmpty())
        		directive.signalWord = otherOwnersPrevRoundWords.get(random.nextInt(otherOwnersPrevRoundWords.size()));
        	else
        		directive.signalWord = words.get(random.nextInt(words.size()));
    		break;
    	case NOTHING:
    		break;
		default:
			break;
    	}
    	    	
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
	private void sortDogs(){
		Collections.sort(this.myDogs, new Comparator<Dog>() {
			@Override
			public int compare(Dog u1, Dog u2) {
			  return compareDogs(u1, u2);
			}
		  });
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