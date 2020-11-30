package dogs.sim;

import java.io.Serializable;
import java.util.Random;

import dogs.sim.DogReference.Breed;


public class Dog implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public static final Double MAX_WAITING_TIME = 30.0;
	public static final Double TOTAL_EXERCISE_TIME = 1800.0;
	
	private Breed breed;
	private Integer realID, randomID;
	private Owner owner, personWaitingFor, personHeadingFor;
	private ParkLocation parkLocation;
	private boolean isRunning = false, isWalking = false;
	private boolean isHeadingForBall = false, isHeadingForPerson = false, isWaitingForPerson = true;
	private boolean hasBall = true;
	private Double waitingTimeRemaining = MAX_WAITING_TIME;
	private Double exerciseTimeRemaining = TOTAL_EXERCISE_TIME;
	
	public Dog(Breed breed, Owner owner, Integer realID) {
		this.breed = breed;
		this.owner = owner;
		this.realID = realID;
		this.personWaitingFor = owner;
		this.personHeadingFor = null;
		this.randomID = new Random().nextInt();
		this.parkLocation = new ParkLocation(1.0, 0.0);
	}
	
	public Breed getBreed() {
		return breed;
	}
	
	public Integer getRealID() {
		return realID;
	}
	
	public Integer getRandomID() {
		return randomID;
	}

	public void setLocation(ParkLocation parkLocation) {
		this.parkLocation = parkLocation;
	}
	
	public void setLocation(Double row, Double column) {
		this.parkLocation = new ParkLocation(row, column);
	}
	
	public ParkLocation getLocation() {
		return parkLocation;
	}
	
	public String getLocationAsString() {
		return parkLocation.toString();
	}
	
	public Owner getOwner() {
		return owner;
	}
	
	public Double getWaitingTimeRemaining() {
		return waitingTimeRemaining;
	}
	
	public void resetWaitingTimeRemaining() {
		this.waitingTimeRemaining = MAX_WAITING_TIME;
	}

	public Double getWaitingTimeCompleted() {
		return MAX_WAITING_TIME - waitingTimeRemaining;
	}

	public void setWaitingTimeRemaining(double waitingTime) {
		this.waitingTimeRemaining = waitingTime;
	}

	public void decreaseWaitingTimeRemaining(double waitingTime) {
		if(waitingTimeRemaining - waitingTime >= 0.0)
			waitingTimeRemaining -= waitingTime;
		else
			waitingTimeRemaining = 0.0;		
	}
	
	public Double getExerciseTimeRemaining() {
		return exerciseTimeRemaining;
	}
	
	public Double getExerciseTimeCompleted() {
		return TOTAL_EXERCISE_TIME - exerciseTimeRemaining;
	}

	public void setExerciseTimeRemaining(double exerciseTime) {
		this.exerciseTimeRemaining = exerciseTime;
	}
	
	public void decreaseExerciseTimeRemaining(double exerciseTime) {
		if(exerciseTimeRemaining - exerciseTime >= 0.0)
			exerciseTimeRemaining -= exerciseTime;
		else
			exerciseTimeRemaining = 0.0;
	}
	
	public Double getRunningSpeed() {
		return DogReference.getRunningSpeed(breed);
	}

	public Double getWalkingSpeed() {
		return DogReference.getWalkingSpeed(breed);
	}

	public Owner getOwnerWaitingFor() {
		return personWaitingFor;
	}
	
	public void setOwnerWaitingFor(Owner owner) {
		this.personWaitingFor = owner;
	}

	public Owner getOwnerHeadingFor() {
		return personHeadingFor;
	}

	public void setOwnerHeadingFor(Owner owner) {
		this.personHeadingFor = owner;
	}

	public boolean isWaitingForItsOwner() {
		return owner.equals(personWaitingFor) || 
				(personWaitingFor != null && 
				 owner.getNameAsString().equals(personWaitingFor.getNameAsString())
				);
	}

	public boolean isWaitingForOwner(Owner owner) {
		return owner.equals(personWaitingFor) || 
				(owner != null && 
				 personWaitingFor != null && 
				 owner.getNameAsString().equals(personWaitingFor.getNameAsString())
				);
	}
	
	public boolean isHeadingForItsOwner() {
		return owner.equals(personHeadingFor) || 
				(personHeadingFor != null && 
				 owner.getNameAsString().equals(personHeadingFor.getNameAsString())
				);
	}

	public boolean isHeadingForOwner(Owner owner) {
		return owner.equals(personHeadingFor) || 
				(owner != null && 
				 personHeadingFor != null && 
				 owner.getNameAsString().equals(personHeadingFor.getNameAsString())
				);
	}

	public boolean isRunning() {
		return isRunning;
	}
	
	public boolean isWalking() {
		return isWalking;
	}
	
	public boolean isMoving() {
		return isRunning || isWalking;		
	}

	public boolean isStationary() {
		return !isMoving();
	}
	
	public boolean isNotRunning() {
		return !isRunning;
	}
	
	public boolean isNotWalking() {
		return !isWalking;
	}

	public boolean isHeadingForBall() {
		return isHeadingForBall;
	}
	
	public boolean isHeadingForPerson() {
		return isHeadingForPerson;
	}

	public boolean isWaitingForPerson() {
		return isWaitingForPerson;
	}
	
	public void setRunning() {
		isRunning = true;
		isWalking = false;
	}
	
	public void setWalking() {
		isRunning = false;
		isWalking = true;
	}
	
	public void setStationary() {
		isRunning = false;
		isWalking = false;
	}
	
	public void setHeadingForBall() {
		isHeadingForBall = true;
		isHeadingForPerson = false;
		isWaitingForPerson = false;
		setHasBall(false);
		setRunning();
		setOwnerWaitingFor(null);
		setOwnerHeadingFor(null);
		resetWaitingTimeRemaining();
	}
	
	public void setHeadingForPerson(Owner owner) {		
		isHeadingForBall = false;
		isHeadingForPerson = true;
		isWaitingForPerson = false;
		setHasBall(true);
		setWalking();
		setOwnerHeadingFor(owner);
		setOwnerWaitingFor(null);
		resetWaitingTimeRemaining();
	}
	
	public void setWaitingForPerson(Owner owner) {
		isHeadingForBall = false;
		isHeadingForPerson = false;
		isWaitingForPerson = true;
		setHasBall(true);
		setStationary();
		setOwnerHeadingFor(null);
		setOwnerWaitingFor(owner);
		resetWaitingTimeRemaining();
	}
	
	public boolean hasBall() {
		return hasBall;
	}
	
	public void setHasBall(boolean hasBall) {
		this.hasBall = hasBall;
	}
}