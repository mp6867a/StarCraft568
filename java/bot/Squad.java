package bot;

import jnibwapi.Unit;
import jnibwapi.Position;
public class Squad {
    public static int maxUnits = 8;
    private Unit[] members;
    private String myName;
    protected Unit squadLeader;
    private int n_members;

    public Squad(String name){
        myName = name;
        members = new Unit[maxUnits];
        n_members = 0;
    }

    /**
     * Adds a member to the squad if there is room.
     * @param unit
     * @return
     */
    public boolean addMember(Unit unit){
        if (n_members == 0){
            squadLeader = unit;
        }
        if (n_members < maxUnits) {
            members[n_members++] = unit;
            return true;
        }
        return false;
    }

    /**
     * Reevaluates the status of all members of the squad and purges the dead.
     * @return
     */
    public int refresh(){
        boolean anyDead = false;
        for (int i = 0; i < n_members; i++){
            if (!members[i].isExists()){
                //it seems this unit is dead.
                members[i] = null;
                anyDead = true;
                n_members--;
            }
        }
        if (anyDead) {
            compress();
        }
        return n_members;
    }

    /**
     * Makes sure there are no gaps in the member array.
     * Will reassign squad leader if the former has died.
     */
    private void compress(){
        for(int i = 0; i < members.length - 1; i++){
            if (members[i] == null) {
                for (int j = i + 1; j < members.length; j++) {
                    if(members[j] != null){
                        members[i] = members[j];
                        members[j] = null;
                        break;
                    }
                }
                if (i == 0){
                    //new leader as the old one has died.
                    squadLeader = members[0];
                }
            }
        }
    }

    /**
     * Move all subordinate units to the leader.
     */
    private void rallyToLeader(){
        Position rallyPoint = squadLeader.getPosition();
        for (int i = 1; i < n_members; i++){
            members[i].move(rallyPoint, false);
        }
    }

    /**
     * Move all units to a given position.
     * @param destination
     */
    private void moveTo(Position destination){
        for (int i = 0; i < n_members; i++){
            members[i].move(destination, false);
        }
    }

    /**
     * Return the number of members in this squad.
     * @return
     */
    public int membersQuantity(){
        return n_members;
    }
}
