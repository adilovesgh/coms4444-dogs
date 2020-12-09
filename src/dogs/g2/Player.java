package dogs.g2;

import java.util.*;
import dogs.sim.*;
import java.lang.Math;

public class Player extends dogs.sim.Player {

    private Double globalDeltaX = 39.95; // 39.95
    private Double polygonSide = 39.95;
    private Double circleCenterX = 100.0;
    private Double circleCenterY = 100.0;

    private double myCircleIndex;
    private double nextPersonIndex;
    private ParkLocation nextPersonLocation;

    final private boolean PRINT_STATEMENTS = true;
    final private String g2InitialSignal = "two";
    final private String g1InitialSignal = "papaya";
    final private String g3InitialSignal = "three";
    final private String g4InitialSignal = "zythum";
    final private String g5InitialSignal = "Zyzzogeton";

    private List<Owner> myOwners = new ArrayList<>();
    private List<Owner> g1Owners = new ArrayList<>();
    private List<Owner> g3Owners = new ArrayList<>();
    private List<Owner> g4Owners = new ArrayList<>();
    private List<Owner> g5Owners = new ArrayList<>();

    // Contains the owners to be arranged in a circle
    private ArrayList<Owner> circleOwners = new ArrayList<>();


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

        if (round == 1) {
            directive.signalWord = this.g2InitialSignal;
            directive.instruction = Directive.Instruction.CALL_SIGNAL;
            return directive;
        }

        // Check what groups are present
        if (round == 6) {
            for (Owner owner : otherOwners) {
                if (owner.getCurrentSignal().equals(g2InitialSignal)) {
                    this.myOwners.add(owner);
                }
                if (owner.getCurrentSignal().equals(this.g1InitialSignal)) {
                    this.g1Owners.add(owner);
                }
                if (owner.getCurrentSignal().equals(this.g3InitialSignal)) {
                    this.g3Owners.add(owner);
                }
                if (owner.getCurrentSignal().equals(this.g4InitialSignal)) {
                    this.g4Owners.add(owner);
                }
                if (owner.getCurrentSignal().equals(this.g5InitialSignal)) {
                    this.g5Owners.add(owner);
                }
            }

            // Add own players to the cirlceOwners list
            circleOwners.add(myOwner);
            circleOwners.addAll(myOwners);

            // Collaborate with G4 if no other teams are present
            if (g4Owners.size() > 0 && g1Owners.isEmpty() && g3Owners.isEmpty() && g5Owners.isEmpty()) {
                circleOwners.addAll(g4Owners);
            }
        
            // Get the initial player location
            this.initiaLocation = this.getCircularInitialLocation(circleCenterX, circleCenterY, myOwner);
        }


        // Move to the initial position
        if (!myOwner.getLocation().getRow().equals(initiaLocation.getRow()) || 
            !myOwner.getLocation().getColumn().equals(initiaLocation.getColumn())) {
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
            directive.parkLocation = getCircularThrowLocation(myOwner);
        }

        return directive;
    }



    //************************** Methods to get the initial position of the player **********************************/

    // Arrange own players in pairs
    private ParkLocation getLinearInitialLocation() {
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

    // Arrange players in a circle
    private ParkLocation getCircularInitialLocation(double centerX, double centerY, Owner myOwner) {

        double n = circleOwners.size();

        // If more than 15 players, make 2 circles
        if (n > 15) {
            // Sort circle owners by name
            Comparator<Owner> byName = (Owner d1, Owner d2) -> (d1.getNameAsString().compareTo(d2.getNameAsString()));
            Collections.sort(circleOwners, byName);

            double n2 = 5;
            double n1 = n - n2;

            double radius1 = this.getPolygonRadius(this.polygonSide, n1);
            double radius2 = this.getPolygonRadius(this.polygonSide, n2);
            double angle1 = 360 / n1;
            double angle2 = 360 / n2;


            ArrayList<Owner> owners1 = new ArrayList(circleOwners.subList(0, (int)n1));
            ArrayList<Owner> owners2 = new ArrayList(circleOwners.subList((int)n1, (int)n));

            double myCircleIndex1 = owners1.indexOf(myOwner);
            double myCircleIndex2 = owners2.indexOf(myOwner);

            double myCircleIndex = 0;
            double angle = 0;
            double radius = 0;

            if (myCircleIndex1 != -1) {
                myCircleIndex = myCircleIndex1;
                angle = angle1;
                radius = radius1;
                n = n1;
            }
            else if (myCircleIndex2 != -1) {
                myCircleIndex = myCircleIndex2;
                angle = angle2;
                radius = radius2;
                n = n2;
            } else {
                print("ERROR: Unexpected behavior in getCircularInitialLocation()");
            }

            this.myCircleIndex = myCircleIndex;
            this.nextPersonIndex = (myCircleIndex + 1) % n;

            Double nextX = centerX + Math.cos(Math.toRadians(180 + nextPersonIndex * angle)) * radius;
            Double nextY = centerY + Math.sin(Math.toRadians(180 + nextPersonIndex * angle)) * radius;
            this.nextPersonLocation = new ParkLocation(nextX, nextY);

            // Get initial location on circumference
            Double myX;
            Double myY;
            myX = centerX + Math.cos(Math.toRadians(180 + myCircleIndex * angle)) * radius;
            myY = centerY + Math.sin(Math.toRadians(180 + myCircleIndex * angle)) * radius;

            print("" + myX + ", " + myY);
            print("" + nextX + ", " + nextY);

            return new ParkLocation(myX, myY);
        }
        else {
            double radius = this.getPolygonRadius(this.polygonSide, n);

            // Sort circle owners by name
            Comparator<Owner> byName = (Owner d1, Owner d2) -> (d1.getNameAsString().compareTo(d2.getNameAsString()));
            Collections.sort(circleOwners, byName);
            double angle = 360 / n;
    
            double myCircleIndex = circleOwners.indexOf(myOwner);
            this.myCircleIndex = myCircleIndex;
            this.nextPersonIndex = (myCircleIndex + 1) % n;

            Double nextX = centerX + Math.cos(Math.toRadians(180 + nextPersonIndex * angle)) * radius;
            Double nextY = centerY + Math.sin(Math.toRadians(180 + nextPersonIndex * angle)) * radius;
            this.nextPersonLocation = new ParkLocation(nextX, nextY);

            // Get initial location on circumference
            Double myX;
            Double myY;
            myX = centerX + Math.cos(Math.toRadians(180 + myCircleIndex * angle)) * radius;
            myY = centerY + Math.sin(Math.toRadians(180 + myCircleIndex * angle)) * radius;

            print("" + myX + ", " + myY);
            print("" + nextX + ", " + nextY);

            return new ParkLocation(myX, myY);
        }
    }


    //************************** Methods to get the next throw location **********************************/

    private ParkLocation getCircularThrowLocation(Owner myOwner) {
        print ("=============  " + computeDistance(myOwner.getLocation(), this.nextPersonLocation));

        return this.nextPersonLocation;
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

    //************************** Helper methods **********************************/

    private double computeDistance(ParkLocation l1, ParkLocation l2) {
        return Math.sqrt(Math.pow(l1.getColumn() - l2.getColumn(), 2) + Math.pow(l1.getRow() - l2.getRow(), 2));
    }

    private double getPolygonRadius(double sideLength, double n) {
        return sideLength / (2*Math.sin(Math.toRadians(180/n)));
    }


}