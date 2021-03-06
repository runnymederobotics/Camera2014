package dashboard2014;

import static dashboard2014.CircleFinder.BALL_DIAMETER_INCHES;
import static dashboard2014.RectangleFinder.HOT_GOAL_TARGET_WIDTH_INCHES;
import java.awt.Color;
import java.awt.Graphics2D;

abstract class Finder {

    Color colour;
    int imageWidth, imageHeight;
    int xLongest, yLongest;
    int longestWidth, longestHeight;
    int xFinishLast, yLast;
    boolean aboveRed, aboveGreen, aboveBlue;
    double actualWidthInches;

    public Finder(Color colour, boolean aboveRed, boolean aboveGreen, boolean aboveBlue) {
        this.colour = colour;

        /*int redColour = colour.getRed(), greenColour = colour.getGreen(), blueColour = colour.getBlue();

         int biggestComponent = Math.max(redColour, Math.max(greenColour, blueColour));

         aboveRed = biggestComponent == redColour;
         aboveGreen = biggestComponent == greenColour;
         aboveBlue = biggestComponent == blueColour;*/
        this.aboveRed = aboveRed;
        this.aboveGreen = aboveGreen;
        this.aboveBlue = aboveBlue;

        reset();
    }

    public void setImageSize(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public void reset() {
        xLongest = 0;
        yLongest = 0;

        longestWidth = 0;
        longestHeight = 0;

        xFinishLast = 0;
        yLast = 0;
    }

    public int getX() {
        return xLongest;
    }

    public int getY() {
        return yLongest;
    }

    public int getWidth() {
        return longestWidth;
    }

    public int getHeight() {
        return longestHeight;
    }

    //Checks whether the specified pixel is in range for this finder
    protected boolean checkThreshold(int rgb) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;

        int redColour = colour.getRed(), greenColour = colour.getGreen(), blueColour = colour.getBlue();

        boolean redInRange, greenInRange, blueInRange;

        if (aboveRed) {
            redInRange = r > redColour;
        } else {
            redInRange = r < redColour;
        }

        if (aboveGreen) {
            greenInRange = g > greenColour;
        } else {
            greenInRange = g < greenColour;
        }

        if (aboveBlue) {
            blueInRange = b > blueColour;
        } else {
            blueInRange = b < blueColour;
        }

        return redInRange && greenInRange && blueInRange;
    }

    public boolean doProcessing(int pixels[], int filteredPixels[], int x, int y, int rgb) {
        if (checkThreshold(rgb)) {
            //If we're in a different "chunk" of this horizontal slice, or if we've moved to the next line
            if (x > xFinishLast || y > yLast) {
                int xFinish, yFinish, yStart;
                //Start at the first x value of the current "chunk" of this horizontal slice
                //Walk through this chunk until we find a pixel outside of our threshold
                for (xFinish = x; xFinish < imageWidth - 1 && checkThreshold(pixels[y * imageWidth + xFinish]); xFinish++) {
                }

                //We now have the left and right endpoints of this horizontal chunk

                //Find the midpoint of the horizontal chunk
                int xMid = (x + xFinish) / 2;

                //Start at the current y value, and walk down the image until we get outside the threshold
                //The x-value we're walking at is the midpoint we found earlier
                for (yFinish = y; yFinish < imageHeight - 1 && checkThreshold(pixels[yFinish * imageWidth + xMid]); yFinish++) {
                }

                //Start at the current y value, and walk up the image until we get outside the threshold
                //The x-value we're walking at is the midpoint we found earlier
                for (yStart = y; yStart >= 0 && checkThreshold(pixels[yStart * imageWidth + xMid]); yStart--) {
                }

                //Find the diameter of the horiztonal chunk
                int xLength = Math.abs(xFinish - x);
                //Find the length to the bottom and top of the ball
                int yLengthDown = Math.abs(yFinish - y), yLengthUp = Math.abs(y - yStart);

                //Determine if this sequence is the defining factor of the shape we are searching for
                checkIfLongest(xMid, y, xLength, yLengthDown, yLengthUp);

                //Record where the endpoint of the current chunk is
                //Record the current y-position of this chunk
                //These are used to know when to start looking for the next chunk
                //Either the next chunk will be on the same line with a higher x value, or the next line
                xFinishLast = xFinish;
                yLast = y;
            }
            return true;
        } else {
            return false;
        }
    }

    //Determines the distance, in inches of the shape
    public double getDistance() {
        int x = getX(), y = getY();

        //Find the distance of the ball from the center of the image
        double deltaX = x - imageWidth / 2;
        //Approximate the angle from the middle of the image to the ball
        double angleToBall = CameraPanel.FIELD_OF_VIEW / imageWidth * deltaX;
        //Use trig to find the distance from camera to ball, in pixels
        double distanceInPixels = deltaX / (Math.tan(Math.toRadians(angleToBall)));
        //Find ratio of inches to pixels
        final double INCHES_PER_PIXEL = actualWidthInches / getWidth();
        //Convert pixels to inches
        double distanceInInches = distanceInPixels * INCHES_PER_PIXEL;

        return distanceInInches;
    }

    //public abstract boolean doProcessing(int pixels[], int filteredPixels[], int x, int y, int rgb);
    public abstract void checkIfLongest(int xMid, int yMid, int xLength, int yLengthDown, int yLengthUp);

    public abstract void draw(Graphics2D g);
}

class CircleFinder extends Finder {

    public static final double BALL_DIAMETER_INCHES = 24.0;
    int longestRadius;

    public CircleFinder(Color colour, boolean aboveRed, boolean aboveGreen, boolean aboveBlue) {
        super(colour, aboveRed, aboveGreen, aboveBlue);
        actualWidthInches = BALL_DIAMETER_INCHES;
    }

    public int getRadius() {
        return longestRadius;//Math.min(longestHorizontal, longestVertical) / 2;
    }

    @Override
    public int getWidth() {
        return longestRadius * 2;
    }

    @Override
    public int getHeight() {
        return longestRadius * 2;
    }

    @Override
    public void reset() {
        super.reset();
        longestRadius = 0;
    }

    @Override
    public void checkIfLongest(int xMid, int yMid, int xLength, int yLengthDown, int yLengthUp) {
        //Take the smaller vertical length
        int yLength = Math.min(yLengthDown, yLengthUp);

        //Take the average of the x-radius and y-radius
        //xLength is the diameter so divide it by 2
        int currentRadius = (xLength / 2 + yLength) / 2;

        //If we've found a new longest horizontal chunk then record it and its midpoint
        //If we've found a new longest vertical chunk then record it and its midpoint
        if (currentRadius > longestRadius) {
            xLongest = xMid; //x is at the left of the ball, xMid is in the middle
            yLongest = yMid; //y is in the middle of the ball
            longestRadius = currentRadius;
        }
    }

    @Override
    public void draw(Graphics2D g) {
        int x = getX(), y = getY(), radius = getRadius();

        g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
    }
}

class RectangleFinder extends Finder {

    public static final double HOT_GOAL_TARGET_WIDTH_INCHES = 23.5;

    public RectangleFinder(Color colour, boolean aboveRed, boolean aboveGreen, boolean aboveBlue) {
        super(colour, aboveRed, aboveGreen, aboveBlue);
        actualWidthInches = HOT_GOAL_TARGET_WIDTH_INCHES;
    }

    @Override
    public void checkIfLongest(int xMid, int yMid, int xLength, int yLengthDown, int yLengthUp) {
        //Take the smaller vertical length
        int yLength = yLengthDown + yLengthUp;

        //Take the average of the x-radius and y-radius
        //xLength is the diameter so divide it by 2
        int currentWidth = xLength, currentHeight = yLength;

        //If we've found a new longest horizontal chunk then record it and its midpoint
        //If we've found a new longest vertical chunk then record it and its midpoint
        if (currentWidth * currentHeight > longestWidth * longestHeight) {
            xLongest = xMid; //x is at the left of the ball, xMid is in the middle
            yLongest = yMid; //y is in the middle of the ball
            longestWidth = currentWidth;
            longestHeight = currentHeight;
        }
    }

    @Override
    public void draw(Graphics2D g) {
        int x = getX(), y = getY(), width = getWidth(), height = getHeight();

        g.drawRect(x - width / 2, y - height / 2, width, height);
    }
}