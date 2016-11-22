package bot;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.sun.jmx.snmp.SnmpUnknownSecModelException;
import javafx.geometry.Pos;
import jnibwapi.BWAPIEventListener;
import jnibwapi.JNIBWAPI;
import jnibwapi.Position;
import jnibwapi.Unit;
import jnibwapi.types.RaceType;
import jnibwapi.types.TechType;
import jnibwapi.types.TechType.TechTypes;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.UpgradeType;
import jnibwapi.types.UpgradeType.UpgradeTypes;
import jnibwapi.util.BWColor;
import bot.BuildOrder;
import jnibwapi.ChokePoint;
import java.util.Collections;
import  java.util.LinkedList;
import javax.lang.model.type.UnionType;


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
	//the number of zealots which have been trained
	private int numZealots;
	private int armySize;
	
	//is this a buildable area of creep
	private boolean buildHere;
	
	//the area where the spawning pool is built
	private Position buildArea;
	//position of the first pylon
	private Position pyPos;
	//buildable position near first pylon
	private Position gatePos;
	private Position gasFieldPos;
	//is the first pylon built
	private boolean pylonUp=false;
	//is the first gateway built
	private boolean gatewayUp=false;
	private Unit[] allZealots = new Unit[0];
	private Unit[] zealotsAttacking = new Unit[0];
	private double maxProbes = 5;
	private double probesIncrement = 1.0;
	private double getProbesIncrementDivision = 0.95;
	private boolean zealotAttackUnderway = false;
	private RaceType myRaceType;
	private RaceType enemyRaceType;
	private UnitType builderType;
	private UnitType supplyType;
	private int gasCollecters;
	private boolean waitTilStarted;
	private List<Unit> gateways;
	private List<Unit> probes;
	private List<Unit> zealots;
	private List<Unit> nexus;

	private List<Integer> buildQuadrants;

	private BuildOrder buildOrder;
	private CentralCommand centralCommand;

	private UnitType unitTypeUnderConstruction;

	public static final int SUCCESSFUL = 0;
	public static final int NOT_ENOUGH_MINERALS = 1;
	public static final int NOT_ENOUGH_GAS = 2;
	public static final int NOT_ENOUGH_MINERALS_AND_GAS = 3;
	public static final int REQUISITE_BUILDING_DOES_NOT_EXIST = 4;
	public static final int NO_SUITABLE_BUILD_LOCATION = 5;

	private int lastState;
	private int currentCount;
	//Diagnostic mode provides extra logging information
	private boolean diagnosticMode;

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
		diagnosticMode = true;

		System.out.println("Game Started");
		
		bwapi.enableUserInput();
		bwapi.enablePerfectInformation();
		bwapi.setGameSpeed(0);
		
		// reset agent state
		claimedMinerals.clear();
		warpedProbe = false;
		poolDrone = null;
		myRaceType = bwapi.getSelf().getRace();
		supplyCap = 0;
		gasCollecters = 2;
		currentCount = -1;
		//Something that cannot be the first unit to be built.
		unitTypeUnderConstruction = UnitTypes.Terran_Nuclear_Silo;
		lastState = REQUISITE_BUILDING_DOES_NOT_EXIST;

		probes = new ArrayList<Unit>();
		gateways = new ArrayList<Unit>();
		zealots = new ArrayList<Unit>();
		nexus = new ArrayList<Unit>();

		buildOrder = new BuildOrder(bwapi.getSelf(), bwapi.getEnemies().iterator().next());
		determineBuildQuadrants();
		 setRaceSpecificUnits();
	}
	
	/**
	 * Called each game cycle.
	 */
	@Override
	public void matchFrame() {
		countPopulation();
		// print out some info about any upgrades or research happening
		// draw the terrain information
		bwapi.getMap().drawTerrainData(bwapi);
		//We always need to make sure there are no idle probes.
		dispatchProbes();

		if(!buildSupplyIfNeeded()){
			if (lastState == SUCCESSFUL || lastState == REQUISITE_BUILDING_DOES_NOT_EXIST ||
					lastState == NO_SUITABLE_BUILD_LOCATION) {
				//If the build was successful, but it was a building, then we must check if building has actually started!
				if (lastState == REQUISITE_BUILDING_DOES_NOT_EXIST || lastState == NO_SUITABLE_BUILD_LOCATION ||
						!unitTypeUnderConstruction.isBuilding() ||
						(getUnitsOfType(unitTypeUnderConstruction).size() != currentCount && isAllCompleted(unitTypeUnderConstruction))){
					unitTypeUnderConstruction = buildOrder.getNextBuild();
					currentCount = getUnitsOfType(unitTypeUnderConstruction).size();
					lastState = buildAgnostic(unitTypeUnderConstruction);
				}
				else{

					//lastState = buildAgnostic(unitTypeUnderConstruction);
				}
			}
			else{
				//we lacked the minerals or gas on the last try... try again...
				lastState = buildAgnostic(unitTypeUnderConstruction);
			}

		}
		//Attack and defence logic here!
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

	/**
	 * Get all units that are idle.
	 * TODO remove this method. It serves no purpose.
	 * @return An array of idle units.
	 */
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

	/**
	 * How dangerous is a given Unit.
	 * @param unit A Unit's danger level to be evaluated.
	 * @return A double describing how dangerous a unit is.
	 */
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

	/**
	 * A scoring mechanism to determine the balance of power on the map.
	 * Postive scores favor self. Negative scores favor enemy. Scores are magnitudinal (the farther from zero,
	 * the more unbalanced the game).
	 * @return A floating point number describing the relative power in the map.
	 */
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
            tempHealth = (enemyUnit.getHitPoints() + 1) / (enemyUnit.getInitialHitPoints() + 1);
            if (tempHealth <  weakest){
                weakest = tempHealth;
                minIndex = index;
            }
            index += 1;
        }
        return enemyUnits.size() == 0 ? null : enemyUnits.get(minIndex);
    }

	/**
	 * Tests if the given position is on the grid.
	 * @param test The position being tested if it is on the grid.
	 * @return A boolean describing if 'test' is on the map.
	 */
    private boolean onGrid(Position test){
		int testY = test.getPY();
		int testX = test.getPX();
		return testY >= 0 && testX >= 0 && testY < bwapi.getMap().getSize().getPY() && testX < bwapi.getMap().getSize().getPX();
	}

	/**
	 * Finds a good place to put a building given a staring position and reading off of the best grids.
	 * @param base The starting position of this search.
	 * @param offsetFromCenter How far to search the grid.
	 * @param type The type of unit being built.
	 * @return
	 */
    private Position getBuildPosition(Position base, int offsetFromCenter, UnitType type){
		int max = 1000;
		boolean nullDiagnostic = true;
		int newX, newY;
		bwapi.drawCircle(base, 50,  BWColor.Black, false, false);
		for (int x_off = 0; x_off <= max; x_off += (int)((max - offsetFromCenter) / 50)){
			for (int y_off = 0; y_off <= x_off; y_off += (int)((max - offsetFromCenter) / 50)){
				for (Position buildArea : inBuildQuadrants(base, x_off, x_off)) {
					if (onGrid(buildArea)) {
						if (bwapi.canBuildHere(buildArea, type, true)) {
							return buildArea;
						}
					}
				}
			}
		}
		return null;
	}

	private List<ChokePoint> getBaseChokePoints(){
        List<ChokePoint> chokePoints = bwapi.getMap().getChokePoints();
        List<ChokePoint> baseChokes = new LinkedList<>();
        for (ChokePoint cp : chokePoints) {
            if (cp.getFirstRegion() == bwapi.getMap().getRegion(buildArea)) {
                baseChokes.add(cp);
                System.out.println("in");
            } else if (cp.getSecondRegion() == bwapi.getMap().getRegion(buildArea)) {
                baseChokes.add(cp);
                System.out.println("in");
            }
        }
        return baseChokes;
    }

	/**
	 * Get all of your units of a given type.
	 * @param unitTypeSought the UnitType of Units sought.
	 * @return
	 */
	private List<Unit> getUnitsOfType(UnitType unitTypeSought){
		List<Unit> unitsOfTypeSought = new ArrayList<Unit>();
		for(Unit unit: bwapi.getMyUnits()){
			if(unit.getType() == unitTypeSought){
				unitsOfTypeSought.add(unit);
			}
		}
		return unitsOfTypeSought;
	}

	/**
	 * Build a gas field
	 * @param refinery the race specific refinery
	 * @return the success state
	 */
	private int buildGasField(UnitType refinery){
		gasFieldPos = getGasLocation();
		try {
			Unit bestProbe = getBestNUnits(builderType, 1).get(0);
			bestProbe.build(gasFieldPos, refinery);
			return SUCCESSFUL;
		}
		catch (IndexOutOfBoundsException e){
			System.out.println("No builders exist!");
			return REQUISITE_BUILDING_DOES_NOT_EXIST;
		}
	}

	/**
	 * Move a worker to gather gas. First look for idle workers, then only workers not already gathering gas.
	 */
	private void dispatchToGasField(){
		Unit geyser = getUnitsOfType(UnitTypes.Protoss_Assimilator).get(0);
		for (Unit builder : getUnitsOfType(builderType)) {
			if ((builder.isIdle() && builder != poolDrone) || builder.isGatheringMinerals()){
				builder.gather(geyser, false);
				return;
			}
		}
	}

	/**
	 * get the best units availible by selecting idle ones first.
	 * @param n_units
	 * @return
	 */
	private List<Unit> getBestNUnits(UnitType type, int n_units) {
		List<Unit> builders = getUnitsOfType(type);
		List<Unit> best = new ArrayList<Unit>();
		for (Unit builder : builders){
			if (n_units == 0){
				return best;
			}
			if (builder.isIdle()){
				best.add(builder);
				n_units -= 1;
			}
		}
		//Now we don't care if the unit is idle.
		for (Unit builder : builders){
			if (n_units == 0){
				return best;
			}
			if(!best.contains(builder) && !builder.isConstructing()){
				best.add(builder);
				n_units -= 1;
			}
		}
		return best;
	}

	/**
	 * Find the nearest gas field to your base location.
	 * @return The nearest gas field's Tile Position (can be used to build a refinery).
	 */
	private Position getGasLocation(){
		List<Unit> geysers = new ArrayList<Unit>();
		List<Integer> distances = new ArrayList<Integer>();
		int minDistance = Integer.MAX_VALUE;
		int minIndex = 0;
		for (Unit unit : bwapi.getNeutralUnits()){
			if (unit.getType() == UnitTypes.Resource_Vespene_Geyser){
				geysers.add(unit);
				bwapi.drawCircle(unit.getTilePosition(), 50, BWColor.Blue, true, false);
				distances.add(bwapi.getSelf().getStartLocation().getApproxBDistance(unit.getTilePosition()));
			}
		}
		for (int i = 0; i < distances.size(); i++){
			if (distances.get(i) < minDistance){
				minIndex = i;
				minDistance = distances.get(i);
			}
		}
		return geysers.get(minIndex).getTilePosition();
	}

	/**
	 * Method to determine race specific units
	 * Supply and Builders are included.
	 * Could expand into other units in future.
	 */
	private void setRaceSpecificUnits(){
		if (myRaceType == RaceType.RaceTypes.Protoss) {
			builderType = UnitTypes.Protoss_Probe;
			supplyType = UnitTypes.Protoss_Pylon;
		}
		if (myRaceType == RaceType.RaceTypes.Zerg) {
			builderType = UnitTypes.Zerg_Drone;
			supplyType = UnitTypes.Zerg_Overlord;
		}
		if (myRaceType == RaceType.RaceTypes.Terran) {
			builderType = UnitTypes.Terran_SCV;
			supplyType = UnitTypes.Terran_Supply_Depot;
		}
	}

	/**
	 * Delegates workers to gather minerals if they are not doing anything. Also ensures a certain number of workers are
	 * gathering gas if it is possible
	 */
	private void dispatchProbes(){
		List<Unit> probes = getUnitsOfType(builderType);
		List<Unit> minerals = bwapi.getNeutralUnits();

		int gasGatherers = 0;
		int mineralGatherers = 0;
		int builders = 0;
		boolean doingSomething = false;
		for (Unit builder : probes){
			if (builder.isGatheringGas()){
				gasGatherers += 1;
				doingSomething = true;
			}
			if (builder.isConstructing()){
				builders += 1;
				doingSomething = true;
			}
			if (builder.isGatheringMinerals()){
				mineralGatherers += 1;
				doingSomething = true;
			}
			if (!doingSomething && builder != poolDrone){
				for (Unit mineral : minerals){
					if(mineral.getType() == UnitTypes.Resource_Mineral_Field && (builder.getDistance(mineral) < 300 || mineral.getDistance(bwapi.getSelf().getStartLocation()) < 300)) {
						builder.gather(mineral, false);
					}
				}
			}
			doingSomething = false;
		}
		if (getUnitsOfType(UnitTypes.Protoss_Assimilator).size() > 0) {
			while (gasGatherers < gasCollecters) {
				dispatchToGasField();
				gasGatherers += 1;
			}
		}
	}

	/**
	 * Internal method to eliminate population counts.
	 */
	private void wipePopulations(){
		zealots = new ArrayList<Unit>();
		gateways = new ArrayList<Unit>();
		probes = new ArrayList<Unit>();
		nexus = new ArrayList<Unit>();
	}

	/**
	 * Some UnitTypes will be cached to save on computation.
	 */
	private void countPopulation(){
		wipePopulations();
		for (Unit unit : bwapi.getMyUnits()){
			if (unit.getType() == UnitTypes.Protoss_Probe){
				probes.add(unit);
			}
			if (unit.getType() == UnitTypes.Protoss_Zealot){
				zealots.add(unit);
			}
			if (unit.getType() == UnitTypes.Protoss_Gateway){
				gateways.add(unit);
			}
			if (unit.getType() == UnitTypes.Protoss_Nexus){
				nexus.add(unit);
			}
		}
	}

	/**
	 * Builds a unit or a building.
	 * @param buildingOrUnit A UnitType to build
	 * @return code describing success
	 */
	private int buildAgnostic(UnitType buildingOrUnit){
		if (diagnosticMode){
			System.out.print("Attempting to build a " + buildingOrUnit.getName() + " which is a ");
		}
		if (buildingOrUnit.isBuilding()){
			if (diagnosticMode){
				System.out.println("building.");
			}
			return build(buildingOrUnit);
		}
		else{
			if (diagnosticMode){
				System.out.println("unit.");
			}
			return train(buildingOrUnit);
		}
	}

	/**
	 * Build a building without providing a position.
	 * @param building
	 * @return
	 */
	private int build(UnitType building){
		if (poolDrone == null || !poolDrone.isExists())
		{
			poolDrone = getBestNUnits(builderType, 1).get(0);
		}
		return build(building, bwapi.getSelf().getStartLocation());
	}

	/**
	 * Build a building with a start position
	 * TODO pull pyPos logic out of this method
	 * Currently only Pylons are considered to be availible for basePos.
	 * Otherwise position is overwritten with the position of the last pylon placed.
	 * @param building
	 * @param basePos
	 * @return
	 */
	private int build(UnitType building, Position basePos){
		Position buildPos;
		try {
			Unit bestProbe = getBestNUnits(builderType, 1).get(0);
			if (bwapi.getSelf().getMinerals() >= building.getMineralPrice() && bwapi.getSelf().getGas() >= building.getGasPrice()){
				if (UnitTypes.Protoss_Robotics_Facility == building){
					System.out.println("Building robo.");
				}
				if(building.isRefinery()){
					return buildGasField(building);
				}
				if (myRaceType == RaceType.RaceTypes.Protoss && building != UnitTypes.Protoss_Pylon){
					buildPos = getBuildPosition(pyPos, 0, building);
				}
				else {
					buildPos = getBuildPosition(basePos, 0, building);
				}
				if (buildPos == null){
					return NO_SUITABLE_BUILD_LOCATION;
				}
				if(diagnosticMode){
					bwapi.drawLine(bestProbe.getPosition(), buildPos, BWColor.White, false);
				}
				if(buildPos != null) {
					poolDrone.build(buildPos, building);
					if (building == UnitTypes.Protoss_Pylon){
						pyPos = buildPos;
					}
					return SUCCESSFUL;
				}
			}
		}
		catch (IndexOutOfBoundsException e){
			//No probes exist
			return REQUISITE_BUILDING_DOES_NOT_EXIST;
		}
		//Not enough minerals
		return mineral_gas_deficit(building);
	}

	/**
	 * Given a UnitType, train it.
	 * @param unit The UnitType to build.
	 * @return the success state of the training.
	 */
	private int train(UnitType unit){

		UnitType buildingThatMakes = UnitTypes.getUnitType(unit.getWhatBuildID());
		try {
			//this should be the building that constructs this unit.
			Unit bestBuilding = getBestNUnits(buildingThatMakes, 1).get(0);
			if (bwapi.getSelf().getMinerals() >= unit.getMineralPrice() && bwapi.getSelf().getGas() >= unit.getGasPrice()){
				bestBuilding.train(unit);
				return SUCCESSFUL;
			}
		}
		catch (IndexOutOfBoundsException e) {
			//No building exists
			System.out.println("Unable to build " +  unit.getName() + " as there is no " + buildingThatMakes + ".");
			return REQUISITE_BUILDING_DOES_NOT_EXIST;
		}
		//Not enough minerals and/or gas
		return mineral_gas_deficit(unit);
	}

	/**
	 * Determine what resources is in deficit.
	 * @param unitType The type of Unit attempted to be built or trained.
	 * @return Code describing the deficit.
	 */
	private int mineral_gas_deficit(UnitType unitType){
		int myGas = bwapi.getSelf().getGas();
		int myMinerals = bwapi.getSelf().getMinerals();
		int unitMinerals = unitType.getMineralPrice();
		int unitGas = unitType.getGasPrice();
		if (myGas < unitGas && myMinerals < unitMinerals){
			if (diagnosticMode){
				System.out.println("Not enough gas and minerals to build " + unitType.getName() + ".");
			}
			return NOT_ENOUGH_MINERALS_AND_GAS;
		}
		if (myGas < unitGas){
			if (diagnosticMode){
				System.out.println("Not enough gas to build " + unitType.getName() + ".");
			}
			return NOT_ENOUGH_GAS;
		}
		else{
			if (diagnosticMode){
				System.out.println("Not enough minerals to build " + unitType.getName() + ".");
			}
			return NOT_ENOUGH_MINERALS;
		}
	}

	/**
	 * If this supply is running low, the build order is overriden and a supply unit is built.
	 * @return
	 */
	private boolean buildSupplyIfNeeded(){
		int supply = bwapi.getSelf().getSupplyUsed();
		int supplyTotal = bwapi.getSelf().getSupplyTotal();
		if (supply + 2 > supplyTotal){
			build(supplyType);
			return true;
		}
		return false;
	}

	/**
	 * Find the half of the map closest to the center from your base location.
	 * Used to place buildings in a more logical area (hopefully away from minerals too)..
	 * TODO Ensure that buildings are not placed in the path between mineral fields and geysers.
	 */
	private void determineBuildQuadrants(){
		Position base = bwapi.getSelf().getStartLocation();
		buildQuadrants = new ArrayList<Integer>();
		if (getDistanceToEdge(new Position(base.getPX() - 1, base.getPY() + 1)) > getDistanceToEdge(new Position(base.getPX() + 1, base.getPY() - 1))){
			buildQuadrants.add(1);
		}
		else{
			buildQuadrants.add(8);
		}
		if (getDistanceToEdge(new Position(base.getPX() - 0, base.getPY() + 1)) > getDistanceToEdge(new Position(base.getPX() + 0, base.getPY() - 1))){
			buildQuadrants.add(2);
		}
		else{
			buildQuadrants.add(7);
		}
		if (getDistanceToEdge(new Position(base.getPX() + 1, base.getPY() + 1)) > getDistanceToEdge(new Position(base.getPX() - 1, base.getPY() - 1))){
			buildQuadrants.add(3);
		}
		else{
			buildQuadrants.add(6);
		}
		if (getDistanceToEdge(new Position(base.getPX() - 1, base.getPY() + 0)) > getDistanceToEdge(new Position(base.getPX() + 1, base.getPY() - 0))){
			buildQuadrants.add(4);
		}
		else{
			buildQuadrants.add(5);
		}
	}

	/**
	 * Get a position from the quadrants that have been selected as good relative to the center of the map.
	 * @param base The starting position.
	 * @param x_off Displacement in the X direction.
	 * @param y_off Displacement in the Y direction.
	 * @return
	 */
	private List<Position> inBuildQuadrants(Position base, int x_off, int y_off){
		List<Position> positions = new ArrayList<Position>();
		int index = 1;
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX() - x_off, base.getPY() + y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX(), base.getPY() + y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX() + x_off, base.getPY() + y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX() - x_off, base.getPY() - y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX(), base.getPY() + y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX() - x_off, base.getPY() - y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX(), base.getPY() - y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX() + x_off, base.getPY() - y_off));
		}
		return positions;
	}

	/**
	 * Determines how far a point is from the edge of the map.
	 * @param coordinate The position of which we are seeking its distance to the edge of the map.
	 * @return The number of pixels from 'coordinate' to the nearest edge.
	 */
	private int getDistanceToEdge(Position coordinate){
		int distanceToEdgeMinX = coordinate.getPX();
		int distanceToEdgeMaxX = bwapi.getMap().getSize().getPX() - coordinate.getPX();
		int distanceToEdgeMinY = coordinate.getPY();
		int distanceToEdgeMaxY = bwapi.getMap().getSize().getPY() - coordinate.getPY();
		return Math.min(Math.min(distanceToEdgeMinX, distanceToEdgeMaxX), Math.min(distanceToEdgeMinY, distanceToEdgeMaxY));
	}
	private boolean isAllCompleted(UnitType type){
		for (Unit unit : getUnitsOfType(type)){
			if (!unit.isCompleted()){
				return false;
			}
		}
		return true;
	}

	private void isWarped(){
		if (myRaceType == RaceType.RaceTypes.Protoss){
			for (Unit building : bwapi.getMyUnits()){
				if (building.getType().isBuilding() && building.getType() != UnitTypes.Protoss_Nexus && building.getType() != UnitTypes.Protoss_Assimilator){
					if (building.isUnpowered()){
						if(!poolDrone.isConstructing()) {
							build(UnitTypes.Protoss_Pylon, building.getPosition());
							return;
						}
					}
				}
			}
		}
	}
}
