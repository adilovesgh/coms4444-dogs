package dogs.g4; // TODO modify the package name to reflect your team

import java.util.*;

import dogs.sim.*;
import dogs.sim.Dictionary;
import dogs.sim.Owner.OwnerName;


public class Player extends dogs.sim.Player {

    private String firstSignal = "zythum";
    //private List<Owner> coOwners = new ArrayList<>();
    // private List<Owner> collaboraters new ArrayList<>();
    //private List<Owner> g1Owners = new ArrayList<>();
    //private List<Owner> g2Owners = new ArrayList<>();
    //private List<Owner> g3Owners = new ArrayList<>();
    //private List<Owner> g4Owners = new ArrayList<>();
    //private List<Owner> g5Owners = new ArrayList<>();
    
    private List<OwnerName> coOwners = new ArrayList<>();
    // private List<Owner> collaboraters new ArrayList<>();
    private List<OwnerName> g1Owners = new ArrayList<>();
    private List<OwnerName> g2Owners = new ArrayList<>();
    private List<OwnerName> g3Owners = new ArrayList<>();
    private List<OwnerName> g4Owners = new ArrayList<>();
    private List<OwnerName> g5Owners = new ArrayList<>();
    // private List<Owner> unknownOwners = new ArrayList<>();




    HashMap<String, Owner> collaboraters = new HashMap<String, Owner>();//Creating HashMap   
	
    /**
     * Player constructor
     *
     * @param rounds           number of rounds
     * @param numDogsPerOwner  number of dogs per owner
     * @param numOwners	       number of owners
     * @param seed             random seed
     * @param simPrinter       simulation printer
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
        Directive myDirective = new Directive();
        

        if (round == 1) {
            myDirective.signalWord = firstSignal;
            myDirective.instruction = Directive.Instruction.CALL_SIGNAL;
            return myDirective;
        }

        if (round > 0 && coOwners.size() == 0) {
        	
            for (Owner owner : otherOwners) {
            	//System.out.println("round 2, owner: "+ owner.getNameAsString()+ "; call: "+owner.getCurrentSignal());
                //if (owner.getCurrentSignal().equals(firstSignal)) {
                //    this.coOwners.add(owner);
                //}
                if (owner.getCurrentSignal().equals("papaya")) {
                    this.g1Owners.add(owner.getNameAsEnum());
                    this.coOwners.add(owner.getNameAsEnum());
                }
                if (owner.getCurrentSignal().equals("two")) {
                    this.g2Owners.add(owner.getNameAsEnum());
                    this.coOwners.add(owner.getNameAsEnum());
                }
                if (owner.getCurrentSignal().equals("three")) {
                    this.g3Owners.add(owner.getNameAsEnum());
                    this.coOwners.add(owner.getNameAsEnum());
                }
                if (owner.getCurrentSignal().equals("zythum")) {
                    this.g4Owners.add(owner.getNameAsEnum());
                    this.coOwners.add(owner.getNameAsEnum());
                }
                if (owner.getCurrentSignal().equals("cuprodescloizite")) {
                    this.g5Owners.add(owner.getNameAsEnum());
                    this.coOwners.add(owner.getNameAsEnum());
                }
            }
            
            
            
        }
        //System.out.println("coOwners:");
        //for (OwnerName o: this.coOwners) {
        //	System.out.println(o);
        //}
        Map<OwnerName, ParkLocation> locations = getCircularLocations(200, 200, myOwner, 20.0);
        ParkLocation finalLocation = locations.get(myOwner.getNameAsEnum());

        if (finalLocation.getRow().intValue() != myOwner.getLocation().getRow().intValue() ||
                finalLocation.getColumn().intValue() != myOwner.getLocation().getColumn().intValue()){
            myDirective.instruction = Directive.Instruction.MOVE;
            myDirective.parkLocation = getMyCircularNextLocation(myOwner, finalLocation);
            return myDirective;
        }
        

        List<OwnerDogs> ownersDogs = getDistances(myOwner, otherOwners);
        Collections.sort(ownersDogs);
        Owner throwto = getOwnerThrowTo(ownersDogs);
        //simPrinter.println(ownersDistances);

        List<String> otherSignals = getOtherOwnersSignals(otherOwners);
        List<Dog> waitingDogs = getWaitingDogs(myOwner, otherOwners);

        //Comparator<Dog> bySpeed = (Dog d1, Dog d2) -> Double.compare(d1.getRunningSpeed(), d2.getRunningSpeed());
        Comparator<Dog> byWaitingTime = (Dog d1, Dog d2) -> Double.compare(d1.getWaitingTimeRemaining(), d2.getWaitingTimeRemaining());
        //Collections.sort(waitingDogs, bySpeed.reversed());
        Collections.sort(waitingDogs, byWaitingTime);
        //System.out.println("Owner: "+myOwner.getNameAsString() + "; waiting: "+waitingDogs.size());
        //for (Dog d: waitingDogs) {
        //	System.out.println(d.getWaitingTimeRemaining());
        //}

        if(waitingDogs.size() > 0) {
            myDirective.instruction = Directive.Instruction.THROW_BALL;
            myDirective.dogToPlayWith = waitingDogs.get(0);
            if (throwto != null) {
            	myDirective.parkLocation = throwto.getLocation();
            } else {
            	//System.out.println("no owner in distance");
            	myDirective.parkLocation = ownersDogs.get(0).location;
            }
            
        }

        return myDirective;
    }
    
    private Owner getOwnerThrowTo(List<OwnerDogs> ownersDogs) {
    	for (OwnerDogs od: ownersDogs) {
    		//System.out.println(od.owner.getNameAsString() + ": "+ od.dist);
    		if (od.dist <= 40.0 + 1.0) {
    			return od.owner;
    		}
    	}
    	return null;
    }
    
    private int getNumWaitingDogsForOwner(Owner owner, List<Owner> otherOwners) {
        int num = 0;
        for (Owner o : otherOwners) {
        	if (this.coOwners.contains(o.getNameAsEnum())) {
        		for (Dog dog : o.getDogs()) {
                    if (dog.isWaitingForOwner(owner)) {
                    	num++;
                    }      
                }
        	}
        }
        return num;
    }

    private List<OwnerDogs> getDistances(Owner myOwner, List<Owner> otherOwners) {
        List<OwnerDogs> coDistances = new ArrayList<>();

        for (Owner owner : otherOwners) {
        	if (coOwners.contains(owner.getNameAsEnum())) {
        		int num = getNumWaitingDogsForOwner(owner, otherOwners);
                coDistances.add(new OwnerDogs(owner, getDist(owner.getLocation(), myOwner.getLocation()), owner.getLocation(), num));
        	}
        	
        }

        return coDistances;
    }

    private Double getDist(ParkLocation l1, ParkLocation l2) {
        Double x1 = l1.getRow();
        Double y1 = l1.getColumn();
        Double x2 = l2.getRow();
        Double y2 = l2.getColumn();

        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }

    private ParkLocation getMyCircularNextLocation(Owner myOwner, ParkLocation finalLocation) {
        Double x = myOwner.getLocation().getRow();
        Double y = myOwner.getLocation().getColumn();

//        simPrinter.println(myOwner.getNameAsString());
//        simPrinter.println(myOwner.getLocationAsString());
//        simPrinter.println(finalLocation.toString());

        if (finalLocation.getRow().intValue() != x.intValue()) {
            if (finalLocation.getRow() >= x + 5) {
                x = x + 5;
            }
            else if (finalLocation.getRow() < x + 5) {
                x = x + Math.abs(finalLocation.getRow() - x);
            }
        }
        else if (finalLocation.getColumn().intValue() != y.intValue()) {
            if (finalLocation.getColumn() >= y + 5) {
                y = y + 5;
            }
            else if (finalLocation.getColumn() < y + 5) {
                y = y + Math.abs(finalLocation.getColumn() - y);
            }
        }

        return new ParkLocation(x, y);
    }

    private Map<OwnerName, ParkLocation> getCircularLocations(Integer ySize, Integer xSize, Owner myOwner, Double maxDist) {
        Map<OwnerName, ParkLocation> circularLocations = new HashMap<>();
        List<OwnerName> owners = new ArrayList<>(this.coOwners);
        owners.add(myOwner.getNameAsEnum());

        //Comparator<Owner> byName = (Owner o1, Owner o2) -> o1.getNameAsString().compareTo(o2.getNameAsString());
        Comparator<OwnerName> byName = (OwnerName o1, OwnerName o2) -> o1.compareTo(o2);
        Collections.sort(owners, byName);

        Double dividedAngle = 360.0/(owners.size());
        Double angle = 0.0;
        Double maxRadius = Math.min(ySize, xSize)/2.0;
        Double radius = (maxDist/2)/Math.sin(Math.toRadians(dividedAngle/2));

        for (OwnerName owner : owners) {
            Double x = maxRadius - radius*Math.cos(Math.toRadians(angle));
            Double y = maxRadius - radius*Math.sin(Math.toRadians(angle));
            circularLocations.put(owner, new ParkLocation(x,y));
            angle = angle + dividedAngle;
        }

        return circularLocations;
    }

    private Map<Owner, ParkLocation> getIdealLocations(List<Owner> coOwners, Integer ySize, Integer xSize, Double dLimit, Owner myOwner) {
        Map<Owner, ParkLocation> idealLocations = new HashMap<>();
        coOwners.add(myOwner);

        List<String> coOwnersNames = new ArrayList<>();
        for (Owner owner : coOwners) {
            coOwnersNames.add(owner.getNameAsString());
        }

        Collections.sort(coOwnersNames);


        return idealLocations;
    }

    private List<String> getOtherOwnersSignals(List<Owner> otherOwners) {
        List<String> otherOwnersSignals = new ArrayList<>();
        for (Owner otherOwner : otherOwners)
            if (!otherOwner.getCurrentSignal().equals("_"))
                otherOwnersSignals.add(otherOwner.getCurrentSignal());
        return otherOwnersSignals;
    }

    private List<Dog> getWaitingDogs(Owner myOwner, List<Owner> otherOwners) {
        List<Dog> waitingDogs = new ArrayList<>();
        for (Dog dog : myOwner.getDogs()) {
            if (dog.isWaitingForItsOwner())
                waitingDogs.add(dog);
        }
        for (Owner otherOwner : otherOwners) {
            for (Dog dog : otherOwner.getDogs()) {
                if (dog.isWaitingForOwner(myOwner))
                    waitingDogs.add(dog);
            }
        }
        return waitingDogs;
    }

    private class OwnerDogs implements Comparable<OwnerDogs> {
        Owner owner;
        Double dist;
        ParkLocation location;
        int waitingDogs;

        OwnerDogs(Owner owner, Double dist, ParkLocation location, int waitingDogs) {
            this.owner = owner;
            this.dist = dist;
            this.location = location;
            this.waitingDogs = waitingDogs;
        }

        @Override
        public int compareTo(OwnerDogs other) {
            return Integer.compare(this.waitingDogs, other.waitingDogs);
        }

        public String toString() {
            return this.owner.getNameAsString() + ": " + this.dist + ": " + this.location.toString();
        }

    }

}

