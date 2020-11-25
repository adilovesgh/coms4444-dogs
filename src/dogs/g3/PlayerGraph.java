package dogs.g3;
import java.util.*;

import dogs.sim.Directive;
import dogs.sim.Directive.Instruction;
import dogs.sim.Owner.OwnerName;
import dogs.sim.Dictionary;
import dogs.sim.Dog;
import dogs.sim.Owner;
import dogs.sim.ParkLocation;
import dogs.sim.SimPrinter;

public class PlayerGraph {
    HashMap<OwnerName, PlayerNode> owners;
    public PlayerGraph(List<Owner> allOwners){
        owners = new HashMap<>();
        for(Owner owner: allOwners){
            PlayerNode player = new PlayerNode(owner, getNeighbors(owner, allOwners));
            owners.put(owner.getNameAsEnum(), player);
        }
        // System.out.println(this.owners.toString());
    }

    private List<Owner> getNeighbors(Owner owner, List<Owner> allOwners){
        return new ArrayList<>();
    }

}

class PlayerNode{
    Owner owner;
    List<Owner> connectedOwners;

    public PlayerNode(Owner owner,List<Owner> connectedOwners){
        this.owner = owner;
        this.connectedOwners = connectedOwners;
    }
}
