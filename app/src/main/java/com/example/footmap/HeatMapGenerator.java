package com.example.footmap;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Created by srq on 1/11/17.
 */

public class HeatMapGenerator {
    static double minimumDistance(float x, float  y, float x1, float y1, float x2, float y2) {
        float A = x - x1;
        float B = y - y1;
        float C = x2 - x1;
        float D = y2 - y1;

        float dot = A * C + B * D;
        float len_sq = C * C + D * D;
        float param = -1;
        if (len_sq != 0) //in case of 0 length line
            param = dot / len_sq;

        float xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        }
        else if (param > 1) {
            xx = x2;
            yy = y2;
        }
        else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        float dx = x - xx;
        float dy = y - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    static double distancePoints(float x1, float y1, float x2, float y2) {
        return Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));

    }

    static final int x[]={108,237,294,296,260,195};
    static final int y[]={132,150,272,400,519,584};
    static final int maxDistance=115;
    static final int gridSizeX=371;
    static final int gridSizeY=698;

    static double truncator(double x) {
        return x<0?0:x;
    }

    static Bitmap generateGrid(int[] z, Bitmap mask) {
        if(z==null)
            return null;
        if(z.length!=6)
            return null;
        Bitmap heatMap = Bitmap.createBitmap(gridSizeX, gridSizeY, Bitmap.Config.ARGB_8888);

        if(mask==null)
            return null;
        if(mask.getWidth()!=371)
            return null;
        if(mask.getHeight()!=698)
            return null;

        for(int i=0;i<gridSizeX;i++) {
            for(int j=0;j<gridSizeY;j++) {
                double minimumDistance=9999;
                int minimumLineSegmentIndex=-1;

                for(int k=0;k<4;k++) {
                    double newDistance=minimumDistance(i,j,x[k],y[k],x[k+1],y[k+1]);
                    if(newDistance<minimumDistance) {
                        minimumDistance = newDistance;
                        minimumLineSegmentIndex=k;
                    }
                }
                if(minimumLineSegmentIndex==-1)
                    continue;

                double distancePoint1=distancePoints(i,j,x[minimumLineSegmentIndex],y[minimumLineSegmentIndex]);
                double distancePoint2=distancePoints(i,j,x[minimumLineSegmentIndex+1],y[minimumLineSegmentIndex+1]);

                double value=(truncator(maxDistance-distancePoint1)*z[minimumLineSegmentIndex]+truncator(maxDistance-distancePoint2)*z[minimumLineSegmentIndex+1])/(truncator(maxDistance-distancePoint1)+truncator(maxDistance-distancePoint2));

                value=minimumDistance*(0-value)/(maxDistance-0)+value;

                int rgb=(int)(value/4);

                int alpha=Color.red(mask.getPixel(i,j));

                heatMap.setPixel(i, j, Color.argb(alpha, 255, rgb, 0));
            }
        }

        return heatMap;
    }
}
