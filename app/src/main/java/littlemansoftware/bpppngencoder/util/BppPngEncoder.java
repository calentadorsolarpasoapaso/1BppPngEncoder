package littlemansoftware.bpppngencoder.util;

import android.graphics.*;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * PngEncoder takes a Bitmap.
 *
 *
 * <p>This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.</p>
 *
 * <p>This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.</p>
 *
 * <p>You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * A copy of the GNU LGPL may be found at
 * <code>http://www.gnu.org/copyleft/lesser.html</code></p>
 *
 * @author Jose Miguel Jimenez Villanueva
 * @version 0.1, 2 April 2017
 *
 */

public class BppPngEncoder extends Object {

    String tag="1BppPngEncoder";


    /** Constants for filter (NONE) */
    public static final int FILTER_NONE = 0;


    /** Constants for filter (LAST) */
    public static final int FILTER_LAST = 2;

    /** IHDR tag. */
    protected static final byte IHDR[] = {73, 72, 68, 82};

    /** IDAT tag. */
    protected static final byte IDAT[] = {73, 68, 65, 84};

    /** IEND tag. */
    protected static final byte IEND[] = {73, 69, 78, 68};

    /** The png bytes. */
    protected byte[] pngBytes;

    /** The prior row. */
    protected byte[] priorRow;

    /** The left bytes. */
    protected byte[] leftBytes;

    /** The image. */
    protected Bitmap image;

    /** The width. */
    protected int width, height;

    /** The byte position. */
    protected int bytePos, maxPos;

    /** CRC. */
    protected CRC32 crc = new CRC32();

    /** The CRC value. */
    protected long crcValue;

    /** The filter type. */
    protected int filter;

    /** The bytes-per-pixel. */
    //protected int bytesPerPixel;

    /** The compression level. */
    protected int compressionLevel;


    private HashMap<Integer,Integer> palette_map = new HashMap<Integer, Integer>();



    /**
     * Set the image to be encoded
     *
     * @param image A Java Image object which uses the DirectColorModel
     */
    public void setImage(Bitmap image) {
        this.image = image;
        pngBytes = null;
    }

    /**
     * Creates an array of bytes that is the PNG equivalent of the current image, specifying
     * whether to encode alpha or not.
     *
     * @return an array of bytes, or null if there was a problem
     */
    public byte[] pngEncode() {
        byte[]  pngIdBytes = {-119, 80, 78, 71, 13, 10, 26, 10};

        if (image == null) {
            return null;
        }
        width = image.getWidth();
        height = image.getHeight();

		/*
		 * start with an array that is big enough to hold all the pixels
		 * (plus filter bytes), and an extra 200 bytes for header info
		 */
        pngBytes = new byte[((width * height)/8 ) + 200];

		/*
		 * keep track of largest byte written to the array
		 */
        maxPos = 0;

        bytePos = writeBytes(pngIdBytes, 0);

        writeHeader();

        // Compress image data
        // 1bpp
        byte[] compressedimage = compressImageData2();

        // Write the Palette
        bytePos = writeBytes(createPebblePalette(), bytePos);

        //dataPos = bytePos;
        writeImageData(compressedimage);
        writeEnd();
        pngBytes = resizeByteArray(pngBytes, maxPos);
        return pngBytes;
    }



    /**
     * Set the filter to use
     *
     * @param whichFilter from constant list
     */
    public void setFilter(int whichFilter) {
        this.filter = FILTER_NONE;
        if (whichFilter <= FILTER_LAST) {
            this.filter = whichFilter;
        }
    }

    /**
     * Retrieve filtering scheme
     *
     * @return int (see constant list)
     */
    public int getFilter() {
        return filter;
    }

    /**
     * Set the compression level to use
     *
     * @param level 0 through 9
     */
    public void setCompressionLevel(int level) {
        if (level >= 0 && level <= 9) {
            this.compressionLevel = level;
        }
    }

    /**
     * Retrieve compression level
     *
     * @return int in range 0-9
     */
    public int getCompressionLevel() {
        return compressionLevel;
    }

    /**
     * Increase or decrease the length of a byte array.
     *
     * @param array The original array.
     * @param newLength The length you wish the new array to have.
     * @return Array of newly desired length. If shorter than the
     *         original, the trailing elements are truncated.
     */
    protected byte[] resizeByteArray(byte[] array, int newLength) {
        byte[]  newArray = new byte[newLength];
        int     oldLength = array.length;

        System.arraycopy(array, 0, newArray, 0, Math.min(oldLength, newLength));
        return newArray;
    }

    /**
     * Write an array of bytes into the pngBytes array.
     * Note: This routine has the side effect of updating
     * maxPos, the largest element written in the array.
     * The array is resized by 1000 bytes or the length
     * of the data to be written, whichever is larger.
     *
     * @param data The data to be written into pngBytes.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    protected int writeBytes(byte[] data, int offset) {
        maxPos = Math.max(maxPos, offset + data.length);
        if (data.length + offset > pngBytes.length) {
            pngBytes = resizeByteArray(pngBytes, pngBytes.length + Math.max(1000, data.length));
        }
        System.arraycopy(data, 0, pngBytes, offset, data.length);
        return offset + data.length;
    }

    /**
     * Write an array of bytes into the pngBytes array, specifying number of bytes to write.
     * Note: This routine has the side effect of updating
     * maxPos, the largest element written in the array.
     * The array is resized by 1000 bytes or the length
     * of the data to be written, whichever is larger.
     *
     * @param data The data to be written into pngBytes.
     * @param nBytes The number of bytes to be written.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    protected int writeBytes(byte[] data, int nBytes, int offset) {
        maxPos = Math.max(maxPos, offset + nBytes);
        if (nBytes + offset > pngBytes.length) {
            pngBytes = resizeByteArray(pngBytes, pngBytes.length + Math.max(1000, nBytes));
        }
        System.arraycopy(data, 0, pngBytes, offset, nBytes);
        return offset + nBytes;
    }

    /**
     * Write a two-byte integer into the pngBytes array at a given position.
     *
     * @param n The integer to be written into pngBytes.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    protected int writeInt2(int n, int offset) {
        byte[] temp = {(byte) ((n >> 8) & 0xff), (byte) (n & 0xff)};
        return writeBytes(temp, offset);
    }

    /**
     * Write a four-byte integer into the pngBytes array at a given position.
     *
     * @param n The integer to be written into pngBytes.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    protected int writeInt4(int n, int offset) {
        byte[] temp = {(byte) ((n >> 24) & 0xff),
                (byte) ((n >> 16) & 0xff),
                (byte) ((n >> 8) & 0xff),
                (byte) (n & 0xff)};
        return writeBytes(temp, offset);
    }

    /**
     * Write a single byte into the pngBytes array at a given position.
     *
     * @param b The integer to be written into pngBytes.
     * @param offset The starting point to write to.
     * @return The next place to be written to in the pngBytes array.
     */
    protected int writeByte(int b, int offset) {
        byte[] temp = {(byte) b};
        return writeBytes(temp, offset);
    }

    /**
     * Write a PNG "IHDR" chunk into the pngBytes array.
     */
    protected void writeHeader() {
        int startPos;

        startPos = bytePos = writeInt4(13, bytePos);
        bytePos = writeBytes(IHDR, bytePos);
        width = image.getWidth();
        height = image.getHeight();
        bytePos = writeInt4(width, bytePos);
        bytePos = writeInt4(height, bytePos);
        bytePos = writeByte(1, bytePos); // bit depth
        //		bytePos = writeByte((encodeAlpha) ? 6 : 2, bytePos); // direct model
        // Colour Type GrayScale
        bytePos = writeByte(0, bytePos); // Palette

        // Compression Method
        bytePos = writeByte(0, bytePos); // compression method
        bytePos = writeByte(0, bytePos); // filter method
        bytePos = writeByte(0, bytePos); // no interlace
        crc.reset();
        crc.update(pngBytes, startPos, bytePos - startPos);
        crcValue = crc.getValue();
        bytePos = writeInt4((int) crcValue, bytePos);
    }

    /**
     * Perform "sub" filtering on the given row.
     * Uses temporary array leftBytes to store the original values
     * of the previous pixels.  The array is 16 bytes long, which
     * will easily hold two-byte samples plus two-byte alpha.
     *
     * @param pixels The array holding the scan lines being built
     * @param startPos Starting position within pixels of bytes to be filtered.
     * @param width Width of a scanline in pixels.

    protected void filterSub(byte[] pixels, int startPos, int width) {
        int i;
        int offset = bytesPerPixel;
        int actualStart = startPos + offset;
        int nBytes = width * bytesPerPixel;
        int leftInsert = offset;
        int leftExtract = 0;

        for (i = actualStart; i < startPos + nBytes; i++) {
            leftBytes[leftInsert] =  pixels[i];
            pixels[i] = (byte) ((pixels[i] - leftBytes[leftExtract]) % 256);
            leftInsert = (leftInsert + 1) % 0x0f;
            leftExtract = (leftExtract + 1) % 0x0f;
        }
    }
     */
    /**
     * Perform "up" filtering on the given row.
     * Side effect: refills the prior row with current row
     *
     * @param pixels The array holding the scan lines being built
     * @param startPos Starting position within pixels of bytes to be filtered.
     * @param width Width of a scanline in pixels.

    protected void filterUp(byte[] pixels, int startPos, int width) {
        int     i, nBytes;
        byte    currentByte;

        nBytes = width * bytesPerPixel;

        for (i = 0; i < nBytes; i++) {
            currentByte = pixels[startPos + i];
            pixels[startPos + i] = (byte) ((pixels[startPos  + i] - priorRow[i]) % 256);
            priorRow[i] = currentByte;
        }
    }
     */
    /**
     * Write the image data into the pngBytes array.
     * This will write one or more PNG "IDAT" chunks. In order
     * to conserve memory, this method grabs as many rows as will
     * fit into 32K bytes, or the whole image; whichever is less.
     *
     *
     * @return the compress image data or 0 length array on error
     */
    protected byte[] compressImageData() {
        //int rowsLeft = height;  // number of rows remaining to write
        //int startRow = 0;       // starting row to process this time through
        //int nRows;              // how many rows to grab at a time

        byte[] scanLines;       // the scan lines to be compressed
        int scanPos;            // where we are in the scan lines
        int startPos;           // where this line's actual pixels start (used for filtering)

        byte[] compressedLines; // the resultant compressed lines

        Deflater scrunch = new Deflater(compressionLevel);
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream(1024);

        DeflaterOutputStream compBytes = new DeflaterOutputStream(outBytes, scrunch);
        try {

                int[] pixels = new int[width * height];

                image.getPixels(pixels, 0, width, 0, 0, width, height);


                int scanLineLength = width/8;

                scanLines = new byte[scanLineLength * height +  height]; //nrows is the filter per line

                byte output=0x00;

                scanPos = 0;
                for (int i = 0; i < height; i++) {

                        //filter per line
                        scanLines[scanPos++] = (byte) filter;


                        for(int j=0;j<(width);j=j+8){
                            String b0= (pixels[i*width + j  ]==Color.BLACK)?"1":"0";
                            String b1= (pixels[i*width + j+1]==Color.BLACK)?"1":"0";
                            String b2= (pixels[i*width + j+2]==Color.BLACK)?"1":"0";
                            String b3= (pixels[i*width + j+3]==Color.BLACK)?"1":"0";
                            String b4= (pixels[i*width + j+4]==Color.BLACK)?"1":"0";
                            String b5= (pixels[i*width + j+5]==Color.BLACK)?"1":"0";
                            String b6= (pixels[i*width + j+6]==Color.BLACK)?"1":"0";
                            String b7= (pixels[i*width + j+7]==Color.BLACK)?"1":"0";

                            Byte b= (byte)(int)Integer.valueOf(b0+b1+b2+b3+b4+b5+b6+b7, 2);
                            scanLines[scanPos++]=b;
                        }

                        compBytes.write(scanLines, 0, scanPos);
                }


            compBytes.close();

                scrunch.finish();

            compressedLines = outBytes.toByteArray();

            return compressedLines;

        }
        catch (IOException e) {
            System.err.println(e.toString());
            return new byte[0];
        }
    }

    private void writeImageData(byte[] compressedLines) {

		/*
		 * Write the compressed bytes
		 */
        int nCompressed = compressedLines.length;

        crc.reset();
        bytePos = writeInt4(nCompressed, bytePos);
        bytePos = writeBytes(IDAT, bytePos);
        crc.update(IDAT);
        bytePos = writeBytes(compressedLines, nCompressed, bytePos);
        crc.update(compressedLines, 0, nCompressed);

        crcValue = crc.getValue();
        bytePos = writeInt4((int) crcValue, bytePos);
    }

    /**
     * Write a PNG "IEND" chunk into the pngBytes array.
     */
    protected void writeEnd() {
        bytePos = writeInt4(0, bytePos);
        bytePos = writeBytes(IEND, bytePos);
        crc.reset();
        crc.update(IEND);
        crcValue = crc.getValue();
        bytePos = writeInt4((int) crcValue, bytePos);
    }

    private byte[] createPebblePalette() {
        // PLTE = len + header + data + crc

        int cols=64;
        int max_cols= (int) Math.pow(2,1);

        cols=max_cols;

        int data_len=cols*3;

        int len=4+4+data_len+4;
        byte[] pal = new byte[len];

        // Add the length
        byte[] lenb=intToInt4Bytes(data_len);
        pal[0]=lenb[0];
        pal[1]=lenb[1];
        pal[2]=lenb[2];
        pal[3]=lenb[3];

        // Add the chunk type
        pal[4]='P';
        pal[5]='L';
        pal[6]='T';
        pal[7]='E';

        // Create the palette and add
        int pos=8;
        pal[pos]=(byte)Color.red(Color.BLACK);
        pal[pos+1]=(byte)Color.green(Color.BLACK);
        pal[pos+2]=(byte)Color.blue(Color.BLACK);
        pal[pos+3]=(byte)Color.red(Color.WHITE);
        pal[pos+4]=(byte)Color.green(Color.WHITE);
        pal[pos+5]=(byte)Color.blue(Color.WHITE);

        // Calc CRC and add
        CRC32 palcrc = new CRC32();
        palcrc.update(pal, 4, 4+data_len);
        long palcrcl = palcrc.getValue();
        //7801010b
        byte[] palcrcb = intToInt4Bytes((int)palcrcl);
        pal[4+4+data_len+0]=palcrcb[0];
        pal[4+4+data_len+1]=palcrcb[1];
        pal[4+4+data_len+2]=palcrcb[2];
        pal[4+4+data_len+3]=palcrcb[3];

        return pal;
    }

    private byte[] intToInt4Bytes(int n) {
        byte[] temp = {(byte) ((n >> 24) & 0xff),
                (byte) ((n >> 16) & 0xff),
                (byte) ((n >> 8) & 0xff),
                (byte) (n & 0xff)};
        return temp;
    }

    private static int ConvertTo6Bit(int value) {
        if (value < 43) return 0;
        if (value < 129) return 85;
        if (value < 213) return 170;
        else return 255;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void saveFile(String fileName) throws IOException {
        FileOutputStream fos = new FileOutputStream(fileName);
        fos.write(pngBytes);
        fos.close();

    }


    protected byte[] compressImageData2() {
        int depth=1;
        int rowsLeft = height;  // number of rows remaining to write
        int startRow = 0;       // starting row to process this time through
        int nRows;              // how many rows to grab at a time

        byte[] scanLines;       // the scan lines to be compressed
        int scanPos;            // where we are in the scan lines

        byte[] compressedLines; // the resultant compressed lines

        int bytesPerPixel = 1;// (encodeAlpha) ? 4 : 3;

        Deflater scrunch = new Deflater(compressionLevel);
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream(1024);

        DeflaterOutputStream compBytes = new DeflaterOutputStream(outBytes, scrunch);
        try {
            while (rowsLeft > 0) {

                // Pebble
                // Assume the image is small (i.e. has been resized before sending to no more that 168)
                nRows = height;

                int[] pixels = new int[width * nRows];

                image.getPixels(pixels, 0, width, 0, startRow, width, nRows);

                if (depth==8) {
                    scanLines = new byte[width * nRows * bytesPerPixel +  nRows];
                } else {
                    int pixels_per_byte = 8 / depth;
                    int scanLineLength = (width + (pixels_per_byte-width%pixels_per_byte))/pixels_per_byte;
                    Log.d(tag,"new scanLineLength=" + scanLineLength);
                    scanLines = new byte[scanLineLength * nRows * bytesPerPixel +  nRows];
                }



                int palette_count=0;
                int max = 8 /depth;
                int count = 1;
                byte output=0x00;
                int max_cols= (int) Math.pow(2,depth);


                scanPos = 0;
                for (int i = 0; i < width * nRows; i++) {
                    if (i % width == 0) {
                        scanLines[scanPos++] = (byte) filter;
                    }

                    if (depth==8) {
                        // For 6 bit (64 Colours), get 2 bit value for each of ARGB
                        // in Pebble dp1-4 Alpha is the just the top bit

                        byte pix = (byte) 0x00;
                        // Alpha isn't working yet
                        //					pix = (byte) (pix | ((byte)(Color.alpha(pixels[i])/64))<<6);
                        pix = (byte) (pix | ((byte)(Color.red(pixels[i])/64))<<4);
                        pix = (byte) (pix | ((byte)(Color.green(pixels[i])/64))<<2);
                        pix = (byte) (pix | ((byte)(Color.blue(pixels[i])/64)));
                        scanLines[scanPos++]=pix;
                    } else {



                        // For 4 bit (16 Colours) from Palette
                        // Each pixel is mapped to the relevant palette entry
                        // Assumes we already have a 16 colour image

                        // Create the palette as we go
                        Integer pvalue = 0;

                        if (depth==1) {
                            if (pixels[i] == Color.BLACK) {
                                pvalue = 0;
                            } else {
                                pvalue = 1;
                            }
                        } else {
                            pvalue= palette_map.get(pixels[i]);
                            if (pvalue == null) {
                                pvalue = palette_count;
                                palette_map.put(pixels[i], palette_count);
                                palette_count++;
                            }

                            // Warn if the palette is too big
                            if (palette_count >= max_cols) {
                                Log.d(tag, "Bad colour image - more than " + max_cols + " colours! (" + palette_count + ")");
                                pvalue = 0;
                            }
                        }

                        int shift = 8 - (count * depth);
                        byte pix = (byte)pvalue.intValue();
                        output = (byte) (output | (pix<<shift));
                        if (count==max) {
                            scanLines[scanPos]=output;
                            scanPos++;
                            output=0x00;
                            count=1;
                        }
                        else {
                            if ((i!=0) && ((i+1)%width==0)) {
                                scanLines[scanPos]=output;
                                scanPos++;
                                output=0x00;
                                count=1;
                            } else {
                                count++;
                            }
                        }

                    }
                }
				/*
				 * Write these lines to the output area
				 */
                compBytes.write(scanLines, 0, scanPos);

                startRow += nRows;
                rowsLeft -= nRows;
            }
            compBytes.close();
            scrunch.finish();

            compressedLines = outBytes.toByteArray();

            return compressedLines;

        }
        catch (IOException e) {
            System.err.println(e.toString());
            return new byte[0];
        }
    }
}
