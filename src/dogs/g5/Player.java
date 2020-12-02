package dogs.g5;

import java.util.*;

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



			//System.out.println("calculating");
			List<Double> init = new ArrayList<>();
			init.add(2.0);
			init.add(2.0);
			List<List<Double>> locations = generateLocations(init, clonesPresent.size());
			//System.out.println("List:" + locations);

			List<Double> targetLoc = locations.get(cloneOrder);

			targetRow = targetLoc.get(0);
			targetColumn = targetLoc.get(1);

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
				setThrowLocation(directive, waitingDogs, myOwner);				
			}
		}

		saveConversation(round, otherOwners);

		return directive;
	}

	private List<List<Double>> generateLocations(List<Double> initialPoint, int numNodes) {
		List<List<Double>> locations = new ArrayList<>();

		//column 1 base point
		Double row = initialPoint.get(0);
		Double column = initialPoint.get(1);

		//column 2 base point
		Double rowBase2 = row + CLONE_DISTANCE/2;
		Double colBase2 = column + (CLONE_DISTANCE/2)*Math.tan(Math.toRadians(60.0));
		//System.out.println("tan is " + Math.tan(Math.toRadians(60.0)));
		
		//add first and second base points to output array
		List<Double> secondRowBase = new ArrayList<>();
		secondRowBase.add(rowBase2);
		secondRowBase.add(colBase2);

		//add first two points
		locations.add(initialPoint);
		locations.add(secondRowBase);

		//generate the other n-2 points
		for(int i = 0; i < numNodes - 2; i++) {
			//every other one should be first column
			double rowOffset = (Math.floorDiv(i,2)+1)*CLONE_DISTANCE;

			List<Double> nextPoint = new ArrayList<>();

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

		return locations;
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

	private void setThrowLocation(Directive directive, List<Dog> waitingDogs, Owner myOwner){

		double ballRow = 0.0;
		double ballColumn = 0.0;
		double randomAngle = Math.toRadians(random.nextDouble() * 360);

		directive.instruction = Instruction.THROW_BALL;
		directive.dogToPlayWith = chooseDog(waitingDogs);
		
		
		
		switch (directive.dogToPlayWith.getBreed()){

			case LABRADOR:
				ballRow = myOwner.getLocation().getRow() + LABRADOR_THROW_DISTANCE * Math.sin(C1_OFFSET + LABRADOR_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + LABRADOR_THROW_DISTANCE * Math.cos(C1_OFFSET + LABRADOR_OFFSET_ANGLE);

				if (cloneOrder == 2) {
					ballRow = myOwner.getLocation().getRow() + LABRADOR_THROW_DISTANCE * Math.sin(C2_OFFSET + LABRADOR_OFFSET_ANGLE);
					ballColumn = myOwner.getLocation().getColumn() + LABRADOR_THROW_DISTANCE * Math.cos(C2_OFFSET + LABRADOR_OFFSET_ANGLE);
				}
				break;

			case POODLE:
				ballRow = myOwner.getLocation().getRow() + POODLE_THROW_DISTANCE * Math.sin(C1_OFFSET + POODLE_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + POODLE_THROW_DISTANCE * Math.cos(C1_OFFSET + POODLE_OFFSET_ANGLE);

				if (cloneOrder == 2) {
					ballRow = myOwner.getLocation().getRow() + POODLE_THROW_DISTANCE * Math.sin(C2_OFFSET + POODLE_OFFSET_ANGLE);
					ballColumn = myOwner.getLocation().getColumn() + POODLE_THROW_DISTANCE * Math.cos(C2_OFFSET + POODLE_OFFSET_ANGLE);
				}
				break;

			case SPANIEL:
				ballRow = myOwner.getLocation().getRow() + SPANIEL_THROW_DISTANCE * Math.sin(C1_OFFSET + SPANIEL_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + SPANIEL_THROW_DISTANCE * Math.cos(C1_OFFSET + SPANIEL_OFFSET_ANGLE);

				if (cloneOrder == 2) {
					ballRow = myOwner.getLocation().getRow() + SPANIEL_THROW_DISTANCE * Math.sin(C2_OFFSET + SPANIEL_OFFSET_ANGLE);
					ballColumn = myOwner.getLocation().getColumn() + SPANIEL_THROW_DISTANCE * Math.cos(C2_OFFSET + SPANIEL_OFFSET_ANGLE);
				}
				break;

			case TERRIER:
				ballRow = myOwner.getLocation().getRow() + TERRIER_THROW_DISTANCE * Math.sin(C1_OFFSET + TERRIER_OFFSET_ANGLE);
				ballColumn = myOwner.getLocation().getColumn() + TERRIER_THROW_DISTANCE * Math.cos(C1_OFFSET + TERRIER_OFFSET_ANGLE);

				if (cloneOrder == 2) {
					ballRow = myOwner.getLocation().getRow() + TERRIER_THROW_DISTANCE * Math.sin(C2_OFFSET + TERRIER_OFFSET_ANGLE);
					ballColumn = myOwner.getLocation().getColumn() + TERRIER_THROW_DISTANCE * Math.cos(C2_OFFSET + TERRIER_OFFSET_ANGLE);
				}
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