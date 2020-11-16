package dogs.sim;

import java.io.Serializable;

public class Ball implements Serializable {

	private static final long serialVersionUID = 1L;
	private Dog dog;
	private ParkLocation parkLocation;
	
	public Ball(Dog dog) {
		this.dog = dog;
		this.parkLocation = new ParkLocation(1.0, 0.0);
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
	
	public void setDog(Dog dog) {
		this.dog = dog;
	}
	
	public Dog getDog() {
		return dog;
	}
}
