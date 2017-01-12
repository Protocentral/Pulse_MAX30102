/*
  Part of the G4P library for Processing 
  	http://www.lagers.org.uk/g4p/index.html
	http://sourceforge.net/projects/g4p/files/?source=navbar

  Copyright (c) 2008-13 Peter Lager

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
 */

package g4p_controls;


import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.filechooser.FileFilter;

import processing.core.PApplet;
import processing.core.PConstants;

/**
 * The core class for the global manipulation and execution of G4P. <br>
 * All the methods and constants are static so to call a method or
 * reference a constant prefix it's name with 'G4P.' for example <br>
 * <pre>
 * G4P.<i>method_name(parameters);</i>
 * G4P.<i>name_of_constant;</i>
 * </pre><br>
 * 
 * Prior to version 3.5 G4P used logical fonts to be cross-platform, 
 * unfortunately logical fonts do not use uniform metrics so it caused 
 * serious errors when formatting the text layout. In version 3.5 G4P 
 * will attempt to use a system fonts selected from this list <br>
 * "Arial", "Trebuchet MS", "Tahoma", "Helvetica", "Verdana" <br>
 * 
 * Arial is the preferred font (since most systems will have it installed)
 * but if not found it will attempt to use the next one in the list. If
 * the list is exhausted and no matching system font found it will use
 * a logical font. <br>
 * If this causes a problem with older sketches simple call <br>
 * <pre>
 * G4P.usePre35Fonts();
 * </pre>
 * before you create any GUI controls. <br>
 * 
 * 
 * @author Peter Lager
 *
 */
public class G4P implements GConstants, PConstants {

	static PApplet sketchWindow = null;
	// Relays events to main sketch window
	static GWindowImpl sketchWindowImpl = null;

	// List of all GWindows currently open
	static List<GWindow> allWindows = new LinkedList<GWindow>();
	// List of all GWindows marked for closure
	static List<GWindow> windowsForClosing = new LinkedList<GWindow>();

	static boolean announced = false;

	/**
	 * return the pretty version of the library.
	 */
	public static String getPrettyVersion() {
		return "4.0.4";
	}

	/**
	 * return the version of the library used by Processing
	 */
	public static String getVersion() {
		return "27";
	}

	static int globalColorScheme = GCScheme.BLUE_SCHEME;
	static int globalAlpha = 255;

	// Font used for all text controls
	static Font globalFont = FontManager.getPriorityFont(null, Font.PLAIN, 12);
	// Font used for slider numbers
	static Font numericLabelFont = FontManager.getPriorityFont(null, Font.BOLD, 11);;

	// Used to order controls
	static GAbstractControl.Z_Order zorder = new GAbstractControl.Z_Order();

	/* INTERNAL USE ONLY  Mouse over changer */
	static boolean cursorChangeEnabled = true;
	static int mouseOff = ARROW;

	static boolean showMessages = true;

	// Determines how position and size parameters are interpreted when
	// a control is created. Introduced G4P V3.0
	static GControlMode control_mode = GControlMode.CORNER;

	// Used to create a stack of styles with pushStyle and popStyle
	static LinkedList<G4Pstyle> styles = new LinkedList<G4Pstyle>();

	// Colour chooser
	static JColorChooser chooser = null;
	static Color lastColor = Color.white; // White

	// Mouse wheel direction
	static int wheelForSlider = FORWARD;
	static int wheelForScrollbar = FORWARD;
	
	/**
	 * Register a GWindow so we can keep track of all GWindows in the application.
	 * This will be needed for global transformations e.g. setGlobalAlpha(...)
	 * This is called from the controls constructor, and also when a GWindow is created.
	 * @param window
	 */
	static void registerWindow(GWindow window){
		if(!allWindows.contains(window)){
			allWindows.add(window);
		}
	}

	/**
	 * De-register a window , this is done when a window is closed
	 * @param window
	 */
	static void deregisterWindow(GWindow window){
		allWindows.remove(window);
	}

	/**
	 * Used internally to register a control with its window.
	 * It will replace the addControl method in the controls constructors
	 * @param control
	 */
	static void registerControl(GAbstractControl control){
		PApplet app = control.getPApplet();
		if(app == sketchWindow)
			sketchWindowImpl.addToWindow(control);
		else if(app instanceof GWindow)
			((GWindow)app).addToWindow(control);
	}

	/**
	 * Used to register the main sketch window with G4P. This is ignored if any
	 * G4P controls or windows have already been created because the act of
	 * creating a control will do this for you. <br>
	 * 
	 * Some controls are created without passing a reference to the sketch applet
	 * but still need to know it. An example is the GColorChooser control which
	 * cannot be used until this method is called or some other G4P control has
	 * been created.
	 * 
	 * Also some other libraries such as PeasyCam change the transformation matrix.
	 * In which case either a G4P control should be created or this method called
	 * before creating a PeasyCam object.
	 * 
	 * @param app
	 */
	public static void registerSketch(PApplet app){
		if(sketchWindow == null && app != null) {
			sketchWindow = app;
			sketchWindowImpl = new GWindowImpl(sketchWindow);
			announceG4P();
		}
	}
	
	/**
	 * Set the global colour scheme. This will change the local
	 * colour scheme for every control.
	 * @param cs colour scheme to use (0-15)
	 */
	static void invalidateBuffers(){
		for(GWindow window : allWindows)
			window.invalidateBuffers();
		if(sketchWindowImpl != null)
			sketchWindowImpl.invalidateBuffers();
	}
	
	/**
	 * Set the global colour scheme. This will change the local
	 * colour scheme for every control.
	 * @param cs colour scheme to use (0-15)
	 */
	public static void setGlobalColorScheme(int cs){
		cs = Math.abs(cs) % 16; // Force into valid range
		if(globalColorScheme != cs){
			globalColorScheme = cs;
			for(GWindow window : allWindows)
				window.setColorScheme(globalColorScheme);
			if(sketchWindowImpl != null)
				sketchWindowImpl.setColorScheme(globalColorScheme);
		}
	}

	/**
	 * Versions of G4P prior to 3.5 used logical fonts for the controls. So if you 
	 * have old sketches then the text may look different with this and later versions
	 * of G4P. <br>
	 * If this is causing a problem then call this method before creating any controls.
	 */
	public static void usePre35Fonts(){
		globalFont = new Font("Dialog", Font.PLAIN, 10);
		numericLabelFont = new Font("DialogInput", Font.BOLD, 12);
	}

	/**
	 * Set the colour scheme for all the controls drawn by the given 
	 * PApplet. This will override any previous colour scheme for 
	 * these controls.
	 * @param app
	 * @param cs
	 */
	public static void setWindowColorScheme(PApplet app, int cs){
		cs = Math.abs(cs) % 16; // Force into valid range
		if(app == sketchWindow){
			if(sketchWindowImpl != null)
				sketchWindowImpl.setColorScheme(cs);
		}
		else if(app instanceof GWindow)
			((GWindow)app).setColorScheme(cs);
	}

	/**
	 * Set the transparency of all controls. If the alpha level for a 
	 * control falls below G4P.ALPHA_BLOCK then it will no longer 
	 * respond to mouse and keyboard events.
	 * 
	 * @param alpha value in the range 0 (transparent) to 255 (opaque)
	 */
	public static void setGlobalAlpha(int alpha){
		alpha = Math.abs(alpha) % 256; // Force into valid range
		if(globalAlpha != alpha){
			globalAlpha = alpha;
			for(GWindow window : allWindows)
				window.setAlpha(globalAlpha);
			if(sketchWindowImpl != null)
				sketchWindowImpl.setAlpha(globalAlpha);
		}	
	}

	/**
	 * Set the transparency level for all controls drawn by the given
	 * PApplet. If the alpha level for a control falls below 
	 * G4P.ALPHA_BLOCK then it will no longer respond to mouse
	 * and keyboard events.
	 * 
	 * @param app
	 * @param alpha value in the range 0 (transparent) to 255 (opaque)
	 */
	public static void setWindowAlpha(PApplet app, int alpha){
		alpha = Math.abs(alpha) % 256; // Force into valid range
		if(app == sketchWindow){
			if(sketchWindowImpl != null)
				sketchWindowImpl.setAlpha(alpha);
		}
		else if(app instanceof GWindow)
			((GWindow)app).setAlpha(alpha);
	}

	/**
	 * Display the library version in the ProcessingIDE
	 */
	static void announceG4P(){
		if(!announced){
			System.out.println("===================================================");
			System.out.println("   G4P V4.0.4 created by Peter Lager");
			System.out.println("===================================================");
			announced = true;
		}
	}

	/**
	 * Remove a control from the window. This is used in preparation 
	 * for disposing of a control.
	 * @param control
	 * @return true if control was remove else false
	 */
	static boolean removeControl(GAbstractControl control){
		PApplet app = control.getPApplet();
		if(app == sketchWindow){
			if(sketchWindowImpl != null){
				sketchWindowImpl.removeFromWindow(control);
				return true;
			}
			return false;
		}
		if(app instanceof GWindow){
			((GWindow)app).removeFromWindow(control);
			return true;
		}
		return false;
	}

	/**
	 * Change the way position and size parameters are interpreted when a control is created. 
	 * or added to another control e.g. GPanel. <br>
	 * There are 3 modes. <br><pre>
	 * PApplet.CORNER	 (x, y, w, h) <br>
	 * PApplet.CORNERS	 (x0, y0, x1, y1) <br>
	 * PApplet.CENTER	 (cx, cy, w, h) </pre><br>
	 * 
	 * @param mode illegal values are ignored leaving the mode unchanged
	 */
	public static void setCtrlMode(GControlMode mode){
		if(mode != null)
			control_mode = mode;
	}

	/**
	 * Get the control creation mode @see ctrlMode(int mode)
	 * @return the current control mode
	 */
	public static GControlMode getCtrlMode(){
		return control_mode;
	}

	/**
	 * G4P has a range of support messages eg <br>if you create a GUI component 
	 * without an event handler or, <br>a slider where the visible size of the
	 * slider is less than the difference between min and max values. <br>
	 * 
	 * This method allows the user to enable (default) or disable this option. If
	 * disable then it should be called before any GUI components are created. <br>
	 * 
	 * If you are adding your own event handlers then I suggest that you disable 
	 * messages.
	 * 
	 * @param enable
	 */
	public static void messagesEnabled(boolean enable){
		showMessages = enable;
	}

	/**
	 * Enables or disables cursor over component change. <br>
	 * 
	 * Calls to this method are ignored if no G4P controls have been created.
	 * 
	 * @param enable true to enable cursor change over components.
	 */
	public static void setMouseOverEnabled(boolean enable){
		cursorChangeEnabled = enable;
	}

	/**
	 * Determines how the direction of the mouse wheel rotation is interpreted
	 * for sliders. This value applies to all sliders.<br/>
	 * The default value is FORWARD
	 * @param dir FORWARD or REVERSE, illegal values are ignored
	 */
	public static void mouseWheelForSlider(int dir){
		if(dir == FORWARD || dir == REVERSE)
			wheelForSlider = dir;			
	}
	
	/**
	 * Determines how the direction of the mouse wheel rotation is interpreted
	 * for sliders. This value applies to all sliders.<br/>
	 * The default value is FORWARD
	 * @param dir FORWARD or REVERSE, illegal values are ignored
	 */
	public static void mouseWheelForScrollbar(int dir){
		if(dir == FORWARD || dir == REVERSE)
			wheelForScrollbar = dir;			
	}
	
	/**
	 * @deprecated use setCursor(int)
	 */
	@Deprecated
	public static void setCursorOff(int cursorOff){
		mouseOff = cursorOff;
	}

	/**
	 * Set the cursor shape to be used when the mouse is not over a 
	 * G4P control for the entire application including secondary
	 * windows.
	 * @param cursorOff the cursor shape.
	 */
	public static void setCursor(int cursorOff){
		mouseOff = cursorOff;
		for(GWindow window : allWindows)
			window.cursor(cursorOff);
		if(sketchWindow != null)
			sketchWindow.cursor(cursorOff);
	}

	/**
	 * Get the cursor shape used when the mouse is not over a G4P 
	 * control
	 * set for the 
	 * 
	 */
	public static int getCursor(){
		return mouseOff;
	}

	/**
	 * @deprecated use getCursor()
	 */
	@Deprecated
	public static int getCursorOff(){
		return mouseOff;
	}

	/**
	 * Save the current style on a stack. <br>
	 * There should be a matching popStyle otherwise the program it will
	 * cause a memory leakage.
	 */
	static void pushStyle(){
		G4Pstyle s = new G4Pstyle();
		s.ctrlMode = control_mode;
		s.showMessages = showMessages;
		// Now save the style for later
		styles.addLast(s);
	}

	/**
	 * Remove and restore the current style from the stack. <br>
	 * There should be a matching pushStyle otherwise the program will crash.
	 */
	static void popStyle(){
		G4Pstyle s = styles.removeLast();
		control_mode = s.ctrlMode;
		showMessages = s.showMessages;
	}

	/**
	 * This class represents the current style used by G4P. 
	 * It can be extended to add other attributes but these should be 
	 * included in the pushStyle and popStyle. 
	 * @author Peter
	 *
	 */
	static class G4Pstyle {
		GControlMode ctrlMode;
		boolean showMessages;
	}

	/**
	 * Get a list of all open GWindow objects even if minimised or invisible. <br>
	 * If an ArrayList is provided then its contents are cleared before adding references
	 * to all open GWindow objects. If an ArrayList is not provided then a new 
	 * ArrayList will be created. <br>
	 * This method never returns null, if there are no open windows the list will 
	 * be of size zero.
	 * 
	 * @param list an optional ArrayList to use. In null will create a new ArrayList.
	 * @return an ArrayList of references to all open GWindow objects.
	 */
	public static ArrayList<GWindow> getOpenWindowsAsList(ArrayList<GWindow> list){
		if(list == null)
			list = new ArrayList<GWindow>();
		else
			list.clear();
		for(GWindow window : allWindows)
			list.add(window);
		return list;
	}

	/**
	 * Get an array of GWindow objects even if minimised or invisible. <br>
	 * This method never returns null, if there are no open windows the array
	 *  will be of length zero.
	 * @return an array of references to all open GWindow objects.
	 */
	public static GWindow[] getOpenWindowsAsArray(){
		ArrayList<GWindow> list = getOpenWindowsAsList(null);
		return list.toArray(new GWindow[list.size()]);
	}

	/**
	 * Use this to check whether a GWindow window is still open (as far as G4P is concerned).
	 * @param window the window we are interested in
	 * @return true if G4P still thinks it is open
	 */
	public static boolean isWindowOpen(GWindow window){
		return (window != null && allWindows.contains(window));
	}

	/**
	 * This will open a version of the Java Swing color chooser dialog. The dialog's
	 * UI is dependent on the OS and JVM implementation running. <br>
	 * 
	 * If you click on Cancel then it returns the last color previously selected.
	 * 
	 * @return the ARGB colour as a 32 bit integer (as used in Processing). 
	 */
	public static int selectColor(){
		Frame frame = getFrame(sketchWindow);
		if(chooser == null){
			chooser = new JColorChooser();
			AbstractColorChooserPanel[] oldPanels = chooser.getChooserPanels();
			// Do not assume what panels are present
			LinkedList<AbstractColorChooserPanel> panels = new LinkedList<AbstractColorChooserPanel>();	
			for(AbstractColorChooserPanel p : oldPanels){
				String displayName = p.getDisplayName().toLowerCase();
				if(displayName.equals("swatches"))
					panels.addLast(p);
				else if(displayName.equals("rgb"))
					panels.addFirst(p);
				else if(displayName.startsWith("hs"))
					panels.addFirst(p);
			}
			AbstractColorChooserPanel[] newPanels;
			newPanels = panels.toArray(new AbstractColorChooserPanel[panels.size()]);
			chooser.setChooserPanels(newPanels);
			ColorPreviewPanel pp = new ColorPreviewPanel(lastColor);
			chooser.getSelectionModel().addChangeListener(pp);
			chooser.setPreviewPanel(pp);
		}
		// Set the preview color
		((ColorPreviewPanel)chooser.getPreviewPanel()).setPrevColor(lastColor);
		// Use the last color selected to start it off
		chooser.setColor(lastColor);
		JDialog dialog = JColorChooser.createDialog(frame,
				"Color picker", 
				true, 
				chooser, 
				new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lastColor = chooser.getColor();
			}
		}, 
		null);
		dialog.setVisible(true);
		return lastColor.getRGB();
	}

	/**
	 * Select a folder from the local file system.
	 * 
	 * @param prompt the frame text for the chooser
	 * @return the absolute path name for the selected folder, or null if action 
	 * cancelled.
	 */
	public static String selectFolder(String prompt){
		String selectedFolder = null;
		Frame frame = getFrame(sketchWindow);
		//Frame frame = (sketchWindow == null) ? null : sketchWindow.frame;
		if (PApplet.platform == MACOSX && PApplet.useNativeSelect != false) {
			FileDialog fileDialog =
					new FileDialog(frame, prompt, FileDialog.LOAD);
			System.setProperty("apple.awt.fileDialogForDirectories", "true");
			fileDialog.setVisible(true);
			System.setProperty("apple.awt.fileDialogForDirectories", "false");
			String filename = fileDialog.getFile();
			if (filename != null) {
				try {
					selectedFolder = (new File(fileDialog.getDirectory(), fileDialog.getFile())).getCanonicalPath();
				} catch (IOException e) {
					selectedFolder = null;
				}
			}
		} else {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle(prompt);
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = fileChooser.showOpenDialog(frame);
			if (result == JFileChooser.APPROVE_OPTION) {
				try {
					selectedFolder = fileChooser.getSelectedFile().getCanonicalPath();
				} catch (IOException e) {
					selectedFolder = null;
				}
			}
		}
		return selectedFolder;
	}

	/**
	 * Select a file for input from the local file system. <br>
	 * 
	 * 
	 * @param prompt the frame text for the chooser
	 * @return the absolute path name for the selected folder, or null if action 
	 * cancelled.
	 */
	public static String selectInput(String prompt){
		return selectInput(prompt, null, null);
	}

	/**
	 * Select a file for input from the local file system. <br>
	 * 
	 * This version allows the dialog window to filter the output based on file extensions.
	 * This is not available on all platforms, if not then it is ignored. <br>
	 * 
	 * It is definitely available on Linux systems because it uses the standard Swing
	 * JFileFinder component.
	 * 
	 * @param prompt the frame text for the chooser
	 * @param types a comma separated list of file extensions e.g. "png,gif,jpg,jpeg"
	 * @param typeDesc simple textual description of the file types e.g. "Image files"
	 * @return the absolute path name for the selected folder, or null if action 
	 * cancelled.
	 */
	public static String selectInput(String prompt, String types, String typeDesc){
		return selectImpl(prompt, FileDialog.LOAD, types, typeDesc);
	}

	/**
	 * Select a file for output from the local file system. <br>
	 * 
	 * @param prompt the frame text for the chooser
	 * @return the absolute path name for the selected folder, or null if action is cancelled.
	 */
	public static String selectOutput(String prompt){
		return selectOutput(prompt, null, null);
	}

	/**
	 * Select a file for output from the local file system. <br>
	 * 
	 * This version allows the dialog window to filter the output based on file extensions.
	 * This is not available on all platforms, if not then it is ignored. <br>
	 * 
	 * It is definitely available on Linux systems because it uses the standard swing
	 * JFileFinder component.
	 * 
	 * @param prompt the frame text for the chooser
	 * @param types a comma separated list of file extensions e.g. "png,jpf,tiff"
	 * @param typeDesc simple textual description of the file types e.g. "Image files"
	 * @return the absolute path name for the selected folder, or null if action 
	 * cancelled.
	 */
	public static String selectOutput(String prompt, String types, String typeDesc){
		return selectImpl(prompt, FileDialog.SAVE, types, typeDesc);
	}

	/**
	 * The implementation of the select input and output methods.
	 * @param prompt
	 * @param mode
	 * @param types
	 * @param typeDesc
	 * @return the absolute path name for the selected folder, or null if action 
	 * cancelled.
	 */
	private static String selectImpl(String prompt, int mode, String types, String typeDesc) {
		// If no initial selection made then use last selection	
		// Assume that a file will not be selected
		String selectedFile = null;
		// Get the owner
		Frame frame = getFrame(sketchWindow);
		// Create a file filter
		if (PApplet.useNativeSelect) {
			FileDialog dialog = new FileDialog(frame, prompt, mode);
			FilenameFilter filter = null;
			if(types != null && types.length() > 0){
				filter = new FilenameChooserFilter(types);
				dialog.setFilenameFilter(filter);
			}
			dialog.setVisible(true);
			String directory = dialog.getDirectory();
			if(directory != null){
				selectedFile = dialog.getFile();
				if(selectedFile != null){
					try {
						selectedFile = (new File(directory, selectedFile)).getCanonicalPath();
					} catch (IOException e) {
						selectedFile = null;
					}
				}
			}
		} else {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle(prompt);
			FileFilter filter = null;
			if(types != null && types.length() > 0){
				filter = new FileChooserFilter(types, typeDesc);
				chooser.setFileFilter(filter);
			}
			int result = JFileChooser.ERROR_OPTION;
			if (mode == FileDialog.SAVE) {
				result = chooser.showSaveDialog(frame);
			} else if (mode == FileDialog.LOAD) {
				result = chooser.showOpenDialog(frame);
			}
			if (result == JFileChooser.APPROVE_OPTION) {
				try {
					selectedFile = chooser.getSelectedFile().getCanonicalPath();
				} catch (IOException e) {
					selectedFile = null;
				}
			}
		}
		return selectedFile;
	}

	/*
	Component parentComponent
	    The first argument to each showXxxDialog method is always the parent component, which must be a 
	    Frame, a component inside a Frame, or null. If you specify a Frame or Dialog, then the Dialog 
	    will appear over the center of the Frame and follow the focus behavior of that Frame. If you 
	    specify a component inside a Frame, then the Dialog will appear over the center of that component 
	    and will follow the focus behavior of that component's Frame. If you specify null, then the look 
	    and feel will pick an appropriate position for the dialog generally the center of the screen and 
	    the Dialog will not necessarily follow the focus behavior of any visible Frame or Dialog.

	    The JOptionPane constructors do not include this argument. Instead, you specify the parent frame 
	    when you create the JDialog that contains the JOptionPane, and you use the JDialog 
	    setLocationRelativeTo method to set the dialog position.
	Object message
	    This required argument specifies what the dialog should display in its main area. Generally, you 
	    specify a string, which results in the dialog displaying a label with the specified text. You can 
	    split the message over several lines by putting newline (\n) characters inside the message string. 
	    For example:

	    "Complete the sentence:\n \"Green eggs and...\""

	String title
	    The title of the dialog.
	int optionType
	    Specifies the set of buttons that appear at the bottom of the dialog. Choose from one of the 
	    following standard sets: DEFAULT_OPTION, YES_NO_OPTION, YES_NO_CANCEL_OPTION, OK_CANCEL_OPTION.
	int messageType
	    This argument determines the icon displayed in the dialog. Choose from one of the following 
	    values: PLAIN_MESSAGE (no icon), ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE.
	Icon icon
	    The icon to display in the dialog.
	Object[] options
	    Generally used to specify the string displayed by each button at the bottom of the dialog. See 
	    Customizing Button Text in a Standard Dialog for more information. Can also be used to specify 
	    icons to be displayed by the buttons or non-button components to be added to the button row.
	Object initialValue
	    Specifies the default value to be selected.

	You can either let the option pane display its default icon or specify the icon using the message 
	type or icon argument. By default, an option pane created with showMessageDialog displays the 
	information icon, one created with showConfirmDialog or showInputDialog displays the question 
	icon, and one created with a JOptionPane constructor displays no icon. To specify that the dialog 
	display a standard icon or no icon, specify the message type corresponding to the icon you desire. 
	To specify a custom icon, use the icon argument. The icon argument takes precedence over the 
	message type; as long as the icon argument has a non-null value, the dialog displays the 
	specified icon.
	 */

	private static String PANE_TEXT_STYLE_MACOS = "<html> <head> <style type=\"text/css\">"+
			"b { font: 13pt \"Lucida Grande\" } p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
			"</style> </head> <b>@@TITLE@@</b> <p>@@MESSAGE@@</p>";

	private static String PANE_TEXT_STYLE_OTHER = "<html> <head> <style type=\"text/css\">"+
			"b { font: 12pt \"Lucida Grande\" } p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
			"</style> </head> <b>@@MESSAGE@@ </b>";

	/**
	 * Display a simple message dialog window. <br>
	 * 
	 * The actual UI will depend on the platform your application is running on. <br>
	 * 
	 * The message type should be one of the following <br>
	 * G4P.PLAIN, G4P.ERROR, G4P.INFO, G4P.WARNING, G4P.QUERY <br>
	 * 
	 * @param owner the control responsible for this dialog. 
	 * @param message the text to be displayed in the main area of the dialog
	 * @param title the text to appear in the dialog's title bar.
	 * @param messageType the message type
	 */
	public static void showMessage(PApplet owner, String message, String title, int messageType){
		Frame frame = getFrame(owner);
		String m;
		if(PApplet.platform == PApplet.MACOSX){
			m = PANE_TEXT_STYLE_MACOS.replaceAll("@@TITLE@@", title);
			title = "";
			m = m.replaceAll("@@MESSAGE@@", message);
		}
		else {
			m = PANE_TEXT_STYLE_OTHER.replaceAll("@@MESSAGE@@", message);
		}
		JOptionPane.showMessageDialog(frame, m, title, messageType);
	}

	/**
	 * Display a simple message dialog window. <br>
	 * 
	 * The actual UI will depend on the platform your application is running on. <br>
	 * 
	 * The message type should be one of the following <br>
	 * 	G4P.PLAIN, G4P.ERROR, G4P.INFO, G4P.WARNING, G4P.QUERY <br>
	 * 
	 * The option type  should be one of the following <br>
	 * G4P.YES_NO, G4P.YES_NO_CANCEL, G4P.OK_CANCEL <br>
	 * 
	 * This method returns a value to indicate which button was clicked. It will be
	 * one of the following <br>
	 * G4P.OK, G4P.YES, G4P.NO, G4P.CANCEL, G4P.CLOSED <br>
	 * 
	 * Some comments on the returned value: <ul>
	 * <li>G4P.OK and G4P.YES have the same integer value so can be used interchangeably. </li>
	 * <li>G4P.CLOSED maybe returned if the dialog box is closed although on some 
	 * systems G4P.NO or G4P.CANCEL may be returned instead. </li>
	 * <li>It is better to test for a positive response because they have the same value. </li>
	 * <li> If you must test for a negative response use !G4P.OK or !G4P.YES </li></ul>
	 * 
	 * @param owner the control responsible for this dialog. 
	 * @param message the text to be displayed in the main area of the dialog
	 * @param title the text to appear in the dialog's title bar.
	 * @param messageType the message type
	 * @param optionType
	 * @return which button was clicked
	 */
	public static int selectOption(PApplet owner, String message, String title, int messageType, int optionType){
		Frame frame = getFrame(owner);
		String m;
		if(PApplet.platform == PApplet.MACOSX){
			m = PANE_TEXT_STYLE_MACOS.replaceAll("@@TITLE@@", title);
			title = "";
			m = m.replaceAll("@@MESSAGE@@", message);
		}
		else {
			m = PANE_TEXT_STYLE_OTHER.replaceAll("@@MESSAGE@@", message);
		}
		return JOptionPane.showOptionDialog(frame, m, title, optionType, messageType, null, null, null);
	}

	/**
	 * Find the Frame associated with this object.
	 * 
	 * @param owner the object that is responsible for this message
	 * @return the frame (if any) that owns this object else return null
	 */
	private static Frame getFrame(PApplet owner){
		Frame frame = null;
		try {
			frame = (Frame) ((processing.awt.PSurfaceAWT.SmoothCanvas) owner.getSurface().getNative()).getFrame();
		}
		catch(Exception e){
		}
		return frame;
	}

}
