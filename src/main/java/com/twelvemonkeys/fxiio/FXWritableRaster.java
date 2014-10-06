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
import javafx.scene.image.*;

import java.awt.*;
import java.awt.image.*;
import java.nio.IntBuffer;

/**
 * WritableRaster backed by a JavaFX {@link Image} or {@link WritableImage}
 */
final class FXWritableRaster extends WritableRaster {
    private final PixelReader pixelReader;
    private final PixelWriter pixelWriter;
    private final WritablePixelFormat<IntBuffer> writableIntFormat;

    public FXWritableRaster(final Image fxImage, final SampleModel sampleModel, final DataBuffer dataBuffer) {
        super(sampleModel, dataBuffer, new Point());

        this.pixelReader = fxImage.getPixelReader();
        this.pixelWriter = fxImage instanceof WritableImage ? ((WritableImage) fxImage).getPixelWriter() : null;

        switch (pixelReader.getPixelFormat().getType()) {
            case BYTE_BGRA_PRE:
            case INT_ARGB_PRE:
                writableIntFormat = PixelFormat.getIntArgbPreInstance();
                break;
            case BYTE_BGRA:
            case INT_ARGB:
            case BYTE_RGB:
            case BYTE_INDEXED:
                writableIntFormat = PixelFormat.getIntArgbInstance();
                break;
            default:
                throw new IllegalArgumentException("Unsupported pixel format: " + pixelReader.getPixelFormat().getType());
        }
    }

    public FXWritableRaster(final SampleModel sampleModel,
                            final DataBuffer dataBuffer,
                            final Rectangle region,
                            final Point sampleModelTranslate,
                            final FXWritableRaster parent) {
        super(sampleModel, dataBuffer, region, sampleModelTranslate, parent);

        pixelReader = parent.pixelReader;
        pixelWriter = parent.pixelWriter;
        writableIntFormat = parent.writableIntFormat;
    }

    @Override public FXWritableRaster createWritableChild(final int parentX,
                                                        final int parentY,
                                                        final int w,
                                                        final int h,
                                                        final int childMinX,
                                                        final int childMinY,
                                                        final int[] bandList) {
        // Modified from super.createWritableChild to create an instance of FXWritableRaster, for better performance
        if (parentX < this.minX) {
            throw new RasterFormatException("parentX lies outside raster");
        }
        if (parentY < this.minY) {
            throw new RasterFormatException("parentY lies outside raster");
        }
        if ((parentX+w < parentX) || (parentX+w > this.width + this.minX)) {
            throw new RasterFormatException("(parentX + width) is outside raster");
        }
        if ((parentY+h < parentY) || (parentY+h > this.height + this.minY)) {
            throw new RasterFormatException("(parentY + height) is outside raster");
        }

        // Note: the SampleModel for the child Raster should have the same
        // width and height as that for the parent, since it represents
        // the physical layout of the pixel data.  The child Raster's width
        // and height represent a "virtual" view of the pixel data, so
        // they may be different than those of the SampleModel.
        SampleModel sm = bandList == null ? sampleModel : sampleModel.createSubsetSampleModel(bandList);

        int deltaX = childMinX - parentX;
        int deltaY = childMinY - parentY;

        return new FXWritableRaster(sm,
                                    getDataBuffer(),
                                    new Rectangle(childMinX, childMinY, w, h),
                                    new Point(sampleModelTranslateX + deltaX, sampleModelTranslateY + deltaY),
                                    this);
    }

    @Override public Raster createChild(final int parentX,
                                        final int parentY,
                                        final int width,
                                        final int height,
                                        final int childMinX,
                                        final int childMinY,
                                        final int[] bandList) {
        // Modified from super.createChild to create an instance of FXWritableRaster, for better performance
        if (parentX < this.minX) {
            throw new RasterFormatException("parentX lies outside raster");
        }
        if (parentY < this.minY) {
            throw new RasterFormatException("parentY lies outside raster");
        }
        if ((parentX + width < parentX) || (parentX + width > this.width + this.minX)) {
            throw new RasterFormatException("(parentX + width) is outside raster");
        }
        if ((parentY + height < parentY) || (parentY + height > this.height + this.minY)) {
            throw new RasterFormatException("(parentY + height) is outside raster");
        }

        // Note: the SampleModel for the child Raster should have the same
        // width and height as that for the parent, since it represents
        // the physical layout of the pixel data.  The child Raster's width
        // and height represent a "virtual" view of the pixel data, so
        // they may be different than those of the SampleModel.
        SampleModel subSampleModel = bandList == null ? sampleModel : sampleModel.createSubsetSampleModel(bandList);

        int deltaX = childMinX - parentX;
        int deltaY = childMinY - parentY;

        return new FXWritableRaster(subSampleModel,
                                    getDataBuffer(),
                                    new Rectangle(childMinX, childMinY, width, height),
                                    new Point(sampleModelTranslateX + deltaX, sampleModelTranslateY + deltaY),
                                    this);
    }

    @Override public void setDataElements(final int x, final int y, final Object inData) {
        System.out.println("FXWritableRaster.setDataElements(xy,Object)");
        super.setDataElements(x, y, inData);
    }

    @Override public void setDataElements(final int x, final int y, final int w, final int h, final Object inData) {
        System.out.println("FXWritableRaster.setDataElements(xywh,Object)");
        super.setDataElements(x, y, w, h, inData);
    }

    @Override public void setPixels(final int x, final int y, final int w, final int h, final int[] iArray) {
        // Overriden for performance (JPEG)
        switch (pixelWriter.getPixelFormat().getType()) {
            case INT_ARGB:
            case INT_ARGB_PRE:
                pixelWriter.setPixels(x - sampleModelTranslateX, y - sampleModelTranslateY, w, h, writableIntFormat, iArray, 0, w);

                break;

            case BYTE_BGRA_PRE:
            case BYTE_BGRA:
            {
                int stride = w * numBands;

                // TODO: Most likely there are other conversions...
                // QnD fix for BGRA -> RGB conversion
                if (numBands == 4) {
                    for (int i = 0; i < iArray.length; i += numBands) {
                        int argb = iArray[i] << 16 | iArray[i + 1] << 8 | iArray[i + 2] | iArray[i + 3] << 24;

                        int xtrans = i % stride;
                        int ytrans = i / stride;
                        pixelWriter.setArgb(x - sampleModelTranslateX + xtrans, y - sampleModelTranslateY + ytrans, argb);
                    }
                }
                else {
                    for (int i = 0; i < iArray.length; i += numBands) {
                        int argb = iArray[i] << 16 | iArray[i + 1] << 8 | iArray[i + 2] | 0xFF000000;

                        int xtrans = (i / numBands) % w;
                        int ytrans = i / stride;

                        pixelWriter.setArgb(x - sampleModelTranslateX + xtrans, y - sampleModelTranslateY + ytrans, argb);
                    }
                }

                break;
            }
            case BYTE_RGB:
            {
                int stride = w * numBands;

                for (int i = 0; i < iArray.length; i += numBands) {
                    int argb = iArray[i] << 16 | iArray[i + 1] << 8 | iArray[i + 2] | 0xFF000000;

                    int xtrans = (i / numBands) % w;
                    int ytrans = i / stride;

                    pixelWriter.setArgb(x - sampleModelTranslateX + xtrans, y - sampleModelTranslateY + ytrans, argb);
                }

                break;
            }
            case BYTE_INDEXED:
            default:
                // Fallback to default impl
                super.setPixels(x, y, w, h, iArray);
        }
    }

    @Override public void setPixel(int x, int y, int[] iArray) {
        // Overriden for performance (PNG)
        // TODO: Probably not correct for pre...

        switch (pixelWriter.getPixelFormat().getType()) {
            case INT_ARGB:
            case INT_ARGB_PRE:
                pixelWriter.setArgb(x - sampleModelTranslateX, y - sampleModelTranslateY, iArray[0]);

                break;

            case BYTE_BGRA_PRE:
            case BYTE_BGRA:
                {
                    // TODO: Most likely there are other conversions...
                    // QnD fix for BGRA -> RGB conversion
                    if (numBands == 4) {
                        int argb = iArray[0] << 16 | iArray[1] << 8 | iArray[2] | iArray[3] << 24;
                        pixelWriter.setArgb(x - sampleModelTranslateX, y - sampleModelTranslateY, argb);
                    }
                    else {
                        int argb = iArray[0] << 16 | iArray[1] << 8 | iArray[2] | 0xFF000000;
                        pixelWriter.setArgb(x - sampleModelTranslateX, y - sampleModelTranslateY, argb);

                    }

                break;
            }
            case BYTE_RGB:
            {
                int argb = iArray[0] | iArray[1] << 8 | iArray[2] << 16 | 0xFF000000;
                pixelWriter.setArgb(x - sampleModelTranslateX, y - sampleModelTranslateY, argb);

                break;
            }
            case BYTE_INDEXED:
            default:
                // Fallback to default impl
                super.setPixel(x, y, iArray);
        }
    }

    @Override public int[] getPixels(final int x, final int y, final int w, final int h, final int[] iArray) {
        // Overriden for better performance
        int[] pixels;

        switch (pixelReader.getPixelFormat().getType()) {
            case INT_ARGB:
            case INT_ARGB_PRE:
                pixels = iArray != null ? iArray : new int[w * h];

                pixelReader.getPixels(x - sampleModelTranslateX, y - sampleModelTranslateY, w, h, writableIntFormat, pixels, 0, w);

                return pixels;

            case BYTE_BGRA_PRE:
            case BYTE_BGRA:
                pixels = iArray != null ? iArray : new int[w * h * numBands];

                pixelReader.getPixels(x - sampleModelTranslateX, y - sampleModelTranslateY, w, h, writableIntFormat, pixels, 0, w);

                // Loop through pixels from the back of the read values, and copy values backwards, to have one sample per element (unpack)
                for (int i = (w * h) - 1; i >= 0; i--) {
                    int argb = pixels[i];

                    pixels[i * numBands    ] = (argb & 0xff);              // B
                    pixels[i * numBands + 1] = (argb & 0xff00) >> 8;       // G
                    pixels[i * numBands + 2] = (argb & 0xff0000) >> 16;    // R
                    pixels[i * numBands + 3] = (argb & 0xff000000) >>> 24; // A
                }

                return pixels;
            case BYTE_RGB:
                pixels = iArray != null ? iArray : new int[w * h * numBands];

                pixelReader.getPixels(x - sampleModelTranslateX, y - sampleModelTranslateY, w, h, writableIntFormat, pixels, 0, w);

                // Loop through pixels from the back of the read values, and copy values backwards, to have one sample per element (unpack)
                for (int i = (w * h) - 1; i >= 0; i--) {
                    int argb = pixels[i];

                    pixels[i * numBands    ] = (argb & 0xff0000) >> 16;    // R
                    pixels[i * numBands + 1] = (argb & 0xff00) >> 8;       // G
                    pixels[i * numBands + 2] = (argb & 0xff);              // B
                }

                return pixels;
            case BYTE_INDEXED:
            default:
                // Fallback to default impl
                return super.getPixels(x, y, w, h, iArray);
        }
    }

    @Override public String toString() {
        return "FXWritableRaster@" + Integer.toHexString(hashCode())
                + " width = " + width + ", height = " + height
                + " (" + sampleModel.getClass().getSimpleName() + ": numbands = " + sampleModel.getNumBands()
                + ", " + dataBuffer.getClass().getSimpleName() + ": type = " + dataBuffer.getDataType() +  ")";
    }
}
