# Project 4: Exercising the Dogs

## Course Summary

Course: COMS 4444 Programming and Problem Solving (Fall 2020)  
Website: http://www.cs.columbia.edu/~kar/4444f20  
University: Columbia University  
Instructor: Prof. Kenneth Ross  
TA: Aditya Sridhar

## Project Description



## Implementation

You will be creating your own player that extends the simulator's abstract player. Please follow these steps to begin your implementation:
1.  Enter the `coms4444-dogs/src` source directory, and create a folder called "g*x*" (where *x* is the number of your team). For example, if you are team "g5," please create a folder called "g5" in the `src` directory.
2.  Create a Java file called `Player.java` inside your newly-created folder, and copy the following code into `Player.java` (the TODOs indicate all changes you need to make):

```
```


## Submission
You will be submitting your created team folder, which includes the implemented `Player` class and any other helper classes you create. We ask that you please do not modify any code in the `sim` or `random` directories, especially the simulator, when you submit your code. This makes it easier for us to merge in your code.

To submit your code for each class and for the final deliverable of the project, you will create a pull request to merge your forked repository's *master* branch into the TA's base repository's *master* branch. The TA will merge the commits from the pull request after the deliverable deadline has passed. The base repository will be updated before the start of the next class meeting.

In order to improve performance and readability of code during simulations, we would like to prevent flooding the console with print statements. Therefore, we have provided a printer called `SimPrinter` to allow for toggled printing to the console. When adding print statements for testing/debugging in your code, please make sure to use the methods in `SimPrinter` (instance available in `Player`) rather than use `System.out` statements directly. Additionally, please set the `enablePrints` default variable in `Simulator` to *true* in order to enable printing. This also allows us to not require that you comment out any print statements in your code submissions.

## Simulator

#### Steps to run the simulator:
1.  On your command line, *fork* the Git repository, and then clone the forked version. Do NOT clone the original repository.
2.  Enter `cd coms4444-dogs/src` to enter the source folder of the repository.
3.  Run `make clean` and `make compile` to clean and compile the code.
4.  Update the make file (file called `Makefile`) with the teams participating in the game, as well as with any simulator arguments.
5.  Run one of the following:
    * `make run`: view results/rankings from the command line
    * `make gui`: view results/rankings from the GUI

#### Simulator arguments:
> **[-r | --rounds]**: number of rounds (default = 1000)

> **[-n | --owners]**: space-separated owners

> **[-s | --seed]**: seed value for random player (default = 10)

> **[-l PATH | --log PATH]**: enable logging and output log to both console and log file

> **[-v | --verbose]**: record verbose log when logging is enabled (default = false)

> **[-g | --gui]**: enable GUI (default = false)

> **[-d | --dogs]**: number of dogs (default = 1)

> **[-f | --fps]**: speed (frames per second) of GUI (default = 20)


## API Description

The following provides the API available for students to use:
1. `Player`: the player abstraction that should be extended by implemented players.
2. `SimPrinter`: contains methods for toggled printing
	* `println`: prints with cursor at start of the next line.
	* `print`: prints with cursor at the end of the current line.

Classes that are used by the simulator include:
1. `Simulator`: the simulator and entry point for the project; manages wrappers for individual players, logging, server, and GUI state.
2. `HTTPServer`: a lightweight web server for the simulator.
3. `PlayerWrapper`: a player wrapper that enforces appropriate timeouts on player actions.
4. `Timer`: basic functionality for imposing timeouts.
5. `Log`: basic functionality to log results, with the option to enable verbose logging.


## Piazza
If you have any questions about the project, please post them in the Piazza forum for the course, and an instructor will reply to them as soon as possible. Any updates to the project itself will be available in Piazza.


## Disclaimer
This project belongs to Columbia University. It may be freely used for educational purposes.
