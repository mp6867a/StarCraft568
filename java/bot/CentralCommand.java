package bot;

import java.util.ArrayList;
import java.util.List;
import jnibwapi.Unit;
import jnibwapi.types.UnitType;

public class CentralCommand {
    public List<Squad> squads;
    public List<Unit> units;

    public CentralCommand(){
        squads = new ArrayList<Squad>();
        units = new ArrayList<Unit>();
    }

    /**
     * Issue refresh command to all squads
     */
    public void refresh(){
        for (Squad squad : squads){
            squad.refresh();
        }
        rallyIfNeeded();
    }
    public boolean attack(Unit enemy, int n_squads){
        return attack(enemy, n_squads, false);
    }
    public boolean attack(Unit enemy, int n_squads, boolean override){
        int engaged = 0;
        //could be improved by selecting the nearest n free squads
        for (Squad squad : squads){
            if (squad.isAvailable()){
                squad.attack(enemy);
                engaged += 1;
                if (engaged == n_squads){
                    return true;
                }
            }
        }
        return engaged > 0;
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
            unit.move(least.squadLeader.getPosition(), false);
        }
        else{
            //Create a new squad
            Squad newSquad = new Squad("");
            newSquad.addMember(unit);
            addSquad(newSquad);
        }
        units.add(unit);
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

    public void loadUnits(List<Unit> newUnits){
        for (Unit newUnit : newUnits){
            if (!newUnit.getType().isBuilding() && newUnit.getType() != UnitType.UnitTypes.Protoss_Probe && !units.contains(newUnit)){
                addUnitToSquad(newUnit);
            }
        }
    }

    public void rallyIfNeeded(){
        for (Squad squad : squads){
            if (!squad.isClose() && squad.isAvailable()){
                squad.rallyToLeader();
            }
        }
    }

}
