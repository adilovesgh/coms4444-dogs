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

	private final String OUR_TEAM_NAME = "Zyzzogeton";
	private final String READY_THROW_SIGNAL = "throwster";

	private final String[] OTHER_TEAM_NAMES = {"papaya","two","three","zythum","Zyzzogeton"};
	
	private final Double DOG_SPACING 		= 1.6;
	private final Double THROW_DISTANCE 		= 40.0;
	private final Double LABRADOR_THROW_DISTANCE 	= THROW_DISTANCE;
	private final Double POODLE_THROW_DISTANCE 	= THROW_DISTANCE;// - DOG_SPACING;
	private final Double SPANIEL_THROW_DISTANCE 	= THROW_DISTANCE;// - DOG_SPACING * 2; 
	private final Double TERRIER_THROW_DISTANCE 	= THROW_DISTANCE;// - DOG_SPACING * 3;
	private final Double C1_OFFSET			= Math.PI /2;
	private final Double C2_OFFSET			= 3 * Math.PI / 2;
	private final Double C3_OFFSET			= Math.PI;
	private final Double C4_OFFSET			= 3 * Math.PI / 2;
	private final Double CLONE_DISTANCE 		= Math.sqrt(Math.pow(THROW_DISTANCE, 2) - Math.pow(DOG_SPACING * 4, 2));
	private final Double LABRADOR_OFFSET_ANGLE	= Math.atan(DOG_SPACING * 4/CLONE_DISTANCE);
	private final Double POODLE_OFFSET_ANGLE 	= Math.atan(DOG_SPACING * 3/CLONE_DISTANCE);
	private final Double SPANIEL_OFFSET_ANGLE 	= Math.atan(DOG_SPACING * 2/CLONE_DISTANCE);
	private final Double TERRIER_OFFSET_ANGLE 	= Math.atan(DOG_SPACING * 1/CLONE_DISTANCE);

	private List<String> clonesPresent = new ArrayList<>();
	private Map<String, String> teamsPresent = new HashMap<>();

	private boolean moving = true;
	private boolean throwing = false;
	private Integer cloneOrder = 0;
	private double listeningProbability = 0.2;
	private Double targetColumn = 0.0;
	private Double targetRow = 0.0;
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

		if(round == 1) {
			directive.signalWord = OUR_TEAM_NAME;
			directive.instruction = Instruction.CALL_SIGNAL;

			/*
			List<Double> initialLocation = findLocation();
			targetRow = initialLocation.get(0);
			targetColumn = initialLocation.get(1);
			*/

			targetRow = 2.0;
			targetColumn = 2.0;

			return directive;
		}

		else if(round == 6){
			for (Owner owner : otherOwners){
				if (Arrays.asList(OTHER_TEAM_NAMES).contains(owner.getCurrentSignal()))
					teamsPresent.put(owner.getNameAsString(),owner.getCurrentSignal());
				if (owner.getCurrentSignal().equals(OUR_TEAM_NAME))
					clonesPresent.add(owner.getNameAsString());
			}
			

			if (clonesPresent.size() > 0)
				setCloneOrder(myOwner);

			if (cloneOrder > 1)
				targetRow += CLONE_DISTANCE;

			simPrinter.println(myOwner.getNameAsString() + "'s list: " + clonesPresent);
			simPrinter.println(myOwner.getNameAsString() + "'s order: " + cloneOrder);
		}


		if(moving){

			if(myOwner.getLocation().getColumn() < targetColumn || myOwner.getLocation().getRow() < targetRow) {
				//simPrinter.println(myOwner.getLocation().toString());
				double rowDelta = myOwner.getLocation().getRow() - targetRow;
				double colDelta = myOwner.getLocation().getColumn() - targetColumn;
				double angle = Math.atan(rowDelta/colDelta);
				double scaledRow = 4.99 * Math.sin(angle);
				double scaledCol = 4.99 * Math.cos(angle);

				directive.parkLocation.setRow(myOwner.getLocation().getRow() + scaledRow);
				directive.parkLocation.setColumn(myOwner.getLocation().getColumn() + scaledCol);
				directive.instruction = Instruction.MOVE;

				return directive;

			} else {

				directive.parkLocation.setRow(targetRow);
				directive.parkLocation.setColumn(targetColumn);
				directive.instruction = Instruction.MOVE;
				moving = false;
				return directive;
			}
		}
		
		else if (!moving && !throwing){

			directive.instruction = Instruction.CALL_SIGNAL;
			directive.signalWord = READY_THROW_SIGNAL;
	
			for (Owner owner : otherOwners)
				if (owner.getCurrentSignal().equals(READY_THROW_SIGNAL))
					throwing = true;

			return directive;
		}

		else if (throwing){
			if(waitingDogs.size() > 0){
				// CHANGE PARAMETER
				setThrowLocation(directive, waitingDogs, myOwner, otherOwners.get(1));				
			}
		}

		saveConversation(round, otherOwners);

		return directive;
	}

	private List<Double> findLocation() {
		List<Double> coordinates = new ArrayList<>();

		double colVal = 100.0;
		double rowVal = 100.0;


		double varColOffset = random.nextInt(5000)/100.0 + 25;
		double varRowOffset = random.nextInt(5000)/100.0 + 25;

		if(random.nextInt(2) == 0) {
			colVal += varColOffset;

			if(random.nextInt(2) == 1) {
				rowVal += varRowOffset;
			}
			else {
				rowVal -= varRowOffset;
			}

		}
		else {
			rowVal += varRowOffset;

			if(random.nextInt(2) == 1) {
				colVal += varColOffset;
			}
			else {
				colVal -= varColOffset;
			}

		}

		coordinates.add(rowVal);
		coordinates.add(colVal);

		return coordinates;
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

	private Dog getLeastTiredDog(List<Dog> allDogs) {
		Double timeLeft = 0.0;
		Dog leastTiredDog = null; 
		for (Dog dog : allDogs){
			if (dog.getExerciseTimeRemaining() > timeLeft){
				leastTiredDog = dog;
				timeLeft = dog.getExerciseTimeRemaining();
			}
		}
		simPrinter.println("DOG NEEDS: " + leastTiredDog.getExerciseTimeRemaining());
		return leastTiredDog;
	}

	private Dog getLongestWaitingDog(List<Dog> allDogs) {
		Double waitingTime = 30.0;
		Dog longestWaitingDog = null;
		for (Dog dog : allDogs){
			if (dog.getWaitingTimeRemaining() <= waitingTime){
				longestWaitingDog = dog;
				waitingTime = dog.getWaitingTimeRemaining();
			}
		}
		return longestWaitingDog;
	}

	private boolean isClone(Owner owner){
		for (String clone : clonesPresent){
			if (owner.getNameAsString().equals(clone))
				return true;
		}
		return false;
	}

	private void setThrowLocation(Directive directive, List<Dog> waitingDogs, Owner myOwner, Owner targetOwner){
		double ballRow = 0.0;
		double ballColumn = 0.0;
		double angle = getAngle(myOwner, targetOwner);

		directive.instruction = Instruction.THROW_BALL;
		directive.dogToPlayWith = chooseDog(waitingDogs);
		
		switch (directive.dogToPlayWith.getBreed()){

			case LABRADOR:
				ballRow = myOwner.getLocation().getRow() + LABRADOR_THROW_DISTANCE * Math.sin(angle + LABRADOR_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + LABRADOR_THROW_DISTANCE * Math.cos(angle + LABRADOR_OFFSET_ANGLE);

				/*
				if (cloneOrder == 2) {
					ballRow = myOwner.getLocation().getRow() + LABRADOR_THROW_DISTANCE * Math.sin(C2_OFFSET + LABRADOR_OFFSET_ANGLE);
					ballColumn = myOwner.getLocation().getColumn() + LABRADOR_THROW_DISTANCE * Math.cos(C2_OFFSET + LABRADOR_OFFSET_ANGLE);
				}
				*/

				break;

			case POODLE:
				ballRow = myOwner.getLocation().getRow() + POODLE_THROW_DISTANCE * Math.sin(angle + POODLE_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + POODLE_THROW_DISTANCE * Math.cos(angle + POODLE_OFFSET_ANGLE);
				
				/*
				if (cloneOrder == 2) {
					ballRow = myOwner.getLocation().getRow() + POODLE_THROW_DISTANCE * Math.sin(C2_OFFSET + POODLE_OFFSET_ANGLE);
					ballColumn = myOwner.getLocation().getColumn() + POODLE_THROW_DISTANCE * Math.cos(C2_OFFSET + POODLE_OFFSET_ANGLE);
				}
				*/

				break;

			case SPANIEL:
				ballRow = myOwner.getLocation().getRow() + SPANIEL_THROW_DISTANCE * Math.sin(angle + SPANIEL_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + SPANIEL_THROW_DISTANCE * Math.cos(angle + SPANIEL_OFFSET_ANGLE);

				/*
				if (cloneOrder == 2) {
					ballRow = myOwner.getLocation().getRow() + SPANIEL_THROW_DISTANCE * Math.sin(C2_OFFSET + SPANIEL_OFFSET_ANGLE);
					ballColumn = myOwner.getLocation().getColumn() + SPANIEL_THROW_DISTANCE * Math.cos(C2_OFFSET + SPANIEL_OFFSET_ANGLE);
				}
				*/

				break;

			case TERRIER:
				ballRow = myOwner.getLocation().getRow() + TERRIER_THROW_DISTANCE * Math.sin(angle + TERRIER_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + TERRIER_THROW_DISTANCE * Math.cos(angle + TERRIER_OFFSET_ANGLE);
				
				/*
				if (cloneOrder == 2) {
					ballRow = myOwner.getLocation().getRow() + TERRIER_THROW_DISTANCE * Math.sin(C2_OFFSET + TERRIER_OFFSET_ANGLE);
					ballColumn = myOwner.getLocation().getColumn() + TERRIER_THROW_DISTANCE * Math.cos(C2_OFFSET + TERRIER_OFFSET_ANGLE);
				}
				*/

				break;

			default:
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
		else{
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

	//private boolean allDogsWaiting(List<Dog>))

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

	private void saveConversation(Integer round, List<Owner> otherOwners){
		conversationHistory.put(round, new HashMap<>());
		for(Owner owner : otherOwners)
			if (teamsPresent.containsKey(owner.getNameAsString()))
				conversationHistory.get(round).put(owner.getNameAsString(), owner.getCurrentSignal());
	}

	private void setCloneOrder(Owner myOwner){
        	clonesPresent.add(myOwner.getNameAsString());
        	Collections.sort(clonesPresent);
        	cloneOrder = clonesPresent.indexOf(myOwner.getNameAsString()) + 1;
	}

	private List<Dog> getWaitingDogs(Owner myOwner, List<Owner> otherOwners) {
		List<Dog> waitingDogs = new ArrayList<>();

		for(Dog dog : myOwner.getDogs()) 
			if(dog.isWaitingForItsOwner() && dog.getExerciseTimeRemaining() > 0.0)
				waitingDogs.add(dog);

		for(Owner otherOwner : otherOwners)
			for(Dog dog : otherOwner.getDogs()){
				if(dog.isWaitingForOwner(myOwner)){
					waitingDogs.add(dog);
				}
			}
	
		return waitingDogs;
	}
}