package dogs.g3;

import dogs.g3.PlayerGraph;
import dogs.g3.PlayerGraph.GraphType;

import java.util.*;
import java.util.jar.Attributes.Name;

import javax.swing.plaf.synth.SynthSeparatorUI;

import dogs.sim.Directive;
import dogs.sim.Directive.Instruction;
import dogs.sim.DogReference.Breed;
import dogs.sim.Dictionary;
import dogs.sim.Dog;
import dogs.sim.Owner;
import dogs.sim.Owner.OwnerName;
import dogs.sim.ParkLocation;
import dogs.sim.SimPrinter;
import java.io.StringWriter;

public class Player extends dogs.sim.Player {
	private List<Owner> otherOwners;
	private String identification_signal = "three";
	private List<Dog> myDogs;
	private Integer round;
	private Owner myOwner;
	// private List<Owner> myOtherInstances;
	private List<Owner> otherInstances;
	private List<Owner> group5Instances;
	HashMap<String, ParkLocation> positions;
	private List<Dog> allDogs;
	HashMap<String, String> nextOwners;
	HashMap<String, Integer> classifiedOtherOwners;
	PlayerGraph graph;
	private int count = 0;
	private int numCenterPlayers = 0;
	private ParkLocation center;
	private Double MAX_THROW_DIST = 40.0;
	private Double MAX_MOVE_DIST = 5.0;
	private int radius;

	/**
	 * Player constructor
	 *
	 * @param rounds          number of rounds
	 * @param numDogsPerOwner number of dogs per owner
	 * @param numOwners       number of owners
	 * @param seed            random seed
	 * @param simPrinter      simulation printer
	 *
	 */
	public Player(Integer rounds, Integer numDogsPerOwner, Integer numOwners, Integer seed, Random random,
			SimPrinter simPrinter) {
		super(rounds, numDogsPerOwner, numOwners, seed, random, simPrinter);
	}

	private HashMap<String, ParkLocation> mapOwnerToParkLocationCircle(List<Owner> owners, ParkLocation center,
			int radius) {
		HashMap<String, ParkLocation> ownerToLocation = new HashMap<String, ParkLocation>();
		if (this.graph != null && this.graph.graphType == GraphType.GRID) {

		} else {
			for (int i = 0; i < owners.size(); i++) {
				Owner current = owners.get(i);
				double x = center.getRow() + radius * Math.cos((2 * Math.PI * i) / owners.size() + Math.PI / 4);
				double y = center.getColumn() + radius * Math.sin((2 * Math.PI * i) / owners.size() + Math.PI / 4);
				ownerToLocation.put(current.getNameAsString(), new ParkLocation(y, x));
				//System.out.println(current.getNameAsString() + ": " + x + " " + y);
			}
		}
		return ownerToLocation;
	}

	private void addNodesToGraph(List<Owner> myOwners, List<Owner> otherGroupOwners) {
		for (Owner o1 : myOwners) {
			for (Owner o2 : otherGroupOwners) {
				simPrinter.println("Here");
				if ((o1.getNameAsEnum() != o2.getNameAsEnum()) && (distanceBetweenOwners(o1, o2) <= 40.0)) {
					simPrinter.println(o1.getNameAsEnum());
					this.graph.addNode(o2);
					this.graph.addConnection(o1, o2);
				}
			}
		}
	}

	/**
	 * Build a Graph with owners as nodes
	 * 
	 * @param owners list of owners
	 */
	private PlayerGraph buildPlayerGraph(List<Owner> owners) {

		PlayerGraph graph = new PlayerGraph(owners);
		// simPrinter.println(owners.size());
		// add an edge between each pair of owners within 40 meters of each other
		for (Owner o1 : owners) {
			for (Owner o2 : owners) {
				simPrinter.println(desiredDistanceBetweenOwners(o1, o2));
				if ((o1.getNameAsEnum() != o2.getNameAsEnum()) && (desiredDistanceBetweenOwners(o1, o2) <= 40.0)) {
					graph.addConnection(o1, o2);
				}
			}
		}
		graph.printGraph(simPrinter);
		return graph;
	}

	private ParkLocation getNextLocation(ParkLocation currentLocation, ParkLocation desiredLocation) {
		double x_difference = desiredLocation.getRow() - currentLocation.getRow();
		double y_difference = desiredLocation.getColumn() - currentLocation.getColumn();
		double distance = Math.sqrt(Math.pow(x_difference, 2) + Math.pow(y_difference, 2));
		if (distance < 5) {
			return desiredLocation;
		}
		double x_difference_normalized = x_difference / distance;
		double y_difference_normalized = y_difference / distance;
		return new ParkLocation(currentLocation.getRow() + x_difference_normalized * 2,
				currentLocation.getColumn() + y_difference_normalized * 2);
	}

	private ParkLocation getAdjustedPath(ParkLocation currentLocation, ParkLocation nextLocation, Dog dog) {
		double currentAngle = Math.atan2((nextLocation.getRow() - currentLocation.getRow()),
				(nextLocation.getColumn() - currentLocation.getColumn()));
		double radius = distanceBetweenLocs(currentLocation, nextLocation);
		int offset = 0;
		switch (dog.getBreed()) {
			case LABRADOR:
				offset = 20;
			case POODLE:
				offset = 15;
			case SPANIEL:
				offset = 10;
			default:
				offset = 5;
		}
		double x = currentLocation.getColumn() + radius * Math.cos(currentAngle + offset * 0.017);
		double y = currentLocation.getRow() + radius * Math.sin(currentAngle + offset * 0.017);
		return new ParkLocation(y, x);
	}

	/**
	 * Choose command/directive for next round
	 *
	 * @param round       current round
	 * @param myOwner     my owner
	 * @param otherOwners all other owners in the park
	 * @return a directive for the owner's next move
	 *
	 */
	public Directive chooseDirective(Integer round, Owner myOwner, List<Owner> otherOwners) {
		this.round = round;
		this.myDogs = myOwner.getDogs();
		this.myDogs = getExercisingDogs(myOwner, this.myDogs);
		this.allDogs = getAllDogs(myOwner, otherOwners);
		this.otherOwners = otherOwners;
		this.myOwner = myOwner;

		try {
			List<Owner> allOwners = otherOwners;
			allOwners.add(myOwner);
			sortOwners();
			Directive directive = new Directive();
			// if(round == 6){
			// System.out.println(getOtherOwnersSignals(otherOwners).toString());
			// }

			if (round == 1) {
				directive.instruction = Instruction.CALL_SIGNAL;
				directive.signalWord = this.identification_signal;
				return directive;
			} else if (round == 6) {
				this.group5Instances = getOtherGroupInstances(allOwners);
				this.otherInstances = getOtherInstancesSignals(otherOwners);
				//this.otherInstances.add(myOwner);
				// System.out.println(this.otherInstances.toString());
				for (Owner o : this.otherInstances) {
					if (this.otherOwners.contains(o)) {
						this.otherOwners.remove(o); // remove other instances of group 3 from otherOwners
					}
				}

				// System.out.println(this.otherInstances.size());
				List<Owner> temp2 = new ArrayList<>();
				for (Owner o : this.otherInstances) {
					temp2.add(o);
				}
				List<Owner> alphabeticalOwners = sortOwnersAlphabetically(temp2);
				List<Owner> temp = new LinkedList<Owner>();
				temp.addAll(alphabeticalOwners);
				double radius = 40 * (this.otherInstances.size() - 1);
				radius /= (double) (2.0 * Math.PI);
				// System.out.println(radius + " " + allOwners.size());
				radius = 25;

				HashMap<String, ParkLocation> currentPositions = new HashMap<String, ParkLocation>();

				int unassignedOwners = alphabeticalOwners.size();
				int preferredGroupSize = 4;
				int minGroupSize = 3;
				double rowLocation = 85.0;
				double colLocation = 85.0;
				int row = 1;
				int col = 1;
				// System.out.println("END OF R6 1 " + this.otherInstances.size());
				while (unassignedOwners >= minGroupSize) {
					// simPrinter.println("Iteration");
					// simPrinter.println(unassignedOwners);
					List<Owner> currentRound = new LinkedList<Owner>();

					int limit = preferredGroupSize;
					if (unassignedOwners - preferredGroupSize < minGroupSize) {
						limit = unassignedOwners;
					}

					for (int i = 0; i < limit && unassignedOwners > 0; i++) {
						currentRound.add(alphabeticalOwners.get(0));
						alphabeticalOwners.remove(0);
						unassignedOwners -= 1;
					}

					HashMap<String, ParkLocation> tempPositions = mapOwnerToParkLocationCircle(currentRound,
							new ParkLocation(rowLocation, colLocation), (int) radius);
					currentPositions.putAll(tempPositions);
					rowLocation += 3 * radius;
					if (rowLocation > 200) {
						rowLocation = 25;
						colLocation += 3 * radius;
					}
				}
				// System.out.println("END OF R6 2 " + this.otherInstances.size());
				if (unassignedOwners > 0) {
					currentPositions = mapOwnerToParkLocationCircle(alphabeticalOwners, new ParkLocation(25.0, 25.0),
							(int) radius);
				}

				this.positions = currentPositions;
				this.graph = buildPlayerGraph(temp);
				this.graph.graphType = this.graph.getGraphType(this.otherInstances);

				this.center = new ParkLocation(25.0, 25.0);
				this.radius = (int) radius;

				directive.instruction = Instruction.CALL_SIGNAL;
				directive.signalWord = this.identification_signal;
				return directive;
			} else if (this.round == 501) {
				updateGroup5Instances();
				updateOwnInstances();

				List<OwnerName> myOwnerInstances = new ArrayList<>();
				for (Owner me : this.otherInstances) {
					myOwnerInstances.add(me.getNameAsEnum());
				}
				List<OwnerName> boundaries = new ArrayList<>();
				List<OwnerName> ownBoundaries = new ArrayList<>();
				GraphType g = recognizeFormation();

				// System.out.println(this.otherInstances.toString());
				if (this.group5Instances.size() > 0) {
					boundaries = determineShapeBoundaries(this.group5Instances);
					ownBoundaries = determineShapeBoundaries(this.otherInstances);
				}
				// TL, BL, TR, BR 
				//System.out.println(ownBoundaries.toString());
				//System.out.println(boundaries.toString());
				collaborate(boundaries, ownBoundaries);
				double dist = Integer.MAX_VALUE;
				Owner nearest = null;
				for (Owner o : allOwners) {
					if (!myOwnerInstances.contains(o.getNameAsEnum())) {
						double temp = Math.pow(o.getLocation().getRow() - this.center.getRow(), 2)
								+ Math.pow(o.getLocation().getColumn() - this.center.getColumn(), 2);
						if (temp < dist) {
							dist = temp;
							nearest = o;
						}
					}
				}

				simPrinter.println("Size");
				simPrinter.println(this.group5Instances.size());
				// this.addNodesToGraph(this.otherOwners, this.group5Instances);
				directive.instruction = Instruction.CALL_SIGNAL;
				directive.signalWord = "separate";

				if (g == GraphType.GRID) {
					// System.out.println("GRID");

				} else {
					// System.out.println("POLYGON");
				}

				return directive;
			}
			// System.out.println(this.graph.graphType);
			ParkLocation currentLocation = myOwner.getLocation();
			ParkLocation desiredLocation = this.positions.get(myOwner.getNameAsString());

			if (Math.abs(currentLocation.getRow() - desiredLocation.getRow()) > 1.0
					|| Math.abs(currentLocation.getColumn() - desiredLocation.getColumn()) > 1.0) {
				directive.instruction = Instruction.MOVE;
				ParkLocation next = getNextLocation(currentLocation, desiredLocation);
				directive.parkLocation = next;
				return directive;
			}

			/*
			 * if(round <= 151) { directive.instruction = Instruction.MOVE;
			 * directive.parkLocation = new ParkLocation(myOwner.getLocation().getRow() + 2,
			 * myOwner.getLocation().getColumn() + 2); return directive; } else if (round <=
			 * 271) { directive.instruction = Instruction.MOVE; directive.parkLocation = new
			 * ParkLocation(myOwner.getLocation().getRow(),
			 * myOwner.getLocation().getColumn() + 2); return directive; }
			 */

			// get waiting dogs and separate into others' dogs and my own dogs
			List<Dog> waitingDogs = getWaitingDogs(myOwner, otherOwners);
			waitingDogs = getExercisingDogs(myOwner, waitingDogs);
			List<Dog> notOwnedByMe = new ArrayList<Dog>();
			List<Dog> ownedByMe = new ArrayList<Dog>();
			for (Dog dog : waitingDogs) {
				if (!dog.getOwner().equals(this.myOwner)) {
					notOwnedByMe.add(dog);
				} else {
					ownedByMe.add(dog);
				}
			}

			//check if any dogs can be parked
			if(myOwner.getNameAsEnum() == OwnerName.DAVE){
				simPrinter.println("Dave's dogs:" + this.myDogs.toString());
			}
			for (Dog d : myOwner.getDogs()){
				if(myOwner.getNameAsEnum() == OwnerName.DAVE){
					simPrinter.println("Dave: " + d.getRealID());
					simPrinter.println("Dog object: " + d);
					simPrinter.println("Breed: " + d.getBreed());
					simPrinter.println("Is waiting for Dave? " + d.isWaitingForOwner(myOwner));
					simPrinter.println("Exercise remaining: " + d.getExerciseTimeRemaining());
					simPrinter.println("Dave's parked dogs: " + this.graph.getNode(myOwner).getParkedDogs());
				}
				if (d.isWaitingForOwner(myOwner) && d.getExerciseTimeRemaining()==0.0 && !this.graph.getNode(myOwner).getParkedDogs().contains(d)){
					Dog dogToPark = d;
					List<OwnerName> neighborNames = this.graph.getConnections(myOwner);
					List<Owner> neighbors = new ArrayList<>();
					for (Owner owner : this.otherOwners) {
						if (neighborNames.contains(owner.getNameAsEnum())) {
							neighbors.add(owner);
						}
					}
					directive = parkDog(myOwner, neighbors, dogToPark);
					this.graph.getNode(myOwner).addParkedDog(dogToPark);
					return directive;
				}
			}


			// Owner alice = null;
			for (int i = 0; i < this.otherOwners.size(); i++) {
				if (this.otherOwners.get(i).getNameAsEnum() == OwnerName.ALICE) {
					System.out.println(this.otherOwners.get(i).getLocationAsString());
				}
			}
			// System.out.println(this.graph.getConnections(alice).toString());

			List<Dog> sortedDogs = sortDogsByRemainingWaitTime(waitingDogs);
			for (Dog dog : sortedDogs) {
				simPrinter.println(dog.getWaitingTimeRemaining());
			}

			// out.println(waitingDogs.size() + " " + sortedDogs.size() + " " +
			// sortedDogsNotMine.size() + " " + notOwnedByMe.size() + " " +
			// ownedByMe.size());
			if (!sortedDogs.isEmpty()) {
				directive.instruction = Instruction.THROW_BALL;
				directive.dogToPlayWith = sortedDogs.get(0);

				if ((directive.dogToPlayWith.getBreed() == Breed.LABRADOR
						|| directive.dogToPlayWith.getBreed() == Breed.POODLE) && this.group5Instances.size() > 0) {
					simPrinter.println("Reached");
					Owner required = null;
					List<OwnerName> g5OwnerNames = new LinkedList<OwnerName>();

					for (Owner g5owner : this.group5Instances) {
						for (Owner owner : otherOwners) {
							simPrinter.println("Reached2");
							if (owner.getNameAsEnum() == g5owner.getNameAsEnum()
									&& distanceBetweenOwners(myOwner, owner) <= 40.0) {
								required = owner;
								break;
							}
						}
					}

					if (required != null) {
						simPrinter.println("Reached 2");

						Double ballRow = required.getLocation().getRow();
						Double ballColumn = required.getLocation().getColumn();
						ParkLocation location = getAdjustedPath(myOwner.getLocation(), required.getLocation(),
								directive.dogToPlayWith);
						Random r = new Random();
						count += 1;
						// simPrinter.println(myOwner.getNameAsString() + " " + sortedDogs.size());
						// simPrinter.println(throwToOwnerName + " from " + myOwner.getNameAsString());
						directive.parkLocation = new ParkLocation(location.getRow(), location.getColumn());
						return directive;
					}

				}

				if (sortedDogs.get(0).getWaitingTimeRemaining() > 19) {
					List<Dog> sortedExerciseDogs = sortDogs(waitingDogs);
					directive.dogToPlayWith = sortedExerciseDogs.get(0);
				}
				List<OwnerName> neighborNames = this.graph.getConnections(myOwner);

				// throw the ball toward the least occupied neighbor
				List<Owner> neighbors = new ArrayList<>();
				for (Owner owner : this.otherOwners) {
					if (neighborNames.contains(owner.getNameAsEnum())) {
						neighbors.add(owner);
					}
				}
				Owner requiredOwner = getLeastBusyNeighbor(neighbors, this.allDogs, this.graph);
				if (requiredOwner == null) {
					Directive d = throwRandomly();
					// System.out.println(d.instruction.toString() + " " +
					// d.parkLocation.toString());
					return throwRandomly();
				}
				OwnerName throwToOwnerName = requiredOwner.getNameAsEnum();
				simPrinter.println("Throwing To " + throwToOwnerName + "round " + round);

				/*
				 * Owner requiredOwner = null; for (Owner throwOwner: this.otherOwners) { if
				 * (throwOwner.getNameAsEnum() == throwToOwnerName) { requiredOwner =
				 * throwOwner; break; } }
				 */

				Double ballRow = requiredOwner.getLocation().getRow();
				Double ballColumn = requiredOwner.getLocation().getColumn();
				ParkLocation location = getAdjustedPath(myOwner.getLocation(), requiredOwner.getLocation(),
						directive.dogToPlayWith);
				simPrinter.println(ballRow + " " + location.getRow());
				simPrinter.println(ballColumn + " " + location.getColumn());
				Random r = new Random();
				count += 1;
				// simPrinter.println(myOwner.getNameAsString() + " " + sortedDogs.size());
				// simPrinter.println(throwToOwnerName + " from " + myOwner.getNameAsString());
				directive.parkLocation = new ParkLocation(location.getRow(), location.getColumn());
				return directive;
			}

			// otherwise do nothing
			directive.instruction = Instruction.NOTHING;
			return directive;
		} catch (Exception e) {
			// System.out.println(e.toString());
			e.printStackTrace();
		}
		Directive directive = new Directive();
		return directive;
	}

	private void collaborate(List<OwnerName> ownBoundaries, List<OwnerName> otherBoundaries){
		List<ParkLocation> possibleArea = new ArrayList<>();
		// TL, BL, TR, BR 
		Owner TL = getOwner(ownBoundaries.get(0));
		Owner BL = getOwner(ownBoundaries.get(1));
		Owner TR = getOwner(ownBoundaries.get(2));
		Owner BR = getOwner(ownBoundaries.get(3));

		double sizeVert = Math.max(BL.getLocation().getColumn() - TL.getLocation().getColumn(), BR.getLocation().getColumn() -TR.getLocation().getColumn());
		double sizeHoriz = Math.max(BR.getLocation().getRow() - BL.getLocation().getRow(), TR.getLocation().getRow() - TL.getLocation().getRow());
		
		Owner TLOther = getOwner(otherBoundaries.get(0));
		Owner BLOther = getOwner(otherBoundaries.get(1));
		Owner TROther = getOwner(otherBoundaries.get(2));
		Owner BROther = getOwner(otherBoundaries.get(3));

		double top= Math.min(TLOther.getLocation().getColumn(), TROther.getLocation().getColumn());
		double bottom = Math.max(BLOther.getLocation().getColumn(), BROther.getLocation().getColumn());
		double left = Math.min(TLOther.getLocation().getRow(), BLOther.getLocation().getRow());
		double right = Math.max(TROther.getLocation().getRow(), BROther.getLocation().getRow());
		System.out.println("sizeVert: " + sizeVert + " top: " + top + "radius: " + this.radius + "other: " + TLOther.getLocation().getRow() + "BL: " + BL.getLocation().getRow());
		if(top - sizeVert > 40){
			double horizShift = TLOther.getLocation().getRow() - BL.getLocation().getRow();
			double topShift = TLOther.getLocation().getColumn() - BL.getLocation().getColumn();

			System.out.println(topShift + " -> " + horizShift);
			this.center.setColumn(this.center.getColumn() + topShift);
			this.center.setRow(this.center.getRow() + horizShift);
			this.positions = mapOwnerToParkLocationCircle(this.otherInstances, this.center, this.radius);
		} else {
			double horizShift = BROther.getLocation().getRow() - TL.getLocation().getRow();
			double topShift = BROther.getLocation().getColumn() - TL.getLocation().getColumn()+ 80;

			System.out.println(topShift + " -> " + horizShift);
			this.center.setRow(this.center.getColumn() - topShift);
			this.center.setColumn(this.center.getRow() - horizShift);
			System.out.println(this.positions.toString());
			this.positions = mapOwnerToParkLocationCircle(this.otherInstances, this.center, this.radius);
			System.out.println(this.positions.toString());
			
		}

	}

	private Owner getOwner(OwnerName name){
		for(Owner o: this.otherOwners){
			if(o.getNameAsEnum() == name)
				return o;
		}
		return null;
	}

	/**
	* throw the ball for a dog that has completed exercise
	* to an angle that does not block any edge
	* @param myOwner 	current owner, owner of the dog
	* @param neighbors  owners connected to myOwner in the graph
	* @param dog 		the dog that has finished 30 min of exercise
	* @return 			a Directive that throws the ball toward an open space
	**/
	private Directive parkDog(Owner myOwner, List<Owner> neighbors, Dog dog){
		//first make sure the dog belongs to the owner and has finished exercise 
		/*
		if ((dog.getOwner() != myOwner) || (dog.getExerciseTimeRemaining() > 0.0)){
			simPrinter.println(myOwner.getNameAsString() + "does not own the dog OR the dog hasn't finished exercising.");
			return null;
		}*/

		Directive directive = new Directive();
		directive.instruction = Instruction.THROW_BALL;
		directive.dogToPlayWith = dog;
		
		Integer num_neighbors = neighbors.size();
		Double min_angle_diff = 61 * Math.PI / 180.0;
		ParkLocation neighborLoc, myLoc, neighbor2Loc;
		Double neighbor_angle, target_angle;
		Boolean valid_angle = true;
		simPrinter.println(num_neighbors);
		switch(num_neighbors){
			case 0:
				directive = throwRandomly();
				break;
			/*case 1:
				neighborLoc = neighbors.get(0).getLocation();
				myLoc = myOwner.getLocation();
				Double scale = this.distanceBetweenLocs(myLoc, neighborLoc)/2; //throw to 2 meters way
				Double x = myLoc.getRow() - (neighborLoc.getRow() - myLoc.getRow()) / scale;
				Double y = myLoc.getColumn() - (neighborLoc.getColumn() - myLoc.getColumn()) / scale;

				directive.parkLocation = this.getValidLocation(new ParkLocation(x, y));
				break;*/
			default:
				List<Double> neighbor_angles = new ArrayList<>();
				simPrinter.println("Neighbor name: " + neighbors.get(0).getNameAsString());
				for (int i=0; i<neighbors.size(); i++){
					neighborLoc = neighbors.get(i).getLocation();
					myLoc = myOwner.getLocation();
					neighbor_angle = Math.atan2(neighborLoc.getRow()-myLoc.getRow(), neighborLoc.getColumn()-myLoc.getColumn());
					simPrinter.println("Neighbor angle: " + neighbor_angle);
					target_angle = neighbor_angle + min_angle_diff;
					valid_angle = true;

					//check if this angle is at least 60 degrees away from every other neighbor
					for (int j=0; j<neighbors.size(); j++){
						if (i == j){continue;}

						neighbor2Loc = neighbors.get(j).getLocation();
						Double neighbor2_angle = Math.atan2(neighbor2Loc.getRow()-myLoc.getRow(), neighbor2Loc.getColumn()-myLoc.getColumn());
						if (Math.abs(neighbor2_angle - target_angle) < min_angle_diff){
							valid_angle = false; //too close to one of the neighbors, invalid angle
							break;
						}
					}

					if (valid_angle){
							simPrinter.println("Parking angle: " + target_angle);
							Double dist = 40.0;
							directive.parkLocation = new ParkLocation(myLoc.getRow() + dist*Math.sin(target_angle), myLoc.getColumn() + dist*Math.cos(target_angle));
							directive.parkLocation = this.getValidLocation(directive.parkLocation);
							break;
					}
				}
				if (!valid_angle){
					simPrinter.println("Parking dog in random direction...");
					directive = throwRandomly();
				}
				break;
		}
		return directive;
	}

	private void updateGroup5Instances(){
		for(int i = 0; i < this.group5Instances.size(); i++){
			for(Owner o: this.otherOwners){
				if(this.group5Instances.get(i).getNameAsEnum() == o.getNameAsEnum()){
					this.group5Instances.set(i, o);
				}
			}
		}
	}

	private void updateOwnInstances() {
		for (int i = 0; i < this.otherInstances.size(); i++) {
			for (Owner o : this.otherOwners) {
				if (this.otherInstances.get(i).getNameAsEnum() == o.getNameAsEnum()) {
					this.otherInstances.set(i, o);
				}
			}
		}
	}

	private List<String> getOtherOwnersSignals(List<Owner> otherOwners) {
		List<String> otherOwnersSignals = new ArrayList<>();
		for (Owner otherOwner : otherOwners)
			if (!otherOwner.getCurrentSignal().equals("_"))
				otherOwnersSignals.add(otherOwner.getCurrentSignal());
		return otherOwnersSignals;
	}

	private List<Owner> getOtherGroupInstances(List<Owner> otherOwners) {
		this.group5Instances = new ArrayList<>();
		for (Owner otherOwner : otherOwners) {
			if (otherOwner.getCurrentSignal().equals("Zyzzogeton")) {
				this.group5Instances.add(otherOwner);
			}
		}
		return this.group5Instances;
	}

	private List<Owner> getOtherInstancesSignals(List<Owner> otherOwners) {
		List<Owner> otherInstances = new ArrayList<>();
		for (Owner otherOwner : otherOwners) {
			if (otherOwner.getCurrentSignal().equals(this.identification_signal)) {
				otherInstances.add(otherOwner);
			}
		}
		return otherInstances;
	}

	private List<Dog> getWaitingDogs(Owner myOwner, List<Owner> otherOwners) {
		List<Dog> waitingDogs = new ArrayList<>();
		int count = 0;
		for (Dog dog : myOwner.getDogs()) {
			if (dog.isWaitingForItsOwner()) {
				waitingDogs.add(dog);
				count += 1;
			}
		}
		for (Owner otherOwner : otherOwners) {
			for (Dog dog : otherOwner.getDogs()) {
				if (dog.isWaitingForOwner(myOwner)) {
					waitingDogs.add(dog);
					// simPrinter.println("Found Other Dog Thats Not Ours " +
					// dog.getOwner().getNameAsString() + " " + myOwner.getNameAsString());
					// simPrinter.println(dog.getOwner().getNameAsString());
					count += 1;
				}
			}
		}
		// System.out.println(myOwner.getNameAsString() + " " + count);
		return waitingDogs;
	}

	private GraphType recognizeFormation() {
		Owner nearest = null;
		List<OwnerName> myOwnerInstances = new ArrayList<>();
		double dist = Integer.MAX_VALUE;
		for (Owner o : this.otherOwners) {
			if (!myOwnerInstances.contains(o.getNameAsEnum())) {
				double temp = Math.pow(o.getLocation().getRow() - this.center.getRow(), 2)
						+ Math.pow(o.getLocation().getColumn() - this.center.getColumn(), 2);
				if (temp < dist) {
					dist = temp;
					nearest = o;
				}
			}
		}
		int closeCount = 0;
		for (Owner o : this.otherOwners) {
			double distNearest = Math.pow(o.getLocation().getRow() - nearest.getLocation().getRow(), 2)
					+ Math.pow(o.getLocation().getColumn() - nearest.getLocation().getColumn(), 2);
			if (distNearest <= 40)
				closeCount++;
		}
		if (closeCount > 2) {
			return GraphType.GRID;
		}
		return GraphType.POLYGON;
	}

	private List<OwnerName> determineShapeBoundaries(List<Owner> instances) {
		if (instances.size() < 1) {
			return new ArrayList<OwnerName>();
		}
		List<OwnerName> corners = new ArrayList<>();
		double xPos = Integer.MAX_VALUE;
		double yPos = Integer.MAX_VALUE;
		Owner temp = instances.get(0);
		for (Owner o : instances) {
			if (o.getLocation().getColumn() < xPos && o.getLocation().getRow() < yPos) {
				temp = o;
				yPos = o.getLocation().getRow();
				xPos = o.getLocation().getColumn();
			}
		}
		corners.add(temp.getNameAsEnum());
		xPos = Integer.MIN_VALUE;
		yPos = Integer.MAX_VALUE;
		temp = instances.get(0);
		for (Owner o : instances) {
			if (!corners.contains(o.getNameAsEnum())) {
				temp = o;
			}
		}
		for (Owner o : instances) {
			// System.out.println(o.getNameAsString() + ": " + o.getLocationAsString());
			// System.out.println(o.getNameAsString() + ": " + o.getLocation().getColumn());
			if (o.getLocation().getColumn() > xPos && o.getLocation().getRow() < yPos
					&& !corners.contains(o.getNameAsEnum())) {
				temp = o;
				yPos = o.getLocation().getRow();
				xPos = o.getLocation().getColumn();
			}
		}
		corners.add(temp.getNameAsEnum());

		xPos = Integer.MAX_VALUE;
		yPos = Integer.MIN_VALUE;
		temp = instances.get(0);
		for (Owner o : instances) {
			if (!corners.contains(o.getNameAsEnum())) {
				temp = o;
			}
		}
		for (Owner o : instances) {
			if (o.getLocation().getColumn() < xPos && o.getLocation().getRow() > yPos
					&& !corners.contains(o.getNameAsEnum())) {
				temp = o;
				yPos = o.getLocation().getRow();
				xPos = o.getLocation().getColumn();
			}
		}
		corners.add(temp.getNameAsEnum());

		xPos = Integer.MIN_VALUE;
		yPos = Integer.MIN_VALUE;
		temp = instances.get(instances.size() - 1);
		for (Owner o : instances) {
			if (!corners.contains(o.getNameAsEnum())) {
				temp = o;
			}
		}
		for (Owner o : instances) {
			if (o.getLocation().getColumn() < xPos && o.getLocation().getRow() < yPos) {
				temp = o;
				yPos = o.getLocation().getRow();
				xPos = o.getLocation().getColumn();
			}
		}
		corners.add(temp.getNameAsEnum());
		return corners;
	}

	/* SORTING */
	private List<Dog> sortDogs(List<Dog> dogList) {
		Collections.sort(dogList, new Comparator<Dog>() {
			@Override
			public int compare(Dog u1, Dog u2) {
				return compareDogs(u1, u2);
			}
		});
		return dogList;
	}

	private List<Dog> sortDogsByRemainingWaitTime(List<Dog> dogList) {
		Collections.sort(dogList, new Comparator<Dog>() {
			@Override
			public int compare(Dog u1, Dog u2) {
				return u1.getWaitingTimeRemaining().compareTo(u2.getWaitingTimeRemaining());
			}
		});
		// for(Dog d: dogList){
		// System.out.println(d.getWaitingTimeRemaining());
		// }
		return dogList;
	}

	private int compareDogs(Dog u1, Dog u2) {
		return u2.getExerciseTimeRemaining().compareTo(u1.getExerciseTimeRemaining());
	}

	private void sortOwners() {
		Collections.sort(this.otherOwners, new Comparator<Owner>() {
			@Override
			public int compare(Owner u1, Owner u2) {
				return compareOwners(u1, u2);
			}
		});
	}

	private List<Owner> sortOwnersAlphabetically(List<Owner> owners) {
		Collections.sort(owners, new Comparator<Owner>() {
			@Override
			public int compare(Owner u1, Owner u2) {
				return u1.getNameAsString().compareTo(u2.getNameAsString());
			}
		});
		return owners;
	}

	private int compareOwners(Owner u1, Owner u2) {
		Double distanceToOwner1 = Math.pow(u1.getLocation().getRow() - this.myOwner.getLocation().getRow(), 2);
		distanceToOwner1 += Math.pow(u1.getLocation().getColumn() - this.myOwner.getLocation().getColumn(), 2);

		Double distanceToOwner2 = Math.pow(u2.getLocation().getRow() - this.myOwner.getLocation().getRow(), 2);
		distanceToOwner2 += Math.pow(u2.getLocation().getColumn() - this.myOwner.getLocation().getColumn(), 2);

		return distanceToOwner2.compareTo(distanceToOwner1);
	}

	// calculate the distance between two owners
	private Double distanceBetweenOwners(Owner u1, Owner u2) {
		Double dist = distanceBetweenLocs(u1.getLocation(), u2.getLocation());
		return dist;
	}

	private Double desiredDistanceBetweenOwners(Owner u1, Owner u2) {
		Double dist = distanceBetweenLocs(this.positions.get(u1.getNameAsString()),
				this.positions.get(u2.getNameAsString()));
		return dist;
	}

	// calculate the distance between park locations
	private Double distanceBetweenLocs(ParkLocation loc1, ParkLocation loc2) {
		Double dX = loc1.getRow() - loc2.getRow();
		Double dY = loc1.getColumn() - loc2.getColumn();
		Double dist = Math.sqrt(dX * dX + dY * dY);
		return dist;
	}

	/**
	 * Get the least occupied owner based on the # of dogs waiting for them + # of
	 * dogs heading toward them
	 * 
	 * @param neighbors a list of neighbor owners
	 * @param allDogs   a list of all dogs in the park
	 **/
	private Owner getLeastBusyNeighbor(List<Owner> neighbors, List<Dog> allDogs, PlayerGraph graph) {
		HashMap<Owner, Integer> busyMap = new HashMap<Owner, Integer>();
		for (Owner owner : neighbors) {
			busyMap.put(owner, 0);
		}

		// loop through all dogs to check if they are heading to/waiting for someone
		for (Dog dog : allDogs) {
			if (dog.isWaitingForPerson()) {
				Owner ownerWaited = dog.getOwnerWaitingFor();
				if (neighbors.contains(ownerWaited) && !graph.getNode(ownerWaited).getParkedDogs().contains(dog)) {
					busyMap.put(ownerWaited, busyMap.get(ownerWaited) + 1);
				}
			}

			if (dog.isHeadingForPerson()) {
				Owner ownerHeadingFor = dog.getOwnerHeadingFor();
				if (neighbors.contains(ownerHeadingFor)) {
					busyMap.put(ownerHeadingFor, busyMap.get(ownerHeadingFor) + 1);
				}
			}
		}

		// find the neighbor with the least number of dogs waiting for/heading to
		Owner leastBusyOwner = null;
		if (neighbors.size() > 0) {
			leastBusyOwner = neighbors.get(0);
			for (Owner neighbor : neighbors) {
				if (busyMap.get(neighbor) < busyMap.get(leastBusyOwner)) {
					leastBusyOwner = neighbor;
				}
			}
		}
		return leastBusyOwner;
	}

	// return only the dogs with remaining exercise time > 0
	private List<Dog> getExercisingDogs(Owner owner, List<Dog> dogs) {
		List<Dog> exerciseIncomplete = new ArrayList<>();
		exerciseIncomplete.addAll(dogs);
		// System.out.println(exerciseIncomplete.toString());
		for (Dog d : dogs) {
			if (d.getOwner() == owner && d.getExerciseTimeRemaining() == 0.0) {
				exerciseIncomplete.remove(d);
			}
		}
		// System.out.println(exerciseIncomplete.toString());
		return exerciseIncomplete;
	}

	/**
	 * return a list of all dogs in the configuration
	 **/
	private List<Dog> getAllDogs(Owner myOwner, List<Owner> otherOwners) {
		List<Dog> allDogs = new ArrayList<>();
		allDogs.addAll(myOwner.getDogs());
		for (Owner owner : otherOwners) {
			allDogs.addAll(owner.getDogs());
		}
		return allDogs;
	}

	/**
	 * throw the ball to a specified owner if the owner is more than 40 meters away,
	 * throw toward the direction for 40m
	 * 
	 * @param targetOwner the owner to throw the ball to
	 **/
	private Directive throwToOwner(Owner myOwner, Owner targetOwner) {
		ParkLocation targetLoc = targetOwner.getLocation();
		return throwToLoc(myOwner, targetLoc);
	}

	/**
	 * throw the ball to a specified target Park location if the location is more
	 * than 40m away, throw toward the direction for 40m
	 * 
	 * @param targetLoc the target location to throw the ball to
	 **/
	private Directive throwToLoc(Owner myOwner, ParkLocation targetLoc) {
		Directive directive = new Directive();
		directive.instruction = Instruction.THROW_BALL;
		Double ballRow, ballColumn;

		ParkLocation myLoc = myOwner.getLocation();
		Double dist = distanceBetweenLocs(myLoc, targetLoc);
		if (dist <= this.MAX_THROW_DIST) {
			ballRow = targetLoc.getRow();
			ballColumn = targetLoc.getColumn();
		} else {
			Double curX = myLoc.getRow();
			Double curY = myLoc.getColumn();
			Double targetX = targetLoc.getRow();
			Double targetY = targetLoc.getColumn();
			Double dX = targetX - curX;
			Double dY = targetY - curY;
			ballRow = curX + dX * (this.MAX_THROW_DIST / dist);
			ballColumn = curY + dY * (this.MAX_THROW_DIST / dist);
		}

		directive.parkLocation = new ParkLocation(ballRow, ballColumn);
		directive.parkLocation = getValidLocation(directive.parkLocation);
		return directive;
	}

	/**
	*
	*
	**/
	private Directive moveToPos(Owner myOwner, ParkLocation targetPos) {
		Directive directive = new Directive();
		directive.instruction = Instruction.MOVE;
		Double nextRow, nextColumn;

		ParkLocation myLoc = myOwner.getLocation();
		Double dist = distanceBetweenLocs(myLoc, targetPos);
		if (dist <= this.MAX_MOVE_DIST) {
			nextRow = targetPos.getRow();
			nextColumn = targetPos.getColumn();
		} else { // if targetPos is more than 5m away, move toward the target position for 5m
			Double curX = myLoc.getRow();
			Double curY = myLoc.getColumn();
			Double targetX = targetPos.getRow();
			Double targetY = targetPos.getColumn();
			Double dX = targetX - curX;
			Double dY = targetY - curY;
			nextRow = curX + dX * (this.MAX_THROW_DIST / dist);
			nextColumn = curY + dY * (this.MAX_THROW_DIST / dist);
		}

		directive.parkLocation = new ParkLocation(nextRow, nextColumn);
		directive.parkLocation = getValidLocation(directive.parkLocation);
		return directive;
	}

	private Directive throwRandomly() {
		double randomDistance = 40.0;
		double randomAngle = 0.0;
		Directive directive = new Directive();
		List<Dog> waitingDogs = getWaitingDogs(myOwner, otherOwners);
		directive.instruction = Instruction.THROW_BALL;
		if (waitingDogs.size() > 0)
			directive.dogToPlayWith = waitingDogs.get(random.nextInt(waitingDogs.size()));
		else {
			directive.instruction = Instruction.NOTHING;
			return directive;
		}

		// randomDistance = random.nextDouble() * 40.0;
		randomAngle = Math.toRadians(random.nextDouble() * 360);
		double ballRow = myOwner.getLocation().getRow() + randomDistance * Math.sin(randomAngle);
		double ballColumn = myOwner.getLocation().getColumn() + randomDistance * Math.cos(randomAngle);
		if (ballRow < 0.0)
			ballRow = 0.0;
		if (ballRow > ParkLocation.PARK_SIZE - 1)
			ballRow = ParkLocation.PARK_SIZE - 1;
		if (ballColumn < 0.0)
			ballColumn = 0.0;
		if (ballColumn > ParkLocation.PARK_SIZE - 1)
			ballColumn = ParkLocation.PARK_SIZE - 1;
		directive.parkLocation = new ParkLocation(ballRow, ballColumn);
		return directive;
	}

	/**
	 * checks if a park location is valid if out of range, return a valid park
	 * location
	 * 
	 * @param loc ParkLocation to verify
	 */
	private ParkLocation getValidLocation(ParkLocation loc) {
		Double row = loc.getRow();
		Double col = loc.getColumn();

		row = Math.max(0.0, row);
		col = Math.max(0.0, col);
		row = Math.min(ParkLocation.PARK_SIZE - 1, row);
		col = Math.min(ParkLocation.PARK_SIZE - 1, col);

		return new ParkLocation(row, col);

	}
}