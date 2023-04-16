package com.github.automaton.gui.util;

import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;

/**
 * Helper class for image IO operations
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 */
public final class ImageLoader {
    /** Private Constructor */
    private ImageLoader() {}

    /**
     * Load the generated graph image from file.
     * 
     * @param fileName The name of the image to be loaded
     * @return The image, or null if it could not be loaded
     **/
    public static BufferedImage loadImageFromFile(String fileName) {
        try {
            return ImageIO.read(new File(fileName));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load the generated graph image from file.
     * 
     * @param fileName The name of the image to be loaded
     * @return The image, or {@code null} if it could not be loaded
     **/
    public static SVGDocument loadSVGFromFile(String fileName) {
        try {
            return new SVGLoader().load(new File(fileName).toURI().toURL());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
