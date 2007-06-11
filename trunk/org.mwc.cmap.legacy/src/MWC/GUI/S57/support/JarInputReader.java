// **********************************************************************
// 
// <copyright>
// 
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// 
// </copyright>
// **********************************************************************
// 
// $Source: i:/mwc/coag/asset/cvsroot/util/MWC/GUI/S57/support/JarInputReader.java,v $
// $RCSfile: JarInputReader.java,v $
// $Revision: 1.1 $
// $Date: 2007/04/27 09:20:01 $
// $Author: ian.mayo $
// 
// **********************************************************************

package MWC.GUI.S57.support;

import java.io.*;
import java.net.*;
import java.util.jar.*;
import com.bbn.openmap.util.Debug;

/**
 * An InputReader to handle entries in a Jar file.
 */
public class JarInputReader extends StreamInputReader {

    /** Where to go to hook up with a resource. */
    protected URL inputURL = null;

    protected JarFile jarFile = null;
    protected String jarFileName = null;
    protected String jarEntryName = null;

    /**
     * Create a JarInputReader win the path to a jar file, and the
     * entry name. The entry name should be a path to the entry from
     * the internal root of the jar file.
     */
    public JarInputReader(String jarFilePath, String jarEntryName)
            throws IOException {

        if (Debug.debugging("binaryfile")) {
            Debug.output("JarInputReader created for " + jarEntryName + " in "
                    + jarFilePath);
        }
        this.jarFileName = jarFilePath;
        this.jarEntryName = jarEntryName;
        reopen();
        name = jarFilePath + "!" + jarEntryName;
    }

    /**
     * Reset the InputStream to the beginning, by closing the current
     * connection and reopening it.
     */
    public void reopen() throws IOException {
        super.reopen();

        Debug.message("binaryfile", "JarInputReader: reopening jarFile "
                + jarFileName);
        if (jarFile != null)
            jarFile.close();
        jarFile = null;

        jarFile = new JarFile(URLDecoder.decode(jarFileName, "UTF-8"));
        JarEntry entry = jarFile.getJarEntry(jarEntryName);
        inputStream = jarFile.getInputStream(entry);
        if (inputStream == null) {
            Debug.error("JarInputReader: Problem getting input stream for "
                    + jarEntryName + " in " + jarFileName);
        }
    }

    /**
     * Closes the underlying file
     * 
     * @exception IOException Any IO errors encountered in accessing
     *            the file
     */
    public void close() throws IOException {
        jarFile.close();
        super.close();
    }

}