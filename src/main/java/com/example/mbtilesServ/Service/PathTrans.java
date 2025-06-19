package com.example.mbtilesServ.Service;

import java.util.ArrayList;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Polyline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.shape.ShapeHelper;

/**
 * https://github.com/openjdk/jfx/blob/master/modules/javafx.graphics/src/main/java/javafx/animation/PathTransition.java
 * https://github.com/teamfx/openjfx-8u-dev-rt/blob/master/modules/graphics/src/main/java/javafx/animation/PathTransition.java
 */

public class PathTrans {

    public double totalLength = 0;
    private final ArrayList<Segment> segments = new ArrayList<>();
    private static final int SMOOTH_ZONE = 10;

    /**
     * The constructor
     * @param pts
     */
    public PathTrans(double[] pts) {
        Polyline polyline = new Polyline(pts);
        setPath(polyline);
        recomputeSegments();
    }

    /**
     * The shape on which outline the node should be animated.
     * <p>
     * It is not possible to change the {@code path} of a running
     * {@code PathTransition}. If the value of {@code path} is changed for a
     * running {@code PathTransition}, the animation has to be stopped and
     * started again to pick up the new value.
     *
     * @defaultValue null
     */
    private ObjectProperty<Shape> path;
    private static final Shape DEFAULT_PATH = null;

    public final void setPath(Shape value) {
        if ((path != null) || (value != null /* DEFAULT_PATH */)) {
            pathProperty().set(value);
        }
    }

    public final Shape getPath() {
        return (path == null)? DEFAULT_PATH : path.get();
    }

    public final ObjectProperty<Shape> pathProperty() {
        if (path == null) {
            path = new SimpleObjectProperty<>(this, "path", DEFAULT_PATH);
        }
        return path;
    }

    public trans interpolate(double frac) {
        double part = totalLength * Math.min(1, Math.max(0, frac));
        int segIdx = findSegment(0, segments.size() - 1, part);
        Segment seg = segments.get(segIdx);

        double lengthBefore = seg.accumLength - seg.length;

        double partLength = part - lengthBefore;

        double ratio = partLength / seg.length;
        Segment prevSeg = seg.prevSeg;
        double x = prevSeg.toX + (seg.toX - prevSeg.toX) * ratio;
        double y = prevSeg.toY + (seg.toY - prevSeg.toY) * ratio;
        double rotateAngle = seg.rotateAngle;

        // provide smooth rotation on segment bounds
        double z = Math.min(SMOOTH_ZONE, seg.length / 2);
        if (partLength < z && !prevSeg.isMoveTo) {
            //interpolate rotation to previous segment
            rotateAngle = interpolate(
                    prevSeg.rotateAngle, seg.rotateAngle,
                    partLength / z / 2 + 0.5F);
        } else {
            double dist = seg.length - partLength;
            Segment nextSeg = seg.nextSeg;
            if (dist < z && nextSeg != null) {
                //interpolate rotation to next segment
                if (!nextSeg.isMoveTo) {
                    rotateAngle = interpolate(
                            seg.rotateAngle, nextSeg.rotateAngle,
                            (z - dist) / z / 2);
                }
            }
        }
        return new trans(x,y,rotateAngle);
    }

    public class trans {
        public double x = 0;
        public double y = 0;
        public double rotateAngle = 0;
        /**
         *
         * @param x
         * @param y
         * @param rotateAngle
         */
        trans (double x, double y, double rotateAngle)
        {
            this.x = x;
            this.y = y;
            this.rotateAngle = rotateAngle;
        }
    }

    private void recomputeSegments() {
        segments.clear();
        final Shape p = getPath();
        Segment moveToSeg = Segment.getZeroSegment();
        Segment lastSeg = Segment.getZeroSegment();

        float[] coords = new float[6];
        for (PathIterator i = ShapeHelper.configShape(p).getPathIterator(NodeHelper.getLeafTransform(p), 1.0f); !i.isDone(); i.next()) {
            Segment newSeg = null;
            int segType = i.currentSegment(coords);
            double x = coords[0];
            double y = coords[1];

            switch (segType) {
                case PathIterator.SEG_MOVETO:
                    moveToSeg = Segment.newMoveTo(x, y, lastSeg.accumLength);
                    newSeg = moveToSeg;
                    break;
                case PathIterator.SEG_CLOSE:
                    newSeg = Segment.newClosePath(lastSeg, moveToSeg);
                    if (newSeg == null) {
                        // make the last segment to close the path
                        lastSeg.convertToClosePath(moveToSeg);
                    }
                    break;
                case PathIterator.SEG_LINETO:
                    newSeg = Segment.newLineTo(lastSeg, x, y);
                    break;
            }

            if (newSeg != null) {
                segments.add(newSeg);
                lastSeg = newSeg;
            }
        }
        totalLength = lastSeg.accumLength;
    }

    /**
     * Returns the index of the first segment having accumulated length
     * from the path beginning, greater than {@code length}
     */
    private int findSegment(int begin, int end, double length) {
        // check for search termination
        if (begin == end) {
            // find last non-moveTo segment for given length
            return segments.get(begin).isMoveTo && begin > 0
                    ? findSegment(begin - 1, begin - 1, length)
                    : begin;
        }
        // otherwise continue binary search
        int middle = begin + (end - begin) / 2;
        return segments.get(middle).accumLength > length
                ? findSegment(begin, middle, length)
                : findSegment(middle + 1, end, length);
    }


    /** Interpolates angle according to rate,
     *  with correct 0->360 and 360->0 transitions
     */
    private static double interpolate(double fromAngle, double toAngle, double ratio) {
        double delta = toAngle - fromAngle;
        if (Math.abs(delta) > 180) {
            toAngle += delta > 0 ? -360 : 360;
        }
        return normalize(fromAngle + ratio * (toAngle - fromAngle));
    }

    /** Converts angle to range 0-360
     */
    private static double normalize(double angle) {
        while (angle > 360) {
            angle -= 360;
        }
        while (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    private static class Segment {

        private static final Segment zeroSegment = new Segment(true, 0, 0, 0, 0, 0);
        boolean isMoveTo;
        double length;
        // total length from the path's beginning to the end of this segment
        double accumLength;
        // end point of this segment
        double toX;
        double toY;
        // segment's rotation angle in degrees
        double rotateAngle;
        Segment prevSeg;
        Segment nextSeg;

        private Segment(boolean isMoveTo, double toX, double toY,
                        double length, double lengthBefore, double rotateAngle) {
            this.isMoveTo = isMoveTo;
            this.toX = toX;
            this.toY = toY;
            this.length = length;
            this.accumLength = lengthBefore + length;
            this.rotateAngle = rotateAngle;
        }

        public static Segment getZeroSegment() {
            return zeroSegment;
        }

        public static Segment newMoveTo(double toX, double toY,
                                        double accumLength) {
            return new Segment(true, toX, toY, 0, accumLength, 0);
        }

        public static Segment newLineTo(Segment fromSeg, double toX, double toY) {
            double deltaX = toX - fromSeg.toX;
            double deltaY = toY - fromSeg.toY;
            double length = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
            if ((length >= 1) || fromSeg.isMoveTo) { // filtering out flattening noise
                double sign = Math.signum(deltaY == 0 ? deltaX : deltaY);
                double angle = (sign * Math.acos(deltaX / length));
                angle = normalize(angle / Math.PI * 180);
                Segment newSeg = new Segment(false, toX, toY,
                        length, fromSeg.accumLength, angle);
                fromSeg.nextSeg = newSeg;
                newSeg.prevSeg = fromSeg;
                return newSeg;
            }
            return null;
        }

        public static Segment newClosePath(Segment fromSeg, Segment moveToSeg) {
            Segment newSeg = newLineTo(fromSeg, moveToSeg.toX, moveToSeg.toY);
            if (newSeg != null) {
                newSeg.convertToClosePath(moveToSeg);
            }
            return newSeg;
        }

        public void convertToClosePath(Segment moveToSeg) {
            Segment firstLineToSeg = moveToSeg.nextSeg;
            nextSeg = firstLineToSeg;
            firstLineToSeg.prevSeg = this;
        }
    }
}
