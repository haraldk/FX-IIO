/*
Copyright (c) 2014, Harald Kuhr
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of FX-IIO nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.fxiio;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class FXBufferedImageTest extends Application {

    // TODO: Expand on this idea to create a fully working ImageIO wrapper for JavaFX!

    public static void main(String[] args) throws IOException {
        File input = new File(args[0]);
        Image fxImage = new Image(input.toURI().toString());

        System.err.println("fxImage.getPixelReader().getPixelFormat().getType(): " + fxImage.getPixelReader().getPixelFormat().getType());

        BufferedImage bufferedImage = asReadOnlyBufferdImage(fxImage);

        System.err.println("bufferedImage: " + bufferedImage);

        String format = "JPG";
        File output = new File("foo-test." + format.toLowerCase());

        long start = System.currentTimeMillis();
        if (ImageIO.write(bufferedImage, format, output)) {
            System.err.println("Written using ImageIO in: " + (System.currentTimeMillis() - start) + "ms");
            System.err.println("Wrote " + output.getAbsolutePath());
        } else {
            System.err.println("Could not write!");
        }

        launch(args);
    }

    @Override public void start(final Stage primaryStage) throws Exception {
        File input = new File(getParameters().getRaw().get(0));
        long start = System.currentTimeMillis();
        WritableImage fxWritableImage = readImage(input);
        System.err.println("Loaded using ImageIO in: " + (System.currentTimeMillis() - start) + "ms");

        Scene scene = new Scene(new Group(), fxWritableImage.getWidth(), fxWritableImage.getHeight());
        Label label = new Label();
        label.setGraphic(new ImageView(fxWritableImage));
        scene.setRoot(label);

        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();

        primaryStage.show();
    }


    public static WritableImage readImage(final File file) throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(file);

        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return null;
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                ImageTypeSpecifier defaultSpec = reader.getImageTypes(0).next();

                WritableImage fxImage = new WritableImage(width, height); // TODO: How do we decide the pixel format (we don't, the system decides)?!

                ImageReadParam param = reader.getDefaultReadParam();

                // TODO: Consider ImageTypeSpecifier as extra parameter, to decide format
                BufferedImage destination = asBufferdImage(fxImage, defaultSpec);
                param.setDestination(destination);

                // TODO: This is a QnD hack. Should really compare the bands, and map accordingly
//                if (destination.getSampleModel().getNumBands() != defaultSpec.getNumBands()) {
//                    int[] bands = new int[defaultSpec.getNumBands()];
//                    for (int i = 0; i < bands.length; i++) {
//                        bands[i] = 1;
//                    }
//                    param.setDestinationBands(bands); // TODO: (...besides, it doesn't work, might be a bug in JPEGImageReader, computing band sizes)
//                }

                reader.read(0, param);

                return fxImage;
            }
            finally {
                reader.dispose();
            }
        }
        finally {
            stream.close();
        }
    }

    /**
     * Creates a {@link java.awt.image.BufferedImage} that is backed by a JavaFX {@link javafx.scene.image.Image}.
     * NOTE: The images will be read-only.
     * @param fxImage
     * @return a {@code TYPE_CUSTOM}, read-only {@code BufferedImage}.
     */
    private static BufferedImage asReadOnlyBufferdImage(final Image fxImage) {
        return new FXBufferedImage(fxImage);
    }

    /**
     * Creates a {@link java.awt.image.BufferedImage} that is backed by a JavaFX {@link javafx.scene.image.WritableImage}.
     * @param fxImage
     * @param defaultSpec
     * @return a {@code TYPE_CUSTOM} {@code BufferedImage}.
     */
    private static BufferedImage asBufferdImage(final WritableImage fxImage, final ImageTypeSpecifier defaultSpec) {
        // TODO: Handle this scenario!
//                int[] bands = new int[defaultSpec.getNumBands()];
//                for (int i = 0; i < bands.length; i++) {
//                    bands[i] = 1;
//                }
//                System.err.println("defaultSpec: " + defaultSpec.getNumBands());
//
//                BufferedImage destination = new FXBufferedImage(getColorModel(fxImage),
//                                                                new FXWritableRaster(fxImage, crateSampleModel(fxImage).createSubsetSampleModel(bands), PixelReaderDataBuffer.createDataBuffer(fxImage)));
        return new FXBufferedImage(fxImage, defaultSpec);
    }
}
