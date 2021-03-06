package bot;

import jnibwapi.Unit;
import jnibwapi.Position;

import java.util.HashSet;

public class Squad {
    public static int maxUnits = 8;
    private Unit[] members;
    private String myName;
    protected Unit squadLeader;
    private int n_members;
    private HashSet<Unit> engagedEnemies;

    public Squad(String name){
        myName = name;
        members = new Unit[maxUnits];
        n_members = 0;
        engagedEnemies = new HashSet<Unit>();
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
        rallyToLeader();
        //if any member is under attack, move all of those units to that member
        return support();

    }
    private int support(){
        for (int i = 0; i < n_members; i++){
            if (members[i].isUnderAttack()){
                for(int j = 0; j < n_members; j++) {
                    if (j != i && !members[j].isUnderAttack()) {
                        members[j].attack(members[i].getPosition(), true);
                    }
                }
                return n_members;
            }
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
                        engagedEnemies.clear();
                        break;
                    }
                }
                if (i == 0){
                    //new leader as the old one has died.
                    squadLeader = members[0];
                    //This clears the enemies. If the squad is wiped out this hould be reset.
                    engagedEnemies.clear();
                }
            }
        }
    }

    /**
     * Move all subordinate units to the leader.
     */
    public void rallyToLeader(){
        if(squadLeader != null && squadLeader.isExists()){
            Position rallyPoint = squadLeader.getPosition();
            for (int i = 1; i < n_members; i++){
                if (members[i] != null && members[i].getDistance(rallyPoint) > 150){
                    members[i].attack(squadLeader.getPosition(), true);
                }
            }
        }
    }

    /**
     * Move all units to a given position.
     * @param destination
     */
    private void moveTo(Position destination){
        for (int i = 0; i < n_members; i++){
            members[i].attack(destination, true);
        }
    }

    /**
     * Determines if a squad is under attack or not.
     * @return
     */
    public boolean isAvailable(){
        for (int i = 0; i < n_members; i++){
            if (members[i] != null && (members[i].isAttacking() || members[i].isUnderAttack() || members[i].isMoving())){
                return false;
            }
        }
        return true;
    }
    /**
     * Massed attack on enemy unit
     * @param enemy The unit which is to be assaulted
     * @return A boolean describing if attack orders have been issued.
     */
    public boolean attack(Unit enemy){
        return attack(enemy, true);
    }

    /**
     * Massed attack on an enemy unit. Can override
     * the requirement to be close if in an emergency situation.
     * @param enemy The Unit that the attack order is to be issued against.
     * @param overrideClose A boolean to override the need to be close.
     * @return A boolean describing whether the attack order has been issued.
     */
    public boolean attack(Unit enemy, boolean overrideClose){
        //is there an enemy and has this squad already tried attacking it?
        int attackCount = 0;
        if (enemy == null || engagedEnemies.contains(enemy)){
            return false;
        }
        if (overrideClose || isClose()){
            for (Unit member : members) {
                if (member != null && ((!member.isMoving() && member.isIdle()))){
                    member.attack(enemy.getPosition(), true);
                    attackCount += 1;
                }
            }
            if (attackCount > 0) {
                engagedEnemies.add(enemy);
                return true;
            }
            else{
                return false;
            }
        }
        else{
            rallyToLeader();
            return false;
        }
    }

    /**
     * Determines whether a squad is sufficiently close to one another
     * @return
     */
    public boolean isClose(){
        int limit = 500; //some n that is the maximum two units can be apart from another
        for (int member_a = 0; member_a < n_members - 1; member_a++){
            for(int member_b = member_a + 1; member_b < n_members; member_b++){
                if (members[member_a].getDistance(members[member_b].getPosition()) > limit){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return the number of members in this squad.
     * @return
     */
    public int membersQuantity(){
        return n_members;
    }

    public void patrol(Position patrolTo){
        for (Unit member : members){
            if (member != null) {
                member.attack(patrolTo, true);
            }
        }
    }
    public void move(Position moveTo){
        for (Unit member : members){
            if (member != null) {
                member.attack(moveTo, true);
            }
        }
    }

    public Unit[] returnMemberArray(){
        return members;
    }

}
