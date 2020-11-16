package dogs.sim;

import java.util.List;


public class PlayerWrapper {
	private Timer timer;
    private Player player;
    private String playerName;
    private long timeout;

    public PlayerWrapper(Player player, String playerName, long timeout) {
        this.player = player;
        this.playerName = playerName;
        this.timeout = timeout;
        this.timer = new Timer();
    }

    public Directive chooseDirective(Integer round, Owner myOwner, List<Owner> otherOwners) {
    	
    	Log.writeToVerboseLogFile("Team " + this.playerName + " choosing a directive for round " + round + "...");
        
    	Directive directive = new Directive();

        try {
            if(!timer.isAlive())
            	timer.start();
            timer.callStart(() -> { return player.chooseDirective(round, myOwner, otherOwners); });
            directive = timer.callWait(timeout);
        }
        catch(Exception e) {
            Log.writeToVerboseLogFile("Team " + this.playerName + " generated an exception while choosing a directive.");
            Log.writeToVerboseLogFile("Exception for team " + this.playerName + ": " + e);
        }

        return directive;
    }
           
    public Player getPlayer() {
    	return player;
    }

    public String getPlayerName() {
        return playerName;
    }
}