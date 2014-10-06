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

import javafx.scene.image.*;

import java.awt.image.DataBuffer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * DataBuffer implementation backed by a JavaFX {@link PixelReader}.
 * Optimized for reading/writing rows of pixels (which is normally the case for writing using ImageIO).
 * Probably slow for random access.
 */
abstract class PixelReaderDataBuffer<T extends Buffer> extends DataBuffer {
    protected final int width;
    protected final int sampleSize;

    protected final PixelReader pixelReader;
    protected final PixelWriter pixelWriter;
    protected final WritablePixelFormat<T> writablePixelFormat;
    protected final int writableFormatSampleSize;

    protected int bufferedRow = -1;

    @SuppressWarnings("unchecked") PixelReaderDataBuffer(final int type,
                                                         final int width,
                                                         final int height,
                                                         final int sampleSize,
                                                         final PixelReader pixelReader,
                                                         final PixelWriter pixelWriter) {
        super(type, getBufferSize(width, height, pixelReader));
        this.width = width;
        this.sampleSize = sampleSize;
        this.pixelReader = pixelReader;
        this.pixelWriter = pixelWriter;
        this.writablePixelFormat = getWritablePixelFormat(pixelReader.getPixelFormat());
        this.writableFormatSampleSize = FXBufferedImage.getSampleSize(writablePixelFormat);
    }

    public static DataBuffer createDataBuffer(final Image image) {
        PixelReader pixelReader = image.getPixelReader();
        PixelWriter pixelWriter = image instanceof WritableImage ? ((WritableImage) image).getPixelWriter() : null;

        @SuppressWarnings("rawtypes")
        PixelFormat pixelFormat = pixelReader.getPixelFormat();

        switch (pixelFormat.getType()) {
            case INT_ARGB_PRE:
            case INT_ARGB:
                return new PixelReaderDataBufferInt((int) image.getWidth(), (int) image.getHeight(), pixelReader, pixelWriter);
            case BYTE_BGRA_PRE:
            case BYTE_BGRA:
            case BYTE_RGB:
            case BYTE_INDEXED:
                return new PixelReaderDataBufferByte((int) image.getWidth(), (int) image.getHeight(), pixelReader, pixelWriter);
            default:
                throw new IllegalArgumentException("Unsupported pixel format: " + pixelFormat.getType());
        }
    }

    private static int getBufferSize(final int width, final int height, final PixelReader pixelReader) {
        return FXBufferedImage.getSampleSize(pixelReader.getPixelFormat()) * width * height;
    }

    @SuppressWarnings("rawtypes")
    private static WritablePixelFormat getWritablePixelFormat(final PixelFormat format) {
        return format instanceof WritablePixelFormat ? (WritablePixelFormat) format : PixelFormat.getByteBgraInstance();
    }

    @Override public void setElem(final int bank, final int i, final int val) {
        throw new UnsupportedOperationException("DataBuffer is read-only");
    }

    private static final class PixelReaderDataBufferInt extends PixelReaderDataBuffer<IntBuffer> {
        private final int[] readBuffer;
        private final int[] writeBuffer;

        private PixelReaderDataBufferInt(final int width, final int height, final PixelReader pixelReader, final PixelWriter pixelWriter) {
            super(DataBuffer.TYPE_INT, width, height, 1, pixelReader, pixelWriter);
            this.readBuffer = new int[width];
            this.writeBuffer = new int[1];
        }

        @Override public int getElem(final int bank, final int i) {
            if (bank > 0) {
                throw new IndexOutOfBoundsException("bank (" + bank + ") >= numBanks (" + 1 + ")");
            }

            int x = (i / sampleSize) % width;
            int y = (i / sampleSize) / width;

            if (bufferedRow != y) {
                pixelReader.getPixels(0, y, width, 1, writablePixelFormat, readBuffer, 0, width);
                bufferedRow = y;
            }

            return readBuffer[x * writableFormatSampleSize];
        }

        @Override public void setElem(final int bank, final int i, final int val) {
            if (pixelWriter == null) {
                throw new UnsupportedOperationException("DataBuffer is read-only");
            }
            if (bank > 0) {
                throw new IndexOutOfBoundsException("bank (" + bank + ") >= numBanks (" + 1 + ")");
            }

            int x = (i / sampleSize) % width;
            int y = (i / sampleSize) / width;

            if (y == bufferedRow) {
                // Update buffered value
                readBuffer[i] = val;
            }

            writeBuffer[0] = val;
            pixelWriter.setPixels(x, y, 1, 1, writablePixelFormat, writeBuffer, 0, 1);
        }
    }

    private static final class PixelReaderDataBufferByte extends PixelReaderDataBuffer<ByteBuffer> {
        /** Conversion table, if writablePixelFormat differs from pixelReader.getFormat(). */
        private final int[] conversion;

        private final byte[] readBuffer;
        private final byte[] writeBuffer;

        private PixelReaderDataBufferByte(final int width, final int height, final PixelReader pixelReader, final PixelWriter pixelWriter) {
            super(DataBuffer.TYPE_BYTE, width, height, FXBufferedImage.getSampleSize(pixelReader.getPixelFormat()), pixelReader, pixelWriter);

            this.readBuffer = new byte[width * writableFormatSampleSize];
            this.writeBuffer = new byte[writableFormatSampleSize];
            this.conversion = getConversionTable(pixelReader.getPixelFormat().getType(), writablePixelFormat.getType());
        }

        private static int[] getConversionTable(final PixelFormat.Type pixelFormat, final PixelFormat.Type writablePixelFormat) {
            if (pixelFormat == writablePixelFormat) {
                // No conversion, should work for all known types
                return new int[] {0, 1, 2, 3};
            }
            else if (pixelFormat == PixelFormat.Type.BYTE_RGB && writablePixelFormat == PixelFormat.Type.BYTE_BGRA) {
                // RGB -> ARGB
                return new int[] {2, 1, 0};
            }
            else {
                throw new IllegalArgumentException("Unsupported conversion: " + writablePixelFormat + " -> " + pixelFormat);
            }
        }

        @Override public int getElem(final int bank, final int i) {
            if (bank > 0) {
                throw new IndexOutOfBoundsException("bank (" + bank + ") >= numBanks (" + 1 + ")");
            }

            int x = (i / sampleSize) % width;
            int y = (i / sampleSize) / width;

            if (bufferedRow != y) {
                pixelReader.getPixels(0, y, width, 1, writablePixelFormat, readBuffer, 0, width * sampleSize);
                bufferedRow = y;
            }

            return readBuffer[x * writableFormatSampleSize + conversion[i % sampleSize]];
        }

        @Override public void setElem(final int bank, final int i, final int val) {
            if (pixelWriter == null) {
                throw new UnsupportedOperationException("DataBuffer is read-only");
            }
            if (bank > 0) {
                throw new IndexOutOfBoundsException("bank (" + bank + ") >= numBanks (" + 1 + ")");
            }

            int x = (i / sampleSize) % width;
            int y = (i / sampleSize) / width;

            // Get current row
            if (bufferedRow != y) {
                getElem(bank, i);
            }

            // Update buffer in place
            readBuffer[x * writableFormatSampleSize + conversion[i % sampleSize]] = (byte) val;

            // TODO: Only if ARGB -> RGB conversion, because A component is otherwise left as 0 (transparent)
            readBuffer[x * writableFormatSampleSize + 3] = (byte) 0xff;

            // Copy to writebuffer
            System.arraycopy(readBuffer, x * writableFormatSampleSize, writeBuffer, 0, writableFormatSampleSize);

            pixelWriter.setPixels(x, y, 1, 1, writablePixelFormat, writeBuffer, 0, writableFormatSampleSize);
        }
    }
}
