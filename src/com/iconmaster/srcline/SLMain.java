package com.iconmaster.srcline;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author iconmaster
 */
public class SLMain {

	public static void main(String[] args) {
		try {
			LibraryLoader.addFile("./Source.jar");
		} catch (Exception ex) {
			Logger.getLogger(SourceLine.class.getName()).log(Level.SEVERE, null, ex);
			JOptionPane.showMessageDialog(null, "Source.jar was not sucsessfully loaded! Please make sure it's in the same directory as SourceLine.jar.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		
		SourceLine.mainFunc(args);
	}
}
