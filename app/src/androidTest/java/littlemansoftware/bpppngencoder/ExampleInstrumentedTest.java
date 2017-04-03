package littlemansoftware.bpppngencoder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.ActivityCompat;

import org.junit.Test;
import org.junit.runner.RunWith;

import littlemansoftware.bpppngencoder.util.BppPngEncoder;
import littlemansoftware.bpppngencoder.util.LCDManager;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("littlemansoftware.a1bpppngencoder", appContext.getPackageName());
    }

    @Test
    public void compressPNG() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();

        LCDManager lcd=new LCDManager(appContext);

        Bitmap imageText=lcd.drawText(new String[]{String.format("Cash Ticket %s\nAnnulled %s", "NEW10100aa", String.valueOf(200))},240,64);

        BppPngEncoder encoder=new BppPngEncoder();
        encoder.setImage(imageText);
        encoder.setCompressionLevel(9);
        byte[] bytes=encoder.pngEncode();

        encoder.saveFile(Environment.getExternalStorageDirectory().getPath() + "/Image.png");

        assertTrue("Size<1k",bytes.length<1024);
    }

    @Test
    public void compressPNGSmallFont() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();

        LCDManager lcd=new LCDManager(appContext);

        Bitmap imageText=lcd.drawText(new String[]{"a b c d e f g h i j k","1 2 3 4 5 5 6 6 7 8 ", "! % $ ^ & $ $ ! ","12321242312"},240,64);

        BppPngEncoder encoder=new BppPngEncoder();
        encoder.setImage(imageText);
        encoder.setCompressionLevel(9);
        byte[] bytes=encoder.pngEncode();

        encoder.saveFile(Environment.getExternalStorageDirectory().getPath() + "/ImageSmall.png");

        assertTrue("Size<1k",bytes.length<1024);
    }

    @Test
    public void compressPNGBlackImg() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();

        LCDManager lcd=new LCDManager(appContext);

        Bitmap imageText=lcd.drawText(new String[]{""},240,64);

        BppPngEncoder encoder=new BppPngEncoder();
        encoder.setImage(imageText);
        encoder.setCompressionLevel(9);
        byte[] bytes=encoder.pngEncode();

        encoder.saveFile(Environment.getExternalStorageDirectory().getPath() + "/ImageBlack.png");

        assertTrue("Size<1k",bytes.length<1024);

    }

}
