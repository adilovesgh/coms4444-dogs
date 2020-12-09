run:
	java -cp .:dogs/org.json.jar dogs.sim.Simulator -n random random random random -d simpleConfig.dat -r 3600 -y 0.01 -s 42 -l log.txt -v

gui:
	java -cp .:dogs/org.json.jar dogs.sim.Simulator -n random random random random -d simpleConfig.dat -r 3600 -y 0.01 -s 42 -l log.txt --gui -f 60 -v

compile:
	javac -cp .:dogs/org.json.jar dogs/*/*.java

clean:
	rm dogs/*/*.class