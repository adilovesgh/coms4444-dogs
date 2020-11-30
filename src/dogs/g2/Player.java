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
        ParkLocation myLocation = equidistantPtsOnCircle(myOwner, otherOwners);
        //ParkLocation myLocation = initialLocations.get(1);
        while (myOwner.getLocation().getRow() < myLocation.getRow() || myOwner.getLocation().getColumn() < myLocation.getColumn()) {
            directive.instruction = Instruction.MOVE;
            double rowDiff = Math.max(0,Math.min(myLocation.getRow() - myOwner.getLocation().getRow(), 3.5));
            double colDiff = Math.max(0,Math.min(myLocation.getColumn() - myOwner.getLocation().getColumn(), 3.5));

            directive.parkLocation = new ParkLocation(myOwner.getLocation().getRow() + rowDiff, myOwner.getLocation().getColumn() + colDiff);
            return directive;
        }

        //System.out.println(myOwner.getNameAsEnum().ordinal() + " " + myOwner.getLocation().getRow() + " " + myOwner.getLocation().getColumn());

        if(round <= 70) {
            directive.instruction = Instruction.CALL_SIGNAL;
            directive.signalWord = "aardwolf";
            return directive;
        }

        List<Dog> waitingDogs = getAllWaitingDogs(myOwner, otherOwners);
        //debug(myOwner, otherOwners);

        Comparator<Dog> speed = (Dog dog1, Dog dog2) -> Double.compare(dog1.getRunningSpeed(), dog2.getRunningSpeed());
        Collections.sort(waitingDogs, speed);

        //System.out.println("size: " + waitingDogs.size());

        //Throw Ball in the direction of other owners
        if(waitingDogs.size() > 0){
            directive.instruction = Instruction.THROW_BALL;
            directive.dogToPlayWith = waitingDogs.get(0);
            directive.parkLocation = getBallDirection(myOwner, otherOwners);

            //directive.parkLocation = otherOwners.get(randomNum).getLocation();
        } else {
            List<String> recievedSignals = getOtherOwnersSignals(otherOwners);
            directive.instruction = Instruction.NOTHING;
        }

/*
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
        */
        return directive;
    }

    private ParkLocation equidistantPtsOnCircle(Owner myOwner, List<Owner> otherOwners){
        //Size of the MAP is 200 x 200
        double angle = (2 * Math.PI) / (otherOwners.size() + 1);

        double radius = 40 / (2 * Math.sin(2 * Math.PI / (otherOwners.size() + 1)));

        double row = Math.round(radius - Math.round(radius) * Math.sin(myOwner.getNameAsEnum().ordinal() * angle));
        double column = Math.round(radius - Math.round(radius) * Math.cos(myOwner.getNameAsEnum().ordinal()* angle));

        //System.out.println(myOwner.getNameAsEnum().ordinal() + " " + row + " " + column);
        ParkLocation initialParkLocation = new ParkLocation(row, column);
        return initialParkLocation;
    }

    private ParkLocation getBallDirection(Owner myOwner, List<Owner> otherOwners){
        //Size of the MAP is 200 x 200

        double angle = (2 * Math.PI) / (otherOwners.size() + 1);

        double radius = 39 / (2 * Math.sin(2 * Math.PI / (otherOwners.size() + 1)));

        double row = Math.round(radius - Math.round(radius) * Math.sin(((myOwner.getNameAsEnum().ordinal() + 1 ) % (otherOwners.size() + 1)) * angle));
        double column = Math.round(radius - Math.round(radius) * Math.cos(((myOwner.getNameAsEnum().ordinal() + 1 ) % (otherOwners.size() + 1)) * angle));

        //System.out.println(((myOwner.getNameAsEnum().ordinal() + 1 ) % (otherOwners.size() + 1)));
        ParkLocation initialParkLocation = new ParkLocation(row, column);
        return initialParkLocation;
    }


    private List<String> getOtherOwnersSignals(List<Owner> otherOwners) {
        List<String> otherOwnersSignals = new ArrayList<>();
        for(Owner otherOwner : otherOwners)
            if(!otherOwner.getCurrentSignal().equals("_"))
                otherOwnersSignals.add(otherOwner.getCurrentSignal());
        return otherOwnersSignals;
    }

    private List<Dog> getMyWaitingDogs(Owner myOwner, List<Owner> otherOwners) {
        List<Dog> waitingDogs = new ArrayList<>();
        for(Dog dog : myOwner.getDogs()) {
            if(dog.isWaitingForItsOwner() && dog.getExerciseTimeRemaining() > 0.0)
                waitingDogs.add(dog);
        }
        return waitingDogs;
    }

    private List<Dog> getOtherWaitingDogs(Owner myOwner, List<Owner> otherOwners) {
        List<Dog> waitingDogs = new ArrayList<>();
    
        for(Owner otherOwner : otherOwners) {
            for(Dog dog : otherOwner.getDogs()) {
                if(dog.isWaitingForOwner(myOwner) && dog.getExerciseTimeRemaining() > 0.0)
                    waitingDogs.add(dog);
            }
        }
        return waitingDogs;
    }

    private List<Dog> getAllWaitingDogs(Owner myOwner, List<Owner> otherOwners) {
        List<Dog> waitingDogs = new ArrayList<>();
        for(Dog dog : myOwner.getDogs()) {
            if(dog.isWaitingForItsOwner())
                //if(dog.getExerciseTimeRemaining() > 0.0) {
                    waitingDogs.add(dog);
                //}
        }
        for(Owner otherOwner : otherOwners) {
            for(Dog dog : otherOwner.getDogs()) {
                if(!dog.isMoving()){
                    if (dog.getOwnerWaitingFor().getNameAsEnum().ordinal() == myOwner.getNameAsEnum().ordinal()){
                        //if(dog.getExerciseTimeRemaining() > 0.0) {
                            //System.out.println(dog.getBreed());
                            waitingDogs.add(dog);
                        //}
                    }    
                }     
            }
        }
        //System.out.println(waitingDogs.size());
        return waitingDogs;
    }

    private Dog getDog(List<Dog> waitingDogs) {
        int count = 0;
        for (Dog dog : waitingDogs) {
            //System.out.println(dog.getBreed());
            //System.out.println(dog.getExerciseTimeRemaining());
            if (dog.getExerciseTimeRemaining() != 0.0) {
                System.out.println(count);
                return dog;
            }
            count ++;
        }

        return null;
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