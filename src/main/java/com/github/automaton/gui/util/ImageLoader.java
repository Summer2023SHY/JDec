package com.github.automaton.gui.util;

/* 
 * Copyright (C) 2016 Micah Stairs
 * Copyright (C) 2023 Sung Ho Yoon
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.logging.log4j.*;
import org.w3c.dom.svg.SVGDocument;

/**
 * Helper class for image IO operations
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * @since 1,1
 */
public final class ImageLoader {

    private static Logger logger = LogManager.getLogger();

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
            logger.catching(e);
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
