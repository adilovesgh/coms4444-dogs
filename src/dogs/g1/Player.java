package dogs.g1;

import java.util.*;

// import org.graalvm.compiler.lir.aarch64.AArch64Unary.MemoryOp;

import java.lang.Math;

import dogs.sim.Directive;
import dogs.sim.Dog;
import dogs.sim.Owner;
import dogs.sim.Owner.OwnerName;
import dogs.sim.SimPrinter;
import dogs.sim.ParkLocation;
import dogs.sim.Directive.Instruction;


public class Player extends dogs.sim.Player {
    private List<ParkLocation> path;
    private Set<Owner> randos; 
    private List<Owner> nonRandos;
    private final Double MAX_THROW_DIST = 40.0;
    private HashMap<Owner, ParkLocation> ownerLocations;
    private HashMap<Owner, ParkLocation> trueLocations;
    private List<Owner> ownerCycle;
	
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
        this.path = new ArrayList<>();
        this.randos = new HashSet<Owner>();
        this.nonRandos = new ArrayList<>();
        this.ownerLocations = new HashMap<Owner, ParkLocation>();
        this.trueLocations = new HashMap<Owner, ParkLocation>();
        this.ownerCycle = new ArrayList<Owner>();
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
        if (round == 1) { // gets starting location, calls out name to find random players
            directive.instruction = Instruction.CALL_SIGNAL;
            directive.signalWord = myOwner.getNameAsString();
            simPrinter.println(myOwner.getNameAsString() + " called out " + myOwner.getNameAsString() + " in round " + round);
            return directive;
        }
        else if (round == 6) { // fills ups randos to spot the random player, make starting config with nonrandom players
            findRandos(myOwner, otherOwners);
            updateLocations();
            this.path = shortestPath(ownerLocations.get(myOwner));
            simPrinter.println("It will take "  + myOwner.getNameAsString() + " " + this.path.size() + " rounds to get to target location");
        }
        int roundWithAction = (round-1)/5 - 1;
        // TODO: if current location is not as expected? for flexibility in getting to new shape 
        if (roundWithAction < this.path.size()) {
            directive.instruction = Instruction.MOVE;
            directive.parkLocation = this.path.get(roundWithAction);
            // maybe add a message for other players saying we're still moving into position?
            simPrinter.println(myOwner.getNameAsString() + " is moving to target location to " + this.path.get(roundWithAction) + "\n");
            return directive;
        }

        simPrinter.println(myOwner.getNameAsString() + " is at target location");
        // stay away from the random
        updateRandos(myOwner, otherOwners);

        // keeps track of random player's locations 
        for (Owner o : randos) {
            ownerLocations.put(o, o.getLocation());
            trueLocations.put(o, o.getLocation());
            simPrinter.println("RANDOM: " + o.getNameAsString() + "'s position: " + o.getLocationAsString());
        }
        for (Owner o : nonRandos) {
            trueLocations.put(o, o.getLocation());
        }

        float nodeSeparation = 2.0f;
        return throwToNext(myOwner, otherOwners, nodeSeparation);
    }

    /** 
     *  Throws to the next owner in the geometry OR an owner within 40 m (hopefully not random)
     *  @param A                myOwner, the thrower of the ball 
     *  @param nodeSeparation   how far in between each circle on the top of the isosceles triangle 
     *                          Min: 0      Max: 2 
     */
    private Directive throwToNext(Owner A, List<Owner> otherOwners, float nodeSeparation) {
        // TODO: keep track of throws so that we don't keep throwing to the same dog 
        Owner B = new Owner(); // the next owner to trow to, determined later by who is available 

        Directive ret = new Directive();
        ret.instruction = Instruction.THROW_BALL;

        // TODO: Sharon --> Currently prioritizes dogs based on how much time they have left to wait 
        // TODO: Joseph --> pick which Node to throw to (0 = right at next owner, 3 = farthest possible from owner)
        List<Dog> waitingDogs = getWaitingDogs(A, otherOwners);
        // simPrinter.println();
        // for (Dog d : waitingDogs) 
        //     simPrinter.println("Dog " + d.getBreed() + " has " + d.getWaitingTimeRemaining() + " wait time remaining");
        // simPrinter.println();

        if (waitingDogs.size() >= 1)
            ret.dogToPlayWith = waitingDogs.get(0); 
        else 
            simPrinter.println("There are no waiting dogs for " + A.getNameAsString());

        int N = 3; 
        float offset = N + N*nodeSeparation; // distance between node and next Owner (top of isosceles)
        
        boolean foundTarget = false; 
        if (nonRandos.size() >= 2) { // we can throw to someone else that's smart!  
            // pick the next person in the cycle, ensuring that they fall within 40 meters

            for (Owner o : nonRandos) {
                simPrinter.println("Owner: " + o.getNameAsString() + "\tLocation: " + o.getLocation());
            }

            B = ownerCycle.get((findOwnerIndex(ownerCycle,A) + 1) % ownerCycle.size());
            
            if (distanceBetweenTwoPoints(A.getLocation(), B.getLocation()) > 40) {
                for (Owner o : nonRandos) {
                    if (o == A) continue; 
                    B = o; 
                    if (distanceBetweenTwoPoints(A.getLocation(), B.getLocation()) <= 40) {
                        foundTarget = true;
                        break;
                    }
                }
            }
            else 
                foundTarget = true;
        }
        else if (randos.size() >= 1 && !(foundTarget)) { // we have to trow to a rando :/ 
            // pick the next available person within 40 meters 
            for (Owner o : randos) {
                B = o; 
                if (distanceBetweenTwoPoints(A.getLocation(), B.getLocation()) <= 40) break;
            }
        }
        else { // case where nobody else is within the range
            // TODO: throw in a random direction just to get some exercise? 
            ret.instruction = Instruction.NOTHING;
            return ret;
        }

        Double Ax = A.getLocation().getRow();
        Double Ay = A.getLocation().getColumn();
        Double Bx = B.getLocation().getRow();
        Double By = B.getLocation().getColumn();
        ParkLocation newB = new ParkLocation(Bx, By);

        // distance between thrower and receiver (matching sides of isosceles), maximum = 40m 
        double throwDistance = distanceBetweenTwoPoints(A.getLocation(), B.getLocation()); 
        double theta = -1 * Math.asin((offset/2)/throwDistance) * 2; 

        // Apply translation, rotation, translation to rotate about non-origin
        newB.setRow(Ax + (Bx-Ax)*Math.cos(theta) - (By-Ay)*Math.sin(theta));
        newB.setColumn(Ay + (Bx-Ax)*Math.sin(theta) + (By-Ay)*Math.cos(theta));

        simPrinter.println("\nThrowing from " + A.getNameAsString() + " to " + B.getNameAsString());
        simPrinter.println("Point A: " + A.getLocation() + "\tPoint B: " + newB + "\n");
        ret.parkLocation = newB;
        return ret;
    }

    private int findOwnerIndex(List<Owner> haystack, Owner needle) {
        for (int i = 0; i < haystack.size(); i++) {
            if (haystack.get(i).getNameAsString().equals(needle.getNameAsString()))
                return i;
        }
        return -1; 
    }

    private Owner findOwner(List<Owner> haystack, Owner needle) {
        for (int i = 0; i < haystack.size(); i++) {
            if (haystack.get(i).getNameAsString().equals(needle.getNameAsString()))
                return haystack.get(i);
        }
        return null; 
    }

    private void updateRandos(Owner me, List<Owner> otherOwners) {
        Set<Owner> newRandos = new HashSet<Owner>();
        List<Owner> newOwnerCycle = new ArrayList<>(); 
        List<Owner> newNonRandos = new ArrayList<>();
        newNonRandos.add(me);
        for (Owner o : nonRandos) {
            if (o.getNameAsString().equals(me.getNameAsString())) continue;
            newNonRandos.add(findOwner(otherOwners, o));
        }
        for (Owner o : randos) {
            newRandos.add(findOwner(otherOwners, o));
        }
        for (Owner o : ownerCycle) {
            if (o.getNameAsString().equals(me.getNameAsString())) 
                newOwnerCycle.add(me);
            else
                newOwnerCycle.add(findOwner(otherOwners, o));
        }

        this.randos = newRandos;
        this.nonRandos = newNonRandos; 
        this.ownerCycle = newOwnerCycle;
    }


    /**
     * Get the location where the current player will move to in the circle
     */
    private void updateLocations() {
        int numOwners = nonRandos.size();
        double dist = 40.0;     // use 40 for now
        double fromEdges = 10.0; // how far from the edges of the park 
        List<ParkLocation> optimalStartingLocations = getOptimalLocationShape(numOwners, dist, fromEdges);
   
        Collections.sort(nonRandos, new Comparator<Owner>() {
            @Override public int compare(Owner o1, Owner o2) {
                return o1.getNameAsString().compareTo(o2.getNameAsString());
            }
        });

        // add cycle to array and to tracker for locations 
        for (int i = 0; i < numOwners; i++) {
            ownerLocations.put(nonRandos.get(i), optimalStartingLocations.get(i));
            // ownerLocations.put(nonRandos.get(i), new ParkLocation(optimalStartingLocations.get(i).getRow(),optimalStartingLocations.get(i).getColumn()));
            ownerCycle.add(nonRandos.get(i));
        }

        Collections.reverse(ownerCycle);
    }

    private void findRandos(Owner myOwner, List<Owner> otherOwners) {
        nonRandos.add(myOwner);
        for (Owner person : otherOwners) {
            if (!(person.getCurrentSignal().equals(person.getNameAsString()))) 
                randos.add(person);
            else
                nonRandos.add(person);
        }
        for (Owner person : randos)
            simPrinter.println(person.getNameAsString() + " is a random player");
    }

    /**
     * Get the optimal shape located closest to the park gates
     *
     * @param n            number of players
     * @param dist         distance between each player
     * @param fromEdges    distance between player and gate 
     * @return             list of park locations where each player should go
     *
     */
    private List<ParkLocation> getOptimalLocationShape(Integer n, Double dist, Double fromEdges) {
        List<ParkLocation> shape = new ArrayList<ParkLocation>();
        if (n == 1)
            shape.add(new ParkLocation(fromEdges, fromEdges));
        else if (n == 2) {
            double radian = Math.toRadians(45.0);
            shape.add(new ParkLocation(fromEdges+Math.cos(radian)*dist, fromEdges));
            shape.add(new ParkLocation(fromEdges, fromEdges+Math.cos(radian)*dist));
        }
        else if (n == 3) {
            double radian1 = Math.toRadians(-15.0);
            double radian2 = Math.toRadians(-75.0);
            shape.add(new ParkLocation(fromEdges, fromEdges));
            shape.add(new ParkLocation(fromEdges+Math.cos(radian1)*dist, fromEdges-Math.sin(radian1)*dist));
            shape.add(new ParkLocation(fromEdges+Math.cos(radian2)*dist, fromEdges-Math.sin(radian2)*dist));
        }
        else if (n == 4) {
            shape.add(new ParkLocation(fromEdges,fromEdges));
            shape.add(new ParkLocation(fromEdges+dist,fromEdges));
            shape.add(new ParkLocation(fromEdges+dist,fromEdges+dist));
            shape.add(new ParkLocation(fromEdges,fromEdges+dist));
        }
        else {
            double radianStep = Math.toRadians(360.0/n);
            double radius = (dist/2)/(Math.sin(radianStep/2));
            double center = fromEdges+radius;
            double radian = Math.toRadians(135.0);
            for (int i = 0; i < n; i++) {
                double x = Math.cos(radian) * radius + center;
                double y = Math.sin(radian) * radius + center;
                shape.add(new ParkLocation(x,y));
                radian -= radianStep;
            }
        }
        return shape;
    }

    /**
     * Get the shortest path to starting point along which player will move
     *
     * @param start        starting point
     * @return             list of park locations along which owner will move to get to starting point
     *
     */
    private List<ParkLocation> shortestPath(ParkLocation start) {
        List<ParkLocation> path = new ArrayList<>();
        double magnitude = euclideanDistance(start.getRow(), start.getColumn());
        if (magnitude == 0)
            return path;
        
        double xStep = start.getRow()/magnitude;
        double yStep = start.getColumn()/magnitude;
        double xTemp = xStep*5;
        double yTemp = yStep*5;
        while (xTemp <= start.getRow() && yTemp <= start.getColumn()) {
            path.add(new ParkLocation(xTemp, yTemp));
            xTemp += xStep*5;
            yTemp += yStep*5;
        }
        path.add(start);
        return path;
    }

    private double euclideanDistance(double x, double y) {
        return Math.sqrt(Math.pow(x,2)+Math.pow(y,2));
    }

    private Double distanceBetweenTwoPoints(ParkLocation p1, ParkLocation p2) {
        Double x1 = p1.getColumn();
        Double y1 = p1.getRow();
        Double x2 = p2.getColumn();
        Double y2 = p2.getRow();
        return euclideanDistance(x1-x2, y1-y2);
    }
  
    /**
     * Returns a list of dogs waiting for myOwner, 
     * sorted by decreasing amount of time left to wait 
     * 
     * @param myOwner
     * @param otherOwners
     * @return
     */
    private List<Dog> getWaitingDogs(Owner myOwner, List<Owner> otherOwners) {
        List<Dog> waitingDogs = new ArrayList<>();
    	for(Dog dog : myOwner.getDogs()) {
    		if(dog.isWaitingForOwner(myOwner))
    			waitingDogs.add(dog);
    	}
    	for(Owner otherOwner : otherOwners) {
    		for(Dog dog : otherOwner.getDogs()) {
    			if(dog.isWaitingForOwner(myOwner))
    				waitingDogs.add(dog);
    		}
    	}
        Collections.sort(waitingDogs, new Comparator<Dog>() {
            @Override public int compare(Dog d1, Dog d2) {
                return d1.getWaitingTimeRemaining().compareTo(d2.getWaitingTimeRemaining());
            }
        });
        return waitingDogs;
    }

    // Testing - run with "java dogs/g1/Player.java" in src folder
    public static void main(String[] args) {
        Random random = new Random();
        SimPrinter simPrinter = new SimPrinter(true);
        Player player = new Player(1, 1, 1, 1, random, simPrinter);
        double fromEdges = 10.0;

        // TEST 1 - optimal line
        double dist = 2*Math.sqrt(2);
        int n = 2;
        List<ParkLocation> optimalShape = player.getOptimalLocationShape(n, dist, fromEdges);
        simPrinter.println(optimalShape);

        // TEST 2 - optimal equilateral triangle
        double radian = Math.toRadians(-15);
        dist = Math.cos(radian)*5;
        n = 3;
        optimalShape = player.getOptimalLocationShape(n, dist, fromEdges);
        simPrinter.println(optimalShape);

        // TEST 3 - optimal square
        dist = 2;
        n = 4;
        optimalShape = player.getOptimalLocationShape(n, dist, fromEdges);
        simPrinter.println(optimalShape);

        // TEST 4 - optimal regular pentagon
        dist = 3;
        n = 5;
        optimalShape = player.getOptimalLocationShape(n, dist, fromEdges);
        simPrinter.println(optimalShape);

        // TEST 5 - optimal regular hexagon
        dist = 5;
        n = 6;
        optimalShape = player.getOptimalLocationShape(n, dist, fromEdges);
        simPrinter.println(optimalShape);

        // TEST 6 - optimal regular octagon
        dist = Math.sqrt(10);
        n = 8;
        optimalShape = player.getOptimalLocationShape(n, dist, fromEdges);
        simPrinter.println(optimalShape);
    }
}