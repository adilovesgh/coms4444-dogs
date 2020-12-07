package dogs.g2; // TODO modify the package name to reflect your team

import java.util.*;
import dogs.sim.*;

public class Player extends dogs.sim.Player {

    final boolean PRINT_STATEMENTS = true;

    private void print(String s) {
        if (PRINT_STATEMENTS) {
            System.out.println(s);
        }
    }

    private static int instanceCount = 0;
    private int myInstance = 0;
    private ParkLocation initiaLocation;
	
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
         this.myInstance = this.instanceCount;
         this.instanceCount += 1;
         this.initiaLocation = this.getInitialLocation();
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

        // Move to the initial position
        if (myOwner.getLocation().getRow().intValue() != initiaLocation.getRow().intValue() || 
            myOwner.getLocation().getColumn().intValue() != initiaLocation.getColumn().intValue()) {
            return moveTowardsInitialLocation(myOwner);
        }

        // Get a lit of currently waiting dogs
        ArrayList<Dog> dogs = this.getDogs(myOwner, otherOwners);

        Comparator<Dog> byTime = (Dog d1, Dog d2) -> (d1.getWaitingTimeRemaining() == d2.getWaitingTimeRemaining() ? Double.compare(d1.getRunningSpeed(), d2.getRunningSpeed()) : Double.compare(d1.getWaitingTimeRemaining(), d2.getWaitingTimeRemaining()));
        //Comparator<Dog> byTime = (Dog d1, Dog d2) -> (Double.compare(d1.getRunningSpeed(), d2.getRunningSpeed()));

        Collections.sort(dogs, byTime);

        // TODO: Sort dogs in decreasing speed order

        if (dogs.size() > 0) {
            directive.instruction = Directive.Instruction.THROW_BALL;
            directive.dogToPlayWith = dogs.get(0);
            directive.parkLocation = getLinearThrowLocation();
        }

        return directive;
    }

    private Double globalDeltaX = 39.95; // 39.95

    private ParkLocation getInitialLocation() {
        Double initialX = 1.0;
        Double initialY = 1.0;

        Double deltaX = globalDeltaX;
        Double deltaY = 3.0;

        if (this.myInstance % 2 == 0) {
            return new ParkLocation(initialX, initialY + deltaY*(myInstance/2));
        }
        else {
            return new ParkLocation(initialX + deltaX, initialY + deltaY*((int)(myInstance-1)/2));
        }
    }

    // Throw to the owner across yourself
    private ParkLocation getLinearThrowLocation() {
        Double deltaX = globalDeltaX;
        Double deltaY = 1.5;
        int[] offsets = {-1, 0, 1};

        ParkLocation parkLocation;
        int offset = getRandom(offsets);

        if (this.myInstance % 2 == 0) {
            parkLocation = new ParkLocation(this.initiaLocation.getRow() + deltaX, this.initiaLocation.getColumn() + deltaY*offset);
        }
        else {
            parkLocation = new ParkLocation(this.initiaLocation.getRow() - deltaX, this.initiaLocation.getColumn() + deltaY*offset);
        }
        return parkLocation;
    }

    // Randomly throw to one of the closest 3 owners across
    private ParkLocation getOffsetThrowLocation() {
        Double deltaX = globalDeltaX;
        Double deltaY = 1.1;
        int[] offsets = {-1, 0, 1};

        ParkLocation parkLocation;
        int offset = getRandom(offsets);

        if (this.myInstance % 2 == 0) {
            parkLocation = new ParkLocation(this.initiaLocation.getRow() + deltaX, this.initiaLocation.getColumn() + deltaY*offset);
        }
        else {
            parkLocation = new ParkLocation(this.initiaLocation.getRow() - deltaX, this.initiaLocation.getColumn() + deltaY*offset);
        }
        return parkLocation;
    }

    private int getRandom(int[] array) {
        int rand = new Random().nextInt(array.length);
        return array[rand];
    }

    private Directive moveTowardsInitialLocation(Owner owner) {
        Directive directive = new Directive();
        directive.instruction = Directive.Instruction.MOVE;

        Double deltaRow = this.initiaLocation.getRow() - owner.getLocation().getRow();
        if (deltaRow > 0) {
            directive.parkLocation = new ParkLocation(owner.getLocation().getRow() + Math.min(5.0, deltaRow), owner.getLocation().getColumn());
            return directive;
        }
        
        Double deltaCol = this.initiaLocation.getColumn() - owner.getLocation().getColumn();
        if (deltaCol > 0) {
            directive.parkLocation = new ParkLocation(owner.getLocation().getRow(), owner.getLocation().getColumn() + Math.min(5.0, deltaCol));
            return directive;
        }

        System.out.println(owner.getLocationAsString());
        System.out.println(this.initiaLocation.toString());
        print("G2 WARNING: Unexpected behavior in moveTowardsInitialLocation().");
        return directive;
    }

    private ArrayList<Dog> getDogs(Owner owner, List<Owner> otherOwners) {
        ArrayList<Dog> dogs = new ArrayList<>();
        for (Dog dog : owner.getDogs()) {
            if (dog.isWaitingForItsOwner()) {
                if (dog.getExerciseTimeRemaining() > 0) {
                    dogs.add(dog);
                }
            }
        }
        for (Owner otherOwner : otherOwners) {
            for (Dog dog : otherOwner.getDogs()) {
                if (dog.isWaitingForOwner(owner)) {
                    dogs.add(dog);
                }
            }
        }
        return dogs;
    }

}