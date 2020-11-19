# Project 4: Exercising the Dogs

## Course Summary

Course: COMS 4444 Programming and Problem Solving (Fall 2020)  
Website: http://www.cs.columbia.edu/~kar/4444f20  
University: Columbia University  
Instructor: Prof. Kenneth Ross  
TA: Aditya Sridhar

## Project Description

Please see the [course webpage](http://www.cs.columbia.edu/~kar/4444f20/node22.html) for a full project description.

## Implementation

You will be creating your own player that extends the simulator's abstract player. Please follow these steps to begin your implementation:
1.  Enter the `coms4444-dogs/src/dogs` source directory, and create a folder called "g*x*" (where *x* is the number of your team). For example, if you are team "g5," please create a folder called "g5" in the `src/dogs` directory.
2.  Create a Java file called `Player.java` inside your newly-created folder, and copy the following code into `Player.java` (the TODOs indicate all changes you need to make):

```
package dogs.gx; // TODO modify the package name to reflect your team

import java.util.*;

import dogs.sim.Directive;
import dogs.sim.Owner;
import dogs.sim.SimPrinter;


public class Player extends dogs.sim.Player {
	
    /**
     * Player constructor
     *
     * @param rounds           number of rounds
     * @param numDogsPerOwner  number of dogs per owner
     * @param numOwners	       number of owners
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
    
    	// TODO add your code here to choose a directive
	
        return null; // TODO modify the return statement to return your directive
    }
}
```

Note that the constructor contains all of the global information that you need, including the total number of rounds (`rounds`), the number of dogs for each owner (`numDogsPerOwner`), the number of owners (`numOwners`), the random seed (`seed`), the random generator itself (`random`), and the printing library (`simPrinter`). They can be accessed as public fields of the `Player` superclass.

## Submission
You will be submitting your created team folder, which includes the implemented `Player` class and any other helper classes you create. We ask that you please do not modify any code in the `sim` or `random` directories, especially the simulator, when you submit your code. This makes it easier for us to merge in your code.

To submit your code for each class and for the final deliverable of the project, you will create a pull request to merge your forked repository's *master* branch into the TA's base repository's *master* branch. The TA will merge the commits from the pull request after the deliverable deadline has passed. The base repository will be updated before the start of the next class meeting.

In order to improve performance and readability of code during simulations, we would like to prevent flooding the console with print statements. Therefore, we have provided a printer called `SimPrinter` to allow for toggled printing to the console. When adding print statements for testing/debugging in your code, please make sure to use the methods in `SimPrinter` (instance available in `Player`) rather than use `System.out` statements directly. Additionally, please set the `enablePrints` default variable in `Simulator` to *true* in order to enable printing. This also allows us to not require that you comment out any print statements in your code submissions.

## Simulator

#### Steps to run the simulator:
1.  On your command line, *fork* the Git repository, and then clone the forked version. Do NOT clone the original repository.
2.  Enter `cd coms4444-dogs/src` to enter the source folder of the repository.
3.  Run `make clean` and `make compile` to clean and compile the code.
4.  Update the make file (file called `Makefile`) with the teams participating in the simulation, as well as with any simulator arguments.
5.  Run one of the following:
    * `make run`: run the simulation and view the results/exercise scores from the command line
    * `make gui`: run the simulation and view the results/exercise scores from the GUI

#### Simulator arguments:
> **[-r | --rounds]**: number of rounds (default = 3600)

> **[-n | --owners]**: space-separated owners

> **[-d PATH | --dogs PATH]**: path to the configuration file of dog breed mixes

> **[-s | --seed]**: seed value for random player (default = 42)

> **[-l PATH | --log PATH]**: enable logging and output log to both console and log file

> **[-v | --verbose]**: record verbose log when logging is enabled (default = false)

> **[-g | --gui]**: enable GUI (default = false)

> **[-y | --granularity]**: granularity (inverse number of steps per round) of simulation (default = 0.01)

> **[-f | --fps]**: speed (frames per second) of GUI (default = 30)


## Dog Breed Configuration

A configuration file (*.dat* extension) contains the set of dog breed mixes across all owners. Dog breeds are represented as a list of space-separated strings (case-insensitive), each indicating one of the four available dog breeds: *labrador*, *poodle*, *spaniel*, and *terrier*. Each owner has a separate list of dog breeds, which may differ among owners; however, the total number of dog breeds listed must be the same for each owner.

An example of a dog breed configuration is as follows:

```
LABRADOR poodle Spaniel TERRIER
POODLE POODLE POODLE POODLE
terrier spaniel SPANIEL labrador
Poodle terrier Labrador spaniel
terrier terrier SPANIEL Terrier
poodle poodle poodle Poodle
```

The example contains 6 owners, each having 4 dogs. Note that you can have owners with the same dog breed mix (owners 2 and 6, owners 1 and 4) and owners with multiple dogs of the same breed (owners 2, 3, 5, and 6).

If you would like to create a new configuration, please add it to the `src/configs/` directory. The `simpleConfig.dat` configuration file has already been added for you, and it is the configuration used by the random player. Keep in mind that everyone will be sharing this directory for their configurations.


## Dictionary

The simulator provides a dictionary of nearly 235,000 inoffensive English words, including the names of the available owners, that can be used for signalling; the full list of words can be found in `src/dogs/sim/dictionary.txt`. You can also check to see if your word or words are in the dictionary by calling the `isInDictionary` and `areAllInDictionary` methods of the `Dictionary` class, respectively. The `Dictionary` class also provides a few other simple methods to retrieve information about the dictionary. "_" in the dictionary signifies an empty string or a no-op, indicating that no signal is given; this is the default signal.

## GUI Features

The GUI is divided into two sections. In the left section, you can view the simulation of the park and track where your owners and dogs are each round. Owners and dogs enter through the gate (upper-left-hand corner of the park) at the beginning of the simulation and may choose to exit the park at any time during the simulation by exiting through the gate. In the right section, you will be able to view the exercise status board and the signal history window. The exercise status board contains the `T`, `A`, and `score` values for each player in real time; these values depend on the amount of exercise dogs have completed. You can track the status of your dogs' exercise completion by monitoring the respective progress bars for those dogs. Hovering over the images of an owner and its dogs will reveal their current location in the park. Finally, the signal history window shows a running history of words previously signalled by owners; you can view the timestamp, round number, owner, and word for each signal entry.


## API Description

The following provides the API available for students to use:
1. `Dictionary`: a static class to access the available words in the dictionary, including basic functionality to extract words.
2. `Directive`: a directive/command specified by the player, including the instruction, park location (if applicable), the dog to interact with (if applicable), and a word signal (if applicable).
3. `Dog`: a class to represent a specific dog, including functionality to track and maintain the status of that dog.
4. `DogReference`: a simple helper class to extract dog breeds and speeds
5. `Owner`: a class to represent a specific owner, including functionality to track and maintain the status of that owner.
6. `ParkLocation`: a class to represent a specific location in the park, including basic functionality to extract and maintain location information.
7. `Player`: the player abstraction that should be extended by implemented players.
8. `SimPrinter`: contains methods for toggled printing
	* `println`: prints with cursor at start of the next line.
	* `print`: prints with cursor at the end of the current line.

Classes that are used by the simulator include:
1. `Simulator`: the simulator and entry point for the project; manages wrappers for individual players, logging, server, and GUI state.
2. `Ball`: a class to represent a specific dog's tennis ball (not available to students to use, used for behind-the-scenes computation only)
3. `HTTPServer`: a lightweight web server for the simulator.
4. `PlayerWrapper`: a player wrapper that enforces appropriate timeouts on player actions.
5. `Timer`: basic functionality for imposing timeouts.
6. `Log`: basic functionality to log results, with the option to enable verbose logging.


## Piazza
If you have any questions about the project, please post them in the [Piazza forum](https://piazza.com/class/kdjd7v2b8925zz?cid=90) for the course, and an instructor will reply to them as soon as possible. Any updates to the project itself will be available in Piazza.


## Disclaimer
This project belongs to Columbia University. It may be freely used for educational purposes.
