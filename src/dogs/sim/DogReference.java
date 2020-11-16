package dogs.sim;

import java.util.HashMap;
import java.util.Map;


public class DogReference {

	public enum Breed {
		LABRADOR, POODLE, SPANIEL, TERRIER
	}
	
	public static Map<Breed, Double> runningReference, walkingReference;
	static {
		runningReference = new HashMap<>();
		runningReference.put(Breed.LABRADOR, 10.0);
		runningReference.put(Breed.POODLE, 8.0);
		runningReference.put(Breed.SPANIEL, 7.0);
		runningReference.put(Breed.TERRIER, 4.0);

		walkingReference = new HashMap<>();
		walkingReference.put(Breed.LABRADOR, 2.5);
		walkingReference.put(Breed.POODLE, 2.0);
		walkingReference.put(Breed.SPANIEL, 1.75);
		walkingReference.put(Breed.TERRIER, 1.0);	
	}
	
	public static Double getWalkingSpeed(Breed breed) {
		return walkingReference.get(breed);
	}
	
	public static Double getRunningSpeed(Breed breed) {
		return runningReference.get(breed);
	}
}