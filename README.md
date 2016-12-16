# StarCraft568
Fall 2016 CSC 568

This is a project for our AI class, our goal is to run a starcraft AI capable of executing basic stratagies. 

## Goal
The Goal of this AI bot is to crush anyone who challenges our authority. 

## API Reference

We used the Java interface version to implement our bot, JNIBWAPI (  Java interface for the Brood War API), please refer to https://github.com/JNIBWAPI/JNIBWAPI/wiki

## instructions 
Install a suitable operating system or virtual machine cappable of running StarCraft one. Download JNIBWAPI, Download the program and run it through the BWAPI injector ( Chaos Launcher). Run the main class, ProtossClient. Once connected to the bridge, run the sample  through the chaos launcher. In game - choose expansion,  - campaign - Protoss class in your player's choice. 


#Technical Summary 
Our main classes
-ExampleAIClient, this class was given to us. It instantiates the JNI-BWAPI interface and connects to BWAP. It basic calls methods such as getMinerals, getmyUnits and matchFrame. 
-ProtossClient is based off of the ExampleAIClient making it our main class. It creates arrays of probes, gateways, zealots, nexus and miningProbes. 
-BuildOrder
Depending on the race it will take in a list of commands 
-TestProtoss
It starts off with one pylon and four probes 
-UnitOrder
-Squad
-CentralCommand
What the agent does overtime 
It attacks the most valurnable squad
It will build an asslimator 
