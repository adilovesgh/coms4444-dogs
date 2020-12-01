package dogs.g4;

import java.util.*;

import dogs.sim.*;
import dogs.sim.Dictionary;


public class Player extends dogs.sim.Player {

    private String firstSignal = "zythum";
    private List<Owner> coOwners = new ArrayList<>();
    private ParkLocation lastThrow = new ParkLocation();
    private Map<Owner, OwnerDistance> allLocationMap = new HashMap<>();
    private Double dividedAngle = 0.0;

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

        if (round > 0) {
            for (Owner owner : otherOwners) {
                if (owner.getCurrentSignal().equals(firstSignal)) {
                    this.coOwners.add(owner);
                }
            }
        }

        this.allLocationMap = getCircularLocations(200, 200, myOwner, 39.0, false, 1.0);
        ParkLocation finalLocation = this.allLocationMap.get(myOwner).location;

        if (finalLocation.getRow().intValue() != myOwner.getLocation().getRow().intValue() ||
                finalLocation.getColumn().intValue() != myOwner.getLocation().getColumn().intValue()){
            myDirective.instruction = Directive.Instruction.MOVE;
            myDirective.parkLocation = getMyCircularNextLocation(myOwner, finalLocation);
            return myDirective;
        }

//        List<OwnerDistance> ownersDistances = getDistances(otherOwners, myOwner);
//        Collections.sort(ownersDistances);
        //simPrinter.println(ownersDistances);

        List<String> otherSignals = getOtherOwnersSignals(otherOwners);
        List<Dog> waitingDogs = getWaitingDogs(myOwner, otherOwners);

        Comparator<Dog> bySpeed = (Dog d1, Dog d2) -> Double.compare(d1.getRunningSpeed(), d2.getRunningSpeed());
        Collections.sort(waitingDogs, bySpeed.reversed());

        if(waitingDogs.size() > 0) {
            int i = 0;
            while (waitingDogs.get(i).getExerciseTimeRemaining() == 0.0 && myOwner.hasDog(waitingDogs.get(i))) {
                if (i < waitingDogs.size()) i++;
            }
            myDirective.dogToPlayWith = waitingDogs.get(i);

            myDirective.instruction = Directive.Instruction.THROW_BALL;
//            simPrinter.println(ownersDistances.get(0).location.toString().equals(this.lastThrow.toString()));
//            simPrinter.println(ownersDistances.get(0).location.toString());
//            simPrinter.println(this.lastThrow.toString());
            this.lastThrow = getClockWiseNextLane(this.allLocationMap, myOwner, waitingDogs.get(i).getBreed());
            myDirective.parkLocation = this.lastThrow;

        }

        updateDistances(otherOwners, myOwner);

//        simPrinter.println("----------------------------");
//        for (Owner owner : this.allLocationMap.keySet()) {
//            simPrinter.println(allLocationMap.get(owner).dist);
//        }
//        simPrinter.println("----------------------------");

        return myDirective;
    }

    private ParkLocation getClockWiseNextLane(Map<Owner, OwnerDistance> owners, Owner myOwner, DogReference.Breed breed) {
        ParkLocation target = getClockWiseNext(this.allLocationMap, myOwner);

        switch (breed) {
            case LABRADOR:
                return new ParkLocation(target.getRow(), target.getColumn());
            case POODLE:
                return new ParkLocation(target.getRow(), target.getColumn());
            case SPANIEL:
                return new ParkLocation(target.getRow(), target.getColumn());
            case TERRIER:
                return new ParkLocation(target.getRow(), target.getColumn());
        }
        return target;
    }

    private ParkLocation getClockWiseNext(Map<Owner, OwnerDistance> owners, Owner myOwner) {

        Double myAngle = owners.get(myOwner).angle;

        for (OwnerDistance owner : owners.values()) {
//            simPrinter.println("--------------------------");
//            simPrinter.println(owner.angle);
//            simPrinter.println(myAngle);
//            simPrinter.println(myAngle + this.dividedAngle);
//            simPrinter.println("--------------------------");
            if (Math.round(myAngle + this.dividedAngle) == 360.0) {
                if (owner.angle == 0.0) return owner.location;
            }
            else if (Math.round(owner.angle) == Math.round(myAngle + this.dividedAngle)) {
                return owner.location;
            }
        }

        return new ParkLocation(0.0,0.0);
    }

    private Owner getOwnerWithName(Owner.OwnerName name) {
        for (Owner owner : this.allLocationMap.keySet()) {
            if (owner.getNameAsEnum() == name) return owner;
        }
        return new Owner();
    }

    private void updateDistances(List<Owner> otherOwners, Owner myOwner) {
        for (Owner owner : otherOwners) {
            Owner o = getOwnerWithName(owner.getNameAsEnum());
            this.allLocationMap.get(o).dist = getDist(owner.getLocation(), myOwner.getLocation());
        }
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

    private Map<Owner, OwnerDistance> getCircularLocations(Integer ySize, Integer xSize, Owner myOwner, Double maxDist, boolean centralized, Double margin) {
        Map<Owner, OwnerDistance> circularLocations = new HashMap<>();
        List<Owner> owners = new ArrayList<>(this.coOwners);
        owners.add(myOwner);

        Comparator<Owner> byName = (Owner o1, Owner o2) -> o1.getNameAsString().compareTo(o2.getNameAsString());
        Collections.sort(owners, byName);

        this.dividedAngle = 360.0/(owners.size());
        Double angle = 0.0;
        Double maxRadius = 0.0;
        Double radius = (maxDist/2)/Math.sin(Math.toRadians(this.dividedAngle/2));
        if (centralized) maxRadius = Math.min(ySize, xSize)/2.0;
        else maxRadius = radius + margin;

        for (Owner owner : owners) {
            Double x = maxRadius - radius*Math.cos(Math.toRadians(angle));
            Double y = maxRadius - radius*Math.sin(Math.toRadians(angle));
            circularLocations.put(owner, new OwnerDistance(owner, 0.0, new ParkLocation(x,y), angle));
            angle = angle + this.dividedAngle;
        }

        return circularLocations;
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

    private class OwnerDistance implements Comparable<OwnerDistance> {
        Owner owner;
        Double dist;
        ParkLocation location;
        Double angle;

        OwnerDistance(Owner owner, Double dist, ParkLocation location, Double angle) {
            this.owner = owner;
            this.dist = dist;
            this.location = location;
            this.angle = angle;
        }

        @Override
        public int compareTo(OwnerDistance other) {
            return Double.compare(this.dist, other.dist);
        }

        public String toString() {
            return this.owner.getNameAsString() + ": " + this.dist + ": " + this.location.toString();
        }

    }

}

