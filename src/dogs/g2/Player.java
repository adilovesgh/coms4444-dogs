package dogs.g2; // TODO modify the package name to reflect your team

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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

        if(round <= 1) {
            directive.instruction = Instruction.CALL_SIGNAL;
            directive.signalWord = "aardwolf";
            return directive;
        } 
        List<ParkLocation> initialLocations = equidistantPtsOnCircle(otherOwners);
        ParkLocation myLocation = initialLocations.get(1);
        while (myOwner.getLocation().getRow() <= myLocation.getRow() || myOwner.getLocation().getColumn() <= myLocation.getColumn()) {
            directive.instruction = Instruction.MOVE;
            directive.parkLocation = new ParkLocation(myOwner.getLocation().getRow() + 2, myOwner.getLocation().getColumn() + 2);
            return directive;
        }


        // Sort dogs by speed 
        List<Dog> waitingDogs = getWaitingDogs(myOwner, otherOwners);

        Comparator<Dog> speed = (Dog dog1, Dog dog2) -> Double.compare(dog1.getRunningSpeed(), dog2.getRunningSpeed());
        Collections.sort(waitingDogs, speed);


        //Throw Ball in the direction of other owners
        if(waitingDogs.size() > 0){
            directive.instruction = Instruction.THROW_BALL;
            directive.dogToPlayWith = waitingDogs.get(0);

            //directive.parkLocation = otherOwners.get(randomNum).getLocation();
        } else {
            List<String> recievedSignals = getOtherOwnersSignals(otherOwners);
            directive.instruction = Instruction.NOTHING;
        }


        double randomDistance, randomAngle;             
        randomDistance = 40.0;
        randomAngle = Math.toRadians(random.nextDouble() * 360);
        int randomNum = ThreadLocalRandom.current().nextInt(0, otherOwners.size());
        System.out.println(randomNum);
        double ballRow = otherOwners.get(randomNum).getLocation().getRow() + randomDistance * Math.sin(randomAngle);
        double ballColumn = otherOwners.get(randomNum).getLocation().getColumn() + randomDistance * Math.cos(randomAngle);

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

    private List<ParkLocation> equidistantPtsOnCircle(List<Owner> otherOwners){
        //Size of the MAP is 200 x 200
        List<ParkLocation> initialParkLocations = new ArrayList<>();
        int radius = 10;
        int theta = 360/(otherOwners.size()+1);
        for ( int i = 1; i <= otherOwners.size()+1; i++ ) {
            Double ownerX = ( radius * Math.cos(theta * i) + 100 );
            Double ownerY = ( radius * Math.sin(theta * i) + 100 );
            ParkLocation pkloc = new ParkLocation(ownerX, ownerY);
            initialParkLocations.add(pkloc);
        }
        return initialParkLocations;
    }

    private List<String> getOtherOwnersSignals(List<Owner> otherOwners) {
        List<String> otherOwnersSignals = new ArrayList<>();
        for(Owner otherOwner : otherOwners)
            if(!otherOwner.getCurrentSignal().equals("_"))
                otherOwnersSignals.add(otherOwner.getCurrentSignal());
        return otherOwnersSignals;
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