package dogs.sim;

import java.util.*;


public abstract class Player {

    public Integer rounds, numDogsPerOwner, numOwners, seed;
    public Random random;
    public SimPrinter simPrinter;
    
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
		this.rounds = rounds;
		this.numDogsPerOwner = numDogsPerOwner;
		this.numOwners = numOwners;
        this.seed = seed;
        this.random = random;
        this.simPrinter = simPrinter;
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
    public abstract Directive chooseDirective(Integer round,
    										  Owner myOwner,
    									  	  List<Owner> otherOwners);

}