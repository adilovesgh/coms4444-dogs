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
	private final String[] OTHER_TEAM_NAMES = {"papaya","two","three","zythum","Zyzzogeton"};
	private final Double CLONE_DISTANCE = Math.sqrt(1344);

	private List<String> clonesPresent = new ArrayList<>();
	private Map<String, String> teamsPresent = new HashMap<>();

	private boolean moving = true;
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

	//TODO:
	//1: calc destination we want to go to
	//2: move til we get to that destination

	//3: throwing algo
	//pt 1: (sort our dogs based on how much exercise they have, target ones & ignore other teams' dogs)
	//pt 2: direction that we through them
	//pt 3: distance (hardcoded 9m)

	//4: exit park


	//long term: minimize distance from each dog out
	////pipeline when dogs get back
	////mode where we're done: help other owners, ignore our dogs


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

			targetRow = 75.0;
			targetColumn = 75.0;

			return directive;
		}

		if(round == 6){
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
		simPrinter.println("WAITING ON " + myOwner.getNameAsString() + ": " + waitingDogs);
		if(waitingDogs.size() > 0){ 
			directive.instruction = Instruction.THROW_BALL;
			//directive.dogToPlayWith = getLeastTiredDog(waitingDogs);
			directive.dogToPlayWith = getLongestWaitingDog(waitingDogs);
			//simPrinter.println("THOWING FOR " + directive.dogToPlayWith.getOwner().getNameAsString() + "'s " + directive.dogToPlayWith.getBreed());

			//setThrowLocation(directive, myOwner, otherOwners);


			double randomAngle = Math.toRadians(random.nextDouble() * 360); 
		
			double ballRow = myOwner.getLocation().getRow() + 40.0 * Math.sin(1.6977);
			double ballColumn = myOwner.getLocation().getColumn() + 40.0 * Math.cos(1.6977);

			if (cloneOrder == 2) {
				ballRow = myOwner.getLocation().getRow() + 40.0 * Math.sin(4.83933);
				ballColumn = myOwner.getLocation().getColumn() + 40.0 * Math.cos(4.83933);
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

	private Dog getLeastTiredDog(List<Dog> allDogs) {
		Double timeLeft = 0.0;
		Dog mostTiredDog = null; 
		for (Dog dog : allDogs){
			if (dog.getExerciseTimeRemaining() > timeLeft){
				mostTiredDog = dog;
				timeLeft = dog.getExerciseTimeRemaining();
			}
		}
		simPrinter.println("DOG NEEDS: " + mostTiredDog.getExerciseTimeRemaining());
		return mostTiredDog;
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

	private ParkLocation setThrowLocation(Directive directive, Owner myOwner, List<Owner> otherOwners){
		directive.dogToPlayWith.getOwner().getLocation();
		return null;
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
			if(dog.isWaitingForItsOwner())
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