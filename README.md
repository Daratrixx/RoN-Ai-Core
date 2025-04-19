# RonApi & RonAi Core
This project aims to add Ai opponents to the Reign of Nether mod.
- [Curse Forge page](https://www.curseforge.com/minecraft/mc-mods/reign-of-nether-rts-in-minecraft)
- [Github repository](https://github.com/SoLegendary/reignofnether)

This project consists of:
- An abstraction layer to manipulate the Reign of Nether codebase like a pure RTS solution instead of a minecraft mod (RonApi add-on)
- An RTS AI framework, fine-tuned for Reign of Nether data, build on top of the RonApi (RoNAi Core add-on)
  - In case of drastic changes to the RoN codebase, only the RonApi will need to be updated to accommodate changes

## RonApi features
### THIS WILL BE MOVED TO A SEPARATE PROJECT LATER
- Timers: utilities to run code after delays
- TypeIds: abstracting building/units/abilities/orders as int values
- World tracking: wrappers and utilities for easier manipulation of the RTS simulation
  - Resource - gradually scan chunks to detect resource block, and agglomerate blocks into resource nodes
  - Player - aggregate the units and structures for each player, as well as resources and other information
  - Unit - simplify unit manipulation
  - Building - simplify building manipulation

## RonAi Core features
### Creating and registering a new Ai script
- Extending IAiLogic class to define the Priorities and parameters of the Ai
  - [x] Name must be unique as it is used to display the script for users to select when spawning bots
  - [x] Unit priorities indicate in which order units should be created
  - [x] Building priorities indicate in which order structures must be built
  - [ ] Research priorities indicate when researches need to be done
  - [X] Harvest priorities indicate how many workers should be assigned to different tasks at all time
  - [ ] Attack priorities indicate what units to attack, raid, and defend with
- Registering the newly created class as an available script for the server to use
  - [x] `AiLogics.registerAiLogic(new AiLogic());`
  - [x] Buttons to spawn bots for each faction
  - [ ] Dropdown/ui to select which script to use when spawning in new bots
### Available features
- 60% Building structures
  - [x] Process the Building priorities to decide when to start new structures
    - [x] Bug: Ai sometimes starts additional capitols
  - [x] Automatically select a location around the capitol to build structures
    - [ ] Allow structures to be built on to specific locations (Main, Farms, Woodline, Ore, Proxy...)
      - [ ] Allow structures to prioritize certain areas in specific location (Front, Back...)
    - [x] Bug: Ai buildings sometimes clip into each others
    - [X] Bug: buildings are too far apart and the spiral becomes exponentially bigger
  - [ ] Build Stockpiles/Portals near distant resources
  - [ ] Build multiple Capitols
  - [x] Automatically select workers for construction projects
  - [x] Automatically pull new workers to complete buildings in case builders are killed
    - [ ] Enforce number of builders per structure (powerbuild castle etc...)
  - [ ] Enforce required priorities that will prevent the list from progressing until fulfilled
  - [ ] Automatically fulfil techtree requirements ('autobuild')
  - [ ] Support building upgrades (portals, lightning rod, grand library...)
- 80% Training units
  - [x] Process the Unit priorities to decide when to start new units
  - [x] Automatically queue new units when a production building becomes idle
  - [ ] Enforce required priorities that will prevent the list from progressing until fulfilled
  - [ ] Automatically fulfil techtree requirements ('autobuild')
- 70% Assigning workers to resources
  - [X] Define resource gathering priorities
  - [X] Automatically adjust worker assignment to fulfil priorities
    - [ ] Bug: reassigned workers sometimes do not switch tasks
    - [ ] Bug: sometimes multiple workers are assigned to the same farm when there are farms with no workers assigned
- 20% Army management
  - [X] Army gather point
    - [X] Bug: Army gather point is sometimes on the wrong side of the base
  - [X] Attack
    - [X] Select an attack target and send a group to attack
    - [ ] Attack group staging point
  - [ ] Defense
    - [ ] Seek and destroy enemy units near bases using entire idle army
    - [ ] Defense group staging point
    - [ ] Use at little units as possible to defend
  - [ ] Harass
    - [ ] Select a harass target and send a group to attack
    - [ ] Harass group staging point
  - [ ] Micro
    - [ ] Focus-fire
    - [ ] Pullback
    - [ ] Spell-casting // will most likely use the wave survival code
  - [ ] Map analysis
    - [ ] Routing
    - [ ] Choke points
    - [ ] Improved gather points and attack angles
- 40% Prebuilt AIs
  - Villager AI
    - [X] Attack
    - [X] Produces
    - [X] Harvest
    - [ ] Upgrades
    - [ ] Expand
    - [ ] Harass
    - [ ] Enchant
    - [ ] Job management
    - [ ] Militia
  - Monster AI
    - [X] Attack
    - [X] Produces
    - [ ] Harvest
    - [ ] Upgrades
    - [ ] Expand
    - [ ] Harass
    - [ ] Night timing attacks
    - [ ] Sculk sensor expansion
  - Piglin AI
    - [ ] Attack
    - [ ] Produces
    - [ ] Harvest
    - [ ] Upgrades
    - [ ] Expand
    - [ ] Harass
    - [ ] Portal Network