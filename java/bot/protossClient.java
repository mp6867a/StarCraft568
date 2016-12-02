package bot;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

import jnibwapi.BWAPIEventListener;
import jnibwapi.JNIBWAPI;
import jnibwapi.Position;
import jnibwapi.Position.PosType;
import jnibwapi.Unit;
import jnibwapi.types.TechType;
import jnibwapi.types.TechType.TechTypes;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.UpgradeType;
import jnibwapi.types.UpgradeType.UpgradeTypes;
import jnibwapi.util.BWColor;

/**
 * Example Java AI Client using JNI-BWAPI.
 * 
 * Executes a 5-pool rush and cheats using perfect information.
 * 
 * Note: the agent often gets stuck when attempting to build the spawning pool. It works best on
 * maps where the overlord spawns with plenty of free space around it.
 */
public class protossClient implements BWAPIEventListener {
	
	/** reference to JNI-BWAPI */
	private final JNIBWAPI bwapi;
	
	/** used for mineral splits */
	private final HashSet<Unit> claimedMinerals = new HashSet<>();
	
	/** have probe 5 been warped */
	private boolean warpedProbe;
	
	/** the drone that has been assigned to building a pool */
	private Unit poolDrone;
	
	/** when should the next overlord be spawned? */
	private int supplyCap;
	
	//the number of zerglings massed at the base
	private int garrisonZLots;
	
	//number of probes
	private int numProbes;
	//the number of zerglings which have been trained
	private int armySize;
	
	//is this a buildable area of creep
	private boolean buildHere;
	
	//the area where the spawning pool is built
	private Position buildArea;
	//position of the first pylon
	private Position pyPos;
	//buildable position near first pylon
	private Position gatePos;
	//is the first pylon built
	private boolean pylonUp=false;
	//is the first gateway built
	private boolean gatewayUp=false;
	private Unit[] allZealots = new Unit[0];
	private Unit[] zealotsAttacking = new Unit[0];

	private boolean zealotAttackUnderway = false;
	/**
	 * Create a Java AI.
	 */
	public static void main(String[] args) {
		new protossClient();
	}
	
	/**
	 * Instantiates the JNI-BWAPI interface and connects to BWAPI.
	 */
	public protossClient() {
		bwapi = new JNIBWAPI(this, true);
		bwapi.start();
	}
	
	/**
	 * Connection to BWAPI established.
	 */
	@Override
	public void connected() {
		System.out.println("Connected");
	}
	
	/**
	 * Called at the beginning of a game.
	 */
	@Override
	public void matchStart() {
		System.out.println("Game Started");
		
		bwapi.enableUserInput();
		bwapi.enablePerfectInformation();
		bwapi.setGameSpeed(0);
		
		// reset agent state
		claimedMinerals.clear();
		warpedProbe = false;
		poolDrone = null;
		supplyCap = 0;
	}
	
	/**
	 * Called each game cycle.
	 */
	@Override
	public void matchFrame() {
		// print out some info about any upgrades or research happening
		String msg = "=";
        numProbes = countUnits(UnitTypes.Protoss_Probe);
		for (TechType t : TechTypes.getAllTechTypes()) {
			if (bwapi.getSelf().isResearching(t)) {
				msg += "Researching " + t.getName() + "=";
			}
			// Exclude tech that is given at the start of the game
			UnitType whatResearches = t.getWhatResearches();
			if (whatResearches == UnitTypes.None) {
				continue;
			}
			if (bwapi.getSelf().isResearched(t)) {
				msg += "Researched " + t.getName() + "=";
			}
		}
		for (UpgradeType t : UpgradeTypes.getAllUpgradeTypes()) {
			if (bwapi.getSelf().isUpgrading(t)) {
				msg += "Upgrading " + t.getName() + "=";
			}
			if (bwapi.getSelf().getUpgradeLevel(t) > 0) {
				int level = bwapi.getSelf().getUpgradeLevel(t);
				msg += "Upgraded " + t.getName() + " to level " + level + "=";
			}
		}
		bwapi.drawText(new Position(0, 20), msg, true);
		// draw the terrain information
		bwapi.getMap().drawTerrainData(bwapi);
		// spawn a drone
		for (Unit unit : bwapi.getMyUnits()) {
			// Note you can use referential equality
			if (unit.getType() == UnitTypes.Protoss_Nexus) {
				if (bwapi.getSelf().getMinerals() >= 50 && !warpedProbe) {
					unit.morph(UnitTypes.Protoss_Probe);
					warpedProbe = true;
				}
			}
		}
		/*
		// collect minerals
		for (Unit unit : bwapi.getMyUnits()) {
			if (unit.getType() == UnitTypes.Protoss_Probe) {
				// You can use referential equality for units, too
				if (unit.isIdle()) {
					for (Unit minerals : bwapi.getNeutralUnits()) {
						if (minerals.getType().isMineralField()
								&& !claimedMinerals.contains(minerals)) {
							double distance = unit.getDistance(minerals);

							if (distance < 300) {
								unit.rightClick(minerals, false);
								claimedMinerals.add(minerals);
								break;
							}
						}
					}
				}
			}
		}*/
		//This seems like a pretty weak search for a suitable build location. Could we do an iteration of random values?
		buildArea = getBuildPosition(200);
		// build a pylon
		if (bwapi.getSelf().getMinerals() >= 100 && poolDrone == null && pylonUp == false) {
			for (Unit unit : bwapi.getMyUnits()) {
				if (unit.getType() == UnitTypes.Protoss_Probe) {
					poolDrone = unit;
					unit.build(buildArea, UnitTypes.Protoss_Pylon);
					pylonUp=true;
					break;
					//while(bwapi.canBuildHere(pyPos, UnitTypes.Protoss_Pylon, true))
					//{
					//	System.out.println("while triggered");
					//}
					//System.out.println("while not  triggered");
					//for (Unit minerals : bwapi.getNeutralUnits()) {
					//	if (minerals.getType().isMineralField()
					//			&& !claimedMinerals.contains(minerals)) {
					//		double distance = poolDrone.getDistance(minerals);
					//
					//		if (distance < 300) {
					//			poolDrone.rightClick(minerals, false);
					//			claimedMinerals.add(minerals);
					//			poolDrone=null;
					//			break;
					//			}
					//		}
					//	}
					}
				}
			}
		
		// build the gateway next to the pylon
		if(bwapi.getSelf().getMinerals() >= UnitTypes.Protoss_Gateway.getBuildScore() && gatewayUp == false){
			for (Unit unit : bwapi.getMyUnits()) {
				if (unit.getType() == UnitTypes.Protoss_Pylon && unit.isCompleted()) {
					pyPos = new Position(unit.getPosition().getPX(), unit.getPosition().getPY());
					bwapi.drawCircle(unit.getPosition(), 25, BWColor.Orange, false, false);

					gatePos = new Position(pyPos.getPX() + 100, pyPos.getPY());
					if (bwapi.canBuildHere(gatePos, UnitTypes.Protoss_Gateway, true) == false) {
						gatePos = new Position(pyPos.getPX() - 100, pyPos.getPY());
						if (bwapi.canBuildHere(gatePos, UnitTypes.Protoss_Gateway, true) == false) {
							gatePos = new Position(pyPos.getPX(), pyPos.getPY() + 100);
							if (bwapi.canBuildHere(gatePos, UnitTypes.Protoss_Gateway, true) == false) {
								gatePos = new Position(pyPos.getPX(), pyPos.getPY() - 100);
							}
						}
					}

					// build a gateway
					bwapi.drawCircle(gatePos, 50, BWColor.Orange, false, false);
					for (Unit probe : bwapi.getMyUnits()) {
						if (probe.getType() == UnitTypes.Protoss_Probe) {
							poolDrone = probe;

							probe.build(gatePos, UnitTypes.Protoss_Gateway);
							gatewayUp = true;
							break;
							/*for (Unit minerals : bwapi.getNeutralUnits()) {
								if (minerals.getType().isMineralField()
										&& !claimedMinerals.contains(minerals)) {
									double distance = poolDrone.getDistance(minerals);

									if (distance < 300) {
										poolDrone.rightClick(minerals, false);
										claimedMinerals.add(minerals);
										poolDrone = null;
										break;
									}
								}
							}*/
						}
					}
					break;
				}
		}
		}
		// Build Pylons
		numProbes = countUnits(UnitTypes.Protoss_Probe);
		if (bwapi.getSelf().getSupplyUsed() + 2 >= bwapi.getSelf().getSupplyTotal()
				&& bwapi.getSelf().getSupplyTotal() >= supplyCap) {
			bwapi.sendText("pylon required");
			if (bwapi.getSelf().getMinerals() >= 100) {
				for (Unit builder : bwapi.getMyUnits()) {
					if (builder.getType() == UnitTypes.Protoss_Probe) {
						builder.build(buildArea, UnitTypes.Protoss_Pylon);
						supplyCap = bwapi.getSelf().getSupplyTotal();
						break;
					}

					//for (Unit minerals : bwapi.getNeutralUnits()) {
					//	if (minerals.getType().isMineralField()
					//			&& !claimedMinerals.contains(minerals)) {
					//		double distance = poolDrone.getDistance(minerals);

					//	if (distance < 300) {
					//			builder.rightClick(minerals, false);
					//			claimedMinerals.add(minerals);
					//			break;
					//		}
					//	}
					//}
				}
			}
		}
		// spawn probes

		else if (bwapi.getSelf().getMinerals() >= 50 && (numProbes <= 5 || (numProbes / (countZealots() +  1)) < (1 /2.5))) {
			for (Unit unit : bwapi.getMyUnits()) {
				if (unit.getType() == UnitTypes.Protoss_Nexus && unit.isCompleted()) {
					unit.train(UnitTypes.Protoss_Probe);
					break;
						}
					}
				}
					
		// spawn zealots if we didn't spawn anything else
		else if (bwapi.getSelf().getMinerals() >= 100) {
			for (Unit unit : bwapi.getMyUnits()) {
				if (unit.getType() == UnitTypes.Protoss_Gateway && unit.isCompleted()) {
					unit.train(UnitTypes.Protoss_Zealot);
					break;
					}
				}
		}
        // Force probes to gather minerals if they are idle
        for (Unit probe : bwapi.getMyUnits()){
            if (probe.getType() == UnitTypes.Protoss_Probe){
                if(!(probe.isGatheringGas() || probe.isGatheringMinerals() || probe.isConstructing())){
                    for (Unit minerals : bwapi.getNeutralUnits()) {
                        if (minerals.getType().isMineralField()) {
                            double distance = probe.getDistance(minerals);
                            if (distance < 300) {
                                probe.gather(minerals, false);
                                break;
                            }
                        }
                    }
                }
            }
        }
		//mass at least 8 zealots before attacking
		// '8 zealots' should be a configurable value based on environment
		//nitpicking on names here, we shouldn't use 'garrison' to describe units being massed for attack
		//also, this is an all-out attack; I'm changing this to a configurable amount to attack with
		int minZealotsForAttack = 8;
		double reserveRatio = 0.2;
		double lossTolerance = 0.5;
		double minAdvantage = 0.1;
		double strengthDisadvantage = -0.15;
		double strengthBalance = strengthBalance();
		if (!zealotAttackUnderway && strengthBalance >= minAdvantage) {
			//this logic leaves some Zealots in reserve
			allZealots = getZealots();
			zealotsAttacking = new Unit[(int)(allZealots.length * (1 - reserveRatio))];
			System.arraycopy(allZealots, 0, zealotsAttacking, 0, (int) (allZealots.length * (1 - reserveRatio)));
			if (zealotsAttacking.length >= minZealotsForAttack) {
				//Mass the units
				massZealots(zealotsAttacking);
				//I'm guessing that we need to wait until they are actually massed together...
				// attack move toward an enemy
				zealotAttackUnderway = zealotAttack(zealotsAttacking);
			}
		}
		else {
			if(countZealots() < lossTolerance * zealotsAttacking.length || strengthBalance <= strengthDisadvantage) {
				//some form of retreat if we are taking on large losses or at a disadvantage
			}
			else if (zealotAttackUnderway){
				//keep attacking if the attack isn't going horribly
				zealotAttackUnderway = zealotAttack(zealotsAttacking);
			}
		}
	}
	@Override
	public void keyPressed(int keyCode) {}
	@Override
	public void matchEnd(boolean winner) {}
	@Override
	public void sendText(String text) {}
	@Override
	public void receiveText(String text) {}
	@Override
	public void nukeDetect(Position p) {}
	@Override
	public void nukeDetect() {}
	@Override
	public void playerLeft(int playerID) {}
	@Override
	public void unitCreate(int unitID) {}
	@Override
	public void unitDestroy(int unitID) {}
	@Override
	public void unitDiscover(int unitID) {}
	@Override
	public void unitEvade(int unitID) {}
	@Override
	public void unitHide(int unitID) {}
	@Override
	public void unitMorph(int unitID) {}
	@Override
	public void unitShow(int unitID) {}
	@Override
	public void unitRenegade(int unitID) {} 	 	
	@Override
	public void saveGame(String gameName) {}
	@Override
	public void unitComplete(int unitID) {}
	@Override
	public void playerDropped(int playerID) {}

	private int countZealots()
	{
		int zealots = 0;
		for (Unit unit : idleUnits()) {
			if (unit.getType() == UnitTypes.Protoss_Zealot){
				zealots += 1;
			}
		}
		return zealots;
	}

    private int countUnits(UnitType searchType)
    {
        int count = 0;
        for (Unit unit : bwapi.getMyUnits()) {
            if (unit.getType() == searchType){
                count += 1;
            }
        }
        return count;
    }
	private Unit[] getZealots() {
        ArrayList<Unit> zealots = new ArrayList<Unit>();
		for (Unit unit : bwapi.getMyUnits()) {
			if (unit.getType() == UnitTypes.Protoss_Zealot){
				zealots.add(unit);
			}
		}
		return zealots.toArray(new Unit[0]);
	}
	private Unit[] idleUnits()
	{
		ArrayList<Unit> idleUnitsList = new ArrayList<Unit>();
		for (Unit unit : bwapi.getMyUnits()) {
			if (unit.isIdle()) {
				idleUnitsList.add(unit);
			}
		}
		return idleUnitsList.toArray(new Unit[0]);
    }

	private void massZealots(Unit[] zealotsArray) {
		//get the first zealots position
		Position basePosition = zealotsArray[0].getPosition();
		for (Unit zealot : zealotsArray) {
			//move to base location
		}
	}

	/**
	have our zealots attack the FIRST unit in the stack.
	Returns if the zealots are actually attacking an enemy.
	Maybe we should calculate the nearest enemy to attack?
	*/
	private boolean zealotAttack(Unit[] zealotArray) {
		boolean attackMoveMade = false;
        //all should attack the most vulnerable enemy
        //maybe should look for closest?
        Unit enemyToAttack = getMostVulnerableEnemy();
		for (Unit unit : zealotArray) {

			if (unit.getType() == UnitTypes.Protoss_Zealot) {
                if (unit.isAttacking() || unit.isMoving()){
                    //either the unit is attacking or moving (could be rallying) and no action should be taken
                    attackMoveMade = true;
                    continue;
                }
                unit.attack(enemyToAttack.getPosition(), false);
                attackMoveMade = true;
			}
		}
		return attackMoveMade;
	}

	private boolean destroyEnemyBuildings() {
		boolean attackMoveMade = false;
		for (Unit unit : idleUnits()) {
			if (unit.getType() == UnitTypes.Protoss_Zealot) {
				//some logic to check if 'unit' is a building
				for (Unit enemy : bwapi.getEnemyUnits()) {
					unit.attack(enemy.getPosition(), false);
					attackMoveMade = true;
					break;
				}
			}
		}
		return attackMoveMade;
	}

	private double getEnemyHealth(){
		double strength = 0.0;
		//included for future use
		double initialStrength = 0.0;
		for (Unit enemyUnit: bwapi.getEnemyUnits()) {
			strength += enemyUnit.getHitPoints();
			initialStrength += enemyUnit.getInitialHitPoints();
		}
		return strength;
	}

	private double getSelfHealth(){
		double strength = 0.0;
		double initialStrength = 0.0;
		for (Unit enemyUnit: bwapi.getMyUnits()) {
			strength += enemyUnit.getHitPoints();
			initialStrength += enemyUnit.getInitialHitPoints();
		}
		return strength;
	}

	private double getEnemyAttackStrength(){
		double strength = 0.0;
		double score = 0.0;
		for (Unit enemyUnit: bwapi.getEnemyUnits()){
			strength += scoreUnitAttackThreat(enemyUnit);
		}
		return strength;
	}

	private double getSelfAttackStrength(){
		double strength = 0.0;
		double score = 0.0;
		for (Unit myUnit: bwapi.getMyUnits()){
			strength += scoreUnitAttackThreat(myUnit);
		}
		return strength;
	}
	private double scoreUnitAttackThreat(Unit unit)
	{
		UnitType thisUnitType = unit.getType();
		//takes the damage the unit can do and adds in some modification based on armor and speed.
		double speedBuffer = 10.0;
		double armorBuffer = 1.5;
		double baseAttack = thisUnitType.getGroundWeapon().getDamageAmount();
		double armorMod = (thisUnitType.getArmor() + armorBuffer) / armorBuffer;
		double speedMod = ((thisUnitType.getTopSpeed() + speedBuffer) / speedBuffer);
		double allMods = speedMod * armorMod;
		return baseAttack * allMods;
	}

	private double strengthBalance()
	{
		double myStrength = getSelfAttackStrength();
		double myHealth = getSelfHealth();
		double enemyStrength = getEnemyAttackStrength();
		double enemyHealth = getEnemyHealth();
		double myScore = (myStrength * myHealth);
		double enemyScore = (enemyStrength * enemyHealth);
		//negative scores favor the enemy, positive favors Self
		if (enemyScore > myScore){
			//subtract by 1 to set a tie at 0.0
			//multiply by -1 to show enemy advantage
			return -1 * (enemyScore / myScore - 1);
		}
		else {
			return (myScore / enemyScore - 1);
		}
	}

    private Unit getMostVulnerableEnemy(){
        int index = 0;
        int minIndex = 0;
        double weakest = 1; //1 is the maximum value of the divison of hitpoints / init hitpoints
        double tempHealth;
        List<Unit> enemyUnits = bwapi.getEnemyUnits();
        for (Unit enemyUnit: enemyUnits){
            tempHealth = enemyUnit.getHitPoints() / enemyUnit.getInitialHitPoints();
            if (tempHealth <  weakest){
                weakest = tempHealth;
                minIndex = index;
            }
            index += 1;
        }
        return enemyUnits.get(minIndex);
    }
    private Position getBuildPosition(int offsetFromCenter){
		Position buildArea= new Position(bwapi.getSelf().getStartLocation().getPX()+offsetFromCenter,bwapi.getSelf().getStartLocation().getPY());
		if(bwapi.canBuildHere(buildArea, UnitTypes.Protoss_Pylon, true) == false){
			buildArea= new Position(bwapi.getSelf().getStartLocation().getPX()-offsetFromCenter,bwapi.getSelf().getStartLocation().getPY());
			if(bwapi.canBuildHere(buildArea, UnitTypes.Protoss_Pylon, true) == false){
				buildArea= new Position(bwapi.getSelf().getStartLocation().getPX(),bwapi.getSelf().getStartLocation().getPY()+offsetFromCenter);
				if(bwapi.canBuildHere(buildArea, UnitTypes.Protoss_Pylon, true) == false){
					buildArea= new Position(bwapi.getSelf().getStartLocation().getPX(),bwapi.getSelf().getStartLocation().getPY()-offsetFromCenter);
					if(bwapi.canBuildHere(buildArea, UnitTypes.Protoss_Pylon, true) == false){
						buildArea= new Position(bwapi.getSelf().getStartLocation().getPX()-offsetFromCenter,bwapi.getSelf().getStartLocation().getPY()+offsetFromCenter);
						if(bwapi.canBuildHere(buildArea, UnitTypes.Protoss_Pylon, true) == false){
							buildArea= new Position(bwapi.getSelf().getStartLocation().getPX()-offsetFromCenter,bwapi.getSelf().getStartLocation().getPY()+offsetFromCenter);
							if(bwapi.canBuildHere(buildArea, UnitTypes.Protoss_Pylon, true) == false){
								buildArea= new Position(bwapi.getSelf().getStartLocation().getPX()+offsetFromCenter,bwapi.getSelf().getStartLocation().getPY()-offsetFromCenter);
								if(bwapi.canBuildHere(buildArea, UnitTypes.Protoss_Pylon, true) == false) {
									buildArea= new Position(bwapi.getSelf().getStartLocation().getPX()+offsetFromCenter,bwapi.getSelf().getStartLocation().getPY()+offsetFromCenter);
									if(bwapi.canBuildHere(buildArea, UnitTypes.Protoss_Pylon, true) == false){
										buildArea = getBuildPosition((int)(offsetFromCenter * 1.1));
									}
								}
							}
						}
					}
				}
			}
		}

		return buildArea;
	}
}
