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

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageTypeSpecifier;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;

/**
 * Wrapper class for JavaFX {@link Image} and {@link WritableImage} to masquerade as a {@link BufferedImage}.
 */
final class FXBufferedImage extends BufferedImage {

    private static final ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);

    public FXBufferedImage(final Image fxImage) {
        this(getColorModel(fxImage), new FXWritableRaster(fxImage, crateSampleModel(fxImage), PixelReaderDataBuffer.createDataBuffer(fxImage)));
    }

    public FXBufferedImage(final WritableImage fxImage, final ImageTypeSpecifier spec) {
        // TODO: Create a Color model, sample model and raster, compatible with spec!
        this(spec.getColorModel(), new FXWritableRaster(fxImage,
                                                        crateSampleModel(fxImage).createSubsetSampleModel(createIndicies(spec.getNumBands())),
//                                                        spec.getSampleModel((int) fxImage.getWidth(), (int) fxImage.getHeight()),
                                                        PixelReaderDataBuffer.createDataBuffer(fxImage)));
    }

    private FXBufferedImage(ColorModel cm, FXWritableRaster raster) {
        super(cm, raster, cm.isAlphaPremultiplied(), null);
    }

    private static int[] createIndicies(final int count) {
        int[] indices = new int[count];

        for (int i = 0; i < count; i++) {
            indices[i] = i;
        }

        return indices;
    }

    static int getSampleSize(@SuppressWarnings("rawtypes") final PixelFormat pixelFormat) {
        switch (pixelFormat.getType()) {
            case INT_ARGB_PRE:
            case INT_ARGB:
            case BYTE_INDEXED:
                return 1;
            case BYTE_BGRA_PRE:
            case BYTE_BGRA:
                return 4;
            case BYTE_RGB:
                return 3;
            default:
                throw new IllegalArgumentException("Unsupported pixel format: " + pixelFormat.getType());
        }
    }

    private static ColorModel getColorModel(final Image image) {
        @SuppressWarnings("rawtypes")
        PixelFormat pixelFormat = image.getPixelReader().getPixelFormat();
        switch (pixelFormat.getType()) {
            case INT_ARGB_PRE:
            case INT_ARGB:
                return ColorModel.getRGBdefault();
            case BYTE_BGRA_PRE:
                return new ComponentColorModel(sRGB, true, true, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
            case BYTE_BGRA:
                return new ComponentColorModel(sRGB, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
            case BYTE_RGB:
                return new ComponentColorModel(sRGB, false, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
            case BYTE_INDEXED:
                // TODO: Will need to create an IndexColorModel
            default:
                throw new IllegalArgumentException("Unsupported pixel format: " + pixelFormat.getType());
        }
    }

    private static SampleModel crateSampleModel(final Image image) {
        PixelReader pixelReader = image.getPixelReader();

        @SuppressWarnings("rawtypes")
        PixelFormat pixelFormat = pixelReader.getPixelFormat();

        switch (pixelFormat.getType()) {
            case INT_ARGB_PRE:
            case INT_ARGB:
                return new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, (int) image.getWidth(), (int) image.getHeight(), new int[] {0xFF000000, 0xFF0000, 0xFF00, 0xFF});
            case BYTE_BGRA_PRE:
            case BYTE_BGRA:
            case BYTE_RGB:
                int sampleSize = getSampleSize(pixelFormat);
                return new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, (int) image.getWidth(), (int) image.getHeight(), sampleSize, sampleSize * (int) image.getWidth(), createOffsets(pixelFormat));
            case BYTE_INDEXED:
            default:
                throw new IllegalArgumentException("Unsupported pixel format: " + pixelFormat.getType());
        }
    }

    private static int[] createOffsets(@SuppressWarnings("rawtypes") final PixelFormat pixelFormat) {
        switch (pixelFormat.getType()) {
            case BYTE_BGRA:
            case BYTE_BGRA_PRE:
                return new int[] {2, 1, 0, 3};
            case BYTE_RGB:
                return new int[] {0, 1, 2};
            case BYTE_INDEXED:
                return new int[] {0};
            case INT_ARGB_PRE:
            case INT_ARGB:
            default:
                throw new IllegalArgumentException("Unsupported pixel format: " + pixelFormat.getType());
        }
    }

    @Override public String toString() {
        return "FX" + super.toString();
    }
}
