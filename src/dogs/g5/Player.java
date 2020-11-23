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

	private double listeningProbability = 0.2;

	private Double targetColumn = 0.0;
	private Double targetRow = 0.0;
	
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
	
		//first round: say group number, calc location
		if(round == 1) {
			directive.signalWord = "five";

			//////todo 1
			List<Double> initialLocation = findLocation();
			targetRow = initialLocation.get(0);
			targetColumn = initialLocation.get(1);

			return directive;
		}

		

		//get to target location //////todo 2
		////calc slope, go towards it
		if(myOwner.getLocation().getColumn() < targetColumn || myOwner.getLocation().getRow() < targetRow) {
			simPrinter.println(myOwner.getLocation().toString());
			double rowDelta = myOwner.getLocation().getRow() - targetRow;
			double colDelta = myOwner.getLocation().getColumn() - targetColumn;

			double angle = Math.atan(rowDelta/colDelta);

			double scaledRow = 4.99*Math.sin(angle);
			double scaledCol = 4.99*Math.cos(angle);

			directive.instruction = Instruction.MOVE;
			directive.parkLocation = new ParkLocation(myOwner.getLocation().getRow() + scaledRow, myOwner.getLocation().getColumn() + scaledCol);
			return directive;
		}

		//pt4:
		//if done, leave the park

		//////////todo 3
		/////pt 1sort dogs, ignore dogs that aren't ours
		//pt 2: direction that we through them
		//pt 3: distance (hardcoded 9m)

		List<Dog> waitingDogs = getWaitingDogs(myOwner, otherOwners);

		if(waitingDogs.size() > 0 && ){ 
			directive.instruction = Instruction.THROW_BALL;
			simPrinter.println(waitingDogs);
			directive.dogToPlayWith = waitingDogs.get(random.nextInt(waitingDogs.size()));

			double randomAngle = Math.toRadians(random.nextDouble() * 360);
			double ballRow = myOwner.getLocation().getRow() + 40.0 * Math.sin(randomAngle);
			double ballColumn = myOwner.getLocation().getColumn() + 40.0 * Math.cos(randomAngle);
			simPrinter.println("THROW ROW: " + ballRow +  " THROW COLUMN: " + ballColumn);

			if(ballRow < 0.0)
				ballRow = 0.0;
			if(ballRow > ParkLocation.PARK_SIZE - 1)
				ballRow = ParkLocation.PARK_SIZE - 1;
			if(ballColumn < 0.0)
				ballColumn = 0.0;
			if(ballColumn > ParkLocation.PARK_SIZE - 1)
				ballColumn = ParkLocation.PARK_SIZE - 1;
			
			directive.parkLocation = new ParkLocation(ballRow, ballColumn);
		} else {
				
		}
    	    	
		return directive;
    }

    //go to a random place away from random player, edges, and center
    private List<Double> findLocation() {
        List<Double> coordinates = new ArrayList<>();

        double colVal = 100.0;
        double rowVal = 100.0;


        //random offset between 25 and 75
        double varColOffset = random.nextInt(5000)/100.0 + 25;
        double varRowOffset = random.nextInt(5000)/100.0 + 25;

        //randomly add or subtract offset
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

        simPrinter.println("row is " + rowVal);
        simPrinter.println("col is " + colVal);

        return coordinates;
    }

    private List<String> pickNeedyDog(List<Owner> otherOwners) {
    	List<String> otherOwnersSignals = new ArrayList<>();
    	for(Owner otherOwner : otherOwners)
    		if(!otherOwner.getCurrentSignal().equals("_"))
    			otherOwnersSignals.add(otherOwner.getCurrentSignal());
    	return otherOwnersSignals;
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
    	/*
    	for(Owner otherOwner : otherOwners) {
    		for(Dog dog : otherOwner.getDogs()) {
    			if(dog.isWaitingForOwner(myOwner))
    				waitingDogs.add(dog);
    		}
    	}*/
    	return waitingDogs;
    }
}