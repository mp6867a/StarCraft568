# StarCraft568
Fall 2016 CSC 568

This is a project for our AI class; our goal is to run a starcraft AI capable of executing basic strategies. 

## Goal
Defeat the enemy as quickly as possible, while avoiding self-destruction.
Also known as crushing any bot that challenges our authority. 

## API Reference
We used the Java interface version to implement our bot, JNIBWAPI (  Java interface for the Brood War API), please refer to https://github.com/JNIBWAPI/JNIBWAPI/wiki

## Instructions 

1. Install a suitable operating system or virtual machine capable of running StarCraft one (with the Broodwar Expansion). 
2. Download JNIBWAPI
3. Download the our program and run it through the BWAPI injector (Chaos Launcher). 
4. Run the main class, ProtossClient. Once connected to the bridge, run the sample  through the chaos launcher. 
5. In game - choose expansion,  - campaign - Protoss class in your player's choice. 
6. Sit back and watch the bot do its thing.

## Overview of Approach

  Our approach primarily focused on creating a streamlined, highly generalized build that could easily be adapted to support multiple strategies and even races. We chose this as our goal for several reasons, the first being that it provided a basis for a simple framework that those not familiar with Starcraft could use and build upon without too much trouble. The second reason was its malleability; with an “insert recipe” build as the final product, we could run multiple different strategies and build orders with minimal hassle to see what works best against a particular enemy. Our goal was to create a Protoss bot that could change its strategy in a responsive and intelligent way to best counter and defeat its enemy, whatever faction they may be. As an added bonus, most of the code was written in a generalized form that uses given search methods to find friendly units, making it able to play as any race.

  We began by focusing on building construction and base planning. The best strategy means little if your bot can’t construct pylons. We chose Protoss as our main faction due to the fact that we could focus on building and fortifying one base without worrying about having to scout and expand. Once buildings were being properly placed and our base was traversable, we could test whether the build orders were being properly followed. Being able to gather enough materials fast enough also allowed us to experiment with some early-game strategies, namely small-pool zealot rushes (which were ineffective), then two-gate and three-gate rushes (which matched and occasionally beat the built-in AI). We did hit a snag with commands being ignored or given so frequently that they were rendering units helpless, but these bugs were eventually worked out. By dividing methods into distinct classes of action (defense, offense, and build/support), we were able to have multiple people working with minimal overlap. This was a hard-earned lesson, and initial stages saw some loss of work due to push conflicts on Git.

  Unlike a typical player, our bot has access to perfect information, and we attempted to utilize this to the fullest. While our bot has a default build order, it also tries to monitor whether the enemy is planning to rush by counting the number of units being spawned as well as the number of hatcheries/stargates/etc… the enemy has built. As a Zerg rush is a very effective and common strategy, this was something very important that had to be addressed. Perfect information also allowed us to not have to devote any units to scouting, which was convenient. We could tell if our base was about to be attacked by monitoring the closest enemy’s distance from our base, and switching to our emergency build list if necessary.

  Ideally, we would have liked to use some high-level strategies focusing on cloaked units (untargetable even with perfect information without a detecting unit), but these strategies were mid to late game and our bot tended to live or die by early game. As the game never progressed long enough to build the necessary structures to create cloaked armies, we scrapped the idea in favor of simpler strategies that had quicker payoff and were more easily programmable.
  
  
## Technical Summary 
The ExampleAIClient class was given to us. It instantiates the JNI-BWAPI interface and connects to BWAP. It calls basic methods such as getMinerals, getmyUnits and matchFrame. 
 ProtossClient is based off of the ExampleAIClient class which makes it our main class. It contains multiple methods such as a countPopulation and wipePopulation. It also references to  other classes such as BuildOrder, CentralCommand, and Squad.
 BuildOrder depending on the race it will take in a queue of commands and then execute them depending if they fulfill their if statement. It executes actions of building units and buildings. 
UnitOrder has a createUnitOrder method which tries to 
The Squad class keeps units in groups of eight to attack so we don't have lost sheep wandering around the game. We also assign a squadleader to guide the lost zealots. 
CentralCommand controls the smaller things such as the class squad. It also contains the attack method, which we had bugs in and have been continously trying to fix. 
What the agent does overtime 
Note: if you see TestProtoss as a class it is currently not functioning, and obselete.

