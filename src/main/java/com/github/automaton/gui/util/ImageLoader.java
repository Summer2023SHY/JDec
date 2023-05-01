package com.github.automaton.gui.util;

import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

/**
 * Helper class for image IO operations
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * @since 1,1
 */
public final class ImageLoader {

    /** Internally used {@link SVGDocumentFactory} object */
    private static SVGDocumentFactory svgFactory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());

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
     * Load the generated graph SVG from filename.
     * 
     * @param fileName The name of the image to be loaded
     * @return the SVG document
     * @throws IOException if I/O error occurs
     **/
    public static SVGDocument loadSVGFromFile(String fileName) throws IOException {
        return loadSVGFromFile(new File(fileName));
    }

    /**
     * Load the generated graph SVG from file.
     * 
     * @param file {@link File} that points to the image to be loaded
     * @return the SVG document
     * @throws IOException if I/O error occurs
     **/
    public static SVGDocument loadSVGFromFile(File file) throws IOException {
        return svgFactory.createSVGDocument(file.toURI().toString());
    }
}
