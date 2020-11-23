package dogs.g2; // TODO modify the package name to reflect your team

import java.util.*;

import dogs.sim.Directive;
import dogs.sim.Directive.Instruction;
import dogs.sim.Dictionary;
import dogs.sim.Dog;
import dogs.sim.Owner;
import dogs.sim.ParkLocation;
import dogs.sim.SimPrinter;


public class Player extends dogs.sim.Player {
    
    /**
     * Player constructor
     *
     * @param rounds           number of rounds
     * @param numDogsPerOwner  number of dogs per owner
     * @param numOwners        number of owners
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
                        
        Directive directive = new Directive();
        
        if(round <= 151) {
            directive.instruction = Instruction.MOVE;
            directive.parkLocation = new ParkLocation(myOwner.getLocation().getRow() + 2, myOwner.getLocation().getColumn() + 2);
            return directive;
        }

        directive.instruction = Instruction.THROW_BALL;
        List<Dog> waitingDogs = getWaitingDogs(myOwner, otherOwners);
        if(waitingDogs.size() > 0)
            directive.dogToPlayWith = waitingDogs.get(0);
        else {
            directive.instruction = Instruction.NOTHING;
        }

        double randomAngle = Math.toRadians(random.nextDouble() * 360);
        double ballRow = myOwner.getLocation().getRow() + 40.0 * Math.sin(randomAngle);
        double ballColumn = myOwner.getLocation().getColumn() + 40.0 * Math.cos(randomAngle);
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
    
    
    private List<Dog> getWaitingDogs(Owner myOwner, List<Owner> otherOwners) {
        List<Dog> waitingDogs = new ArrayList<>();
        for(Dog dog : myOwner.getDogs()) {
            if(dog.isWaitingForItsOwner())
                waitingDogs.add(dog);
        }
        for(Owner otherOwner : otherOwners) {
            for(Dog dog : otherOwner.getDogs()) {
                if(dog.isWaitingForOwner(myOwner))
                    waitingDogs.add(dog);
            }
        }
        return waitingDogs;
    }
}