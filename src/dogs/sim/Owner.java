package dogs.sim;

import java.io.Serializable;
import java.util.List;

import dogs.sim.Directive.Instruction;


public class Owner implements Serializable {

	private static final long serialVersionUID = 1L;
	private OwnerName name;
	private List<Dog> dogs;
	private ParkLocation parkLocation;
	private Instruction currentAction;
	private Double actionTimeRemaining;
	private String currentSignalWord;
	private Double allExerciseCompletionTime;
	private boolean allExerciseCompleted;
	

	public Owner() {
		this.parkLocation = new ParkLocation();
		this.currentAction = Instruction.NOTHING;
		this.actionTimeRemaining = 0.0;
		this.currentSignalWord = "_";
		this.allExerciseCompletionTime = Double.MAX_VALUE;
		this.allExerciseCompleted = false;
	}
	
	public Owner(OwnerName name) {
		this.name = name;
		this.parkLocation = new ParkLocation();
		this.currentAction = Instruction.NOTHING;
		this.actionTimeRemaining = 0.0;
		this.currentSignalWord = "_";
		this.allExerciseCompletionTime = Double.MAX_VALUE;
		this.allExerciseCompleted = false;
	}

	public Owner(OwnerName name, List<Dog> dogs) {
		this.name = name;
		this.dogs = dogs;
		this.parkLocation = new ParkLocation();
		this.currentAction = Instruction.NOTHING;
		this.actionTimeRemaining = 0.0;
		this.currentSignalWord = "_";
		this.allExerciseCompletionTime = Double.MAX_VALUE;
		this.allExerciseCompleted = false;
	}

	public enum OwnerName {
		ALICE, BOB, CAROL, DAVE, DORA,
		ERIN, FRANK, GEORGE, GRACE, HEIDI,
		IVAN, JIMMY, JUDY, MICHAEL, OLIVIA,
		OSCAR, PATRICK, PEGGY, RUPERT, SYBIL,
		TIMMY, TRENT, VICTOR, WALTER, WENDY
	}
	
	public void setName(OwnerName name) {
		this.name = name;
	}
	
	public OwnerName getNameAsEnum() {
		return name;
	}
	
	public String getNameAsString() {
		return name.name().charAt(0) + name.name().substring(1).toLowerCase();
	}
	
	public void setCurrentAction(Instruction newAction) {
		if(actionTimeRemaining == 0.0) {
			this.currentAction = newAction;
			actionTimeRemaining = 5.0;			
		}
	}
	
	public Double getActionTimeRemaining() {
		return actionTimeRemaining;
	}
	
	public void decrementActionTimeRemaining() {
		if(actionTimeRemaining >= 1.0)
			actionTimeRemaining -= 1;
		else
			actionTimeRemaining = 0.0;
	}
	
	public Instruction getCurrentAction() {
		return currentAction;
	}
	
	public void setCurrentSignal(String newSignalWord) {
		this.currentSignalWord = newSignalWord;
	}
	
	public String getCurrentSignal() {
		return currentSignalWord;
	}
	
	public void setDogs(List<Dog> dogs) {
		this.dogs = dogs;
	}
	
	public List<Dog> getDogs() {
		return dogs;
	}
	
	public boolean hasDog(Dog dog) {
		if(dogs.contains(dog))
			return true;
		for(Dog existingDog : dogs)
			if(dog.getRealID().equals(existingDog.getRealID()) && dog.getRandomID().equals(existingDog.getRandomID()))
				return true;
		return false;
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
	
	public boolean allExerciseCompleted() {
		return allExerciseCompleted;
	}
	
	public void setAllExerciseCompleted(boolean allExerciseCompleted) {
		this.allExerciseCompleted = allExerciseCompleted;
	}
	
	public Double getAllExerciseCompletionTime() {
		return allExerciseCompletionTime;
	}
	
	public void setAllExerciseCompletionTime(double allExerciseCompletionTime) {
		this.allExerciseCompletionTime = allExerciseCompletionTime;
	}
		
	public void resetAction() {
		this.currentAction = Instruction.NOTHING;
		this.actionTimeRemaining = 0.0;
		this.currentSignalWord = "_";
	}
}