package dogs.g3;
import dogs.g3.PlayerGraph;
import java.util.*;

import dogs.sim.Directive;
import dogs.sim.Directive.Instruction;
import dogs.sim.Dictionary;
import dogs.sim.Dog;
import dogs.sim.Owner;
import dogs.sim.Owner.OwnerName;
import dogs.sim.ParkLocation;
import dogs.sim.SimPrinter;
import java.io.StringWriter;


public class Player extends dogs.sim.Player {
	private List<Owner> otherOwners;
	private List<Dog> myDogs;
	private Integer round;
	private Owner myOwner;
	private double listeningProbability = 0.2;
	HashMap<String, ParkLocation> positions; 
	HashMap<String, String> nextOwners;
	PlayerGraph graph; 
	
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

	private HashMap<String, ParkLocation> mapOwnerToParkLocationCircle(List<Owner> owners, ParkLocation center, int radius) {
		HashMap<String, ParkLocation> ownerToLocation = new HashMap<String, ParkLocation>();
		for (int i = 0; i < owners.size(); i++) {
			Owner current = owners.get(i);
			double x = center.getRow()  + radius * Math.cos((2 * Math.PI * i)/owners.size());
			double y = center.getColumn()  + radius * Math.sin((2 * Math.PI * i)/owners.size());	
			ownerToLocation.put(current.getNameAsString(), new ParkLocation(x,y));
		}
		return ownerToLocation;
	}

	private PlayerGraph buildPlayerGraph(List<Owner> owners) {
		PlayerGraph graph = new PlayerGraph(owners);
		for (int i=0; i < owners.size()-1; i++) {
			Owner current = owners.get(i);
			Owner next = owners.get(i+1);
			graph.addConnection(current, next);
		}
		graph.addConnection(owners.get(owners.size()-1), owners.get(0));
		return graph;
	}

	private ParkLocation getNextLocation(ParkLocation currentLocation, ParkLocation desiredLocation) {
		double x_difference = desiredLocation.getRow() - currentLocation.getRow(); 
		double y_difference = desiredLocation.getColumn() - currentLocation.getColumn(); 
		double distance = Math.sqrt(Math.pow(x_difference, 2) + Math.pow(y_difference, 2));
		if (distance < 5) {
			return desiredLocation;
		}
		double x_difference_normalized = x_difference/distance;
		double y_difference_normalized = y_difference/distance;
		return new ParkLocation(currentLocation.getRow() + x_difference_normalized*2, currentLocation.getColumn() + y_difference_normalized*2);
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

		try {
			List<Owner> allOwners = otherOwners;
			allOwners.add(myOwner);
			sortOwners();
	
			Directive directive = new Directive();
			
			if (round == 1) {
				List<Owner> alphabeticalOwners = sortOwnersAlphabetically(allOwners);
				HashMap<String, ParkLocation> currentPositions = mapOwnerToParkLocationCircle(alphabeticalOwners, new ParkLocation(25.0,25.0), 30);
				this.positions = currentPositions;
				this.graph = buildPlayerGraph(alphabeticalOwners);
			}
			
			ParkLocation currentLocation = myOwner.getLocation(); 
			ParkLocation desiredLocation = this.positions.get(myOwner.getNameAsString());

			if (Math.abs(currentLocation.getRow() - desiredLocation.getRow()) > 1.0 || Math.abs(currentLocation.getColumn() - desiredLocation.getColumn()) > 1.0) {
				directive.instruction = Instruction.MOVE;
				ParkLocation next = getNextLocation(currentLocation, desiredLocation);
				directive.parkLocation = next;
				return directive;
			}
	
			/*
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
			*/
			
			//get waiting dogs and separate into others' dogs and my own dogs
			List<Dog> waitingDogs = getWaitingDogs(myOwner, otherOwners);
			List<Dog> notOwnedByMe = new LinkedList<Dog>();
			List<Dog> ownedByMe = new LinkedList<Dog>();
			for (Dog dog: waitingDogs) {
				if (dog.getOwner() != this.myOwner) {
					notOwnedByMe.add(dog);
				} else {
					ownedByMe.add(dog);
				}
			}

			List<Dog> sortedDogs = sortDogsByRemainingWaitTime(waitingDogs);
			if (!sortedDogs.isEmpty()) {
				directive.instruction = Instruction.THROW_BALL;
				directive.dogToPlayWith = sortedDogs.get(0);
				List<OwnerName> neighbors = this.graph.getConnections(myOwner);
				OwnerName throwToOwnerName = neighbors.get(0);
				Owner requiredOwner = null; 
				for (Owner throwOwner: this.otherOwners) {
					if (throwOwner.getNameAsEnum() == throwToOwnerName) {
						requiredOwner = throwOwner;
						break;
					}
				}

				Double ballRow = requiredOwner.getLocation().getRow();
				Double ballColumn = requiredOwner.getLocation().getColumn();
				Random r = new Random();
				ballRow += r.nextInt(5 + 5) - 5;

				simPrinter.println(throwToOwnerName + " from " + myOwner.getNameAsString());

				directive.parkLocation = new ParkLocation(ballRow, ballColumn);
				return directive;
			}

			

			
			/*
	
			//if any of my own dogs are waiting, throw the ball for the least exercised dog to some other owner
			if (!ownedByMe.isEmpty()){
				directive.instruction = Instruction.THROW_BALL;
				List<Dog> sortedMyDogs = sortDogs(ownedByMe);
				directive.dogToPlayWith = sortedMyDogs.get(0);
				for (Owner throwOwner : this.otherOwners){
					Double dist = distanceBetweenOwners(throwOwner, this.myOwner);
					//System.out.println(dist);
					if (dist < 40) {
						Double ballRow = throwOwner.getLocation().getRow();
						Double ballColumn = throwOwner.getLocation().getColumn();
						Random r = new Random();
						ballRow += r.nextInt(5 + 5) - 5;
						directive.parkLocation = new ParkLocation(ballRow, ballColumn);
						return directive;
					}
				}
	
				//if all other owners are >40 distance away, throw the ball randomly
				double randomDistance = 40.0;
				double randomAngle = Math.toRadians(random.nextDouble() * 360);
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
				return directive;
				
			}
	
			//if any of the others' dogs is waiting, throw the ball back to its owner
			if (!ownedByMe.isEmpty()) {
				directive.instruction = Instruction.THROW_BALL;
				List<Dog> sortedOtherDogs = sortDogs(notOwnedByMe);
				directive.dogToPlayWith = sortedOtherDogs.get(0);
				Owner throwOwner = directive.dogToPlayWith.getOwner();
				Double dist = distanceBetweenOwners(throwOwner, this.myOwner);
				if (dist < 40) {
					Double ballRow = throwOwner.getLocation().getRow();
					Double ballColumn = throwOwner.getLocation().getColumn();
					Random r = new Random();
					ballRow += r.nextInt(5 + 5) - 5;
					directive.parkLocation = new ParkLocation(ballRow, ballColumn);
					return directive;
				}
	
				//if its owner is >40 distance away, throw the ball in its owner's direction for 40 meters
				double dx = throwOwner.getLocation().getRow() - myOwner.getLocation().getRow();
				double dy = throwOwner.getLocation().getColumn() - myOwner.getLocation().getColumn();
				double ballRow = myOwner.getLocation().getRow() + dx * (40.0/dist);
				double ballColumn = myOwner.getLocation().getColumn() + dy * (40.0/dist);
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
			*/
	
			// otherwise do nothing
			directive.instruction = Instruction.NOTHING;
			return directive;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		Directive directive = new Directive();
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

	private List<Dog> sortDogsByRemainingWaitTime(List<Dog> dogList){
		Collections.sort(dogList, new Comparator<Dog>() {
			@Override
			public int compare(Dog u1, Dog u2) {
			  return u1.getWaitingTimeRemaining().compareTo(u2.getWaitingTimeRemaining());
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

	private List<Owner> sortOwnersAlphabetically(List<Owner> owners){
		Collections.sort(owners, new Comparator<Owner>() {
			@Override
			public int compare(Owner u1, Owner u2) {
			  return u1.getNameAsString().compareTo(u2.getNameAsString());
			}
		  });
		return owners;
	}

	private int compareOwners(Owner u1, Owner u2){
		Double distanceToOwner1 = Math.pow(u1.getLocation().getRow() - this.myOwner.getLocation().getRow(), 2);
		distanceToOwner1 += Math.pow(u1.getLocation().getColumn() - this.myOwner.getLocation().getColumn(), 2);

		Double distanceToOwner2 = Math.pow(u2.getLocation().getRow() - this.myOwner.getLocation().getRow(), 2);
		distanceToOwner2 += Math.pow(u2.getLocation().getColumn() - this.myOwner.getLocation().getColumn(), 2);

		return distanceToOwner2.compareTo(distanceToOwner1);
	}

	private Double distanceBetweenOwners(Owner u1, Owner u2){
		Double dX = u1.getLocation().getRow() - u2.getLocation().getRow();
		Double dY = u1.getLocation().getColumn() - u2.getLocation().getColumn();
		Double dist = Math.sqrt(dX * dX + dY * dY);
		return dist;
	}

}