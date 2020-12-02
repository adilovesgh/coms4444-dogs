run:
	java -cp .:dogs/org.json.jar dogs.sim.Simulator -n g5 g5 g5 g5 g5 g5 g5 -d rconfig.dat -r 7200 -y 0.01 -s 42 -l log.txt -v

gui:
	java -cp .:dogs/org.json.jar dogs.sim.Simulator -n random random random random -d simpleConfig.dat -r 7200 -y 0.01 -s 42 -l log.txt --gui -f 60 -v

compile:
	javac -cp .:dogs/org.json.jar dogs/*/*.java

clean:
	rm dogs/*/*.class
]