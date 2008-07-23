/**
 * Copyright 2008 Michael Ogawa
 *
 * This file is part of code_swarm.
 *
 * code_swarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * code_swarm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with code_swarm.  If not, see <http://www.gnu.org/licenses/>.
 */

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.xml.XMLElement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
//import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Properties;
import javax.vecmath.Vector2f;

/**
 * 
 *
 */
public class code_swarm extends PApplet {
  /** @remark needed for any serializable class */ 
  public static final long serialVersionUID = 0;
  
  // User-defined variables
  CodeSwarmConfig config;
  int FRAME_RATE = 24;
  long UPDATE_DELTA = -1;
  String SPRITE_FILE = "particle.png";
  String SCREENSHOT_FILE;
  int background;
 
  // Data storage
  PriorityBlockingQueue<FileEvent> eventsQueue; // USE PROCESSING 0142 or higher
  protected static CopyOnWriteArrayList<FileNode> nodes;
  protected static CopyOnWriteArrayList<Edge> edges;
  protected static CopyOnWriteArrayList<PersonNode> people;
  LinkedList<ColorBins> history;

  // Temporary variables
  FileEvent currentEvent;
  Date nextDate;
  Date prevDate;
  FileNode prevNode;
  int maxTouches;

  // Graphics objects
  PFont font;
  PFont boldFont;
  PImage sprite;

  // Graphics state variables
  boolean looping = true;
  boolean showHistogram = true;
  boolean showDate = true;
  boolean showLegend = false;
  boolean showPopular = false;
  boolean showEdges = false;
  boolean showHelp = false;
  boolean takeSnapshots = false;
  boolean showDebug = false;
  boolean drawNamesSharp = false;
  boolean drawNamesHalos = false;
  boolean drawFilesSharp = false;
  boolean drawFilesFuzzy = false;
  boolean drawFilesJelly = false;

  // Color mapper
  ColorAssigner colorAssigner;
  int currentColor;

  // Edge Length
  private int EDGE_LEN = 25;
  // Drawable object life decrement
  private int EDGE_LIFE_INIT = 255;
  private int FILE_LIFE_INIT = 255;
  private int PERSON_LIFE_INIT = 255;
  private final int EDGE_LIFE_DECREMENT = -2;
  private final int FILE_LIFE_DECREMENT = -2;
  private final int PERSON_LIFE_DECREMENT = -1;
  // Physical engine configuration
  String          physicalEngineConfigDir;
  String          physicalEngineSelection;
  LinkedList<peConfig> mPhysicalEngineChoices = new LinkedList<peConfig>();
  PhysicalEngine  mPhysicalEngine = null;
  

  // Default Physical Engine (class) name
  static final String PHYSICAL_ENGINE_LEGACY  = "PhysicalEngineLegacy";
  
  // Formats the date string nicely
  DateFormat formatter = DateFormat.getDateInstance();

  private static CodeSwarmConfig cfg;
  private long lastDrawDuration = 0;
  private boolean loading = true;
  private String loadingMessage = "Reading input file";
  protected static int width=0;
  protected static int height=0;

  /**
   * Initialization
   */
  public void setup() {
    width=cfg.getIntProperty(CodeSwarmConfig.WIDTH_KEY,640);
    if (width <= 0) {
      width = 640;
    }
    
    height=cfg.getIntProperty(CodeSwarmConfig.HEIGHT_KEY,480);
    if (height <= 0) {
      height = 480;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.USE_OPEN_GL, false)) {
      size(width, height, OPENGL);
    } else {
      size(width, height);
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_LEGEND, false)) {
      showLegend = true;
    } else {
      showLegend = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_HISTORY, false)) {
      showHistogram = true;
    } else {
      showHistogram = false;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_DATE, false)) {
      showDate = true;
    } else {
      showDate = false;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_EDGES, false)) {
      showEdges = true;
    } else {
      showEdges = false;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_DEBUG, false)) {
      showDebug = true;
    } else {
      showDebug = false;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.TAKE_SNAPSHOTS_KEY,false)) {
      takeSnapshots = true;
    } else {
      takeSnapshots = false;
    }
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_NAMES_SHARP, true)) {
      drawNamesSharp = true;
    } else {
      drawNamesSharp = false;
    }   
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_NAMES_HALOS, false)) {
      drawNamesHalos = true;
    } else {
      drawNamesHalos = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_FILES_SHARP, false)) {
      drawFilesSharp = true;
    } else {
      drawFilesSharp = false;
    }   
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_FILES_FUZZY, true)) {
      drawFilesFuzzy = true;
    } else {
      drawFilesFuzzy = false;
    }   
    
    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_FILES_JELLY, false)) {
      drawFilesJelly = true;
    } else {
      drawFilesJelly = false;
    }   
    
    background = cfg.getBackground().getRGB();
    
    // Ensure we have sane values.
    EDGE_LIFE_INIT = cfg.getIntProperty(CodeSwarmConfig.EDGE_LIFE_KEY,255);
    if (EDGE_LIFE_INIT <= 0) {
      EDGE_LIFE_INIT = 255;
    }
    
    FILE_LIFE_INIT = cfg.getIntProperty(CodeSwarmConfig.FILE_LIFE_KEY,255);
    if (FILE_LIFE_INIT <= 0) {
      FILE_LIFE_INIT = 255;
    }
    
    PERSON_LIFE_INIT = cfg.getIntProperty(CodeSwarmConfig.PERSON_LIFE_KEY,255);
    if (PERSON_LIFE_INIT <= 0) {
      PERSON_LIFE_INIT = 255;
    }
    
    UPDATE_DELTA = cfg.getIntProperty("testsets"/*CodeSwarmConfig.MSEC_PER_FRAME_KEY*/, -1);
    if (UPDATE_DELTA == -1) {
      int framesperday = cfg.getIntProperty(CodeSwarmConfig.FRAMES_PER_DAY_KEY, 4);
      if (framesperday > 0) {
        UPDATE_DELTA = (long) (86400000 / framesperday);
      }
    }
    if (UPDATE_DELTA <= 0) {
      // Default to 4 frames per day.
      UPDATE_DELTA = 21600000;
    }
    
    /**
     * This section loads config files and calls the setup method for all physics engines.
     */

    physicalEngineConfigDir = cfg.getStringProperty( CodeSwarmConfig.PHYSICAL_ENGINE_CONF_DIR, "physics_engine");
    File f = new File(physicalEngineConfigDir);
    String[] configFiles = null;
    if ( f.exists()  &&  f.isDirectory() ) {
      configFiles = f.list();
    }
    for (int i=0; configFiles != null  &&  i<configFiles.length; i++) {
      if (configFiles[i].endsWith(".config")) {
        Properties p = new Properties();
        String ConfigPath = physicalEngineConfigDir + System.getProperty("file.separator") + configFiles[i];
        try {
          p.load(new FileInputStream(ConfigPath));
        } catch (FileNotFoundException e) {
          e.printStackTrace();
          System.exit(1);
        } catch (IOException e) {
          e.printStackTrace();
          System.exit(1);
        }
        String ClassName = p.getProperty("name", "__DEFAULT__");
        if ( ! ClassName.equals("__DEFAULT__")) {
          Class<?> c = null;
          try {
            c = Class.forName(ClassName);
          } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
          }
          PhysicalEngine pe = null;
          try {
            Constructor peConstructor = c.getConstructor();
            pe = (PhysicalEngine) peConstructor.newInstance();
            pe.setup(p);
          } catch (InstantiationException e) {
            e.printStackTrace();
            System.exit(1);
          } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(1);
          } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.exit(1);
          } catch (InvocationTargetException e) {
            e.printStackTrace();
            System.exit(1);
          } catch (SecurityException e) {
            e.printStackTrace();
            System.exit(1);
          } catch (NoSuchMethodException e) {
            e.printStackTrace();
            System.exit(1);
          }
          peConfig pec = new peConfig(ClassName,pe);
          mPhysicalEngineChoices.add(pec);
        } else {
          System.out.println("Skipping config file '" + ConfigPath + "'.  Must specify class name via the 'name' parameter.");
          System.exit(1);
        }
      }
    }
    
    if (mPhysicalEngineChoices.size() == 0) {
      System.out.println("No physics engine config files found in '" + physicalEngineConfigDir + "'.");
      System.exit(1);
    }
    
    // Physical engine configuration and instantiation
    physicalEngineSelection = cfg.getStringProperty( CodeSwarmConfig.PHYSICAL_ENGINE_SELECTION, PHYSICAL_ENGINE_LEGACY );
    
    ListIterator<peConfig> peIterator = mPhysicalEngineChoices.listIterator();
    while (peIterator.hasNext()) {
      peConfig p = peIterator.next();
      if (physicalEngineSelection.equals(p.name)) {
        mPhysicalEngine = p.pe;
      }
    }
    
    if (mPhysicalEngine == null) {
      System.out.println("No physics engine matches your choice of '" + physicalEngineSelection + "'. Check '" + physicalEngineConfigDir + "' for options.");
      System.exit(1);
    }
    
    smooth();
    frameRate(FRAME_RATE);

    // init data structures
    eventsQueue = new PriorityBlockingQueue<FileEvent>();
    nodes       = new CopyOnWriteArrayList<FileNode>();
    edges       = new CopyOnWriteArrayList<Edge>();
    people      = new CopyOnWriteArrayList<PersonNode>();
    history     = new LinkedList<ColorBins>();

    // Init color map
    initColors();

    /**
     * TODO Fix this Thread code.  It is broken somehow.
     * TODO It causes valid setups to exit with no message.
     * TODO Only after several attempts will it eventually work.
     */
//    Thread t = new Thread(new Runnable() {
//      public void run() {
        loadRepEvents(cfg.getStringProperty(CodeSwarmConfig.INPUT_FILE_KEY)); // event formatted (this is the standard)
        prevDate = eventsQueue.peek().date;
//      }
//    });
//    t.setDaemon(true);
//    t.start();
    /** TODO: use adapter pattern to handle different data sources */

    SCREENSHOT_FILE = cfg.getStringProperty(CodeSwarmConfig.SNAPSHOT_LOCATION_KEY);
    EDGE_LEN = cfg.getIntProperty(CodeSwarmConfig.EDGE_LENGTH_KEY);
    if (EDGE_LEN <= 0) {
      EDGE_LEN = 25;
    }

    // Create fonts
    /**
     * TODO Put this in the config.
     */
    font = createFont("SansSerif", 10);
    boldFont = createFont("SansSerif.bold", 14);
    textFont(font);

    String SPRITE_FILE = cfg.getStringProperty(CodeSwarmConfig.SPRITE_FILE_KEY);
    // Create the file particle image
    sprite = loadImage(SPRITE_FILE);
    // Add translucency (using itself in this case)
    sprite.mask(sprite);
  }

  /**
   * Load a colormap
   */
  public void initColors() {
    colorAssigner = new ColorAssigner();
    int i = 1;
    String property;
    while ((property = cfg.getColorAssignProperty(i)) != null) {
      ColorTest ct = new ColorTest();
      ct.loadProperty(property);
      colorAssigner.addRule(ct);
      i++;
    }
    // Load the default.
    ColorTest ct = new ColorTest();
    ct.loadProperty(CodeSwarmConfig.DEFAULT_COLOR_ASSIGN);
    colorAssigner.addRule(ct);
  }

  /**
   * Main loop
   */
  public void draw() {
    long start = System.currentTimeMillis();
    background(background); // clear screen with background color

    if (loading) {
      drawLoading();
    }
    else {
      this.update(); // update state to next frame
      
      // Draw edges (for debugging only)
      if (showEdges) {
        for (Edge edge : edges) {
          edge.draw();
        }
      }

      // Surround names with aura
      // Then blur it
      if (drawNamesHalos) {
        drawPeopleNodesBlur();
      }
      
      // Then draw names again, but sharp
      if (drawNamesSharp) {
        drawPeopleNodesSharp();
      }

      // Draw file particles
      for (FileNode node : nodes) {
        node.draw();
      }

      textFont(font);

      // help, legend and debug information are exclusive
      if (showHelp) {
        // help override legend and debug information
        drawHelp();
      }
      else if (showDebug) {
        // debug override legend information
        drawDebugData();
      }
      else if (showLegend) {
        // legend only if nothing "more important"
        drawLegend();
      }
      
      if (showPopular) {
        drawPopular();
      }

      if (showHistogram) {
        drawHistory();
      }

      if (showDate) {
        drawDate();
      }

      if (takeSnapshots) {
        dumpFrame();
      }

      // Stop animation when we run out of data
      if (eventsQueue.isEmpty()) {
        // noLoop();
        exit();
      }
    }
    long end = System.currentTimeMillis();
    lastDrawDuration = end - start;
  }

  /**
   * Surround names with aura
   */
  public void drawPeopleNodesBlur() {
    colorMode(HSB);
    // First draw the name
    for (int i = 0; i < people.size(); i++) {
      PersonNode p = (PersonNode) people.get(i);
      fill(hue(p.flavor), 64, 255, p.life);
      p.draw();
    }

    // Then blur it
    filter(BLUR, 3);
  }

  /**
   * Draw person's name
   */
  public void drawPeopleNodesSharp() {
    colorMode(RGB);
    for (int i = 0; i < people.size(); i++) {
      PersonNode p = (PersonNode) people.get(i);
      fill(lerpColor(p.flavor, color(255), 0.5f), max(p.life - 50, 0));
      p.draw();
    }
  }

  /**
   * Draw date in lower-right corner
   */
  public void drawDate() {
    fill(255);
    String dateText = formatter.format(prevDate);
    textAlign(RIGHT, BASELINE);
    textSize(10);
    text(dateText, width - 1, height - textDescent());
  }

  /**
   *  Draw histogram in lower-left
   */
  public void drawHistory() {
    Iterator<ColorBins> itr = history.iterator();
    int counter = 0;

    while (itr.hasNext()) {
      ColorBins cb = itr.next();

      for (int i = 0; i < cb.num; i++) {
        int c = cb.colorList[i];
        stroke(c, 200);
        point(counter, height - i - 3);
      }
      counter++;
    }
  }
  
  /**
   * Show the Loading screen.
   */

  public void drawLoading() {
    noStroke();
    textFont(font, 20);
    textAlign(LEFT, TOP);
    fill(255, 200);
    text(loadingMessage, 0, 0);
  }

  /**
   *  Show color codings
   */
  public void drawLegend() {
    noStroke();
    textFont(font);
    textAlign(LEFT, TOP);
    fill(255, 200);
    text("Legend:", 0, 0);
    for (int i = 0; i < colorAssigner.tests.size(); i++) {
      ColorTest t = colorAssigner.tests.get(i);
      fill(t.c1, 200);
      text(t.label, 10, (i + 1) * 10);
    }
  }

  /**
   *  Show short help on avaible commands
   */
  public void drawHelp() {
    int line = 0;
    noStroke();
    textFont(font);
    textAlign(LEFT, TOP);
    fill(255, 200);
    text("Help on Keyboard commands:", 0, 10*line++);
    text("  space bar : pause", 0, 10*line++);
    text("  a : show name hAlos", 0, 10*line++);
    text("  b : show deBug", 0, 10*line++);
    text("  d : show Date", 0, 10*line++);
    text("  e : show Edges", 0, 10*line++);
    text("  f : draw files Fuzzy", 0, 10*line++);
    text("  h : show Histogram", 0, 10*line++);
    text("  j : draw files Jelly", 0, 10*line++);
    text("  l : show Legend", 0, 10*line++);
    text("  p : show Popular", 0, 10*line++);
    text("  q : Quit code_swarm", 0, 10*line++);
    text("  s : draw names Sharp", 0, 10*line++);
    text("  S : draw files Sharp", 0, 10*line++);
    text("  + : use next Physics Engine", 0, 10*line++);
    text("  - : use previous Physics Engine", 0, 10*line++);
    text("  ? : show help", 0, 10*line++);
  }
  /**
   *  Show debug information about all drawable objects
   */
  public void drawDebugData() {
    noStroke();
    textFont(font);
    textAlign(LEFT, TOP);
    fill(255, 200);
    text("Nodes: " + nodes.size(), 0, 0);
    text("People: " + people.size(), 0, 10);
    text("Queue: " + eventsQueue.size(), 0, 20);
    text("Last render time: " + lastDrawDuration, 0, 30);
  }

  /**
   * TODO This could be made to look a lot better.
   */
  public void drawPopular() {
    CopyOnWriteArrayList <FileNode> al=new CopyOnWriteArrayList<FileNode>();
    noStroke();
    textFont(font);
    textAlign(RIGHT, TOP);
    fill(255, 200);
    text("Popular Nodes (touches):", width-120, 0);
    for (int i = 0; i < nodes.size(); i++) {
      FileNode fn = (FileNode) nodes.get(i);
      if (fn.qualifies()) {
        // Insertion Sort
        if (al.size() > 0) {
          int j = 0;
          for (; j < al.size(); j++) {
            if (fn.compareTo(al.get(j)) <= 0) {
              continue;
            } else {
              break;
            }
          }
          al.add(j,fn);
        } else {
          al.add(fn);
        }
      }
    }
    
    int i = 1;
    ListIterator<FileNode> it = al.listIterator();
    while (it.hasNext()) {
      FileNode n = it.next();
      // Limit to the top 10.
      if (i <= 10) {
        text(n.name + "  (" + n.touches + ")", width-100, 10 * i++);
      } else if (i > 10) {
        break;
      }
    }
  }

  /**
   *  Take screenshot
   */
  public void dumpFrame() {
    if (frameCount < 100000)
      saveFrame(SCREENSHOT_FILE);
  }

  /**
   *  Update the particle positions
   */
  public void update() {
    // Create a new histogram line
    ColorBins cb = new ColorBins();
    history.add(cb);

    nextDate = new Date(prevDate.getTime() + UPDATE_DELTA);
    currentEvent = eventsQueue.peek();

    while (currentEvent != null && currentEvent.date.before(nextDate)) {
      currentEvent = eventsQueue.poll();
      if (currentEvent == null)
        return;

      FileNode n = findNode(currentEvent.path + currentEvent.filename);
      if (n == null) {
        n = new FileNode(currentEvent);
        nodes.add(n);
      } else {
        n.freshen();
      }

      // add to color bin
      cb.add(n.nodeHue);

      PersonNode p = findPerson(currentEvent.author);
      if (p == null) {
        p = new PersonNode(currentEvent.author);
        people.add(p);
      } else {
        p.freshen();
      }
      p.addColor(n.nodeHue);

      Edge ped = findEdge(n, p);
      if (ped == null) {
        ped = new Edge(n, p);
        edges.add(ped);
      } else
        ped.freshen();

      /*
       * if ( currentEvent.date.equals( prevDate ) ) { Edge e = findEdge( n, prevNode
       * ); if ( e == null ) { e = new Edge( n, prevNode ); edges.add( e ); } else {
       * e.freshen(); } }
       */
      
      // prevDate = currentEvent.date;
      prevNode = n;
      currentEvent = eventsQueue.peek();
    }

    prevDate = nextDate;

    // sort colorbins
    cb.sort();

    // restrict history to drawable area
    while (history.size() > 320)
      history.remove();

    for (Edge edge : edges) {
      //edge.relax();
      mPhysicalEngine.onRelaxEdge(edge);
    }

    for (FileNode node : nodes) {
      //node.relax();
      mPhysicalEngine.onRelaxNode(node);
    }

    for (PersonNode person : people) {
      //aPeople.relax();
      mPhysicalEngine.onRelaxPerson(person);
    }

    for (Edge edge : edges) {
      //edge.update();
      mPhysicalEngine.onUpdateEdge(edge);
    }

    for (FileNode node : nodes) {
      //node.update();
      mPhysicalEngine.onUpdateNode(node);
    }

    for (PersonNode person : people) {
      //aPeople.update();
      mPhysicalEngine.onUpdatePerson(person);
    }
  }

  /**
   * Searches the nodes array for a given name
   * @param name
   * @return FileNode with matching name or null if not found.
   */
  public FileNode findNode(String name) {
    for (FileNode node : nodes) {
      if (node.name.equals(name))
        return node;
    }
    return null;
  }

  /**
   * Searches the nodes array for a given name
   * @param n1 From
   * @param n2 To
   * @return Edge connecting n1 to n2 or null if not found
   */
  public Edge findEdge(Node n1, Node n2) {
    for (Edge edge : edges) {
      if (edge.nodeFrom == n1 && edge.nodeTo == n2)
        return edge;
    }
    return null;
  }

  /**
   * Searches the people array for a given name.
   * @param name
   * @return PersonNode for given name or null if not found.
   */
  public PersonNode findPerson(String name) {
    for (PersonNode p : people) {
      if (p.name.equals(name))
        return p;
    }
    return null;
  }

  /**
   *  Load the standard event-formatted file.
   *  @param filename
   */
  public void loadRepEvents(String filename) {
    XMLElement doc = new XMLElement(this, filename);
    for (int i = 0; i < doc.getChildCount(); i++) {
      XMLElement xml = doc.getChild(i);
      String eventFilename = xml.getStringAttribute("filename");
      String eventDatestr = xml.getStringAttribute("date");
      long eventDate = Long.parseLong(eventDatestr);
      String eventAuthor = xml.getStringAttribute("author");
      // int eventLinesAdded = xml.getIntAttribute( "linesadded" );
      // int eventLinesRemoved = xml.getIntAttribute( "linesremoved" );
      FileEvent evt = new FileEvent(eventDate, eventAuthor, "", eventFilename);
      eventsQueue.add(evt);
      if (eventsQueue.size() % 100 == 0)
        loadingMessage = "Creating events: " + eventsQueue.size();
    }
    loading = false;
    // reset the Frame Counter. Only needed if Threaded.
    // frameCount = 0;
  }

  /*
   * Output file events for debugging void printQueue() { while(
   * eventsQueue.size() > 0 ) { FileEvent fe = (FileEvent)eventsQueue.poll();
   * println( fe.date ); } }
   */

  /**
   * @note Keystroke callback function
   */
  public void keyPressed() {
    switch (key) {
      case ' ': {
        pauseButton();
        break;
      }
      case 'a': {
        drawNamesHalos = !drawNamesHalos;
        break;
      }
      case 'b': {
        showDebug = !showDebug;
        break;
      }
      case 'd': {
        showDate = !showDate;
        break;
      }
      case 'e' : {
        showEdges = !showEdges;
        break;
      }
      case 'f' : {
        drawFilesFuzzy = !drawFilesFuzzy;
        break;
      }
      case 'h': {
        showHistogram = !showHistogram;
        break;
      }
      case 'j' : {
        drawFilesJelly = !drawFilesJelly;
        break;
      }
      case 'l': {
        showLegend = !showLegend;
        break;
      }
      case 'p': {
        showPopular = !showPopular;
        break;
      }
      case 'q': {
        exit();
        break;
      }
      case 's': {
        drawNamesSharp = !drawNamesSharp;
        break;
      }
      case 'S': {
        drawFilesSharp = !drawFilesSharp;
        break;
      }
      case '+': {
        switchPhysicsEngine(true);
        break;
      }
      case '-': {
        switchPhysicsEngine(false);
        break;
      }
      case '?': {
        showHelp = !showHelp;
        break;
      }
    }
  }
  
  /**
   * Method to switch between Physics Engines
   * @param increment Indicates whether or not to go left or right on the list
   */
  public void switchPhysicsEngine(boolean increment) {
    if (mPhysicalEngineChoices.size() > 1) {
      ListIterator<peConfig> peIterator = mPhysicalEngineChoices.listIterator();
      while (peIterator.hasNext()) {
        peConfig p = peIterator.next();
        if (physicalEngineSelection.equals(p.name)) {
          if (increment) {
            if (peIterator.hasNext()) {
              p = peIterator.next();
            } else {
              p = mPhysicalEngineChoices.listIterator().next();
            }
            mPhysicalEngine = p.pe;
          } else {
            if (peIterator.hasPrevious()) {
              p = peIterator.previous();
            } else {
              while (peIterator.hasNext()) {
                p = peIterator.next();
              }
            }
            mPhysicalEngine = p.pe;
          }
        }
      }
    }
  }

  /**
   *  Toggle pause
   */
  public void pauseButton() {
    if (looping)
      noLoop();
    else
      loop();
    looping = !looping;
  }
  
  /**
   * Describe an event on a file
   */
  class peConfig {
    protected String name;
    protected PhysicalEngine pe;
    
    peConfig(String n, PhysicalEngine p) {
      name = n;
      pe = p;
    }
  }

  /**
   * Describe an event on a file
   */
  class FileEvent implements Comparable<Object> {
    Date date;
    String author;
    String filename;
    String path;
    int linesadded;
    int linesremoved;

    /**
     * short constructor with base data
     */
    FileEvent(long datenum, String author, String path, String filename) {
      this(datenum, author, path, filename, 0, 0);
    }

    /**
     * constructor with number of modified lines
     */
    FileEvent(long datenum, String author, String path, String filename, int linesadded, int linesremoved) {
      this.date = new Date(datenum);
      this.author = author;
      this.path = path;
      this.filename = filename;
      this.linesadded = linesadded;
      this.linesremoved = linesremoved;
    }

    /**
     * Comparing two events by date (Not Used)
     * @param o
     * @return -1 if <, 0 if =, 1 if >
     */
    public int compareTo(Object o) {
      return date.compareTo(((FileEvent) o).date);
    }
  }

  /**
   * Base class for all drawable objects
   * 
   *        Lists and implements features common to all drawable objects
   *        Edge and Node, FileNode and PersonNode
   */
  abstract class Drawable {
    public int life;

    final public int LIFE_INIT;
    final public int LIFE_DECREMENT;
    /**
     * 1) constructor(s)
     * 
     * Init jobs common to all objects
     */
    Drawable(int lifeInit, int lifeDecrement) {
      // save config vars
      LIFE_INIT      = lifeInit;
      LIFE_DECREMENT = lifeDecrement;
      // init life relative vars
      life           = LIFE_INIT;
    }

    /**
     *  4) shortening life.
     */
    public void decay() {
      if (life > 0) {
        life += LIFE_DECREMENT;
        if (life < 0) {
          life = 0;
        }
      }
    }
    
    /**
     * 5) drawing the new state => done in derived class.
     */
    public abstract void draw();

    /**
     * 6) reseting life as if new.
     */
    public abstract void freshen();
  }

  /**
   * An Edge link two nodes together : a File to a Person.
   */
  class Edge extends Drawable {
    protected Node  nodeFrom;
    protected Node  nodeTo;
    private float len;

    /**
     * 1) constructor.
     * @param from FileNode
     * @param to PersonNode
     */
    Edge(Node from, Node to) {
      super(EDGE_LIFE_INIT, EDGE_LIFE_DECREMENT);
      this.nodeFrom = from;
      this.nodeTo   = to;
      this.len      = EDGE_LEN;  // 25
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (life > 240) {
        stroke(255, life);
        strokeWeight(0.35f);
        line(nodeFrom.mPosition.x, nodeFrom.mPosition.y, nodeTo.mPosition.x, nodeTo.mPosition.y);
      }
    }
    
    public void freshen() {
      life = EDGE_LIFE_INIT;
    }
    
    public float getLen()
    {
      return len;
    }
  }

  /**
   * A node is an abstraction for a File or a Person.
   */
  public abstract class Node extends Drawable {
    protected String name;
    protected Vector2f mPosition;
    protected Vector2f mSpeed;
    /** TODO: add configuration for max speed */
    protected float maxSpeed = 7.0f;

    /**
     * mass of the node
     */
    public float mass;
    
    /**
     * 1) constructor.
     */
    Node(int lifeInit, int lifeDecrement) {
      super(lifeInit, lifeDecrement);
      /** TODO: implement new sort of (random or not) arrival, with configuration
                => to permit things like "injection points", circular arrival, and so on */
      mPosition = new Vector2f((float)Math.random()*width, (float)Math.random()*height);
      mSpeed = new Vector2f();
    }

    /**
     *  4) shortening life.
     */
    public void decay() {
      if (life > 0) {
        life += LIFE_DECREMENT;
        if (life < 0) {
          life = 0;
        }
      }
    }
  }

  /**
   * A node describing a file, which is repulsed by other files.
   */
  class FileNode extends Node implements Comparable<FileNode> {
    private int nodeHue;
    private int minBold;
    private int touches;

    /**
     * @return file node as a string
     */
    public String toString() {
      return "FileNode{" + "name='" + name + '\'' + ", nodeHue=" + nodeHue + ", touches=" + touches + '}';
    }

    /**
     * 1) constructor.
     */
    FileNode(FileEvent fe) {
      super(FILE_LIFE_INIT, FILE_LIFE_DECREMENT); // 255, -2
      name = fe.path + fe.filename;
      touches = 1;
      life = FILE_LIFE_INIT;
      colorMode(RGB);
      minBold = (int)(FILE_LIFE_INIT * 0.95f);
      nodeHue = colorAssigner.getColor(name);
      mass = 1.0f;
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (life > 0) {
        if (drawFilesSharp) {
          drawSharp();
        }
        if (drawFilesFuzzy) {
          drawFuzzy();
        }
        if (drawFilesJelly) {
          drawJelly();
        }
        
        /** TODO : this would become interesting on some special event, or for special materials
         * colorMode( RGB ); fill( 0, life ); textAlign( CENTER, CENTER ); text( name, x, y );
         * Example below:
         */
        if (showPopular) {
          textAlign( CENTER, CENTER );
          if (this.qualifies()) {
            text(touches, mPosition.x, mPosition.y - (8 + (int)Math.sqrt(touches)));
          }
        }
      }
    }
    
    /**
     * 6) reseting life as if new.
     */
    public void freshen() {
      life = FILE_LIFE_INIT;
      if (++touches > maxTouches) {
        maxTouches = touches;
      }
    }
    
    public boolean qualifies() {
      if (this.touches >= (maxTouches * 0.5f)) {
        return true;
      }
      return false;
    }
    
    public int compareTo(FileNode fn) {
      int retval = 0;
      if (this.touches < fn.touches) {
        retval = -1;
      } else if (this.touches > fn.touches) {
        retval = 1;
      }
      return retval;
    }

    public void drawSharp() {
      colorMode(RGB);
      fill(nodeHue, life);
      float w = 3;

      if (life >= minBold) {
        stroke(255, 128);
        w *= 2;
      } else {
        noStroke();
      }
      
      ellipseMode(CENTER);
      ellipse(mPosition.x, mPosition.y, w, w);
    }

    public void drawFuzzy() {
      tint(nodeHue, life);

      float w = 8 + (sqrt(touches) * 4);
      // not used float dubw = w * 2;
      float halfw = w / 2;
      if (life >= minBold) {
        colorMode(HSB);
        tint(hue(nodeHue), saturation(nodeHue) - 192, 255, life);
        // image( sprite, x - w, y - w, dubw, dubw );
      }
      // else
      image(sprite, mPosition.x - halfw, mPosition.y - halfw, w, w);
    }

    public void drawJelly() {
      noFill();
      if (life >= minBold)
        stroke(255);
      else
        stroke(nodeHue, life);
      float w = sqrt(touches);
      ellipseMode(CENTER);
      ellipse(mPosition.x, mPosition.y, w, w);
    }
  }

  /**
   * A node describing a person, which is repulsed by other persons.
   */
  class PersonNode extends Node {
    private int flavor = color(0);
    private int colorCount = 1;
    private int minBold;
    private int touches;

    /**
     * 1) constructor.
     */
    PersonNode(String n) {
      super(PERSON_LIFE_INIT, PERSON_LIFE_DECREMENT); // -1
      maxSpeed = 2.0f;
      name = n;
      /** TODO: add config */
      minBold = (int)(PERSON_LIFE_INIT * 0.95f);
      mass = 10.0f; // bigger mass to person then to node, to stabilize them
      // range (-1,1)
      mSpeed.set((float)(Math.random()*2-1),(float)(Math.random()*2-1));
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (life <= 0)
        return;

      textAlign(CENTER, CENTER);

      /** TODO: proportional font size, or light intensity,
                or some sort of thing to disable the flashing */
      if (life >= minBold)
        textFont(boldFont);
      else
        textFont(font);

      text(name, mPosition.x, mPosition.y);
    }
    
    public void freshen () {
      life = PERSON_LIFE_INIT;
      touches++;
    }
    
    public void addColor(int c) {
      colorMode(RGB);
      flavor = lerpColor(flavor, c, 1.0f / colorCount);
      colorCount++;
    }
  }

  /**
   * code_swarm Entry point.
   * @param args : should be the path to the config file
   */
  static public void main(String args[]) {
    try {
      if (args.length > 0) {
        System.out.println("code_swarm is free software: you can redistribute it and/or modify");
        System.out.println("it under the terms of the GNU General Public License as published by");
        System.out.println("the Free Software Foundation, either version 3 of the License, or");
        System.out.println("(at your option) any later version.");
        cfg = new CodeSwarmConfig(args[0]);
        PApplet.main(new String[] { "code_swarm" });
      } else {
        System.err.println("Specify a config file.");
      }
    } catch (IOException e) {
      System.err.println("Failed due to exception: " + e.getMessage());
    }
  }
}
