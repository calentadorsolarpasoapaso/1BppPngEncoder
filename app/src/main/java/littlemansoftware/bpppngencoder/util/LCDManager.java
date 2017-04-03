package littlemansoftware.bpppngencoder.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import static android.R.attr.textSize;

/**
 * Created by Jose on 02/04/2017.
 */

public class LCDManager {
    static final String TAG = LCDManager.class.getName();

    private Context context;

    public LCDManager(Context context) {
        this.context = context;
    }


    public Bitmap drawText(String[] lines, int lcdWidth, int lcdHeight) {
        String textToPrint = calculateTextToPrint(lines);
        // up to 4 lines supported
        int fontSize=calculateFontSize(lines.length);

        Log.v(TAG, "Generating Bitmap with LCD Capabilities. Width: " + lcdWidth + " height: " + lcdHeight + " TextSize: " + textSize + " Text: " + textToPrint);

        Typeface fontAsset = Typeface.createFromAsset(context.getAssets(), "MONOS.TTF");
        Typeface typeFaceDefault = Typeface.create(fontAsset, Typeface.NORMAL);

        StaticLayout mTextLayout = buildStaticLayout(fontSize,textToPrint,lcdWidth,typeFaceDefault);

        int lNewLines=mTextLayout.getLineCount();

        // There are more lines, recalculate size
        if(lNewLines>lines.length){
            mTextLayout=buildStaticLayout(calculateFontSize(lNewLines),textToPrint,lcdWidth,typeFaceDefault);
        }

        // Create bitmap and canvas to draw to
        Bitmap b = Bitmap.createBitmap(lcdWidth, lcdHeight, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(b);

        // Draw background
//        Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG );
//        paintFill.setTypeface(typeFaceDefault);
//        paintFill.setStyle(Paint.Style.FILL);
//        paintFill.setColor(Color.BLACK);
//        paintFill.setTextAlign(Paint.Align.CENTER);
//        c.drawPaint(paintFill);

        // Border in canvas
        Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG );
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(2); // set stroke width
        paintStroke.setColor(Color.WHITE);
        c.drawRect(0, 0, lcdWidth, lcdHeight, paintStroke);

        // Draw text
        c.save();
        float yAxis = (lcdHeight - mTextLayout.getHeight()) / 2;
        c.translate(0, yAxis);
        mTextLayout.draw(c);
        c.restore();

        return b;
    }

    private int calculateFontSize(int iNumberLines) {
        int size=25;
        switch (iNumberLines){
            case 1: size=25;break; // size=30
            case 2: size= 25;break;
            case 3: size=18;break;
            case 4:size=8;break;
            case 5:size=6;break;

        }

        return size;
    }

    private String calculateTextToPrint(String[] lines ) {
        String textToPrint="";

        Log.d(TAG, "DisplayScreen Lines: " + lines.length);
        for(int i=0;i<lines.length;i++) {
            Log.d(TAG, "DisplayScreen Line " + i +": " + lines[i]);
            textToPrint+=lines[i];
            if(i<lines.length-1) textToPrint+=System.getProperty("line.separator");
        }
        return textToPrint;
    }

    public StaticLayout buildStaticLayout(int fontSize, String textToPrint, int lcdWidth, Typeface typeFaceDefault){
        TextPaint textPaint = new TextPaint();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(fontSize);
        textPaint.setTypeface(typeFaceDefault);
        textPaint.setFakeBoldText(true);

        StaticLayout mTextLayout = new StaticLayout(textToPrint, textPaint, lcdWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);

        return mTextLayout;
    }

}
