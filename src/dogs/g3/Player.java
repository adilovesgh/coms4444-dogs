package dogs.g3;

import dogs.g3.PlayerGraph;
import dogs.g3.PlayerGraph.GraphType;

import java.util.*;
import java.util.jar.Attributes.Name;

import javax.swing.plaf.synth.SynthSeparatorUI;

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
	private String identification_signal = "three";
	private List<Dog> myDogs;
	private Integer round;
	private Owner myOwner;
	private List<Owner> otherInstances;
	HashMap<String, ParkLocation> positions;
	private List<Dog> allDogs;
	HashMap<String, String> nextOwners;
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
		if (this.graph.graphType == GraphType.GRID) {

		} else {
			for (int i = 0; i < owners.size(); i++) {
				Owner current = owners.get(i);
				double x = center.getRow() + radius * Math.cos((2 * Math.PI * i) / owners.size());
				double y = center.getColumn() + radius * Math.sin((2 * Math.PI * i) / owners.size());
				ownerToLocation.put(current.getNameAsString(), new ParkLocation(x, y));
			}
		}
		return ownerToLocation;
	}

	/**
	 * Build a Graph with owners as nodes
	 * 
	 * @param owners list of owners
	 */
	private PlayerGraph buildPlayerGraph(List<Owner> owners) {

		PlayerGraph graph = new PlayerGraph(owners);
		// add an edge between each pair of owners within 40 meters of each other
		for (Owner o1 : owners) {
			for (Owner o2 : owners) {
				if ((o1.getNameAsEnum() != o2.getNameAsEnum()) && (distanceBetweenOwners(o1, o2) <= 40.0)) {
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
				this.otherInstances = getOtherInstancesSignals(allOwners);
				// System.out.println(this.otherInstances.size());
				List<Owner> alphabeticalOwners = sortOwnersAlphabetically(this.otherInstances);
				double radius = 40 * (this.otherInstances.size() - 1);
				radius /= (double) (2.0 * Math.PI);
				// System.out.println(radius + " " + allOwners.size());
				radius = 20;
				this.graph = buildPlayerGraph(alphabeticalOwners);
				this.graph.graphType = this.graph.getGraphType(this.otherInstances);
				// if(this.graph.graphType == GraphType.SPOKE){
				// numCenterPlayers = (int) Math.ceil((double) radius / 51.0);
				// radius = 30 * (this.otherOwners.size()-numCenterPlayers);
				// radius /= (double) (2.0 * Math.PI);
				// }
				this.center = new ParkLocation(25.0, 25.0);
				this.radius = (int) radius;
				HashMap<String, ParkLocation> currentPositions = mapOwnerToParkLocationCircle(alphabeticalOwners,
						new ParkLocation(25.0, 25.0), (int) radius);
				this.positions = currentPositions;
				simPrinter.println("calling signal...");
				directive.instruction = Instruction.CALL_SIGNAL;
				directive.signalWord = this.identification_signal;
				return directive;
			} else if (this.round == 401) {
				List<OwnerName> myOwnerInstances = new ArrayList<>();
				for (Owner me : this.otherInstances) {
					myOwnerInstances.add(me.getNameAsEnum());
				}
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

				if (dist > 30) {
					this.center = new ParkLocation(nearest.getLocation().getRow() - 45,
							nearest.getLocation().getColumn());
					this.positions = mapOwnerToParkLocationCircle(sortOwnersAlphabetically(this.otherInstances),
							this.center, this.radius);
					// System.out.println(this.center.toString());
				}
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
			List<Dog> notOwnedByMe = new ArrayList<Dog>();
			List<Dog> ownedByMe = new ArrayList<Dog>();
			for (Dog dog : waitingDogs) {
				if (!dog.getOwner().equals(this.myOwner)) {
					notOwnedByMe.add(dog);
					// simPrinter.println("found");
				} else {
					ownedByMe.add(dog);
				}
			}
			// Owner alice = null;
			// for(int i = 0; i < this.otherOwners.size(); i++){
			// if(this.otherOwners.get(i).getNameAsEnum() == OwnerName.ALICE){
			// alice = this.otherOwners.get(i);
			// }
			// }
			// System.out.println(this.graph.getConnections(alice).toString());
			List<Dog> sortedDogs = new ArrayList<>();
			for (Dog d : ownedByMe) {
				sortedDogs.add(d);
			}
			List<Dog> sortedDogsNotMine = sortDogsByRemainingWaitTime(notOwnedByMe);

			for (Dog d : sortedDogsNotMine) {
				sortedDogs.add(d);
			}
			// out.println(waitingDogs.size() + " " + sortedDogs.size() + " " +
			// sortedDogsNotMine.size() + " " + notOwnedByMe.size() + " " +
			// ownedByMe.size());
			if (!sortedDogs.isEmpty()) {
				directive.instruction = Instruction.THROW_BALL;
				directive.dogToPlayWith = sortedDogs.get(0);
				List<OwnerName> neighborNames = this.graph.getConnections(myOwner);

				// throw the ball toward the least occupied neighbor
				List<Owner> neighbors = new ArrayList<>();
				for (Owner owner : this.otherOwners) {
					if (neighborNames.contains(owner.getNameAsEnum())) {
						neighbors.add(owner);
					}
				}
				Owner requiredOwner = getLeastBusyNeighbor(neighbors, this.allDogs);
				if(requiredOwner == null){
					Directive d = throwRandomly();
					//System.out.println(d.instruction.toString() + " " + d.parkLocation.toString());
					return throwRandomly();
				}
				OwnerName throwToOwnerName = requiredOwner.getNameAsEnum();

				/*
				 * Owner requiredOwner = null; for (Owner throwOwner: this.otherOwners) { if
				 * (throwOwner.getNameAsEnum() == throwToOwnerName) { requiredOwner =
				 * throwOwner; break; } }
				 */

				Double ballRow = requiredOwner.getLocation().getRow();
				Double ballColumn = requiredOwner.getLocation().getColumn();
				Random r = new Random();
				count += 1;
				// simPrinter.println(myOwner.getNameAsString() + " " + sortedDogs.size());
				// simPrinter.println(throwToOwnerName + " from " + myOwner.getNameAsString());
				directive.parkLocation = new ParkLocation(ballRow, ballColumn);
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

	private List<String> getOtherOwnersSignals(List<Owner> otherOwners) {
		List<String> otherOwnersSignals = new ArrayList<>();
		for (Owner otherOwner : otherOwners)
			if (!otherOwner.getCurrentSignal().equals("_"))
				otherOwnersSignals.add(otherOwner.getCurrentSignal());
		return otherOwnersSignals;
	}

	private List<Owner> getOtherInstancesSignals(List<Owner> otherOwners) {
		List<Owner> otherInstances = new ArrayList<>();
		for (Owner otherOwner : otherOwners) {
			if (otherOwner.getCurrentSignal().equals("three")) {
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
					// simPrinter.println("Found Other Dog Thats Not Ours");
					count += 1;
				}
			}
		}
		// System.out.println(myOwner.getNameAsString() + " " + count);
		return waitingDogs;
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

	//calculate the distance between park locations
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
	private Owner getLeastBusyNeighbor(List<Owner> neighbors, List<Dog> allDogs) {
		HashMap<Owner, Integer> busyMap = new HashMap<Owner, Integer>();
		for (Owner owner : neighbors) {
			busyMap.put(owner, 0);
		}

		// loop through all dogs to check if they are heading to/waiting for someone
		for (Dog dog : allDogs) {
			if (dog.isWaitingForPerson()) {
				Owner ownerWaited = dog.getOwnerWaitingFor();
				if (neighbors.contains(ownerWaited)) {
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
	* throw the ball to a specified owner
	* if the owner is more than 40 meters away, throw toward the direction for 40m
	* @param targetOwner	the owner to throw the ball to
	**/
	private Directive throwToOwner(Owner myOwner, Owner targetOwner){
		ParkLocation targetLoc = targetOwner.getLocation();
		return throwToLoc(myOwner, targetLoc);
	}

	/**
	* throw the ball to a specified target Park location
	* if the location is more than 40m away, throw toward the direction for 40m
	* @param targetLoc	the target location to throw the ball to
	**/
	private Directive throwToLoc(Owner myOwner, ParkLocation targetLoc){
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
			ballRow = curX + dX * (this.MAX_THROW_DIST/dist);
			ballColumn = curY + dY * (this.MAX_THROW_DIST/dist); 
		}

		directive.parkLocation = new ParkLocation(ballRow, ballColumn);
		directive.parkLocation = getValidLocation(directive.parkLocation);
		return directive;
	}

	/**
	*
	*
	**/
	private Directive moveToPos(Owner myOwner, ParkLocation targetPos){
		Directive directive = new Directive();
		directive.instruction = Instruction.MOVE;
		Double nextRow, nextColumn;

		ParkLocation myLoc = myOwner.getLocation();
		Double dist = distanceBetweenLocs(myLoc, targetPos);
		if (dist <= this.MAX_MOVE_DIST) {
			nextRow = targetPos.getRow();
			nextColumn = targetPos.getColumn();
		} else { //if targetPos is more than 5m away, move toward the target position for 5m
			Double curX = myLoc.getRow();
			Double curY = myLoc.getColumn();
			Double targetX = targetPos.getRow();
			Double targetY = targetPos.getColumn();
			Double dX = targetX - curX;
			Double dY = targetY - curY;
			nextRow = curX + dX * (this.MAX_THROW_DIST/dist);
			nextColumn = curY + dY * (this.MAX_THROW_DIST/dist); 
		}

		directive.parkLocation = new ParkLocation(nextRow, nextColumn);
		directive.parkLocation = getValidLocation(directive.parkLocation);
		return directive;
	}

	private Directive throwRandomly(){
		double randomDistance = 40.0;
		double randomAngle = 0.0;
		Directive directive = new Directive();
		List<Dog> waitingDogs = getWaitingDogs(myOwner, otherOwners);
		directive.instruction = Instruction.THROW_BALL;
		if(waitingDogs.size() > 0)
			directive.dogToPlayWith = waitingDogs.get(random.nextInt(waitingDogs.size()));
		else {
			directive.instruction = Instruction.NOTHING;
			return directive;
		}
		
		//randomDistance = random.nextDouble() * 40.0;
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
		return directive;
	}

	/**
	* checks if a park location is valid
	* if out of range, return a valid park location
	* @param loc 	ParkLocation to verify
	*/
	private ParkLocation getValidLocation(ParkLocation loc){
		Double row = loc.getRow();
		Double col = loc.getColumn();
		
		row = Math.max(0.0, row);
		col = Math.max(0.0, col);
		row = Math.min(ParkLocation.PARK_SIZE - 1, row);
		col = Math.min(ParkLocation.PARK_SIZE - 1, col);

		return new ParkLocation(row, col);

	}
}