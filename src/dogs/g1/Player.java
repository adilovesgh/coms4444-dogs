package dogs.g1;

import java.util.*;
import java.lang.Math; 

import dogs.sim.Directive;
import dogs.sim.Owner;
import dogs.sim.SimPrinter;
import dogs.sim.ParkLocation;
import dogs.sim.Directive.Instruction;


public class Player extends dogs.sim.Player {
	
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
    
    	// TODO add your code here to choose a directive

        return null; // TODO modify the return statement to return your directive
    }

    /**
     * Get the location where the current player will move to in this round
     *
     * @param myOwner      my owner
     * @param otherOwners  all other owners in the park
     * @return             a directive for the owner's next move
     *
     */
    public Directive getLocation(Owner myOwner, List<Owner> otherOwners) {
    
    	// TODO add code
	
        return null;
    }

    /**
     * Get the optimal shape located closest to the park gates
     *
     * @param n            number of players
     * @param dist         distance between each player
     * @return             list of park locations where each player should go
     *
     */
    public List<ParkLocation> getOptimalLocationShape(Integer n, Double dist) {
        List<ParkLocation> shape = new ArrayList<ParkLocation>();
        if (n == 1)
            shape.add(new ParkLocation(0.0, 0.0));
        else if (n == 2) {
            double radian = Math.toRadians(45.0);
            shape.add(new ParkLocation(Math.cos(radian)*dist, 0.0));
            shape.add(new ParkLocation(0.0, Math.cos(radian)*dist));
        }
        else if (n == 3) {
            double radian1 = Math.toRadians(-15.0);
            double radian2 = Math.toRadians(-75.0);
            shape.add(new ParkLocation(0.0, 0.0));
            shape.add(new ParkLocation(Math.cos(radian1)*dist, -Math.sin(radian1)*dist));
            shape.add(new ParkLocation(Math.cos(radian2)*dist, -Math.sin(radian2)*dist));
        }
        else if (n == 4) {
            shape.add(new ParkLocation(0.0,0.0));
            shape.add(new ParkLocation(dist,0.0));
            shape.add(new ParkLocation(dist,dist));
            shape.add(new ParkLocation(0.0,dist));
        }
        else {
            double radian = Math.toRadians(360.0/n);
            double center = Math.sin(radian/2)*dist/2;
            double radius = center;
            double tempRadian = 0.0;
            for (int i = 0; i < n; i++) {
                double x = Math.cos(tempRadian) * radius + center;
                double y = Math.sin(tempRadian) * radius + center;
                shape.add(new ParkLocation(x,y));
                tempRadian -= radian;
            }
        }
        return shape;
    }

    /**
     * Get the shortest path to starting point along which player will move
     *
     * @param start        starting point
     * @return             a directive for the owner's next move
     *
     */
    public List<ParkLocation> shortestPath(ParkLocation start) {

        // TODO add code
        
        return null;
    }

    // Testing - run with "java dogs/g1/Player.java" in src folder
    public static void main(String[] args) {
        Random random = new Random();
        SimPrinter simPrinter = new SimPrinter(true);
        Player player = new Player(1, 1, 1, 1, random, simPrinter);

        // TEST 1 - optimal line
        double dist = 2*Math.sqrt(2);
        int n = 2;
        List<ParkLocation> optimalShape = player.getOptimalLocationShape(n, dist);
        System.out.println(optimalShape);

        // TEST 2 - optimal triangle
        double radian = Math.toRadians(-15);
        dist = Math.cos(radian)*5;
        n = 3;
        optimalShape = player.getOptimalLocationShape(n, dist);
        System.out.println(optimalShape);

        // TEST 3 - optimal square
        dist = 2;
        n = 4;
        optimalShape = player.getOptimalLocationShape(n, dist);
        System.out.println(optimalShape);

        // TEST 4 - optimal regular pentagon
        dist = 3;
        n = 5;
        optimalShape = player.getOptimalLocationShape(n, dist);
        System.out.println(optimalShape);

        // TEST 5 - optimal regular hexagon
        dist = 5;
        n = 6;
        optimalShape = player.getOptimalLocationShape(n, dist);
        System.out.println(optimalShape);

        // TEST 6 - optimal regular octogon
        dist = Math.sqrt(10);
        n = 8;
        optimalShape = player.getOptimalLocationShape(n, dist);
        System.out.println(optimalShape);
    }
}