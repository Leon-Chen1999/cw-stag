## Briefing on STAG assignment
### Task 1: Introduction


This workbook introduces the final assignment that is worth the remaining 45% of the assessment for this unit. The focus of this assignment is the construction of a general-purpose socket-server game-engine for text adventure games. 

A typical game of this genre is illustrated in the screenshot shown below. The following sections of this workbook explain the construction of this Simple Text Adventure Game (STAG) in more detail and provide a breakdown of the features you are required to implement.

<a href="http://www.web-adventures.org/cgi-bin/webfrotz?s=ZorkDungeon&n=4185" target="_blank">
<img src="resources/zork.jpg">
</a>
  


# 
### Task 2: Game Engine
 <a href='02%20Game%20Engine/video/adventure.mp4' target='_blank'> ![](../../resources/icons/video.png) </a>

Your aim is to build a game engine server that communicates with one or more game clients.
Your server will listen for incoming connections from clients (in a similar way to the DB exercise).
When a connection has been made, your server will receive an incoming command, process the actions
that have been requested, make changes to any game state that is required and then finally send back a
suitable response to the client. View the video linked to above for a demonstration of a typical example
of communication from the perspective of the client (note that there is no audio track in this video).

The basic networking operation is provided for you in the 
<a href="resources/cw-stag/" target="_blank">maven template</a> for this project.
As with the DB exercise, you do not need to write the client since this is provided for you.
It is again essential that you do NOT change the code in the client.
The interactive client will be replaced by an automated test client when marking your work.

To execute the server from the command line, type `mvnw exec:java@server`.
To execute the client that will connect to the server, type `mvnw exec:java@client -Dexec.args="simon"`
note that the `-Dexec.args` flag allows us to pass an argument into the client
(in this case the username of the current player).
This username is then passed by the client to the server at the start of each command
(so the server knows which player the command came from).

The aim of this assignment is to build a versatile game engine that it is able to play _any_ text adventure game
(providing that it conforms to certain rules). To support this versatility, two configuration files:
**entities** and **actions** are used to describe the various "things" that are present in the game,
their structural layout and dynamic behaviours.
These two configuration files are passed into the game server when it is instantiated like so:

``` java
public GameServer(File entitiesFile, File actionsFile)
```

The server will load the game scenario in from the two configuration files, thus allowing a range of different games to be played.
During the marking process, we will use some custom game files in order to explore the full range of functionality in your code.
It is therefore essential that your game engine allows these files to be passed in and then reads and interprets their content
(otherwise we won't be able to test your code).

As with the Database assignment, you should build a robust and resilient server which will be able to keep running,
no matter what commands the user throws at it. Note that game state must NOT be made persistent between server invocations.
When the server starts up, the game state should be loaded from the _original_ config files
(do NOT update these with changes as the game progresses). The server should NOT remember the state of any previous games.

  


# 
### Task 3: Basic Commands


In order to communicate with the server, we need an agreed language
(otherwise the user won't know what to type to interact with the game !).
There are a number of standard "built-in" gameplay commands that your game engine should respond to:

- "inventory" (or "inv" for short): lists all of the artefacts currently being carried by the player
- "get": picks up a specified artefact from the current location and adds it into player's inventory
- "drop": puts down an artefact from player's inventory and places it into the current location
- "goto": moves the player to the specified location (if there is a path to that location)
- "look": prints names and descriptions of entities in the current location and lists paths to other locations

It is essential that you conform to this standard set of commands, otherwise it won't be possible to play your game
(and your game engine will fail some of the marking tests !). To illustrate these commands in use (and to demonstrate
the expected responses) some examples are included in a test script found in the maven project's test folder.

In addition to these standard "built-in" commands, it is possible to customise a game with a number of additional **actions**.
These will be introduced in more detail in a later section of this workbook.

The skeleton 
<a href="resources/cw-stag/src/main/java/edu/uob/GameServer.java" target="_blank">GameServer</a> class
(that you have been given as part of the template project) includes all the code required to deal with network communication.
All you will need to do is to complete the command handler that interprets the incoming command string,
makes changes to the game state and then return a suitable text response to send back to the client.


  


**Hints & Tips:**  
Note that built-in commands are reserved words and therefore cannot be used as names for any other elements of the command language.  


# 
### Task 4: Game Entities


Game **entities** are a fundamental building block of any text adventure game.
Entities represent a range of different "things" that exist within a game.
The different types of entity represented in the game are as follows:

- Locations: Rooms or places within the game
- Artefacts: Physical things within the game that can be collected by the player
- Furniture: Physical things that are an integral part of a location
(these can NOT be collected by the player)
- Characters: The various creatures or people involved in game
- Players: A special kind of character that represents the user in the game

It is worth noting that **locations** are complex constructs in their own right and
have various different attributes including:
- Paths to other locations (note: it is possible for paths to be one-way !)
- Characters that are currently at a location
- Artefacts that are currently present in a location
- Furniture that belongs within a location

Entities are defined in one of the game configuration files using a language called "DOT".
This language can be used to express the structure of a graph (which is basically what a text adventure game fundamentally is !)

The big benefit of using DOT files to store game entities is that we can render them graphically
using visualisation tools such as <a href="https://dreampuf.github.io/GraphvizOnline/" target="_blank">this</a>.
These tools allow us to actually SEE the structure of the game configuration
(for example see the graphical representation of an entity file shown below).
As you can see, each location is represented by a rectangular box containing a number of different entities
(each type of entity being represented by a different shape). The paths between locations are
presented in the form of directed arrows.  


![](04%20Game%20Entities/images/basic-entities.png)

# 
### Task 5: Loading Entities


We have provided some example entity files for you to use in your project.
Firstly there is a <a href="resources/cw-stag/config/basic-entities.dot" target="_blank">basic entities file</a>
to help get you started in constructing your game engine.
We have also provided an <a href="resources/cw-stag/config/extended-entities.dot" target="_blank">extended entities file</a>
that can be used for more extensive testing during the later stages of your work.

You already have much experience of writing parsers from previous exercises (both on this unit and others).
We don't really want to cover old ground, so for this assignment you will NOT be required to build your own parser.
Instead, you are able to use existing parsing libraries for reading in the configuration files.
There is considerable educational value in learning to use existing libraries and frameworks in this way.

With this in mind, you should use the
<a href="http://www.alexander-merz.com/graphviz/doc/com/alexmerz/graphviz/Parser.html" target="_blank">JPGD parser</a>
in order to parse the entity DOT files. Use this library to extract a set of 
<a href="http://www.alexander-merz.com/graphviz/doc/com/alexmerz/graphviz/objects/package-summary.html" target="_blank">GraphViz Objects</a>
and then store them in a suitable data structure so that your server can access them.
We have provided you with an abstract `GameEntity` class in the maven project that you should use to represent entities within your game.
You should write a number of concrete subclasses that inherit from this abstract class to represent specific types of entity.

All entities will need at least a name and a description, some may need additional attributes as well.
To make things easier, entity names cannot contain spaces (the DOT parser doesn't like it if they do !).
It is also worth mentioning that entity names defined in the configuration files will be unique.
Additionally, there should only be a single instance of each entity within the game.
You won't have to deal with two things called `door` (although your might see a `blue_door` and a `red_door`).
As such, you can safely use entity names as unique identifiers.

Every game has a "special" location that is the starting point for an adventure.
The start location can be called anything we like, however it will _always_ be the **first** location
that appears in the "entities" file.

There is another special location called `storeroom` that can be found in the entities file.
This location does not appear in the game world (there will be no paths to/from it).
The `storeroom` is a container for all of the entities that have no initial location in the game.
Everything needs to exist somewhere in the game structure (so that they can be defined in the entities file).
These entities will not enter the game until an action places them into another location within the game.
  


**Hints & Tips:**  
To ensure that the basic entities file is in the correct location in the project folder and that it is a valid DOT file, we have provided a
<a href="resources/cw-stag/src/test/java/edu/uob/EntitiesFileTests.java" target="_blank">JUnit test class</a> for you to use.
Not only will these tests verify the basic entities file, but they also provides a clear illustration of the use of the JPGD library
for parsing DOT files.

A `.jar` file containing the JPGD library can be found in the `libs` folder of the maven template.
This _should_ already be part of the project dependencies, but it is useful to know where the library resides
(in case you have add it manually to your IntelliJ project).

Note that the `locations` subgraph will _always_ be **first** in the entities file and the `paths` subgraph will _always_
appear **after** the locations (the parser doesn't like it if we switch this ordering !)

You may assume that all entity files used during marking are in a valid format (our aim isn't to test the robustness of the parsing libraries).
  


# 
### Task 6: Game Actions


In addition to the standard "built-in" commands (e.g. `get`, `goto`, `look` etc.), your game engine should also
respond to any of a number of game-specific commands (as specified in the "actions" file).
Each of these **actions** will have the following elements:

- One or more possible **trigger** phrases (ANY of which can be used to initiate the action)
- One or more **subject** entities that are acted upon (ALL of which need to be available to perform the action)
- An optional set of **consumed** entities that are all removed ("eaten up") by the action
- An optional set of **produced** entities that are all created ("generated") by the action
- A **narration** that provides a human-readable explanation of what happened when the action is performed

Note that being "available" requires the entity to _either_ be in the inventory of the player invoking the action
_or_ for that entity to be in the room/location where the action is being performed. This feature is
intended to be a shortcut so that a player can use an entity in their location without having to explicitly pick it up first.
Additionally, subjects of an action can be locations, characters or furniture (which can't be picked up).

It is worth noting that action trigger keyphrases are NOT unique - for example there may be multiple "open" actions that
act on different entities. Note that trigger phrases cannot (and will not) contain the names of entities,
since this would make incoming commands far too difficult to parse. Just consider the challenge of trying to
interpret the command: `lock lock with key`.

Upon receiving an action command, your server should attempt to find an appropriate matching action.
Note that the action is only valid if ALL **subject** entities (as specified in the actions file) are available to the player.
If a valid action is found, your server must undertake the relevant additions/removals (production/consumption).

When an entity is _produced_, it should be moved from its current location in the game (which might be in the `storeroom`)
to the location in which the action was trigged. The entity should NOT automatically appear in a players inventory -
it might be furniture (which the player can't carry) or it might be an artefact they don't actually want to pick up !

When an entity is _consumed_ it should be removed from its current location (which could be any location within the game)
moved into the `storeroom` location. If the game writer wants to enforce co-location (i.e. the consumed entity must be
in the same location as the player) then they must include that entity as a subject of the action.

Note that it is NOT possible to perform an action where a subject, or a consumed or produced entity is currently in
_another_ player's inventory. You should consider these entities "out of bounds" and not available to a player who does not
currently hold that artefact. You can't unlock a door if another player has the key !

Locations can be used as subjects, consumed and produced entities of an action (just like other entities).
Consumed locations however are not moved to the storeroom - instead, the path between the current location and consumed location is removed
(there may still be other paths to that location in other game locations). For produced locations, a new (one-way) path is added from the
current location to the "produced" location.
  


# 
### Task 7: Loading Actions


We have provided some example action files for you to use in your project.
Firstly there is a <a href="resources/cw-stag/config/basic-actions.xml" target="_blank">basic actions file</a>
to help get you started in constructing your game engine.
We have also provided an <a href="resources/cw-stag/config/extended-actions.xml" target="_blank">extended actions file</a>
that can be used for more extensive testing during the later stages of your work.

Both of these documents are written in eXtensible Markup Language (XML).
In order to successfully parse these XML files, you should use the Java API for XML Processing (JAXP).
In particular, you should make use of the
<a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.xml/javax/xml/parsers/DocumentBuilder.html" target="_blank">DocumentBuilder</a>
class as well as various classes from the 
<a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.xml/org/w3c/dom/package-summary.html" target="_blank">org.w3c.dom</a> package.
See the "Hints and Tips" section of this task for an example of how to use these classes.

Once loaded in from the XML file, you should store the actions in a suitable data structure so that your server can access them quickly and easily.
Your first thought might be to use an `ArrayList` for this purpose.
This approach would however be slow and inefficient for some operations.
In the interests of efficiency (and to provide you with broader experience of using the classes in the Java `Collections` package)
you should use the following data structure to store all of your actions:

```java
HashMap<String,HashSet<GameAction>> actions = new HashMap<String, HashSet<GameAction>>();
```

The data structure described in the above code is illustrated in the diagram shown below. The
<a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/HashMap.html" target="_blank">HashMap</a>
provides a fast and efficient lookup mechanism - using a `String` key (the trigger keyphrase) to map to a 
<a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/HashSet.html" target="_blank">HashSet</a>
of matching actions. Remember that a particular trigger keyphrase may be used
in more than one action, so we need to map to a _composite_ data structure (rather than just a single `GameAction` element).
It is useful for us to make use a _set_ (rather than, for example, an `ArrayList`)
since this allows us to ensure that there are no duplicate actions being stored in the data structure (all elements of a set will be unique).
  


![](07%20Loading%20Actions/images/HashMap-HashSet.jpg)

**Hints & Tips:**  
To ensure that the basic actions file is in the correct location in the project folder and that it is a valid XML file, we have provided a
<a href="resources/cw-stag/src/test/java/edu/uob/ActionsFileTests.java" target="_blank">JUnit test class</a> for you to use.
Not only will these tests verify the basic actions file, but they also provides a clear illustration of the use of the JAXP library
for parsing XML files.

You may assume that all action files used during marking are in a valid format (our aim isn't to test the robustness of the parsing libraries).
  


# 
### Task 8: Command Flexibility


Interpreting the input from users in this assignment is very different from dealing with queries in the Database exercise.
The simplified SQL that we used for the last assignment was limited, rigid and constrained.
As such, it was possible to write a reasonably concise BNF grammar to define the entire language.
The same is not true for natural language, which is very complex, permits much variation and is used diversely (depending on the speaker/writer).
The best we can hope do is to try to make our command interpreter as flexible and versatile as possible - in order to cope with most eventualities.
Below are various features your should implement in your interpreter:

**Case Insensitivity**  
All commands (including entity names, locations, built in commands and action triggers) should be treated as case insensitive.
This ensure that, no matter what capitalisation a player chooses to use in their commands, the server will be able in interpret their intensions.

**Decorated Commands**  
In order to support variability, your interpreter should be able to cope with _additional_ "decorative" words being inserted into a command.
For example, the basic command `chop tree with axe` might well be entered by the user as `please chop the tree using the axe`.
Both versions are equivalent and should both be accepted by your command interpreter.

**Word Ordering**  
The ordering of the words in a command should not effect the server's ability to find appropriate matching actions.
For example `chop tree with axe` and `use axe to chop tree` are equivalent and should both be accepted by your command interpreter.

**Partial Commands**  
To further support flexible natural language communication, your server should be able to operate with shortened, "partial" commands.
It is convenient for the user to be able to omit _some_ of the subjects from a command, whilst still providing enough information for
the correct action to be identified.
For example, the command `unlock trapdoor with key` could alternatively be entered as _either_ `unlock trapdoor` _or_ `unlock with key` -
both of which provide enough detail for an action match to be attempted.
In order to stand a chance of matching a command to an action, each incoming command MUST contain a trigger phrase and _at least_ one subject.
Anything less than this and the intended action will probably be too vague to identify.

**Extraneous Entities**  
When searching for an action, you must match ALL of the subjects that are specified in the incoming command (e.g. `repair door with hammer and nails`).
Extraneous entities included within an incoming command (i.e. entities that are in the incoming command, but not specified in the action file)
should prevent a match from being made. This is to prevent the user attempting to perform actions with inappropriate entities
(e.g. `open potion with hammer` should not succeed).

**Ambiguous Commands**  
Much of the above "fuzzy" matching of actions is risky - there may be situations where _more than one_ action matches a particular command.
If a particular command is ambiguous (i.e. there is _more than one_ **valid** and **performable** action possible - given the current state of the game)
then NO action should be performed and a suitable warning message sent back to the user
(e.g. `there is more than one 'open' action possible - which one do you want to perform ?`)

**Composite Commands**  
Composite commands (commands involving more than one activity) should NOT be supported.
Users are unable to use commands such as `get axe and coin`, `get key and open door` or `open door and potion`.
A single command can only be used to perform a single built-in command or single game action.

**Error messages**  
Note that due to the range of possible response messages it is possible to return to the user, we will not test for specific error/anomaly messages.
Rather, we will check the game state using the standard command set (`look`, `inv` etc) in order to check that inappropriate actions have not
been performed by your server. That said, we still encourage you to return suitable error messages to the user for the sake of playability of your game.

Although there is much flexibility and variability in the command language, the test cases we will use during the marking process
will focus upon testing user input that is "fair and reasonable".
We are interested in assessing the ability of the command interpreter to detect valid, sensible and likely inputs from the user.
This requires the user to provide enough information to allow the interpreter to uniquely identify an action,
whilst at the same time giving them some flexibility about how they go about expressing the command.
We aren't looking for the ability of the interpreter to deal with illogical or silly commands.
We just want to avoid the situation where the user thinks: "That was a fair command, why won't it understand me".
If the user types something weird and strange they shouldn't be surprised if something weird and strange happens.

  


# 
### Task 9: Multiple Players


If you are feeling ambitious, extend your game so that it is able to operate with more than just a single player.
In order to support this feature, incoming command messages begin with the username of the player who issued that command.
For example, a "full" incoming message might therefore take the form of:
```
simon: open door with key
```

Where everything _before_ the first `:` is the player's name and everything _after_ is the command itself.
Valid player names can consist of uppercase and lowercase letters, spaces, apostrophes and hyphens.
Note that there is no "formal" player registration process - when the server encounters a command from
a previously unseen user, a new player should be created and placed in the start location of the game.

Note that there is no need for your server to implement any form of authentication - 
you can assume that the client handles this responsibility.
Your server should simply trust any commands that purport to come from a particular user.
This may seem unsafe, however this is intended to limit the scope and complexity of this assignment.

It is essential that when an incoming message is received, the command is applied to the _correct_ player.
To achieve this, you will need to maintain _some_ elements of game state _separately_ for each individual player.
For example, each player may be in a different location in the game and will carry their own inventory of artefacts.

One final thing to remember is that you should include _other_ players in your description of a location
when a `look` command is issued by a user. There is no point having multiple players in the game if they can't
actually _see_ each other ! Note that even though they can see each other, human players cannot interact directly.
This is because, due to dynamic player naming, it is not possible to write action rules involving player names
(since they are not known in advance of the game being played).  


# 
### Task 10: Player Health


As an extension to the basic game, you might like to add a "health level" feature.
Each player should start with the maximum health level of 3. Consumption of "Poisons & Potions" or interaction with beneficial
or dangerous characters will increase or decrease a player's health by one point. You will see in the 
<a href="resources/cw-stag/config/extended-actions.xml" target="_blank">extended actions file</a>
the use of the `health` keyword in the `produced` and `consumed` fields.
These indicate actions which increase and decrease your health by one unit.
Note that although a player's health can never increase above the maximum (i.e. 3)
actions producing health are still _performable_ but they will have no effect on the player's health level.

When a player's health runs out (i.e. when it becomes zero) they should lose all of the items in their inventory
(which are dropped in the location where they ran out of health).
The player should then be transported to the start location of the game and their health level restored to full (i.e. 3).
You might also like to send a suitable message to the user, for example:
```
you died and lost all of your items, you must return to the start of the game
```
It is important that you should not reset the whole game state when one player dies
(for example, previously opened location paths should still exist).
You should remember that there may be more than one player of the game - they shouldn't see their world change just
because another player has died.

In order to fully support these features in your game engine, you should implement a new `health` built-in command,
so that the player can keep track of their current health level. Upon receiving a `health` command from a user,
the server should report back the player's current health level (as a number).

  


# 
### Task 11: Marking


For consistency and compatibly with all test cases, it is essential that you adhere to the gameplay commands detailed in this workbook.
You should also ensure that you do not change the name of your main class - it must be called `GameServer`.
If you change the name of the class (or the `handleCommand` method) the marking script will not be able to test your code !
It is **ESSENTIAL** that you check your code still passes the original test script provided as part of the maven project.
If your code does not pass these basic tests, then it is likely your code not not pass any of the marking test scripts.

Make sure your code does not contain anything specific to your computer (e.g. absolute file paths, operating system specific code etc).
Before submitting your code, we advise your to test your project on a computer _other than the one it was developed on_ (e.g. a lab machine).
Clear out all temporary files and then ensure the code compiles and runs correctly using Maven. We will apply a penalty mark if we cannot run your code
"out of the box" - we can't spend time fixing everyones projects before we mark them !

You should be sure include a suitable range of tests to your project.
Your aim is to verify the correct operation all of the features of the game that you have implemented.
The completeness and comprehensivity of your test cases will be taken into account during the marking of this assignment.
Remember to include the entire `cw-stag` project structure when you submit your code
(including both your `main` source folder as well as the `test` folder containing your test scripts).

Note that the "quality" of your code will be taken into account when assessing your work.
The code quality metrics outlined earlier in this unit will be used to derive your final mark.
It is important therefore that you adhere to the structure and style guidelines outlined in the "code quality" workbook.
It is also essential that you take heed of the quality feedback you have received for previous exercises,
since this will help you improve your work - not just for this exercise, but in the long term.  


# 
### Task 12: Plagiarism


You are encouraged to discuss assignments and possible solutions with other students.
HOWEVER it is essential that you only submit your own work.
This may feel like a grey area, however if you adhere to the following advice, you should be fine:

- Don't take chunks of code from existing solutions (e.g. those found online)
- Never exchange code with other students (via IM/email, USB stick, GIT, printouts or photos !)
- Although pair programming is encouraged on some units, on this unit you must type your own work !
- It's OK to seek help from online sources (e.g. Stack Overflow) but don't just cut-and-paste chunks of code
- If you don't understand what a line of code actually does, you shouldn't be submitting it !
- Don't submit anything you couldn't re-implement under exam conditions (with a good textbook !)

An automated checker will be used to flag any incidences of possible plagiarism
(both within this year's cohort as well as previous years submissions).
If the markers feel that intentional plagiarism has actually taken place, marks may be deducted.
In serious or extensive cases, the incident may be reported to the faculty plagiarism panel.
This may result in a mark of zero for the assignment, or perhaps even the entire unit
(if it is a repeat offence).
Don't panic - if you stick to the above list of advice, you should remain safe !  


# 
