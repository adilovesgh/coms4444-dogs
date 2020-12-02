package dogs.g2; // TODO modify the package name to reflect your team

import java.util.*;
import dogs.sim.*;

public class Player extends dogs.sim.Player {

    final boolean PRINT_STATEMENTS = true;
    private boolean colab;
    private double run;
  
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
         this.colab = false;
         this.run = 0.0;
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

        if (round <=1) {
            directive.instruction = Directive.Instruction.CALL_SIGNAL;
            directive.signalWord = "two";
            return directive;
        }

        if (round <=6) {
            int count = 0;
            List<String> recievedSignals = getOtherOwnersSignals(otherOwners);
            for (String word: recievedSignals) {
                if(!word.equals("two")){
                    System.out.println("nope");
                    colab = true;
                }
                else {
                    System.out.println("yep");
                    count ++;
                }
            }

            //System.out.println(count);
            if (count >=1) {
                colab = false;
                run = 120.0;
            }
        }

        System.out.println(run + " " + colab);

        if (colab){
            ParkLocation myLocation = equidistantPtsOnCircle(myOwner, otherOwners);
            //ParkLocation myLocation = initialLocations.get(1);
            while (myOwner.getLocation().getRow() < myLocation.getRow() || myOwner.getLocation().getColumn() < myLocation.getColumn()) {
                directive.instruction = Directive.Instruction.MOVE;
                double rowDiff = Math.max(0,Math.min(myLocation.getRow() - myOwner.getLocation().getRow(), 3.5));
                double colDiff = Math.max(0,Math.min(myLocation.getColumn() - myOwner.getLocation().getColumn(), 3.5));

                directive.parkLocation = new ParkLocation(myOwner.getLocation().getRow() + rowDiff, myOwner.getLocation().getColumn() + colDiff);
                return directive;
            }

            //System.out.println(myOwner.getNameAsEnum().ordinal() + " " + myOwner.getLocation().getRow() + " " + myOwner.getLocation().getColumn());
         if(round <= 40) {
                directive.instruction = Directive.Instruction.CALL_SIGNAL;
                directive.signalWord = "here";
                return directive;
            }

            List<Dog> waitingDogs = getDogs(myOwner, otherOwners);
            //debug(myOwner, otherOwners);

            Comparator<Dog> byTime = (Dog d1, Dog d2) -> Double.compare(d1.getWaitingTimeRemaining(), d2.getWaitingTimeRemaining());
            Collections.sort(waitingDogs, byTime);

            //System.out.println("size: " + waitingDogs.size());

            //Throw Ball in the direction of other owners
            if(waitingDogs.size() > 0){
                directive.instruction = Directive.Instruction.THROW_BALL;
                directive.dogToPlayWith = waitingDogs.get(0);
                directive.parkLocation = getBallDirection(myOwner, otherOwners);

                //directive.parkLocation = otherOwners.get(randomNum).getLocation();
            } else {
                List<String> recievedSignals = getOtherOwnersSignals(otherOwners);
                directive.instruction = Directive.Instruction.NOTHING;
            }
            return directive;
        }
        else {  
            // Move to the initial position
            if (myOwner.getLocation().getRow().intValue() != initiaLocation.getRow().intValue() || 
                myOwner.getLocation().getColumn().intValue() != initiaLocation.getColumn().intValue()) {
                return moveTowardsInitialLocation(myOwner);
            }

            // Get a lit of currently waiting dogs
            ArrayList<Dog> dogs = this.getDogs(myOwner, otherOwners);

            Comparator<Dog> byTime = (Dog d1, Dog d2) -> Double.compare(d1.getWaitingTimeRemaining(), d2.getWaitingTimeRemaining());
            Collections.sort(dogs, byTime);

            // TODO: Sort dogs in decreasing speed order

            if (dogs.size() > 0) {
                directive.instruction = Directive.Instruction.THROW_BALL;
                directive.dogToPlayWith = dogs.get(0);
                directive.parkLocation = getLinearThrowLocation();
            }

            return directive;
        }
    }

    private ParkLocation getInitialLocation() {
        Double initialX = 25.0 + run;
        Double initialY = 25.0;

        Double deltaX = 39.95;
        Double deltaY = 2.1;

        if (this.myInstance % 2 == 0) {
            return new ParkLocation(initialX, initialY + deltaY*(myInstance/2));
        }
        else {
            return new ParkLocation(initialX + deltaX, initialY + deltaY*((int)(myInstance-1)/2));
        }
    }

    // Throw to the owner across yourself
    private ParkLocation getLinearThrowLocation() {
        Double deltaX = 39.95;
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

    // Randomly throw to one of the closest 3 owners across
    private ParkLocation getOffsetThrowLocation() {
        Double deltaX = 39.95;
        Double deltaY = 2.1;
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

        this.initiaLocation = this.getInitialLocation();

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

    private ParkLocation equidistantPtsOnCircle(Owner myOwner, List<Owner> otherOwners){
        //Size of the MAP is 200 x 200
        if (otherOwners.size() > 1){
            double angle = (2 * Math.PI) / (otherOwners.size() + 1);

            double radius = 39.95 / (2 * Math.sin(2 * Math.PI / (otherOwners.size() + 1)));

            double row = radius + Math.round(radius) * Math.sin(myOwner.getNameAsEnum().ordinal() * angle);
            double column = radius + Math.round(radius) * Math.cos(myOwner.getNameAsEnum().ordinal()* angle);

            //System.out.println(myOwner.getNameAsEnum().ordinal() + " " + radius + " " + angle + " " + row + " " + column);
            ParkLocation initialParkLocation = new ParkLocation(row, column);
            return initialParkLocation;
        }
        else {
            ParkLocation initialParkLocation = new ParkLocation(12.0, 12.0);
            return initialParkLocation;
        }
    }

    private ParkLocation getBallDirection(Owner myOwner, List<Owner> otherOwners){
        //Size of the MAP is 200 x 200

        double angle = (2 * Math.PI) / (otherOwners.size() + 1);

        double radius = 38.95 / (2 * Math.sin(2 * Math.PI / (otherOwners.size() + 1)));

        double row = radius + Math.round(radius) * Math.sin(((myOwner.getNameAsEnum().ordinal() + 1 ) % (otherOwners.size() + 1)) * angle);
        double column = radius + Math.round(radius) * Math.cos(((myOwner.getNameAsEnum().ordinal() + 1 ) % (otherOwners.size() + 1)) * angle);

        //System.out.println(row + " " + column);
        ParkLocation initialParkLocation = new ParkLocation(row, column);
        return initialParkLocation;
    }

    private ParkLocation getBallDirectionColab(Owner myOwner, List<Owner> otherOwners){
        ParkLocation from = myOwner.getLocation();
        for (Owner o: otherOwners) {
            ParkLocation to = o.getLocation();
            double dist = calDiff(from, to);
            if (dist < 40) {
                ParkLocation initialParkLocation = to;
                return initialParkLocation;
            }
        }

        double angle = (2 * Math.PI) / (otherOwners.size() + 1);

        double radius = 38.95 / (2 * Math.sin(2 * Math.PI / (otherOwners.size() + 1)));

        double row = radius + Math.round(radius) * Math.sin(((myOwner.getNameAsEnum().ordinal() + 1 ) % (otherOwners.size() + 1)) * angle);
        double column = radius + Math.round(radius) * Math.cos(((myOwner.getNameAsEnum().ordinal() + 1 ) % (otherOwners.size() + 1)) * angle);

        //System.out.println(row + " " + column);
        ParkLocation initialParkLocation = new ParkLocation(row, column);
        return initialParkLocation;

    }

    private double calDiff(ParkLocation from, ParkLocation to) {
        double diffX = from.getRow() - to.getRow();
        double diffY = from.getColumn() - to.getColumn();
        return Math.sqrt((Math.pow(diffX, 2) + Math.pow(diffY, 2)));
    }


    private List<String> getOtherOwnersSignals(List<Owner> otherOwners) {
        List<String> otherOwnersSignals = new ArrayList<>();
        for(Owner otherOwner : otherOwners)
            if(!otherOwner.getCurrentSignal().equals("_"))
                otherOwnersSignals.add(otherOwner.getCurrentSignal());
        return otherOwnersSignals;
    }

    private ArrayList<Dog> getDogs(Owner owner, List<Owner> otherOwners) {
        ArrayList<Dog> dogs = new ArrayList<>();
        for (Dog dog : owner.getDogs()) {
            if (dog.isWaitingForItsOwner()) {
                if (dog.getExerciseTimeRemaining() != 0.0){
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

    private void debug(Owner myOwner, List<Owner> otherOwners) {

        for(Dog dog : myOwner.getDogs()) {
            System.out.println(dog.getBreed());
            System.out.println(dog.getLocation());
            System.out.println(dog.getOwner().getNameAsEnum().ordinal());
            System.out.println(dog.isMoving());
            System.out.println(dog.getOwnerWaitingFor().getNameAsEnum().ordinal());
        }
        for(Owner otherOwner : otherOwners) {
            for(Dog dog : otherOwner.getDogs()) {
                System.out.println(dog.getBreed());
                System.out.println(dog.getLocation());
                System.out.println(dog.getOwner().getNameAsEnum().ordinal());
                System.out.println(dog.isMoving());
                System.out.println(dog.getOwnerWaitingFor().getNameAsEnum().ordinal());
            }
        }

    }
}