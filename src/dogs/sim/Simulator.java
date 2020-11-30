/*
    Project: Exercising the Dogs
    Course: COMS 4444 Programming & Problem Solving (Fall 2020)
    Instructor: Prof. Kenneth Ross
    URL: http://www.cs.columbia.edu/~kar/4444f20
    Author: Aditya Sridhar
    Simulator Version: 1.0
*/

package dogs.sim;

import java.awt.Desktop;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.*;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import dogs.sim.Directive.Instruction;
import dogs.sim.DogReference.Breed;
import dogs.sim.Owner.OwnerName;


public class Simulator {
	
	// Simulator structures
	private static String configName;
	private static List<PlayerWrapper> playerWrappers;
	private static List<String> playerNames;
	private static Map<PlayerWrapper, Owner> playerWrapperToOwnerMap;
	private static Map<Owner, PlayerWrapper> ownerToPlayerWrapperMap;
	private static List<Owner> allOwners;
	private static List<Dog> allDogs;
	private static List<Ball> allBalls;
	private static Map<Dog, Ball> dogToBallMap;
	private static Random random;
	
	// Simulator inputs
	private static int seed = 42;
	private static int rounds = 3600;
	private static int numDogsPerOwner = 1;
	private static int numOwners = 5;
	private static int inverseGranularity = 100;
	private static double fps = 30;
	private static boolean showGUI = false;

	// Defaults
	private static double dataError = 1e-7;
	private static boolean enablePrints = false;
	private static long timeout = 1000;
	private static int currentStep = 0;
	private static String version = "1.0";
	private static String projectPath, sourcePath, staticsPath;
    

	private static void setup() {
		random = new Random(seed);
		projectPath = new File(".").getAbsolutePath().substring(0, 
				new File(".").getAbsolutePath().indexOf("coms4444-dogs") + "coms4444-dogs".length());
		sourcePath = projectPath + File.separator + "src";
		staticsPath = projectPath + File.separator + "statics";
	}

	private static void parseCommandLineArguments(String[] args) throws IOException {
		playerWrappers = new ArrayList<>();
		playerNames = new ArrayList<>();
		allOwners = new ArrayList<>();
		allDogs = new ArrayList<>();
		allBalls = new ArrayList<>();
		playerWrapperToOwnerMap = new HashMap<>();
		ownerToPlayerWrapperMap = new HashMap<>();
		dogToBallMap = new HashMap<>();
		
		Map<String, Integer> playerNameMap = new HashMap<>();

		for(int i = 0; i < args.length; i++) {
            switch (args[i].charAt(0)) {
                case '-':
                    if(args[i].equals("-n") || args[i].equals("--owners")) {
                        while(i + 1 < args.length && args[i + 1].charAt(0) != '-') {
                            i++;
                            String playerName = args[i];
                            if(!playerNameMap.containsKey(playerName))
                            	playerNameMap.put(playerName, 0);
                            playerNameMap.put(playerName, playerNameMap.get(playerName) + 1);
                            playerNames.add(playerName + "_" + playerNameMap.get(playerName));
                        }
                        numOwners = playerNames.size();
                    }
                    else if(args[i].equals("-d") || args[i].equals("--dogs")) {
                    	i++;
                        if(i == args.length) 
                            throw new IllegalArgumentException("The dog configuration file is missing!");
                        configName = args[i];
                    }
                    else if(args[i].equals("-g") || args[i].equals("--gui"))
                        showGUI = true;
                    else if(args[i].equals("-l") || args[i].equals("--log")) {
                        i++;
                    	if(i == args.length) 
                            throw new IllegalArgumentException("The log file path is missing!");
                        Log.setLogFile(args[i]);
                        Log.assignLoggingStatus(true);
                    }
                    else if(args[i].equals("-v") || args[i].equals("--verbose"))
                        Log.assignVerbosityStatus(true);
                    else if(args[i].equals("-f") || args[i].equals("--fps")) {
                    	i++;
                        if(i == args.length)
                            throw new IllegalArgumentException("The GUI frames per second is missing!");
                        fps = Double.parseDouble(args[i]);
                    }
                    else if(args[i].equals("-y") || args[i].equals("--granularity")) {
                    	i++;
                        if(i == args.length)
                            throw new IllegalArgumentException("The simulation granularity is not specified!");
                        if(Double.parseDouble(args[i]) == 0.0)
                            throw new IllegalArgumentException("The simulation granularity must be nonzero!");                        	
                        inverseGranularity = (int) (1.0 / Double.parseDouble(args[i]));
                    }
                    else if(args[i].equals("-s") || args[i].equals("--seed")) {
                    	i++;
                        if(i == args.length) 
                            throw new IllegalArgumentException("The seed number is missing!");
                        seed = Integer.parseInt(args[i]);
                        random = new Random(seed);
                    }
                    else if(args[i].equals("-r") || args[i].equals("--rounds")) {
                    	i++;
                        if(i == args.length)
                            throw new IllegalArgumentException("The number of rounds is not specified!");
                        rounds = Integer.parseInt(args[i]);
                    }
                    else
                        throw new IllegalArgumentException("Unknown argument \"" + args[i] + "\"!");
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument \"" + args[i] + "\"!");
            }
        }

		
		if(configName == null)
			throw new IOException("You must specify a configuration file.");
		else {
			File configFile;
			Scanner scanner;
			try {
				configFile = new File(sourcePath + File.separator + "configs" + File.separator + configName);
				scanner = new Scanner(configFile);
			} catch(FileNotFoundException e) {
                throw new FileNotFoundException("Configuration file was not found!");
			}

			try {				
				List<OwnerName> ownerNames = new ArrayList<>(Arrays.asList(OwnerName.values()));
				List<Breed> dogBreeds = new ArrayList<>(Arrays.asList(Breed.values()));
				
				numDogsPerOwner = -1;
				int i = -1;
				for(String name : playerNames) {
					if(!scanner.hasNextLine()) {
		                Log.writeToLogFile("The number of lines in the configuration file should be at least the number of owners specified!");
		                System.exit(1);
					}
					i++;

					Owner owner = new Owner(ownerNames.get(i));
					
					String[] lineDogs = scanner.nextLine().strip().split(" ");
					if(numDogsPerOwner == -1)
						numDogsPerOwner = lineDogs.length;
					else if(numDogsPerOwner != lineDogs.length) {
						Log.writeToLogFile("The number of dogs per owner in the configuration file should be the same for all owners!");
		                System.exit(1);
					}
						
				
					List<Dog> dogs = new ArrayList<>();
					int labradorID = 1, spanielID = 1, terrierID = 1, poodleID = 1;
					for(int j = 0; j < lineDogs.length; j++) {						
						try {
							Breed breed = null;
							for(Breed dogBreed : dogBreeds) {
								if(dogBreed.name().equalsIgnoreCase(lineDogs[j])) {
									breed = dogBreed;
									break;
								}
							}
							if(breed == null)
								throw new IOException("Dog breed \"" + lineDogs[j].toLowerCase() + "\" in the configuration file not found!");							
							
							Dog dog;
							if(breed.equals(Breed.LABRADOR)) {
								dog = new Dog(breed, owner, labradorID);
								labradorID++;
							}
							else if(breed.equals(Breed.SPANIEL)) {
								dog = new Dog(breed, owner, spanielID);
								spanielID++;
							}
							else if(breed.equals(Breed.TERRIER)) {
								dog = new Dog(breed, owner, terrierID);
								terrierID++;								
							}
							else {
								dog = new Dog(breed, owner, poodleID);
								poodleID++;
							}

							dogs.add(dog);
							allDogs.add(dog);
							
							Ball ball = new Ball(dog);
							allBalls.add(ball);
							
							dogToBallMap.put(dog, ball);
						} catch(Exception e) {
							Log.writeToLogFile("An error occurred while creating the dogs!");
							throw e;
						}
					}
					owner.setDogs(dogs);

					allOwners.add(owner);
					
					PlayerWrapper playerWrapper;
					try {
			        	playerWrapper = loadPlayerWrapper(cleanName(name), name);
			        	playerWrappers.add(playerWrapper);
						playerWrapperToOwnerMap.put(playerWrapper, owner);
						ownerToPlayerWrapperMap.put(owner, playerWrapper);
					} catch (Exception e) {
						Log.writeToLogFile("Unable to load player!");
						e.printStackTrace();
					}
				}
			} catch(Exception e) {
				scanner.close();
                Log.writeToLogFile("Cannot interpret one or more lines of the configuration file!");
                System.exit(1);
			}
			
			scanner.close();			
		}
		
		Log.writeToLogFile("\n");
        Log.writeToLogFile("Project: Exercising the Dogs");
        Log.writeToLogFile("Simulator Version: " + version);
        Log.writeToLogFile("Players: " + playerNames.toString());
        Log.writeToLogFile("GUI: " + (showGUI ? "enabled" : "disabled"));
        Log.writeToLogFile("\n");
	}
	
	private static void runSimulation() throws IOException, JSONException {
		
		HTTPServer server = null;
		if(showGUI) {
            server = new HTTPServer();
            Log.writeToLogFile("Hosting the HTTP Server on " + server.addr());
            if(!Desktop.isDesktopSupported())
                Log.writeToLogFile("Desktop operations not supported!");
            else if(!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Log.writeToLogFile("Desktop browse operation not supported!");
            else {
                try {
                    Desktop.getDesktop().browse(new URI("http://localhost:" + server.port()));
                } catch(URISyntaxException e) {}
            }
        }
		
		List<Owner> ownersThatExitedPark = new ArrayList<>();
		Map<Owner, ParkLocation> ownerToMoveLocationMap = new HashMap<>();
		Map<Dog, ParkLocation> dogToMoveLocationMap = new HashMap<>();

	    if(showGUI)
			updateGUI(server, getGUIState(0, ownersThatExitedPark));
		
		boolean allExerciseCompleted = false;
		for(int i = 1; i <= rounds * inverseGranularity; i++) {
			currentStep = i;
						
			Map<Owner, Owner> clonedOwnersMap = new HashMap<>();
			Map<Owner, List<Owner>> clonedOtherOwnersMap = new HashMap<>();
			
			if((currentStep - 1) % inverseGranularity == 0) {
				for(PlayerWrapper playerWrapper : playerWrappers) {
					Owner owner = playerWrapperToOwnerMap.get(playerWrapper);
					List<Owner> otherOwners = new ArrayList<>(allOwners);
					otherOwners.remove(owner);
					clonedOwnersMap.put(owner, deepClone(owner));
					clonedOtherOwnersMap.put(owner, deepClone(otherOwners));
				}
			}
			
			for(PlayerWrapper playerWrapper : playerWrappers) {
				Owner owner = playerWrapperToOwnerMap.get(playerWrapper);
				
				if(ownersThatExitedPark.contains(owner))
					continue;
				
				if((currentStep - 1) % inverseGranularity == 0) {
					int round = (currentStep - 1) / inverseGranularity + 1;
					owner.decrementActionTimeRemaining();
					if(owner.getActionTimeRemaining() == 0.0) {
						owner.resetAction();
						
						Owner clonedOwner = deepClone(clonedOwnersMap.get(owner));
						List<Owner> clonedOtherOwners = deepClone(clonedOtherOwnersMap.get(owner));
						
						for(Owner clonedOtherOwner : clonedOtherOwners) {
							if(!withinRange(clonedOwner.getLocation(), clonedOtherOwner.getLocation(), 50.0))
								clonedOtherOwner.setCurrentSignal("_");
							for(Dog dog : clonedOtherOwner.getDogs())
								dog.setExerciseTimeRemaining(0.0);
							clonedOtherOwner.setAllExerciseCompletionTime(Double.MAX_VALUE);
							clonedOtherOwner.setAllExerciseCompleted(false);
						}
												
						Directive directive = playerWrapper.chooseDirective(round, clonedOwner, clonedOtherOwners);
						Instruction instruction = directive.instruction;
						if(instruction != null) {
							owner.setCurrentAction(instruction);
							owner.setCurrentSignal("_");
						}
						if(instruction.equals(Instruction.CALL_SIGNAL)) {
							String signal = directive.signalWord;
							if(Dictionary.isInDictionary(signal.toLowerCase())) {
								owner.setCurrentSignal(signal);
								Log.writeToVerboseLogFile("Owner " + owner.getNameAsString() + "'s signal word: \"" + signal + "\"");								
							}
							else {
								Log.writeToVerboseLogFile("Owner " + owner.getNameAsString() + "'s signal word \"" + signal + "\" is not found in the dictionary!");
								owner.setCurrentAction(Instruction.NOTHING);
							}
						}
						else if(instruction.equals(Instruction.EXIT_PARK)) {
							if(owner.getLocation().equals(new ParkLocation(0.0, 0.0))) {
								boolean allDogsWaitingForTheirOwner = true;
								for(Dog dog : owner.getDogs()) {
									if(!dog.isWaitingForItsOwner()) {
										Log.writeToVerboseLogFile("The owner " + owner.getNameAsString() + " cannot exit the park because not all dogs are back!");
										owner.setCurrentAction(Instruction.NOTHING);
										allDogsWaitingForTheirOwner = false;
										break;
									}
								}
								if(allDogsWaitingForTheirOwner)
									ownersThatExitedPark.add(owner);
								Log.writeToVerboseLogFile("Owner " + owner.getNameAsString() + " is exiting the park.");								
							}							
							else {
								Log.writeToVerboseLogFile("The owner cannot exit the park, as it is at location " + owner.getLocationAsString() + " instead of (0, 0)!");								
								owner.setCurrentAction(Instruction.NOTHING);
							}
						}
						else if(instruction.equals(Instruction.NOTHING)) {
							Log.writeToVerboseLogFile("Owner " + owner.getNameAsString() + " has decided to do nothing.");
							continue;
						}
						else if(instruction.equals(Instruction.THROW_BALL)) {
							ParkLocation ballLocation = directive.parkLocation;
							Dog dogToPlayWith = getOriginalDogFromClonedDog(directive.dogToPlayWith);
							if(dogToPlayWith == null || !allDogs.contains(dogToPlayWith)) {
								Log.writeToVerboseLogFile("Either no dog specified to throw ball, or the dog specified to throw ball does not exist!");
								owner.setCurrentAction(Instruction.NOTHING);
								continue;
							}
							else if(!dogToPlayWith.isWaitingForOwner(owner)) {
								Log.writeToVerboseLogFile("The dog specified is not waiting for its owner!");
								owner.setCurrentAction(Instruction.NOTHING);
								continue;
							}
							else if(!dogToPlayWith.hasBall()) {
								Log.writeToVerboseLogFile("The dog specified does not have a ball!");
								owner.setCurrentAction(Instruction.NOTHING);
								continue;
							}
							else if(!isWithinBounds(ballLocation)) {
								Log.writeToVerboseLogFile("The ball target location specified must be within bounds of the park!");
								owner.setCurrentAction(Instruction.NOTHING);
								continue;
							}
							else if(!withinRange(owner.getLocation(), ballLocation, 40.0)) {
								Log.writeToVerboseLogFile("The ball target location specified must be within 40 meters of the owner's position!");
								owner.setCurrentAction(Instruction.NOTHING);
								continue;
							}
	
							Ball ballToThrow = dogToBallMap.get(dogToPlayWith);
							
							double randomDistance = random.nextDouble();
							double randomAngle = Math.toRadians(random.nextDouble() * 360);
							double newRow = ballLocation.getRow() + randomDistance * Math.sin(randomAngle);
							double newColumn = ballLocation.getColumn() + randomDistance * Math.cos(randomAngle);
							if(newRow < 0.0)
								newRow = 0.0;
							if(newRow > ParkLocation.PARK_SIZE - 1 + dataError)
								newRow = ParkLocation.PARK_SIZE - 1;
							if(newColumn < 0.0)
								newColumn = 0.0;
							if(newColumn > ParkLocation.PARK_SIZE - 1 + dataError)
								newColumn = ParkLocation.PARK_SIZE - 1;
							
							dogToPlayWith.setHeadingForBall();
							ballToThrow.setLocation(newRow, newColumn);
	
							Log.writeToVerboseLogFile("Owner " + owner.getNameAsString() + " has thrown dog " + dogToPlayWith.getBreed() + " " + dogToPlayWith.getRealID() + "'s ball to " + ballToThrow.getLocationAsString() + ".");
						}
						else if(instruction.equals(Instruction.MOVE)) {
							ParkLocation newLocation = directive.parkLocation;
							if(!isWithinBounds(newLocation)) {
								Log.writeToVerboseLogFile("The owner's new position must be within bounds of the park!");
								owner.setCurrentAction(Instruction.NOTHING);
								continue;
							}
							else if(!withinRange(owner.getLocation(), newLocation, 5.0)) {
								Log.writeToVerboseLogFile("The owner's new position must be within 5 meters of the current position!");
								owner.setCurrentAction(Instruction.NOTHING);
								continue;
							}
							
							ownerToMoveLocationMap.put(owner, newLocation);
							
							for(Dog dog : allDogs) {
								if(!dog.isWaitingForOwner(owner))
									continue;
								if(!owner.hasDog(dog)) {
									dog.setHeadingForPerson(dog.getOwner());
									continue;
								}
								
								double distance = getDistance(dog.getLocation(), owner.getLocation());
								double dogOldRow = dog.getLocation().getRow();
								double dogOldColumn = dog.getLocation().getColumn();
								double ownerNewRow  = newLocation.getRow();
								double ownerNewColumn = newLocation.getColumn();
								double dogNewRow = distance == 0 ? dogOldRow : dogOldRow + (ownerNewRow - dogOldRow) * (distance - 1) / distance;
								double dogNewColumn = distance == 0 ? dogOldColumn : dogOldColumn + (ownerNewColumn - dogOldColumn) * (distance - 1) / distance;
								if(dogNewRow < 0.0)
									dogNewRow = 0.0;
								if(dogNewRow > ParkLocation.PARK_SIZE - 1 + dataError)
									dogNewRow = ParkLocation.PARK_SIZE - 1;
								if(dogNewColumn < 0.0)
									dogNewColumn = 0.0;
								if(dogNewColumn > ParkLocation.PARK_SIZE - 1 + dataError)
									dogNewColumn = ParkLocation.PARK_SIZE - 1;
	
								dogToMoveLocationMap.put(dog, new ParkLocation(dogNewRow, dogNewColumn));
							}
							Log.writeToVerboseLogFile("Owner " + owner.getNameAsString() + " is moving to " + owner.getLocationAsString() + ".");
						}
						else {
							Log.writeToVerboseLogFile("The instruction specified is either unknown or null! Instruction \"nothing\" chosen.");
							owner.setCurrentAction(Instruction.NOTHING);
							continue;
						}
					}					
				}
			}
			
			for(PlayerWrapper playerWrapper : playerWrappers) {
				Owner owner = playerWrapperToOwnerMap.get(playerWrapper);
				
				if(ownerToMoveLocationMap.containsKey(owner)) {
					ParkLocation newLocation = ownerToMoveLocationMap.get(owner);
					if(withinRange(owner.getLocation(), newLocation, (double) 1.0 / inverseGranularity)) {
						owner.setLocation(newLocation);
						ownerToMoveLocationMap.remove(owner);
					}
					else {
						double distance = getDistance(owner.getLocation(), newLocation);
						double ownerOldRow = owner.getLocation().getRow();
						double ownerOldColumn = owner.getLocation().getColumn();
						double ownerNewRow = newLocation.getRow();
						double ownerNewColumn = newLocation.getColumn();
						double ownerIntermediateRow = distance == 0 ? ownerOldRow : ownerOldRow + (ownerNewRow - ownerOldRow) / (distance * inverseGranularity);
						double ownerIntermediateColumn = distance == 0 ? ownerOldColumn : ownerOldColumn + (ownerNewColumn - ownerOldColumn) / (distance * inverseGranularity);
						
						owner.setLocation(new ParkLocation(ownerIntermediateRow, ownerIntermediateColumn));
					}						
				}
				
				for(Dog dog : owner.getDogs()) {
					Ball ball = dogToBallMap.get(dog);
					if(dogToMoveLocationMap.containsKey(dog)) {
						ParkLocation newLocation = dogToMoveLocationMap.get(dog);
						if(withinRange(dog.getLocation(), newLocation, (double) dog.getWalkingSpeed() / inverseGranularity)) {
							dog.setLocation(newLocation);
							ball.setLocation(newLocation);
							dog.setWaitingForPerson(owner);
							dogToMoveLocationMap.remove(dog);
						}
						else {
							double distance = getDistance(dog.getLocation(), newLocation);
							double dogOldRow = dog.getLocation().getRow();
							double dogOldColumn = dog.getLocation().getColumn();
							double dogNewRow = newLocation.getRow();
							double dogNewColumn = newLocation.getColumn();
							double dogIntermediateRow = distance == 0 ? dogOldRow : dogOldRow + (dogNewRow - dogOldRow) * dog.getWalkingSpeed() / (distance * inverseGranularity);
							double dogIntermediateColumn = distance == 0 ? dogOldColumn : dogOldColumn + (dogNewColumn - dogOldColumn) * dog.getWalkingSpeed() / (distance * inverseGranularity);
							
							ParkLocation intermediateLocation = new ParkLocation(dogIntermediateRow, dogIntermediateColumn);
							dog.setLocation(intermediateLocation);
							ball.setLocation(intermediateLocation);
						}
					}
					else if(dog.isHeadingForBall() && !dog.isStationary()) {
						double speed = dog.isRunning() ? dog.getRunningSpeed() : dog.isWalking() ? dog.getWalkingSpeed() : 0.0;
						if(withinRange(dog.getLocation(), ball.getLocation(), (double) speed / inverseGranularity)) {
							dog.setLocation(ball.getLocation());
							dog.setHasBall(true);
							if(dog.isRunning())
								dog.decreaseExerciseTimeRemaining(1.0 / inverseGranularity);
							dog.setStationary();
						}
						else {
							double distance = getDistance(dog.getLocation(), ball.getLocation());
							double dogOldRow = dog.getLocation().getRow();
							double dogOldColumn = dog.getLocation().getColumn();
							double dogNewRow = ball.getLocation().getRow();
							double dogNewColumn = ball.getLocation().getColumn();
							double dogIntermediateRow = distance == 0 ? dogOldRow : dogOldRow + (dogNewRow - dogOldRow) * speed / (distance * inverseGranularity);
							double dogIntermediateColumn = distance == 0 ? dogOldColumn : dogOldColumn + (dogNewColumn - dogOldColumn) * speed / (distance * inverseGranularity);
							
							dog.setLocation(new ParkLocation(dogIntermediateRow, dogIntermediateColumn));
							if(dog.isRunning())
								dog.decreaseExerciseTimeRemaining(1.0 / inverseGranularity);
						}
					}
					else if(dog.isHeadingForPerson()) {
						double speed = dog.getWalkingSpeed();
						Owner ownerHeadingFor = dog.getOwnerHeadingFor();
						
						double fullDistance = getDistance(dog.getLocation(), ownerHeadingFor.getLocation());
						double dogOldRow = dog.getLocation().getRow();
						double dogOldColumn = dog.getLocation().getColumn();
						double ownerNewRow = ownerHeadingFor.getLocation().getRow();
						double ownerNewColumn = ownerHeadingFor.getLocation().getColumn();
						double dogNewRow = fullDistance == 0 ? dogOldRow : dogOldRow + (ownerNewRow - dogOldRow) * (fullDistance - 1) / fullDistance;
						double dogNewColumn = fullDistance == 0 ? dogOldColumn : dogOldColumn + (ownerNewColumn - dogOldColumn) * (fullDistance - 1) / fullDistance;
						if(dogNewRow < 0.0)
							dogNewRow = 0.0;
						if(dogNewRow > ParkLocation.PARK_SIZE - 1 + dataError)
							dogNewRow = ParkLocation.PARK_SIZE - 1;
						if(dogNewColumn < 0.0)
							dogNewColumn = 0.0;
						if(dogNewColumn > ParkLocation.PARK_SIZE - 1 + dataError)
							dogNewColumn = ParkLocation.PARK_SIZE - 1;
						ParkLocation newLocation = new ParkLocation(dogNewRow, dogNewColumn);
											
						if(withinRange(dog.getLocation(), newLocation, (double) speed / inverseGranularity)) {
							dog.setLocation(newLocation);
							ball.setLocation(newLocation);
							dog.setWaitingForPerson(dog.getOwnerHeadingFor());
						}
						else {
							double requiredDistance = getDistance(dog.getLocation(), newLocation);
							double dogIntermediateRow = requiredDistance == 0 ? dogOldRow : dogOldRow + (dogNewRow - dogOldRow) * speed / (requiredDistance * inverseGranularity);
							double dogIntermediateColumn = requiredDistance == 0 ? dogOldColumn : dogOldColumn + (dogNewColumn - dogOldColumn) * speed / (requiredDistance * inverseGranularity);
							
							ParkLocation intermediateLocation = new ParkLocation(dogIntermediateRow, dogIntermediateColumn);
							dog.setLocation(intermediateLocation);
							ball.setLocation(intermediateLocation);
						}
					}
					else if(dog.isWaitingForPerson() && !dog.isWaitingForItsOwner()) {
						if(dog.getOwnerWaitingFor().getCurrentAction().equals(Instruction.MOVE))
							dog.setHeadingForPerson(dog.getOwner());
						else if(dog.getWaitingTimeRemaining() == 0.0)
							dog.setHeadingForPerson(dog.getOwner());
						else
							dog.decreaseWaitingTimeRemaining(1.0 / inverseGranularity);
					}
				}
			}	
			
			for(Dog dog : allDogs) {
				Owner owner = dog.getOwner();
				List<Owner> otherOwners = new ArrayList<>(allOwners);
				otherOwners.remove(owner);

				if(dog.hasBall() && !dog.isHeadingForPerson() && !dog.isWaitingForPerson()) {
					if(withinRange(dog.getLocation(), owner.getLocation(), 10.0))
						dog.setHeadingForPerson(owner);
					else {
						double distance = Double.MAX_VALUE;
						Owner bestOwner = null;
						for(Owner otherOwner : otherOwners) {
							if(withinRange(dog.getLocation(), otherOwner.getLocation(), Math.min(10.0, distance))) {
								distance = getDistance(dog.getLocation(), otherOwner.getLocation());
								bestOwner = otherOwner;
							}
						}
						if(bestOwner != null)
							dog.setHeadingForPerson(bestOwner);
						else
							dog.setHeadingForPerson(dog.getOwner());
					}
				}
				else if(dog.isHeadingForBall()) {
					List<Dog> otherDogs = new ArrayList<>(allDogs);
					otherDogs.remove(dog);

					boolean dogObstacleFound = false;
					for(Dog otherDog : otherDogs) {
						if(withinRange(dog.getLocation(), otherDog.getLocation(), 1.0)) {
							dog.setWalking();
							dogObstacleFound = true;
							break;
						}
					}
					if(dogObstacleFound)
						continue;
					boolean ownerObstacleFound = false;
					for(Owner anyOwner : allOwners) {
						if(withinRange(dog.getLocation(), anyOwner.getLocation(), 1.0)) {
							dog.setWalking();
							ownerObstacleFound = true;
							break;
						}							
					}
					if(ownerObstacleFound)
						continue;
					dog.setRunning();
				}
			}
			
			for(Owner owner : allOwners) {
				if(owner.allExerciseCompleted())
					continue;
				boolean allOwnerExerciseCompleted = true;
				for(Dog dog : owner.getDogs()) {
					if(Dog.TOTAL_EXERCISE_TIME - dog.getExerciseTimeCompleted() > dataError) {
						allOwnerExerciseCompleted = false;
						break;
					}
				}
				
				if(allOwnerExerciseCompleted) {
					owner.setAllExerciseCompleted(true);
					owner.setAllExerciseCompletionTime(currentStep * 1.0 / inverseGranularity);
				}
			}
			
			allExerciseCompleted = true;
			for(Owner owner : allOwners) {
				if(!owner.allExerciseCompleted()) {
					allExerciseCompleted = false;
					break;
				}
			}
			if(allExerciseCompleted)
				break;
			
			if(currentStep % inverseGranularity == 0) {
				int round = currentStep / inverseGranularity;
			    if(showGUI)
					updateGUI(server, getGUIState(round, ownersThatExitedPark));				
			}
		}
		
	    if(showGUI)
			updateGUI(server, getGUIState((int) Math.floor(currentStep / inverseGranularity), ownersThatExitedPark));

		
		DecimalFormat decimalFormat = new DecimalFormat("######.####");
		
		if(allExerciseCompleted)
			Log.writeToLogFile("All exercise has been completed!");
		else
			Log.writeToLogFile("Not all exercise has been completed!");
		for(Owner owner : allOwners) {
			List<Owner> otherOwners = new ArrayList<>(allOwners);
			otherOwners.remove(owner);

			String teamName = ownerToPlayerWrapperMap.get(owner).getPlayerName();
			
			double exerciseT = getExerciseT(owner);
			double exerciseA = getExerciseA(otherOwners);
			double exerciseScore = 0.5 * (exerciseT + exerciseA);
			
			Log.writeToLogFile("Owner: " + owner.getNameAsString() + " (" + teamName + ")");
			Log.writeToLogFile("\tScore: " + decimalFormat.format(exerciseScore) + " seconds");
			Log.writeToLogFile("\tT: " + decimalFormat.format(exerciseT) + " seconds");
			Log.writeToLogFile("\tA: " + decimalFormat.format(exerciseA) + " seconds");
			
			for(Dog dog : owner.getDogs())
				Log.writeToLogFile("\t" + dog.getBreed().name() + " " + dog.getRealID() + ": " + decimalFormat.format(100 * dog.getExerciseTimeCompleted() / Dog.TOTAL_EXERCISE_TIME) + "% exercise completed (" + decimalFormat.format(dog.getExerciseTimeCompleted()) + " seconds)");					
		}
		
		
		if(!showGUI)
			System.exit(0);
	}
	
	private static double getDistance(ParkLocation parkLocation1, ParkLocation parkLocation2) {
		double deltaRow = parkLocation2.getRow() - parkLocation1.getRow();
		double deltaColumn = parkLocation2.getColumn() - parkLocation1.getColumn();
		
		return Math.sqrt(Math.pow(deltaRow, 2) + Math.pow(deltaColumn, 2));
	}
	
	private static boolean withinRange(ParkLocation parkLocation1, ParkLocation parkLocation2, double range) {		
		return getDistance(parkLocation1, parkLocation2) <= (range + dataError);
	}
	
	private static boolean isWithinBounds(ParkLocation parkLocation) {
		double parkSize = ParkLocation.PARK_SIZE;
		
		return parkLocation.getRow() >= 0.0 && parkLocation.getRow() <= (parkSize - 1 + dataError) && 
				parkLocation.getColumn() >= 0.0 && parkLocation.getColumn() <= (parkSize - 1 + dataError);
	}
	
	private static Dog getOriginalDogFromClonedDog(Dog clonedDog) {
		if(clonedDog == null)
			return null;
		for(Dog dog : allDogs) {
			if(clonedDog.getRandomID().equals(dog.getRandomID()) &&
					clonedDog.getBreed().equals(dog.getBreed()) && 
					clonedDog.getOwner().getNameAsString().equals(dog.getOwner().getNameAsString()))
				return dog;
		}
		return null;
	}
	
	private static double getExerciseT(Owner owner) {
		double allExerciseCompletionTime = owner.getAllExerciseCompletionTime();
		if(allExerciseCompletionTime != Double.MAX_VALUE)
			return allExerciseCompletionTime;
		
		double computedExerciseT = rounds;
		for(Dog dog : owner.getDogs())
			computedExerciseT += dog.getExerciseTimeRemaining() * 2;
		return computedExerciseT;
	}
	
	private static double getExerciseA(List<Owner> owners) {
		if(owners.size() == 0)
			return 0;
		
		double averageExerciseT = 0.0;
		for(Owner owner : owners)
			averageExerciseT += getExerciseT(owner);
		averageExerciseT /= owners.size();
		
		return averageExerciseT;
	}
	
	private static String cleanName(String playerName) {
		String cleanedPlayerName = " ";
		if(playerName.contains("_")) {
			Integer index = playerName.lastIndexOf("_");
			cleanedPlayerName = playerName.substring(0, index);
		}
		else
			return playerName;

		return cleanedPlayerName;
	}	
	
	private static <T extends Object> T deepClone(T obj) {
        if(obj == null)
            return null;

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            
            return (T) objectInputStream.readObject();
        }
        catch(Exception e) {
            return null;
        }
	}
	
	private static PlayerWrapper loadPlayerWrapper(String playerName, String modifiedPlayerName) throws Exception {
		Log.writeToLogFile("Loading team " + playerName + "...");

		int teamID = playerWrappers.size() + 1;
		Player player = loadPlayer(playerName, teamID);
        if(player == null) {
            Log.writeToLogFile("Cannot load team " + playerName + "!");
            System.exit(1);
        }

        return new PlayerWrapper(player, modifiedPlayerName, timeout);
    }
	
	private static Player loadPlayer(String playerName, int teamID) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		String playerPackagePath = sourcePath + File.separator + "dogs" + File.separator + playerName;
        Set<File> playerFiles = getFilesInDirectory(playerPackagePath, ".java");
		String simPath = sourcePath + File.separator + "dogs" + File.separator + "sim";
        Set<File> simFiles = getFilesInDirectory(simPath, ".java");

        File classFile = new File(playerPackagePath + File.separator + "Player.class");

        long classModified = classFile.exists() ? classFile.lastModified() : -1;
        if(classModified < 0 || classModified < lastModified(playerFiles) || classModified < lastModified(simFiles)) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if(compiler == null)
                throw new IOException("Cannot find the Java compiler!");

            StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, null);
            Log.writeToLogFile("Compiling for team " + playerName + "...");

            if(!compiler.getTask(null, manager, null, null, null, manager.getJavaFileObjectsFromFiles(playerFiles)).call())
                throw new IOException("The compilation failed!");
            
            classFile = new File(playerPackagePath + File.separator + "Player.class");
            if(!classFile.exists())
                throw new FileNotFoundException("The class file is missing!");
        }
        
        ClassLoader loader = Simulator.class.getClassLoader();
        if(loader == null)
            throw new IOException("Cannot find the Java class loader!");

        @SuppressWarnings("rawtypes")
        Class rawClass = loader.loadClass("dogs." + playerName + ".Player");
        Class[] classArgs = new Class[]{Integer.class, Integer.class, Integer.class, Integer.class, Random.class, SimPrinter.class};
        
        return (Player) rawClass.getDeclaredConstructor(classArgs).newInstance(rounds, numDogsPerOwner, numOwners, seed, random, new SimPrinter(enablePrints));
    }

	private static long lastModified(Iterable<File> files) {
        long lastDate = 0;
        for(File file : files) {
            long date = file.lastModified();
            if(lastDate < date)
                lastDate = date;
        }
        return lastDate;
    }
	
	private static Set<File> getFilesInDirectory(String path, String extension) {
		Set<File> files = new HashSet<File>();
        Set<File> previousDirectories = new HashSet<File>();
        previousDirectories.add(new File(path));
        do {
        	Set<File> nextDirectories = new HashSet<File>();
            for(File previousDirectory : previousDirectories)
                for(File file : previousDirectory.listFiles()) {
                    if(!file.canRead())
                    	continue;
                    
                    if(file.isDirectory())
                        nextDirectories.add(file);
                    else if(file.getPath().endsWith(extension))
                        files.add(file);
                }
            previousDirectories = nextDirectories;
        } while(!previousDirectories.isEmpty());
        
        return files;
	}
	
	private static void updateGUI(HTTPServer server, String content) {
		if(server == null)
			return;
		
        String guiPath = null;
        while(true) {
            while(true) {
                try {
                	guiPath = server.request();
                    break;
                } catch(IOException e) {
                    Log.writeToVerboseLogFile("HTTP request error: " + e.getMessage());
                }
            }
            
            if(guiPath.equals("data.txt")) {
                try {
                    server.reply(content);
                } catch(IOException e) {
                    Log.writeToVerboseLogFile("HTTP dynamic reply error: " + e.getMessage());
                }
                return;
            }
            
            if(guiPath.equals(""))
            	guiPath = "webpage.html";
            else if(!Character.isLetter(guiPath.charAt(0))) {
                Log.writeToVerboseLogFile("Potentially malicious HTTP request: \"" + guiPath + "\"");
                break;
            }

            try {
                File file = new File(staticsPath + File.separator + guiPath);
                server.reply(file);
            } catch(IOException e) {
                Log.writeToVerboseLogFile("HTTP static reply error: " + e.getMessage());
            }
        }		
	}
	
	private static String getGUIState(Integer currentRound, List<Owner> ownersThatExitedPark) throws JSONException {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("refresh", 1000.0 / fps);
		jsonObj.put("totalRounds", rounds);
		jsonObj.put("currentRound", currentRound);
		jsonObj.put("numDogsPerOwner", numDogsPerOwner);
		jsonObj.put("numOwners", numOwners);
		jsonObj.put("granularity", 1.0 / inverseGranularity);
		
		DecimalFormat decimalFormat = new DecimalFormat("######.####");
		
		JSONObject ownersObj = new JSONObject();
		for(Owner owner : allOwners) {
			List<Owner> otherOwners = new ArrayList<>(allOwners);
			otherOwners.remove(owner);
			
			JSONObject ownerObj = new JSONObject();
			
			String ownerNameAsString = owner.getNameAsString();
			String ownerLocationAsString = owner.getLocationAsString();
			String ownerCurrentActionAsString = owner.getCurrentAction().name();
			String ownerCurrentSignal = owner.getCurrentSignal();
			String ownerTeam = ownerToPlayerWrapperMap.get(owner).getPlayerName();
			double ownerActionTimeRemaining = owner.getActionTimeRemaining();
			double exerciseT = getExerciseT(owner);
			double exerciseA = getExerciseA(otherOwners);
			double exerciseScore = 0.5 * (exerciseT + exerciseA);
			
			JSONArray dogsArray = new JSONArray();
			for(Dog dog : owner.getDogs()) {
				JSONObject dogObj = new JSONObject();
				
				int dogRealID = dog.getRealID();
				String dogBreedAsString = dog.getBreed().name();
				String dogOwnerNameAsString = dog.getOwner().getNameAsString();
				String dogOwnerWaitingForNameAsString = dog.isWaitingForPerson() ? dog.getOwnerWaitingFor().getNameAsString() : "";
				String dogOwnerHeadingForNameAsString = dog.isHeadingForPerson() ? dog.getOwnerHeadingFor().getNameAsString() : "";
				String dogLocationAsString = dog.getLocationAsString();
				String ballLocationAsString = dogToBallMap.get(dog).getLocationAsString();				
				boolean dogIsRunning = dog.isRunning();
				boolean dogIsWalking = dog.isWalking();
				boolean dogIsStationary = dog.isStationary();
				boolean dogIsHeadingForBall = dog.isHeadingForBall();
				boolean dogIsHeadingForPerson = dog.isHeadingForPerson();
				boolean dogIsWaitingForPerson = dog.isWaitingForPerson();
				boolean dogHasBall = dog.hasBall();
				
				dogObj.put("breed", dogBreedAsString.substring(0, 1) + dogBreedAsString.substring(1).toLowerCase());
				dogObj.put("realID", dogRealID);
				dogObj.put("owner", dogOwnerNameAsString);
				dogObj.put("ownerWaitingFor", dogOwnerWaitingForNameAsString);
				dogObj.put("ownerHeadingFor", dogOwnerHeadingForNameAsString);
				dogObj.put("location", dogLocationAsString);
				dogObj.put("ballLocation", ballLocationAsString);
				dogObj.put("isRunning", dogIsRunning);
				dogObj.put("isWalking", dogIsWalking);
				dogObj.put("isStationary", dogIsStationary);
				dogObj.put("isHeadingForBall", dogIsHeadingForBall);
				dogObj.put("isHeadingForPerson", dogIsHeadingForPerson);
				dogObj.put("isWaitingForPerson", dogIsWaitingForPerson);
				dogObj.put("hasBall", dogHasBall);
				dogObj.put("maxWaitingTime", decimalFormat.format(Dog.MAX_WAITING_TIME));
				dogObj.put("totalExerciseTime", decimalFormat.format(Dog.TOTAL_EXERCISE_TIME));
				dogObj.put("totalExerciseCompleted", decimalFormat.format(dog.getExerciseTimeCompleted()));
				dogObj.put("completedExercise", Dog.TOTAL_EXERCISE_TIME - dog.getExerciseTimeCompleted() <= dataError);
				dogObj.put("percentageExerciseCompleted", decimalFormat.format(100 * dog.getExerciseTimeCompleted() / Dog.TOTAL_EXERCISE_TIME));
				
				dogsArray.put(dogObj);				
			}
			
			ownerObj.put("dogs", dogsArray);
			ownerObj.put("location", ownerLocationAsString);
			ownerObj.put("action", ownerCurrentActionAsString);
			ownerObj.put("actionTimeRemaining", ownerActionTimeRemaining);
			ownerObj.put("signal", ownerCurrentSignal);
			ownerObj.put("team", ownerTeam);
			ownerObj.put("T", decimalFormat.format(exerciseT));
			ownerObj.put("A", decimalFormat.format(exerciseA));
			ownerObj.put("score", decimalFormat.format(exerciseScore));
			
			if(ownersThatExitedPark.contains(owner))
				ownerObj.put("exitedPark", true);
			else
				ownerObj.put("exitedPark", false);
						
			ownersObj.put(ownerNameAsString, ownerObj);			
		}
		jsonObj.put("owners", ownersObj);

		return jsonObj.toString();
	}
	
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, JSONException {
		setup();
		parseCommandLineArguments(args);
		runSimulation();
	}
}