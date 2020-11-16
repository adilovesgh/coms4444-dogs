package dogs.sim;


public class Directive {

	public Instruction instruction = Instruction.NOTHING;
	public ParkLocation parkLocation = new ParkLocation();
	public Dog dogToPlayWith = null;
	public String signalWord = "_";
	
	public enum Instruction {
		THROW_BALL, MOVE, CALL_SIGNAL, EXIT_PARK, NOTHING
	}
}
