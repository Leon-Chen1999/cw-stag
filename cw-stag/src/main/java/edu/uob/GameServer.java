package edu.uob;

import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;
import edu.uob.Entities.*;
import edu.uob.exception.GameException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.*;
import java.io.FileNotFoundException;


/** This class implements the STAG server. */
public final class GameServer {
    private HashMap<GameEntity, HashMap<String, HashSet<GameEntity>>> entityMap;
    private HashSet<String> locationStringMap;
    private HashSet<String> artefactStringMap;
    private HashSet<String> furnitureStringMap;
    private HashSet<String> characterStringMap;
    private HashSet<String> keyWords;
    private HashMap<String, HashSet<GameAction>> actionsMap;
    private HashMap<String,HashSet<String>> pathMap;
    private HashSet<ArtefactEntity> invSet;
    private HashMap<String, PlayerEntity> playersMap;
    private LocationEntity initLocation;
    private LocationEntity changedLocation;



    private static final char END_OF_TRANSMISSION = 4;

    public static void main(String[] args) throws IOException {
//        File entitiesFile = Paths.get("config" + File.separator + "basic-entities.dot").toAbsolutePath().toFile();
        File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
//        File actionsFile = Paths.get("config" + File.separator + "basic-actions.xml").toAbsolutePath().toFile();
        GameServer server = new GameServer(entitiesFile, actionsFile);
        server.blockingListenOn(8888);
    }

    /**
    * KEEP this signature (i.e. {@code edu.uob.GameServer(File, File)}) otherwise we won't be able to mark
    * your submission correctly.
    *
    * <p>You MUST use the supplied {@code entitiesFile} and {@code actionsFile}
    *
    * @param entitiesFile The game configuration file containing all game entities to use in your game
    * @param actionsFile The game configuration file containing all game actions to use in your game
    *
    */
    public GameServer(File entitiesFile, File actionsFile) {
        // TODO implement your server logic here
        this.entityMap = new HashMap<GameEntity, HashMap<String, HashSet<GameEntity>>>();
        this.actionsMap = new HashMap<String, HashSet<GameAction>>();
        this.pathMap = new HashMap<String,HashSet<String>>();
        this.invSet = new HashSet<ArtefactEntity>();
        this.playersMap = new HashMap<String, PlayerEntity>();
        this.locationStringMap = new HashSet<String>();
        this.artefactStringMap = new HashSet<String>();
        this.furnitureStringMap = new HashSet<String>();
        this.characterStringMap = new HashSet<String>();
        this.keyWords = new HashSet<String>();
        parseEntitiesFile(entitiesFile,entityMap);
        parseActionsFile(actionsFile, actionsMap);
    }

    /**
    * KEEP this signature (i.e. {@code edu.uob.GameServer.handleCommand(String)}) otherwise we won't be
    * able to mark your submission correctly.
    *
    * <p>This method handles all incoming game commands and carries out the corresponding actions.
    */
    public String handleCommand(String command){
        // TODO implement your server logic here
//        String clientMessage = "";
        String playName = getPlayerName(command);
        if (!playersMap.containsKey(playName)) {
            PlayerEntity player = new PlayerEntity(playName, playName, 3);
            playersMap.put(playName, player);
//            player.setCurrentLocation(initLocation);
//            showOtherPlayer(player);
        }
        PlayerEntity player = playersMap.get(playName);
//        //todo !!!!!!!!!!!!!!!!!
        player.setCurrentLocation(changedLocation);

        return completeTheCommand(command, player);
    }

    //  === Methods below are there to facilitate server related operations. ===

    /**
    * Starts a *blocking* socket server listening for new connections. This method blocks until the
    * current thread is interrupted.
    *
    * <p>This method isn't used for marking. You shouldn't have to modify this method, but you can if
    * you want to.
    *
    * @param portNumber The port to listen on.
    * @throws IOException If any IO related operation fails.
    */

    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.out.println("Connection closed");
                }
            }
        }
    }

    /**
    * Handles an incoming connection from the socket server.
    *
    * <p>This method isn't used for marking. You shouldn't have to modify this method, but you can if
    * * you want to.
    *
    * @param serverSocket The client socket to read/write from.
    * @throws IOException If any IO related operation fails.
    */

    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
            System.out.println("Connection established");
            String incomingCommand = reader.readLine();
            if(incomingCommand != null) {
                System.out.println("Received message from " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }

    public String completeTheCommand( String command, PlayerEntity player){
        String clientMessage = "";
        ArrayList<String> splitCommand = splitCommand(command);
        splitCommand = separateCommand(splitCommand);
        if (isBasicCommand(splitCommand)){
            try {
                clientMessage = basicCommand(splitCommand, player);
            } catch (GameException e) {
                return e.getMessage();
            }
        }else {
            try {
                clientMessage = actionCommand(splitCommand, player);
            }catch (GameException e){
                return e.getMessage();
            }
        }
        return clientMessage;
    }

    public ArrayList<String> separateCommand(ArrayList<String> splitCommand){
        for (int i = 0; i < splitCommand.size(); i++) {
            String command = splitCommand.get(i);
            if (keyWords.contains(command) || characterStringMap.contains(command)) {
//                continue;
            }else if (furnitureStringMap.contains(command) || artefactStringMap.contains(command)){
//                continue;
            }else if (locationStringMap.contains(command)){
//                continue;
            }else splitCommand.remove(i);
        }
        return splitCommand;
    }

    public boolean isBasicCommand(ArrayList<String> splitCommand){
        ArrayList<String> list = new ArrayList<>(Arrays.asList("look", "goto", "inv", "inventory", "get", "drop"));
        for (int i = 0; i < splitCommand.size(); i++) {
            if (list.contains(splitCommand.get(i))) return true;
        }
        return false;

    }

    public String basicCommand(ArrayList<String> splitCommand, PlayerEntity player) throws GameException {
        String clientMessage = "";
        if (checkDuplicateCommand(splitCommand)){
            if (containsCommand(splitCommand, "look")){
                if (splitCommand.size() == 1) {
                    clientMessage = lookCommand(player);
                }else clientMessage = "[ERROR] not look syntax\n";
            }else if (containsCommand(splitCommand, "get")){
                if (splitCommand.size() == 2){
                    if (checkOrderInCommand(splitCommand, "get", "artefacts")){
                        clientMessage = getCommand(player,splitCommand);
                    }else clientMessage = "[ERROR] wrong order in get syntax\n";
                }else clientMessage = "[ERROR] not get syntax\n";
            }else if (containsCommand(splitCommand, "drop")){
                if (splitCommand.size() == 2){
                    if (checkOrderInCommand(splitCommand, "drop", "artefacts")){
                        clientMessage = dropCommand(player,splitCommand);
                    }else clientMessage = "[ERROR] wrong order in drop syntax\n";
                }else clientMessage = "[ERROR] not drop syntax\n";
            } else if (containsCommand(splitCommand, "goto")) {
                if (splitCommand.size() == 2){
                    if (checkOrderInCommand(splitCommand, "goto", "location")){
                        clientMessage = gotoCommand(player,splitCommand);
                    }else clientMessage = "[ERROR] wrong order in goto syntax\n";
                }else clientMessage = "[ERROR] not goto syntax\n";
            } else if (containsCommand(splitCommand, "inv") || containsCommand(splitCommand, "inventory")) {
                if (splitCommand.size() == 1){
                    clientMessage = invCommand(player);
                }else clientMessage = "[ERROR] not inv syntax\n";
            }
        }
        return clientMessage;
    }

    public String actionCommand(ArrayList<String> splitCommand, PlayerEntity player) throws GameException {
        //fixme are you sure is the 0th
        String command = splitCommand.get(0);
        String returnMessage = "";
        //fixme modify throw
        if (!checkIfTwoEntities(splitCommand));
        switch (command){
            case ("open"):
            case ("unlock"): returnMessage = openActionCommand(player, splitCommand);
                break;
            case ("chop"):
            case ("cut"):
            case ("cut down"): returnMessage = chopActionCommand(player, splitCommand);
                break;
            case ("drink"): returnMessage = drinkActionCommand(player, splitCommand);
                break;
            case ("fight"):
            case ("hit"):
            case ("attack"): returnMessage = fightActionCommand(player,splitCommand);
                break;
            case ("pay"): returnMessage = payActionCommand(player,splitCommand);
                break;
            case ("bridge"): returnMessage = bridgeActionCommand(player, splitCommand);
                break;
            case ("dig"): returnMessage = digActionCommand(player, splitCommand);
                break;
            case ("blow"): returnMessage = blowActionCommand(player, splitCommand);
                break;
            default:
                returnMessage = "[ERROR] wrong syntax";
                break;
        }
        actionsMap.get(command);
        return returnMessage;
    }

    public void parseEntitiesFile(File entitiesFile, HashMap<GameEntity, HashMap<String, HashSet<GameEntity>>> entityMap){
        try {
            Parser parser = new Parser();
            //fixme why use entitiesFile is wrong?
            FileReader reader = new FileReader(entitiesFile.getAbsolutePath());
            parser.parse(reader);
            Graph wholeDocument = parser.getGraphs().get(0);
            ArrayList<Graph> sections = wholeDocument.getSubgraphs();
            // The locations will always be in the first subgraph
            ArrayList<Graph> locations = sections.get(0).getSubgraphs();
            //get all the entities in the map
            getEntitiesInMap(locations,entityMap);
            //get path
            getPathsInMap(sections,pathMap);
            //how to get path
        } catch (FileNotFoundException e) {
//            String message = e.getMessage();
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void getPathsInMap(ArrayList<Graph> sections, HashMap<String,HashSet<String>> pathMap){
        ArrayList<Edge> paths = sections.get(1).getEdges();
        for (int i = 0; i < paths.size(); i++) {
            String fromLocationName = paths.get(i).getSource().getNode().getId().getId();
            String toLocationName = paths.get(i).getTarget().getNode().getId().getId();
            if (!pathMap.containsKey(fromLocationName)){
                HashSet<String> tmp = new HashSet<>();
                tmp.add(toLocationName);
                pathMap.put(fromLocationName,tmp);
            }else {
                HashSet<String> tmp = pathMap.get(fromLocationName);
                tmp.add(toLocationName);
                pathMap.put(fromLocationName, tmp);
            }
        }
    }

    public void getEntitiesInMap(ArrayList<Graph> locations, HashMap<GameEntity, HashMap<String, HashSet<GameEntity>>> entityMap){
        //except storeroom
        String initLocationName = locations.get(0).getNodes(false).get(0).getId().getId();
        String initLocationDes = locations.get(0).getNodes(false).get(0).getAttribute("description");
        this.initLocation = new LocationEntity(initLocationName, initLocationDes);
        this.changedLocation = new LocationEntity(initLocationName, initLocationDes);
        for (int i = 0; i < locations.size() ; i++) {
            Graph certainLocation = locations.get(i);
            parseCluster(certainLocation,entityMap);
        }
    }

    //analyse each cluster，get location, artefacts, furniture
    public void parseCluster(Graph certainLocation, HashMap<GameEntity, HashMap<String, HashSet<GameEntity>>> entityMap){
        //get entities
        HashMap<String, HashSet<GameEntity>> differentEntityMap = new HashMap<String, HashSet<GameEntity>>();
        String location = certainLocation.getNodes(false).get(0).getId().getId();//location
        String locationDescription = certainLocation.getNodes(false).get(0).getAttribute("description");
        LocationEntity locationEntity = new LocationEntity(location,locationDescription);
        locationStringMap.add(location);
        //store the subgraph into hashset
        ArrayList<Graph> tmpGraph = certainLocation.getSubgraphs();
        for (int i = 0; i < tmpGraph.size(); i++) {
            ArrayList<Node> tmpNode = new ArrayList<Node>();
            tmpNode = tmpGraph.get(i).getNodes(false);
            String entityType = tmpGraph.get(i).getId().getId();
            HashSet<GameEntity> tmpSet = new HashSet<GameEntity>();
            if (tmpNode.size() != 0){
                for (int j = 0; j < tmpNode.size(); j++) {
                    String entityName= tmpNode.get(j).getId().getId();
                    String entityDescription = tmpNode.get(j).getAttribute("description");
                    switch (entityType){
                        case ("artefacts"):
                            ArtefactEntity artefactEntity = new ArtefactEntity(entityName,entityDescription);
                            artefactStringMap.add(entityName);
                            tmpSet.add(artefactEntity);
                            break;
                        case ("furniture"):
                            FurnitureEntity furnitureEntity = new FurnitureEntity(entityName,entityDescription);
                            furnitureStringMap.add(entityName);
                            tmpSet.add(furnitureEntity);
                            break;
                        case ("characters"):
                            CharacterEntity characterEntity = new CharacterEntity(entityName,entityDescription);
                            characterStringMap.add(entityName);
                            tmpSet.add(characterEntity);
                            break;
                        default:
                            break;
                    }
                }
            }
            differentEntityMap.put(entityType,tmpSet);
        }
        entityMap.put(locationEntity,differentEntityMap);
    }

    public void parseActionsFile(File actionsFile, HashMap<String,HashSet<GameAction>> actionsMap) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(actionsFile);
            Element root = document.getDocumentElement();
            NodeList actions = root.getChildNodes();
            getActionsInMap(actions, actionsMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public void getActionsInMap(NodeList actions, HashMap<String,HashSet<GameAction>> actionsMap){
        for (int i = 1; i < actions.getLength(); i+=2) {
            parseEachAction((Element) actions.item(i), actionsMap);
        }
    }

    public void parseEachAction(Element certainAction, HashMap<String,HashSet<GameAction>> actionsMap){
        try {
            NodeList certainTrigger = certainAction.getElementsByTagName("triggers");
            NodeList certainKeyPhrase = certainTrigger.item(0).getChildNodes();
            if (certainKeyPhrase.getLength() != 0){
                for (int i = 1; i < certainKeyPhrase.getLength(); i+=2) {
                    String keyPhraseName = certainKeyPhrase.item(i).getTextContent();
                    GameAction gameAction = new GameAction();
                    elementExceptTrigger(certainAction, gameAction);
                    if (!actionsMap.containsKey(keyPhraseName)){
                        HashSet<GameAction> elementSet = new HashSet<>();
                        elementSet.add(gameAction);
                        actionsMap.put(keyPhraseName, elementSet);
                        keyWords.add(keyPhraseName);
                    }else {
                        actionsMap.get(keyPhraseName).add(gameAction);
                        keyWords.add(keyPhraseName);
                    }
                }
            }
        } catch (DOMException e) {
            throw new RuntimeException(e);
        }
    }

    public void elementExceptTrigger(Element certainAction, GameAction gameAction) {
        NodeList certainSubject = certainAction.getElementsByTagName("subjects");
        NodeList certainConsumed = certainAction.getElementsByTagName("consumed");
        NodeList certainProduced = certainAction.getElementsByTagName("produced");
        NodeList certainNarration = certainAction.getElementsByTagName("narration");
        if (certainSubject.getLength() != 0) {
            for (int i = 0; i < certainSubject.getLength(); i++) {
                String subjectName = certainSubject.item(i).getTextContent().trim();
                String[] words = subjectName.split("\\s+");
                for (String word : words) {
                    gameAction.addSubjectList(word);
                    keyWords.add(word);
                }
            }
        }
        if (certainConsumed.getLength() != 0) {
            for (int i = 0; i < certainConsumed.getLength(); i++) {
                String confusedName = certainConsumed.item(i).getTextContent().trim();
                String[] words = confusedName.split("\\s+");
                for (String word : words) {
                    gameAction.addConsumedList(word);
                    keyWords.add(word);
                }
            }
        }
        if (certainProduced.getLength() != 0) {
            for (int i = 0; i < certainProduced.getLength(); i++) {
                String producedName = certainProduced.item(i).getTextContent().trim();
                String[] words = producedName.split("\\s+");
                for (String word : words) {
                    gameAction.addProducedList(word);
                    keyWords.add(word);
                }
            }
        }
        if (certainNarration.getLength() != 0) {
            String subjectName = certainNarration.item(0).getTextContent().trim();
            gameAction.setNarration(subjectName);
        }
    }

    public String getPlayerName(String command){
        int index = 0;
        for (int i = 0; i < command.length(); i++) {
            if (command.charAt(i) == ':'){
                index = i;
                break;
            }
        }
        return command.substring(0,index);
    }

    public List<String> getEntityInCurLocation(GameEntity currentLocation, HashMap<GameEntity, HashMap<String, HashSet<GameEntity>>> entityMap) {
        List<String> lookList = new ArrayList<>();
        StringBuilder lookMessageBuilder = new StringBuilder();
        HashMap<String, HashSet<GameEntity>> value = new HashMap<>();
        for (GameEntity entity : entityMap.keySet()) {
            if (entity.getName().equals(currentLocation.getName())) {
                value = entityMap.get(entity);
            }
        }
        for (HashSet<GameEntity> entitySet : value.values()) {
            for (GameEntity entity : entitySet) {
                lookList.add(entity.getDescription());
                lookMessageBuilder.append(entity.getDescription()).append("\n");
            }
        }
        return lookList;
    }

    public boolean checkDuplicateCommand(ArrayList<String> commands) {
        ArrayList<String> commandMap = new ArrayList<>(Arrays.asList("look", "get", "drop", "goto", "inv", "inventory"));
        Map<String, Integer> countMap = new HashMap<>();
        for (String command : commands) {
            if (commandMap.contains(command)) {
                countMap.put(command, countMap.getOrDefault(command, 0) + 1);
            }
        }
        for (Integer count : countMap.values()) {
            if (count > 2) {
                return false;
            }
        }
        return true;
    }

    //split command with space
    public static ArrayList<String> splitCommand(String command) {
        return new ArrayList<String>(Arrays.asList(command.split(" ")));
    }

    //check whether the command contains the string like look
    public boolean containsCommand(ArrayList<String> commandList, String command) {
        return commandList.contains(command);
    }

    public String lookCommand(PlayerEntity player){
        List<String> entityInCurLocation = getEntityInCurLocation(player.getCurrentLocation(), entityMap);
        StringBuilder lookMessageBuilder = new StringBuilder("You are in ");
        lookMessageBuilder.append(player.getCurrentLocation().getDescription());
        lookMessageBuilder.append(". You can see:").append("\n");
        for (String entityDescription : entityInCurLocation) {
            lookMessageBuilder.append(entityDescription).append("\n");
        }
        lookMessageBuilder.append("You can access from here:").append("\n");
        for (String s : pathMap.get(player.getCurrentLocation().getName())){
            lookMessageBuilder.append(s).append("\n");
        }
        return lookMessageBuilder.toString();
    }

    public String getCommand(PlayerEntity player, ArrayList<String> splitCommand) throws GameException {
        ArtefactEntity artefactEntity =  getEntityInCommand(player, splitCommand);
        if (!artefactEntity.getName().equals("")){
            //add into inv
            addEntityIntoInv(player, artefactEntity);
            removeFromLocal(player,artefactEntity, "artefacts");
            return "You picked up a " + artefactEntity.getName() + "\n";
        } else {
            throw new GameException("no such artefact");
        }
    }

    public void removeFromLocal(PlayerEntity player, GameEntity gameEntity, String type) throws GameException{
        if (gameEntity.getName().equals("")) throw new GameException("none in the Map");
        for (GameEntity key : entityMap.keySet()) {
            if (key.getName().equals(player.getCurrentLocation().getName())) {
                HashMap<String, HashSet<GameEntity>> map = entityMap.get(key);
                HashSet<GameEntity> artefacts = map.get(type);
                for (GameEntity entity : artefacts) {
                    if (entity.getName().equals(gameEntity.getName())) {
                        //remove from the location
                        artefacts.remove(entity);
                        break;
                    }
                }
                break;
            }
        }
    }

    public FurnitureEntity getEntityFromMap(PlayerEntity player, String name, String type){
        for (GameEntity key : entityMap.keySet()) {
            if (key.getName().equals(player.getCurrentLocation().getName())) {
                HashMap<String, HashSet<GameEntity>> map = entityMap.get(key);
                HashSet<GameEntity> artefacts = map.get(type);
                for (GameEntity entity : artefacts) {
                    if (entity.getName().equals(name)) {
                        //remove from the location
                        return (FurnitureEntity) entity;
                    }
                }
                break;
            }
        }
        return new FurnitureEntity("", "");
    }

    public void addEntityIntoInv(PlayerEntity player ,ArtefactEntity artefactEntity){
        player.addPlayerInv(artefactEntity);
    }

    public void dropEntityFromInv(PlayerEntity player, ArtefactEntity artefactEntity){
        player.dropPlayerInv(artefactEntity);
    }

    public String dropCommand(PlayerEntity player, ArrayList<String> splitCommand) throws GameException {
        ArtefactEntity artefactEntity = getEntityInInv(player,splitCommand);
        if (!artefactEntity.getName().equals("")){
            // drop it from inv
            dropEntityFromInv(player, artefactEntity);
            for (GameEntity key : entityMap.keySet()) {
                if (key.getName().equals(player.getCurrentLocation().getName())) {
                    HashMap<String, HashSet<GameEntity>> map = entityMap.get(key);
                    HashSet<GameEntity> artefacts = map.get("artefacts");
                    //add it to current location
                    artefacts.add(artefactEntity);
                    break;
                }
            }
            return "drop successfully\n";
        }else throw new GameException("no such artefact");
    }

    public ArtefactEntity getEntityInInv(PlayerEntity player, ArrayList<String> splitCommand){
        for (ArtefactEntity a: player.getPlayerInv()){
            for (String command : splitCommand) {
                if (a.getName().equals(command)) {
                    return a;
                }
            }
        }
        return new ArtefactEntity("","");
    }

    public String gotoCommand(PlayerEntity player, ArrayList<String> splitCommand) throws GameException{
        String toLocation = getPathInCommand(splitCommand);
        //check has path，and change the current location
        String fromLocation = player.getCurrentLocation().getName();
        if (pathMap.get(fromLocation).contains(toLocation)){
            for (GameEntity entity : entityMap.keySet()) {
                if (entity.getName().equals(toLocation)) {
                    changedLocation = (LocationEntity) entity;
                    break;
                }
            }
            player.setCurrentLocation(changedLocation);
            return lookCommand(player);
        }else {
            throw new GameException("no such path") ;
        }
    }

    public String getPathInCommand(ArrayList<String> splitCommand) {
        HashSet<String> path = new HashSet<>();
        String pathMessage = "";
        for (String key : pathMap.keySet()) {
            path.add(key);
            for (String t : pathMap.get(key)){
                path.add(t);
            }
        }
        for (String pathName : path) {
            for (String s : splitCommand) {
                if (pathName.equals(s)){
                    return pathName;
                }
            }
        }
        return pathMessage;
    }

    public ArtefactEntity getEntityInCommand(PlayerEntity player, ArrayList<String> splitCommand){
        HashSet<GameEntity> entity = new HashSet<>();
        String currentLocation = player.getCurrentLocation().getName();
        HashMap<String, HashSet<GameEntity>> artifactMap = null;
        for (GameEntity key : entityMap.keySet()) {
            if (key.getName().equals(currentLocation)) {
                artifactMap = entityMap.get(key);
                break;
            }
        }
        if (artifactMap != null && artifactMap.containsKey("artefacts")) {
            entity = artifactMap.get("artefacts");
        }
        for (GameEntity g : entity) {
            for (String s : splitCommand) {
                if (g.getName().equals(s)){
                    return (ArtefactEntity) g;
                }
            }
        }
        return new ArtefactEntity("","");
    }

    public String invCommand(PlayerEntity player){
        StringBuilder invMessageBuilder = new StringBuilder("you have:\n");
        invSet = player.getPlayerInv();
        for (GameEntity g: invSet) {
            invMessageBuilder.append(g.getName()).append("\n");
        }
        return invMessageBuilder.toString();
    }

    public boolean checkOrderInCommand(ArrayList<String> splitCommand, String command, String entityType){
        int indexCommand = 0;
        int indexEntity = 0;
        for (int i = 0; i < splitCommand.size(); i++) {
            if (splitCommand.get(i).equals(command)){
                indexCommand = i;
                break;
            }
        }
        switch (entityType){
            case ("artefacts"): indexEntity = getIndexOfEntity(artefactStringMap, splitCommand);
                break;
            case ("location"): indexEntity = getIndexOfEntity(locationStringMap, splitCommand);
                break;
            case ("furniture"): indexEntity = getIndexOfEntity(furnitureStringMap, splitCommand);
                break;
            case ("character"): indexEntity = getIndexOfEntity(characterStringMap, splitCommand);
                break;
            default:
                break;
        }
        return indexCommand < indexEntity;
    }

    public int getIndexOfEntity(HashSet<String> set, ArrayList<String> splitCommand){
        for (int i = 0; i < splitCommand.size(); i++) {
            for (String s : set) {
                if (splitCommand.get(i).equals(s)){
                    return i;
                }
            }
        }
        return -1;
    }

    /** action part */
    public boolean checkIfTwoEntities(ArrayList<String> splitCommand){
        int keyPhraseNumber = 0;
        String keyPhrase = "";
        int subjectNumber = 0;
        int consumedNumber = 0;
        for (int i = 0; i < splitCommand.size(); i++) {
            String command = splitCommand.get(i);
            if (actionsMap.containsKey(command)) {
                keyPhraseNumber++;
                keyPhrase = command;
            }
        }
        if (keyPhraseNumber != 1) return false;
        HashSet<GameAction> actions = actionsMap.get(keyPhrase);
        for (GameAction action : actions) {
            for (int i = 0; i < splitCommand.size(); i++) {
                if (action.getSubjectList().contains(splitCommand.get(i))) {
                    subjectNumber++;
                }else if (action.getConsumedList().contains(splitCommand.get(i))){
                    consumedNumber++;
                }
            }
        }
        return (subjectNumber - consumedNumber) <= 1;
    }

    public String openActionCommand(PlayerEntity player, ArrayList<String> splitCommand) throws GameException{
        String furniture = findFurnitureInSet(splitCommand);
        String artefact = findConsumedInSet(splitCommand);
        if (!splitCommand.contains(artefact) && !splitCommand.contains(furniture)){
            throw new GameException("no entity in open");
        }
        //check the key is in the inv or in the location
        //todo
        HashSet<GameAction> actions = actionsMap.get("open");
        if (actions != null && !actions.isEmpty()) {
            Iterator<GameAction> iterator = actions.iterator();
            GameAction firstAction = iterator.next();
            String consumedKey = firstAction.getConsumedList().get(0).toString();
            if (playerInvContains(player.getPlayerInv(), consumedKey) || ifEntityInLocation(player,consumedKey, "artefacts")){
                boolean status = false;
                for (int i = 0; i < firstAction.getSubjectList().size(); i++) {
                    if (checkIfSameLocation(firstAction.getSubjectList().get(i).toString(), player)) status = true;
                }
                if (!status) throw new GameException("the door is not here");
                //add a new path in the map
                addPathInMap(firstAction.getProducedList(),player);
                //assumed into storeroom and delete it in the inv
                addConsumedToStoreRoom(firstAction.getConsumedList(),player);
//                 use the firstAction object as needed
                return firstAction.getNarration();
            }else throw new  GameException("You don't have the key") ;
        }else throw new GameException("no such action");
    }

    public boolean playerInvContains(HashSet<ArtefactEntity> playerInv, String name) {
        for (ArtefactEntity artefact : playerInv) {
            if (artefact.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean ifEntityInLocation(PlayerEntity player, String name, String type){
        HashMap<String, HashSet<GameEntity>> tmp;
        if (entityMap.containsKey(player.getCurrentLocation())) {
            tmp = entityMap.get(player.getCurrentLocation());
            HashSet<GameEntity> entitySet = tmp.get(type);
            if (entitySet != null) {
                for (GameEntity gameEntity : entitySet){
                    if (gameEntity.getName().equals(name)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String findFurnitureInSet(ArrayList<String> splitCommand){
        for (int i = 0; i < splitCommand.size(); i++) {
            if (furnitureStringMap.contains(splitCommand.get(i))){
                return splitCommand.get(i);
            }
        }
        return "";
    }

    public String findConsumedInSet(ArrayList<String> splitCommand){
        for (int i = 0; i < splitCommand.size(); i++) {
            if (artefactStringMap.contains(splitCommand.get(i))){
                return splitCommand.get(i);
            }
        }
        return "";
    }

    public String findCharacterInSet(ArrayList<String> splitCommand){
        for (int i = 0; i < splitCommand.size(); i++) {
            if (characterStringMap.contains(splitCommand.get(i))){
                return splitCommand.get(i);
            }
        }
        return "";
    }

    public boolean checkIfSameLocation(String furniture, PlayerEntity player){
        String entityName = "";
        for (GameEntity entity : entityMap.keySet()) {
            HashMap<String, HashSet<GameEntity>> propertiesMap = entityMap.get(entity);
            if (propertiesMap.containsKey("furniture")) {
                HashSet<GameEntity> categorySet = propertiesMap.get("furniture");
                for (GameEntity categoryEntity : categorySet) {
                    if (categoryEntity.getName().equals(furniture)) {
                        entityName = entity.getName();
                        break;
                    }
                }
            }
        }
        return player.getCurrentLocation().getName().equals(entityName);
    }

    public void addPathInMap(ArrayList<String> path, PlayerEntity player) throws GameException{
        if (path.size() != 1){
            throw new GameException("incorrect path");
        }else {
            String currentLocation = player.getCurrentLocation().getName();
            HashSet<String> tmp = pathMap.get(currentLocation);
            tmp.add(path.get(0));
            pathMap.put(currentLocation, tmp);
        }
    }

    public void addConsumedToStoreRoom(ArrayList<String> consumed, PlayerEntity player) throws GameException{
        if (consumed.size() == 0){
            throw new GameException("No consumed artefacts");
        } else {
            HashSet<ArtefactEntity> tmp = player.getPlayerInv();
            Iterator<ArtefactEntity> iterator = tmp.iterator();
            while (iterator.hasNext()) {
                ArtefactEntity artefactEntity = iterator.next();
                if (consumed.contains(artefactEntity.getName())) {
                    //drop consumed
                    iterator.remove();
                    consumedToStoreRoom(artefactEntity);
                }
            }
        }
    }

    public void consumedToStoreRoom(ArtefactEntity artefactEntity){
        for (Map.Entry<GameEntity, HashMap<String, HashSet<GameEntity>>> entry : entityMap.entrySet()) {
            GameEntity key = entry.getKey();
            if (key.getName().equals("storeRoom")) {
                HashMap<String, HashSet<GameEntity>> value = entry.getValue();
                value.get("artefacts").add(artefactEntity);
            }
        }
    }

    public String chopActionCommand(PlayerEntity player, ArrayList<String> splitCommand) throws GameException {
        String tree = findFurnitureInSet(splitCommand);
        String axe = findConsumedInSet(splitCommand);
        if (!splitCommand.contains(tree) && !splitCommand.contains(axe)){
            throw new GameException("no entity in open");
        }
        //todo
        HashSet<GameAction> actions = actionsMap.get("chop");
        if (actions != null && !actions.isEmpty()) {
            Iterator<GameAction> iterator = actions.iterator();
            GameAction firstAction = iterator.next();
            String consumedAxe = firstAction.getSubjectList().get(1).toString();
            String consumedTree = firstAction.getSubjectList().get(0).toString();
            //check the axe is in the inv or in the location
            if (playerInvContains(player.getPlayerInv(), consumedAxe) || ifEntityInLocation(player,consumedAxe,"artefacts")){
                boolean status = false;
                //check if the tree is in the current location
                for (int i = 0; i < firstAction.getSubjectList().size(); i++) {
                    if (checkIfSameLocation(firstAction.getSubjectList().get(i).toString(), player)){
                        status = true;
                    }
                }
                if (!status){
                    throw new GameException("the tree is not here");
                } else {
                    FurnitureEntity furnitureEntity = getEntityFromMap(player,consumedTree, "furniture");
                    removeFromLocal(player, furnitureEntity, "furniture");
                    //consumed into the current location
                    //and delete the artefact from the store room
                    addConsumedToLocal(firstAction.getProducedList(),player, "artefacts");
                    // use the firstAction object as needed
                    return firstAction.getNarration();
                }
            }else throw new  GameException("You don't have the axe") ;
        }else throw new GameException("no such action");
    }

    public void addConsumedToLocal(ArrayList<String> consumed, PlayerEntity player,String type) throws GameException{
        if (consumed.size() == 0){
            throw new GameException("No consumed artefacts");
        }else {
            for (int i = 0; i < consumed.size(); i++) {
                HashSet<ArtefactEntity> tmp = player.getPlayerInv();
                HashMap<String, HashSet<GameEntity>> storeMap = new HashMap<>();
                for (GameEntity gameEntity : entityMap.keySet()) {
                    if (gameEntity.getName().equals("storeroom")) {
                        storeMap = entityMap.get(gameEntity);
                    }
                }
                HashSet<GameEntity> storeSet = storeMap.get(type);
                String name = "";
                String description = "";
                Iterator<GameEntity> iterator = storeSet.iterator();
                while (iterator.hasNext()) {
                    GameEntity gameEntity = iterator.next();
                    if (gameEntity.getName().equals(consumed.get(i))) {
                        name = gameEntity.getName();
                        description = gameEntity.getDescription();
                        iterator.remove();
                    }
                }
                // get log artefactEntity
                ArtefactEntity artefactEntity = new ArtefactEntity(name, description);
                //add consumed to local
                addConsumed(player,artefactEntity);
            }
        }
    }

    public void removedFromStore(ArtefactEntity artefactEntity){
        HashMap<String, HashSet<GameEntity>> tmp = new HashMap<>();
        for (GameEntity key : entityMap.keySet()) {
            if (key.getName().equals("storeroom")) {
                tmp = entityMap.get(key);
            }
        }
        for (HashSet<GameEntity> set: tmp.values()){
            for (GameEntity gameEntity : set){
                if (gameEntity.getName().equals(artefactEntity.getName())){
                    set.remove(gameEntity);
                }
            }
        }
        for (GameEntity key: entityMap.keySet()){
            if (key.getName().equals("storeroom")) {
                entityMap.put(key, tmp);
            }
        }
    }

    public void addConsumed(PlayerEntity player, ArtefactEntity artefactEntity){
        HashMap<String, HashSet<GameEntity>> tmpMap = entityMap.get(player.getCurrentLocation());
        HashSet<GameEntity> entitySet = tmpMap.get("artefacts");
        if (entitySet == null) {
            entitySet = new HashSet<GameEntity>();
        }
        entitySet.add(artefactEntity);
        tmpMap.put("artefacts", entitySet);
        entityMap.put(player.getCurrentLocation(),tmpMap);
        //delete the artefact from storeroom
        removedFromStore(artefactEntity);
    }

    public String drinkActionCommand(PlayerEntity player, ArrayList<String> splitCommand) throws GameException{
        String drink = findConsumedInSet(splitCommand);
        if (!splitCommand.contains(drink)){
            throw new GameException("no entity in drink");
        }
        HashSet<GameAction> actions = actionsMap.get("drink");
        if (actions != null && !actions.isEmpty()) {
            Iterator<GameAction> iterator = actions.iterator();
            GameAction firstAction = iterator.next();
            String consumedPotion = firstAction.getSubjectList().get(0).toString();
            if (playerInvContains(player.getPlayerInv(), consumedPotion)){
                addConsumedToStoreRoom(firstAction.getConsumedList(),player);
                int health = player.getHealth() + 1;
                if (health == 1){
                    return "you can't get potion anymore, you died!";
                }
                if (health >= 3) health = 3;
                player.setHealth(health);
                return firstAction.getNarration() + " and your health is: " + player.getHealth();
            }else throw new  GameException("You don't have the potion") ;
        }else throw new GameException("no such action");
    }

    public String fightActionCommand(PlayerEntity player, ArrayList<String> splitCommand) throws GameException {
        String elf = findCharacterInSet(splitCommand);
        if (!splitCommand.contains(elf)){
            throw new GameException("no elf in fight");
        }
        HashSet<GameAction> actions = actionsMap.get("fight");
        if (actions != null && !actions.isEmpty()) {
            Iterator<GameAction> iterator = actions.iterator();
            GameAction firstAction = iterator.next();
            String characterElf = firstAction.getSubjectList().get(0).toString();
            if (ifEntityInLocation(player,characterElf, "characters")){
                int health = player.getHealth() - 1;
                player.setHealth(health);
                if (health <= 0) {
                    player.setHealth(0);
                    loseAllTheEntity(player);
                    player.setCurrentLocation(initLocation);
                    changedLocation = initLocation;
                    player.setHealth(3);
                    return" you died and lost all of your items, you must return to the start of the game";
                }else {
                    return firstAction.getNarration() + " and your health is: " + player.getHealth();
                }
            }else throw new  GameException("Elf is not here") ;
        }else throw new GameException("no such action");
    }

    public void loseAllTheEntity(PlayerEntity player){
        HashSet<ArtefactEntity> playerInvCopy = new HashSet<ArtefactEntity>(player.getPlayerInv());
        for (ArtefactEntity artefactEntity: playerInvCopy){
            HashMap<String, HashSet<GameEntity>> tmpMap = entityMap.get(player.getCurrentLocation());
            HashSet<GameEntity> entitySet = tmpMap.get("artefacts");
            if (entitySet == null) {
                entitySet = new HashSet<GameEntity>();
            }
            entitySet.add(artefactEntity);
            tmpMap.put("artefacts", entitySet);
            entityMap.put(player.getCurrentLocation(),tmpMap);
            player.getPlayerInv().remove(artefactEntity);
        }
    }


    public String payActionCommand(PlayerEntity player, ArrayList<String> splitCommand) throws GameException{
        String elf = findCharacterInSet(splitCommand);
        String coin = findConsumedInSet(splitCommand);
        if (!splitCommand.contains(elf) && !splitCommand.contains(coin)){
            throw new GameException("no elf/ coin in pay");
        }
        HashSet<GameAction> actions = actionsMap.get("pay");
        if (actions != null && !actions.isEmpty()) {
            Iterator<GameAction> iterator = actions.iterator();
            GameAction firstAction = iterator.next();
            String consumedCoin = firstAction.getConsumedList().get(0).toString();
            String characterElf = firstAction.getSubjectList().get(0).toString();
            if (playerInvContains(player.getPlayerInv(), consumedCoin) || ifEntityInLocation(player,consumedCoin, "artefacts")){
                if (ifEntityInLocation(player,characterElf, "characters")){
                    //produce a new entity in this location
                    addConsumedToLocal(firstAction.getProducedList(),player, "artefacts");
                    // assumed into storeroom and delete it in the inv
                    addConsumedToStoreRoom(firstAction.getConsumedList(),player);
                    //use the firstAction object as needed
                    return firstAction.getNarration();
                }else throw new  GameException("Elf is not here") ;
            }else throw new  GameException("You don't have the coin") ;
        }else throw new GameException("no such action");
    }

    public String bridgeActionCommand(PlayerEntity player, ArrayList<String> splitCommand) throws GameException{
        String river = findFurnitureInSet(splitCommand);
        String log = findConsumedInSet(splitCommand);
        if (!splitCommand.contains(log) && !splitCommand.contains(river)){
            throw new GameException("no entity in open");
        }
        HashSet<GameAction> actions = actionsMap.get("bridge");
        if (actions != null && !actions.isEmpty()) {
            Iterator<GameAction> iterator = actions.iterator();
            GameAction firstAction = iterator.next();
            String consumedKey = firstAction.getConsumedList().get(0).toString();
            if (playerInvContains(player.getPlayerInv(), consumedKey) || ifEntityInLocation(player,consumedKey, "artefacts")){
                boolean status = false;
                for (int i = 0; i < firstAction.getSubjectList().size(); i++) {
                    if (checkIfSameLocation(firstAction.getSubjectList().get(i).toString(), player)) status = true;
                }
                if (!status) throw new GameException("the river is not here");
                //add a new path in the map
                addPathInMap(firstAction.getProducedList(),player);
                //assumed into storeroom and delete it in the inv
                addConsumedToStoreRoom(firstAction.getConsumedList(),player);
                //use the firstAction object as needed
                return firstAction.getNarration();
            }else throw new  GameException("You don't have the log") ;
        }else throw new GameException("no such action");
    }

    public String digActionCommand(PlayerEntity player, ArrayList<String> splitCommand) throws GameException{
        String ground = findFurnitureInSet(splitCommand);
        String shovel = findConsumedInSet(splitCommand);
        if (!splitCommand.contains(ground) && !splitCommand.contains(shovel)){
            throw new GameException("no entity in open");
        }
        HashSet<GameAction> actions = actionsMap.get("dig");
        if (actions != null && !actions.isEmpty()) {
            Iterator<GameAction> iterator = actions.iterator();
            GameAction firstAction = iterator.next();
            String consumedGround = firstAction.getSubjectList().get(0).toString();
            String consumedShovel = firstAction.getSubjectList().get(1).toString();
            //check the axe is in the inv or in the location
            if (playerInvContains(player.getPlayerInv(), consumedShovel) || ifEntityInLocation(player,consumedShovel,"artefacts")){
                boolean status = false;
                //check if the tree is in the current location
                for (int i = 0; i < firstAction.getSubjectList().size(); i++) {
                    if (checkIfSameLocation(firstAction.getSubjectList().get(i).toString(), player)){
                        status = true;
                    }
                }
                if (!status) throw new GameException("the ground is not here");
                FurnitureEntity furnitureEntity = getEntityFromMap(player,consumedGround, "furniture");
                removeFromLocal(player, furnitureEntity, "furniture");
                //consumed into the current location
                //and delete the artefact from the store room
                ArrayList<String> artefactsList = new ArrayList<>(Arrays.asList(firstAction.getProducedList().get(1)));
                ArrayList<String> furnitureList = new ArrayList<>(Arrays.asList(firstAction.getProducedList().get(0)));
                addConsumedToLocal(artefactsList,player, "artefacts");
                addConsumedToLocal(furnitureList,player, "furniture");
                // use the firstAction object as needed
                return firstAction.getNarration();
            }else throw new  GameException("You don't have the shovel") ;
        }else throw new GameException("no such action");
    }

    public String blowActionCommand(PlayerEntity player, ArrayList<String> splitCommand) throws GameException{
        String horn = findConsumedInSet(splitCommand);
        if (!splitCommand.contains(horn)){
            throw new GameException("no horn in blow");
        }
        HashSet<GameAction> actions = actionsMap.get("blow");
        if (actions != null && !actions.isEmpty()) {
            Iterator<GameAction> iterator = actions.iterator();
            GameAction firstAction = iterator.next();
            String characterElf = firstAction.getSubjectList().get(0).toString();
            if (ifEntityInLocation(player,characterElf, "artefacts")){
                addConsumedToLocal(firstAction.getProducedList(),player, "characters");
                return firstAction.getNarration();
            }else throw new  GameException("horn is not here") ;
        }else throw new GameException("no such action");
    }

}
