package dogs.g5;

import java.util.*;
import javax.swing.text.ChangedCharSetException;
import dogs.sim.Directive;
import dogs.sim.Directive.Instruction;
import dogs.sim.Dictionary;
import dogs.sim.Dog;
import dogs.sim.Owner;
import dogs.sim.ParkLocation;
import dogs.sim.SimPrinter;


public class Player extends dogs.sim.Player {
	private final String OUR_TEAM_SIGNAL = "Zyzzogeton";
	private final String IN_POSITION_SIGNAL = "positron";
	private final String READY_THROW_SIGNAL = "throwster";
	private final String SEPARATE_DOGS_SIGNAL = "separate";
	private final String[] OTHER_TEAM_NAMES = {"papaya","two","three","zythum","Zyzzogeton"};
	private final String AVAILABLE_SIGNAL = "available";

	private final Double DOG_SPACING 		= 1.6;
	private final Double THROW_DISTANCE 		= 40.0;
	private final Double LABRADOR_THROW_DISTANCE 	= THROW_DISTANCE - 2;
	private final Double POODLE_THROW_DISTANCE 	= THROW_DISTANCE;// - DOG_SPACING;
	private final Double SPANIEL_THROW_DISTANCE 	= THROW_DISTANCE;// - DOG_SPACING * 2; 
	private final Double TERRIER_THROW_DISTANCE 	= THROW_DISTANCE;// - DOG_SPACING * 3;
	private final Double CLONE_DISTANCE 		= Math.sqrt(Math.pow(THROW_DISTANCE, 2) - Math.pow(DOG_SPACING * 4, 2));
	private final Double LABRADOR_OFFSET_ANGLE	= 0.0; // Math.atan(DOG_SPACING * 4/CLONE_DISTANCE);
	private final Double POODLE_OFFSET_ANGLE 	= Math.atan(DOG_SPACING * 3/CLONE_DISTANCE);
	private final Double SPANIEL_OFFSET_ANGLE 	= Math.atan(DOG_SPACING * 2/CLONE_DISTANCE);
	private final Double TERRIER_OFFSET_ANGLE 	= Math.atan(DOG_SPACING * 1/CLONE_DISTANCE);

	private State state = State.FIRST_ROUND;

	private Integer cloneOrder = 0;

	private Double targetRow = 0.0;
	private Double targetColumn = 0.0;

	private boolean threeCollabEngaged = false;
	private boolean completedExercise = false;

	private List<String> clonesPresent = new ArrayList<>();		//List of simNames
	private List<String> throwingPartners = new ArrayList<>();	//List of who we will throw to
	private List<String> currentPartners = new ArrayList<>();	//Who we are currently throwing to

	private Map<String, String> teamsPresent = new HashMap<>();	// SimName, Team Name

	private Map<String, List<String>> signalLog = new HashMap<>();	// Signal, list of SimNames

	private Map<Integer, HashMap<String, String>> conversationHistory = new HashMap<>();

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
		Directive directive = new Directive();
		List<Dog> waitingDogs = getWaitingDogs(myOwner, otherOwners);
		//System.out.println("Current owner: " + myOwner.getNameAsString());
		//System.out.println(clonesPresent);

		switch (state){
			case FIRST_ROUND:
				directive.signalWord = OUR_TEAM_SIGNAL;
				directive.instruction = Instruction.CALL_SIGNAL;
				state = State.FIRST_MOVE;
				break;

			case FIRST_MOVE:
				buildPlayerLists(myOwner, otherOwners);
				setTargetLocation();
				state = State.MOVING;

			case MOVING:
				if(notAtTarget(myOwner)) {
					setMoveLocation(myOwner, directive);
				} else {
					state = State.JUST_ARRIVED;
					stopMoving(directive);
				}
				break;

			case JUST_ARRIVED:
				directive.instruction = Instruction.CALL_SIGNAL;
				if (cloneOrder == -1 && threeCollabPossible()){
					directive.signalWord = SEPARATE_DOGS_SIGNAL;
					break;
				}
				else {
					checkClonePartnersStatus(State.SELF_THROWING);
				}
				directive.signalWord = IN_POSITION_SIGNAL;
				break;

			case PAIR_THROWING:
				if (threeCollabEngaged && threeReady())
					state = State.THREE_SEPARATE;
				else
					checkClonePartnersStatus(State.PAIR_THROWING);

				if(waitingDogs.size() > 0)
					setThrowLocation(directive, waitingDogs, myOwner, pickReceivingClone(otherOwners));
				
				if (myOwner.allExerciseCompleted() == true && completedExercise == false){
					state = State.COMPLETED;
				}

				break;

			case THREE_SEPARATE:
				if(waitingDogs.size() > 0){
					setThrowLocation(directive, waitingDogs, myOwner, pickReceivingClone(otherOwners));
					separateDogs(directive, getThreeOwner(otherOwners));
				}
				break;

			case SELF_THROWING:
				checkClonePartnersStatus(State.SELF_THROWING);
				if(waitingDogs.size() > 0)
					throwToSelf(directive, myOwner, waitingDogs);
				break;
			
			case COMPLETED:
				completedExercise = true;
				directive.signalWord = AVAILABLE_SIGNAL;
				directive.instruction = Instruction.CALL_SIGNAL;
				state = State.PAIR_THROWING;
		}

		saveConversation(round, myOwner, otherOwners);

		return directive;
	}

	private void buildPlayerLists(Owner myOwner, List<Owner> otherOwners){
		for (Owner owner : otherOwners){
			if (Arrays.asList(OTHER_TEAM_NAMES).contains(owner.getCurrentSignal()))
				teamsPresent.put(owner.getNameAsString(),owner.getCurrentSignal());
			if (owner.getCurrentSignal().equals(OUR_TEAM_SIGNAL))
				clonesPresent.add(owner.getNameAsString());
		}

		if (clonesPresent.size() > 0){
			setCloneOrder(myOwner);
			//System.out.println("CLONE ORDER: " + cloneOrder);
			if (cloneOrder % 10 == 1)
				throwingPartners.add(clonesPresent.get(cloneOrder));
			else if (cloneOrder % 2 == 0){
				if (cloneOrder < (clonesPresent.size()))
					throwingPartners.add(clonesPresent.get(cloneOrder));
				if (cloneOrder + 1 < (clonesPresent.size()))
					throwingPartners.add(clonesPresent.get(cloneOrder + 1));
				if (cloneOrder - 2 >= 0)
					throwingPartners.add(clonesPresent.get(cloneOrder - 2));
			}
			else if (cloneOrder % 2 == 1)
				if (cloneOrder - 3 >= 0)
					throwingPartners.add(clonesPresent.get(cloneOrder - 3));
		}
		Collections.sort(throwingPartners);
	}

	private void setTargetLocation(){
		int index = 0;
		List<Double> init = new ArrayList<>();
		init.add(7.0);
		init.add(7.0);
		List<List<Double>> locations = generateLocations(init, clonesPresent.size());

		if (clonesPresent.size() > 0)
			index = cloneOrder - 1;

		List<Double> targetLoc = locations.get(index);
		targetRow = targetLoc.get(0);
		targetColumn = targetLoc.get(1);
	}

	private List<List<Double>> generateLocations(List<Double> initialPoint, int numNodes) {
		List<List<Double>> locations = new ArrayList<>();

		//column 1 base point
		Double row = initialPoint.get(0);
		Double column = initialPoint.get(1);

		//column 2 base point
		Double rowBase2 = row + CLONE_DISTANCE/2;
		Double colBase2 = column + (CLONE_DISTANCE/2)*Math.tan(Math.toRadians(60.0));
		
		//add first and second base points to output array
		List<Double> secondRowBase = new ArrayList<>();
		secondRowBase.add(rowBase2);
		secondRowBase.add(colBase2);

		//add first two points
		locations.add(initialPoint);
		locations.add(secondRowBase);

		boolean extraLevels = false;
		int level = 3;
		int currInd = 0;

		//generate the other n-2 points
		if (numNodes > 0){
			for(int i = 0; i < numNodes - 2; i++) {
				//every other one should be first column
				double rowOffset = (Math.floorDiv(i,2)+1)*CLONE_DISTANCE;

				//check if past the size of the field
				if(i%2 == 0 && (row + rowOffset) >= 200) {
					extraLevels = true;
				}

				if(i%2 == 1 && (rowBase2 + rowOffset) >= 200) {
					extraLevels = true;
				}

				List<Double> nextPoint = new ArrayList<>();

				//if not, add normally
				if(!extraLevels) {
					if(i%2 == 0) {
						nextPoint.add(row + rowOffset);
						nextPoint.add(column);
					}
					else {
						nextPoint.add(rowBase2 + rowOffset);
						nextPoint.add(colBase2);
					}
					locations.add(nextPoint);
				}
				//if so, add to the current extra row
				else {
					//double newOffset = currInd*CLONE_DISTANCE;
					double newRowOffset = (Math.floorDiv(currInd,2))*CLONE_DISTANCE;
					//double newRow = newOffset + row;
					double newRow;
					double newCol;
					double colOffset = (level-1)*(CLONE_DISTANCE/2)*Math.tan(Math.toRadians(60.0));
					//double newCol = colOffset + column;

					if(currInd%2 == 0) {
						newRow = row + newRowOffset;
						newCol = column + colOffset;
					}
					else {
						newRow = rowBase2 + newRowOffset;
						newCol = colBase2 + colOffset;
					}

					//transition to new row
					if(newRow >= 200) {
						currInd = 0;
						level += 2;
						i--;
					}
					else {
						nextPoint.add(newRow);
						nextPoint.add(newCol);

						locations.add(nextPoint);
						currInd++;
					}
				}
			}
		}

		return locations;
	}

	private boolean notAtTarget(Owner myOwner){
		return (myOwner.getLocation().getColumn() < targetColumn || myOwner.getLocation().getRow() < targetRow);
	}

	private void setMoveLocation(Owner myOwner, Directive directive){
		double rowDelta = myOwner.getLocation().getRow() - targetRow;
		double colDelta = myOwner.getLocation().getColumn() - targetColumn;
		double angle = Math.atan(rowDelta/colDelta);
		double scaledRow = 4.99 * Math.sin(angle);
		double scaledCol = 4.99 * Math.cos(angle);

		directive.parkLocation.setRow(myOwner.getLocation().getRow() + scaledRow);
		directive.parkLocation.setColumn(myOwner.getLocation().getColumn() + scaledCol);
		directive.instruction = Instruction.MOVE;
	}

	private boolean threeCollabPossible(){
		if (teamsPresent.values().contains("three") && !threeCollabEngaged){
			threeCollabEngaged = true;
			return true;
		}
		return false;
	}

	private boolean threeReady(){
		if (signalLog.get(SEPARATE_DOGS_SIGNAL).contains("Frank"))
			return true;
		return false; 
	}

	private Owner getThreeOwner(List<Owner> otherOwners){
		for (Owner owner : otherOwners)
			if (owner.getNameAsString().equals(signalLog.get(SEPARATE_DOGS_SIGNAL).get(0)))
				return owner;
		return pickReceivingClone(otherOwners);
	}

	private void stopMoving(Directive directive){
		directive.parkLocation.setRow(targetRow);
		directive.parkLocation.setColumn(targetColumn);
		directive.instruction = Instruction.MOVE;
	}

	private Dog chooseDog(List<Dog> allDogs){
		Double timeLeft = 0.0;
		Double waitingTime = 30.0;
		Dog leastTiredDog = null;
		Dog longestWaitingDog = null; 

		for (Dog dog : allDogs){
			if (dog.isWaitingForItsOwner()){
				if (dog.getExerciseTimeRemaining() > timeLeft){
					leastTiredDog = dog;
					timeLeft = dog.getExerciseTimeRemaining();
				}
			}
			else
				if (dog.getWaitingTimeRemaining() <= waitingTime){
					longestWaitingDog = dog;
					waitingTime = dog.getWaitingTimeRemaining();
				}
		}
		if (leastTiredDog != null)
			return leastTiredDog;
		
		return longestWaitingDog;
	}

	private void checkClonePartnersStatus(State currentState){
		for (String clone : signalLog.get(IN_POSITION_SIGNAL)){
			if (throwingPartners.contains(clone)){
				throwingPartners.remove(clone);
				if (!currentPartners.isEmpty())
					currentPartners.clear();
				currentPartners.add(clone);
				state = State.PAIR_THROWING;
				return;
			}
		}
		state = currentState;
	}

	private boolean throwingPartnerSignaled(String signal){
		for (String clone : signalLog.get(signal)){
			if (throwingPartners.contains(clone)){
				return true;
			}
		}
		return false;
	}

	private boolean isClone(Owner owner){
		for (String clone : clonesPresent){
			if (owner.getNameAsString().equals(clone))
				return true;
		}
		return false;
	}

	private Owner pickReceivingClone(List<Owner> otherOwners){
		//System.out.println("Herreee");
		//System.out.println(currentPartners);
		for (Owner otherOwner : otherOwners){
			if(currentPartners.contains(otherOwner.getNameAsString()))
				return otherOwner;
		}
		return null;
	}

	private void setThrowLocation(Directive directive, List<Dog> waitingDogs, Owner myOwner, Owner targetOwner){
		double ballRow = 0.0;
		double ballColumn = 0.0;
		double angle = getAngle(myOwner, targetOwner);
		directive.instruction = Instruction.THROW_BALL;
		directive.dogToPlayWith = chooseDog(waitingDogs);
		
		switch (directive.dogToPlayWith.getBreed()){

			case LABRADOR:
				ballRow = myOwner.getLocation().getRow() + LABRADOR_THROW_DISTANCE * Math.sin(angle - LABRADOR_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + LABRADOR_THROW_DISTANCE * Math.cos(angle - LABRADOR_OFFSET_ANGLE);
				break;

			case POODLE:
				ballRow = myOwner.getLocation().getRow() + POODLE_THROW_DISTANCE * Math.sin(angle - POODLE_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + POODLE_THROW_DISTANCE * Math.cos(angle - POODLE_OFFSET_ANGLE);
				break;

			case SPANIEL:
				ballRow = myOwner.getLocation().getRow() + SPANIEL_THROW_DISTANCE * Math.sin(angle - SPANIEL_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + SPANIEL_THROW_DISTANCE * Math.cos(angle - SPANIEL_OFFSET_ANGLE);
				break;

			case TERRIER:
				ballRow = myOwner.getLocation().getRow() + TERRIER_THROW_DISTANCE * Math.sin(angle - TERRIER_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + TERRIER_THROW_DISTANCE * Math.cos(angle - TERRIER_OFFSET_ANGLE);
				break;
		}

		if(ballRow < 0.0)
			ballRow = 0.0;
		if(ballRow > ParkLocation.PARK_SIZE - 1)
			ballRow = ParkLocation.PARK_SIZE - 1;
		if(ballColumn < 0.0)
			ballColumn = 0.0;
		if(ballColumn > ParkLocation.PARK_SIZE - 1)
			ballColumn = ParkLocation.PARK_SIZE - 1;

		directive.parkLocation = new ParkLocation(ballRow, ballColumn);
	}

	private void separateDogs(Directive directive, Owner threeOwner){
		switch (directive.dogToPlayWith.getBreed()){
			case SPANIEL:
			case TERRIER:
				directive.parkLocation = threeOwner.getLocation();
				break;
			default:
				return;
		}
	}

	private void throwToSelf(Directive directive, Owner myOwner, List<Dog> waitingDogs){
		double ballRow = 0.0;
		double ballColumn = 0.0;

		directive.instruction = Instruction.THROW_BALL;
		directive.dogToPlayWith = chooseDog(waitingDogs);
		
		switch (directive.dogToPlayWith.getBreed()){
			case LABRADOR:
				ballRow = myOwner.getLocation().getRow() + LABRADOR_THROW_DISTANCE * Math.sin(0 - LABRADOR_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + LABRADOR_THROW_DISTANCE * Math.cos(0 - LABRADOR_OFFSET_ANGLE);
				break;

			case POODLE:
				ballRow = myOwner.getLocation().getRow() + POODLE_THROW_DISTANCE * Math.sin(0 - POODLE_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + POODLE_THROW_DISTANCE * Math.cos(0 - POODLE_OFFSET_ANGLE);
				break;

			case SPANIEL:
				ballRow = myOwner.getLocation().getRow() + SPANIEL_THROW_DISTANCE * Math.sin(0 - SPANIEL_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + SPANIEL_THROW_DISTANCE * Math.cos(0 - SPANIEL_OFFSET_ANGLE);
				break;

			case TERRIER:
				ballRow = myOwner.getLocation().getRow() + TERRIER_THROW_DISTANCE * Math.sin(0 - TERRIER_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + TERRIER_THROW_DISTANCE * Math.cos(0 - TERRIER_OFFSET_ANGLE);
				break;
		}

		if(ballRow < 0.0)
			ballRow = 0.0;
		if(ballRow > ParkLocation.PARK_SIZE - 1)
			ballRow = ParkLocation.PARK_SIZE - 1;
		if(ballColumn < 0.0)
			ballColumn = 0.0;
		if(ballColumn > ParkLocation.PARK_SIZE - 1)
			ballColumn = ParkLocation.PARK_SIZE - 1;

		directive.parkLocation = new ParkLocation(ballRow, ballColumn);
	}

	private Double getAngle(Owner myOwner, Owner targetOwner){
		int quad = getQuadrant(myOwner, targetOwner);
		Double myOwnerX = myOwner.getLocation().getColumn();
		Double myOwnerY = myOwner.getLocation().getRow();
		Double targetX = targetOwner.getLocation().getColumn();
		Double targetY = targetOwner.getLocation().getRow();
		Double deltaX = Math.abs(myOwnerX - targetX);
		Double deltaY = Math.abs(myOwnerY - targetY);
		Double angle = 0.0;

		if (quad == 1){
			angle = Math.atan(deltaY / deltaX);
		}
		else if (quad == 2){
			angle = Math.atan(deltaX / deltaY) + (Math.PI / 2);
		}
		else if (quad == 3){
			angle = Math.atan(deltaY / deltaX) + Math.PI;
		}
		else if (quad == 4){
			angle = Math.atan(deltaX / deltaY) + (3 * Math.PI / 2);
		}

		return angle;
	}

	private int getQuadrant(Owner myOwner, Owner targetOwner){
		Double myOwnerX = myOwner.getLocation().getColumn();
		Double myOwnerY = myOwner.getLocation().getRow();
		Double targetX = targetOwner.getLocation().getColumn();
		Double targetY = targetOwner.getLocation().getRow();
		Double deltaX = myOwnerX - targetX;
		Double deltaY = myOwnerY - targetY;
		int quad = 0;
		
		if (Double.compare(deltaX, 0.0) > 0){
			if (Double.compare(deltaY, 0.0) > 0){
				quad = 3;
			}
			else if (Double.compare(deltaY, 0.0) < 0){
				quad = 2;
			}
			else{
				quad = 3;
			}
		}
		else if (Double.compare(deltaX, 0.0) < 0){
			if (Double.compare(deltaY, 0.0) > 0){
				quad = 4;
			}
			else if (Double.compare(deltaY, 0.0) < 0){
				quad = 1;
			}
			else{
				quad = 1;
			}
		}
		else {
			if (Double.compare(deltaY, 0.0) > 0){
				quad = 4;
			}
			else if (Double.compare(deltaY, 0.0) < 0){
				quad = 2;
			}
			// Same coordinates
			else{
				quad = 1;
			}
		}

		return quad;
	}

	private boolean dogIsDone(Dog dog){
		if (dog.getExerciseTimeRemaining() == 0.0)
			return true;
		else 
			return false;
	}

	private boolean allDogsDone(List<Dog> allDogs){
		for (Dog dog : allDogs){
			if (dog.getExerciseTimeRemaining() > 0.0)
				return false;
		}
		return true;
	}

	private List<String> getOtherOwnersSignals(List<Owner> otherOwners) {
		List<String> otherOwnersSignals = new ArrayList<>();
		for(Owner otherOwner : otherOwners)
			if(!otherOwner.getCurrentSignal().equals("_"))
				otherOwnersSignals.add(otherOwner.getCurrentSignal());
		return otherOwnersSignals;
	}

	private void saveConversation(Integer round, Owner myOwner, List<Owner> otherOwners){
		conversationHistory.put(round, new HashMap<>());

		if (round  == 1){
			initSignalLog();
		}
		for(Owner owner : otherOwners){
			if (!owner.getCurrentSignal().equals("_")){
				if (teamsPresent.containsKey(owner.getNameAsString()))
					conversationHistory.get(round).put(owner.getNameAsString(), owner.getCurrentSignal());
				if (signalLog.get(owner.getCurrentSignal()) != null){
					simPrinter.println("OWNER: " + myOwner.getNameAsString());
					simPrinter.println("ADDING: " + owner.getNameAsString() + "'s SIGNAL: " + owner.getCurrentSignal());
					signalLog.get(owner.getCurrentSignal()).add(owner.getNameAsString());
				}
			}
		}
	}

	private void initSignalLog(){
		signalLog.put(OUR_TEAM_SIGNAL, new ArrayList<>());
		signalLog.put(IN_POSITION_SIGNAL, new ArrayList<>());
		signalLog.put(READY_THROW_SIGNAL, new ArrayList<>());
		signalLog.put(SEPARATE_DOGS_SIGNAL, new ArrayList<>());
	}

	private void setCloneOrder(Owner myOwner){
        	clonesPresent.add(myOwner.getNameAsString());
        	Collections.sort(clonesPresent);
        	simPrinter.println("CLONES PRESENT (CLONE ORDER): " + clonesPresent);
        	cloneOrder = clonesPresent.indexOf(myOwner.getNameAsString()) + 1;
	}

	private List<Dog> getWaitingDogs(Owner myOwner, List<Owner> otherOwners) {
		List<Dog> waitingDogs = new ArrayList<>();

		for(Dog dog : myOwner.getDogs()) 
			if(dog.isWaitingForItsOwner() && dog.getExerciseTimeRemaining() > 0.0)
				waitingDogs.add(dog);

		for(Owner otherOwner : otherOwners){
			if (clonesPresent.contains(otherOwner.getNameAsString())){
				for(Dog dog : otherOwner.getDogs()){
					if(dog.isWaitingForOwner(myOwner)){
						waitingDogs.add(dog);
					}
				}
			}
			else if(teamsPresent.containsKey(otherOwner.getNameAsString()) && myOwner.allExerciseCompleted()){
				for(Dog dog : otherOwner.getDogs()){
					if(dog.isWaitingForOwner(myOwner)){
						waitingDogs.add(dog);
					}
				}
			}
		}
		return waitingDogs;
	}

	private enum State {
		FIRST_ROUND,
		FIRST_MOVE,
		MOVING,
		JUST_ARRIVED,
		SELF_THROWING,
		PAIR_THROWING,
		THREE_SEPARATE,
		COMPLETED;
	}
}
