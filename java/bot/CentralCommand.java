package bot;

import bot.Squad;

import java.util.ArrayList;
import java.util.List;
import jnibwapi.Unit;

public class CentralCommand {
    public List<Squad> squads;

    public CentralCommand(){
        squads = new ArrayList<Squad>();
    }

    /**
     * Add a Squad to the Command.
     * @param squadToAdd
     */
    public void addSquad(Squad squadToAdd){
        squads.add(squadToAdd);
    }

    /**
     * Adds a unit to the most undersupplied squad and moves that unit to the leader.
     * @param unit The unit to be added to the Command.
     */
    public void addUnitToSquad(Unit unit){
        Squad least = findMostUnderSupplied();
        if (least != null){
            least.addMember(unit);
        }
        unit.move(least.squadLeader.getPosition(), false);
    }

    /**
     * Finds the Squad with the least number of members.
     * @return the most manpower undersupplied squad.
     */
    public Squad findMostUnderSupplied(){
        int min = Integer.MAX_VALUE;
        int temp;
        Squad leastSquad = null;
        for (Squad squad : squads){
            temp = squad.membersQuantity();
            if (temp < min && temp != squad.maxUnits){
                min = temp;
                leastSquad = squad;
            }
        }
        //We need a squad as none exist or all are full
        if (leastSquad == null){
            Squad newSquad = new Squad("");
            addSquad(newSquad);
            return newSquad;
        }
        return leastSquad;
    }

}
