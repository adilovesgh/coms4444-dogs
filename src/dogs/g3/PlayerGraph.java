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
            addNode(owner);
        }
        // System.out.println(this.owners.toString());
    }

    private List<Owner> getNeighbors(Owner owner, List<Owner> allOwners){
        return new ArrayList<>();
    }

    private void addNode(Owner owner) {
        PlayerNode node = new PlayerNode(owner);
        this.owners.put(owner.getNameAsEnum(), node);
    } 

    public void addConnection(Owner owner1, Owner owner2) {
        addConnection(owner1, owner2, false);
    }

    public void addConnection(Owner owner1, Owner owner2, boolean bidirectional) {
        PlayerNode first = this.owners.get(owner1.getNameAsEnum());
        first.addNeighbor(owner2);
        if (bidirectional) {
            PlayerNode second = this.owners.get(owner2.getNameAsEnum());
            second.addNeighbor(owner1);
        }
    }

    public List<OwnerName> getConnections(Owner owner) {
        PlayerNode node = this.owners.get(owner.getNameAsEnum());
        return node.getConnections();
    }

    public void printGraph(SimPrinter simPrinter) {
        simPrinter.println("------------Graph-----------");
        for (OwnerName ownerName : this.owners.keySet()){
            PlayerNode ownerNode = this.owners.get(ownerName);
            List<OwnerName> ownerConnections = ownerNode.getConnections();
            simPrinter.println(ownerName + "--" + ownerConnections);

        }
        simPrinter.println("----------------------------");
    }

}

class PlayerNode{
    OwnerName name;
    List<OwnerName> connectedOwners;

    public PlayerNode(Owner owner) {
        this.name = owner.getNameAsEnum();
        this.connectedOwners = new LinkedList<OwnerName>();
    }

    public void addNeighbor(Owner owner) {
        this.connectedOwners.add(owner.getNameAsEnum());
    }  

    public List<OwnerName> getConnections() {
        return this.connectedOwners;
    }
}
