package com.example.mbtilesServ.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.zip.GZIPInputStream;

import com.example.mbtilesServ.Model.Tile;
import org.apache.commons.io.IOUtils;

import no.ecc.vectortile.VectorTileDecoder;
import org.apache.commons.lang3.ArrayUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;
import javax.imageio.ImageIO;

import javax.vecmath.Vector2d;

import java.awt.Image;
import java.awt.Font;
import java.awt.font.GlyphVector;
import java.awt.BasicStroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

@Service
public class Processor {

    static int imgSizeBase = 384;
    int imgSize = imgSizeBase;
    float scale = (float)imgSize / 256;
    int zoom = 0;
    float widthScale = 1;

    Color background = new Color(242,239,233);
    BufferedImage dummy_image = SetImg(32, background);

    Coordinate[] edges = new Coordinate[] {};
    final Coordinate[] rect = new Coordinate[] {
            new Coordinate(-0.5, -0.5),
            new Coordinate(257, -0.5),
            new Coordinate(257, 257),
            new Coordinate(-0.5, 257),
            new Coordinate(-0.5, -0.5),
    };
    final Polygon tilePoly = new GeometryFactory().createPolygon(rect);

    // draw order
    // https://openmaptiles.org/inspect/
    List<String> layers = Arrays.asList (
            "water",
            "landcover",
            "landuse",
            //"mountain_peak",
            "waterway",
            "park",
            "boundary",
            //"aeroway",
            "transportation",
            "building",
            //"water_name",
            //"transportation_name",
            //"place",
            //"housenumber",
            //"poi",
            //"aerodrome_label",
            "globallandcover"
    );

    // https://openmaptiles.org/schema/
    HashMap<String, Style> StyleMap = new HashMap<String, Style>() {{
        // polygons
        put("polygon_default", new Style(new Color(230,230,230)));
        put("landuse", new Style(new Color(223,223,223)));
        put("farmland", new Style(new Color(201,225,191)));
        put("grass", new Style(new Color(205,235,176)));
        put("wood", new Style(new Color(173,209,158)));
        put("sand", new Style(new Color(255,241,186)));
        put("ice", new Style(new Color(236,235,230)));
        put("water", new Style(new Color(170,211,223)));
        put("building", new Style(new Color(210,200,190)));
        put("polygon_path", new Style(new Color(221,221,232)));
        // lines
        put("line_default", new Style(Color.white,0.1f, true));
        put("waterway", new Style(new Color(170,211,223),0.2f, true));
        put("boundary_solid", new Style(new Color(156,156,181),2.f, false));
        put("boundary_dash", new Style(new Color(156,156,181),2.f, false, -1, BasicStroke.CAP_BUTT, new float[]{30.f,15.f}));
        put("boundary_dot", new Style(new Color(156,156,181),2.f, false, -1, BasicStroke.CAP_BUTT, new float[]{5.f,40.f}, 10.f));
        put("park", new Style(new Color(100,163,86),5.f, false, 0.4f));
        put("trunk", new Style(new Color(192,192,128),0.25f, true));
        put("primary", new Style(new Color(192,192,128),0.23f, true));
        put("secondary", new Style(new Color(192,192,128),0.21f, true));
        put("tertiary", new Style(new Color(192,192,128),0.18f, true));
        put("bridge", new Style(new Color(180,180,180),0.5f, true, -1, BasicStroke.CAP_BUTT));
        put("bridge_path", new Style(new Color(180,180,180),0.4f, true, -1, BasicStroke.CAP_BUTT));
        put("bridge_rail", new Style(Color.darkGray,10.f, false, -1, BasicStroke.CAP_BUTT));
        put("rail", new Style(new Color(112,112,112),5.f, false, -1, BasicStroke.CAP_BUTT));
        put("rail_dash", new Style(Color.lightGray,3.f, false, -1, BasicStroke.CAP_BUTT, new float[]{16.f,16.f}));
        put("tram", new Style(new Color(110,110,110),0.1f, true));
        put("railway", new Style(new Color(110,110,110),0.1f, true));
        put("steps", new Style(Color.gray,4.f, false, -1, BasicStroke.CAP_BUTT, new float[]{2.f,2.f}));
        put("pedestrian", new Style(Color.gray,0.5f, false, -1, BasicStroke.CAP_BUTT));
        put("path", new Style(Color.gray,2.f, false, -1, BasicStroke.CAP_BUTT, new float[]{6.f,6.f}));
        put("subway", new Style(Color.gray,3.f, false, -1, BasicStroke.CAP_BUTT, new float[]{8.f,8.f}));
        put("disused", new Style(Color.lightGray,2.f, false, -1, BasicStroke.CAP_BUTT, new float[]{4.f,6.f}));
        // ...
        //construction
        //service
        //ventilation_shaft
        //platform
        //razed
        //abandoned
        //minor
        // ...
    }};

    public Processor(){};

    public static class OverZoom
    {
        public int z;
        public int x;
        public int y;
        public int imgSize;
        public int tileSize;
        public int offsetX = 0;
        public int offsetY = 0;
        public int exponent = 0;
        public int zoom = 0;

        public OverZoom(int zoom, int X, int Y, int max_zoom)
        {
            this.zoom = zoom;
            this.exponent = zoom > max_zoom ? zoom - max_zoom : 0;
            this.z = zoom - this.exponent;
            this.imgSize = Math.min(imgSizeBase * (1 << this.exponent),4096);
            this.tileSize = imgSize / (1 << this.exponent);
            int factor = (1 << this.exponent); // 2^(zoom - max_zoom)
            this.x = X / factor;
            this.y = Y / factor;
            this.offsetX = (X % factor) * this.tileSize;
            this.offsetY = (Y % factor) * this.tileSize;
        }
    }

    public byte[] vectorToRaster(OverZoom oz, List<Tile> data) throws IOException {
        int Z = oz.z;
        int X = oz.x;
        int Y = oz.y;
        imgSize = oz.imgSize;
        scale = (float)imgSize / 256;
        zoom = Z;
        widthScale = (float)zoom * (float)imgSize/imgSizeBase;
        updStreetBounds();

        HashMap<String, byte[]> tiles = data.stream().map((rs) -> new tile(rs.getZoom(),rs.getColumn(), rs.getRow(), rs.getTileData()))
                .collect(Collector.of(HashMap::new, (map, item) -> {
                    map.put(item.getId(), item.getBlob());
                }, (l, r) -> {
                    l.putAll(r);
                    return l;
                }, Characteristics.IDENTITY_FINISH));

        BufferedImage bimage = SetImg(imgSize, background);
        String id = Z + "_" + X + "_" + ((1 << Z) - Y - 1);

        VectorTileDecoder vectorTileDecoder = new VectorTileDecoder();

        for (String layerName : layers) {
            VectorTileDecoder.FeatureIterable decode = vectorTileDecoder.decode(tiles.get(id),layerName);
            if (layerName.equals("transportation"))
                for(VectorTileDecoder.Feature Feature : decode)
                {
                    AtomicReference<Map<String, Object>> ref = new AtomicReference<>(Feature.getAttributes());
                    if(isBrunnel(ref))
                    {
                        Geometry geometry = Feature.getGeometry();
                        draw(geometry, layerName, ref.get(), bimage);
                    }
                }
            for(VectorTileDecoder.Feature Feature : decode)
            {
                Geometry geometry = Feature.getGeometry();
                Map<String, Object> attributes = Feature.getAttributes();
                draw(geometry, layerName, attributes, bimage);
            }
        }

        /*if (oz.zoom < 15)
            drawPlaceNames(Z, X, Y, tiles, bimage);
        else
            drawAddresses(Z, X, Y, tiles, bimage);*/

        if (oz.exponent == 0)
        {
            drawPlaceNames(Z, X, Y, tiles, bimage);
        }
        else
        {
            drawAddresses(Z, X, Y, tiles, bimage);
            bimage = bimage.getSubimage(oz.offsetX, oz.offsetY, oz.tileSize, oz.tileSize);
        }

        BufferedImage setImgSize = resizeImage(bimage, 256, 256);
        return getBytes(setImgSize);
    }

    private BufferedImage SetImg(int size, Color color)
    {
        int w = size;
        int h = size;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        image.setAccelerationPriority(1.0f);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        return image;
    }

    public byte[] getBytes(BufferedImage img)
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", output);
        } catch (Exception ignored){
            try {
                ImageIO.write(dummy_image, "png", output);
            } catch (IOException ignored1) {}
        }
        return output.toByteArray();
    }

    public BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_AREA_AVERAGING);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
        return outputImage;
    }

    public byte[] cropImage(OverZoom oz, byte[] tile) throws IOException {
        int f = oz.imgSize/256;
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(tile));
        image = image.getSubimage(oz.offsetX / f, oz.offsetY / f, Math.max(oz.tileSize / f, 1), Math.max(oz.tileSize / f, 1));
        BufferedImage setImgSize = resizeImage(image, 256, 256);
        return getBytes(setImgSize);
    }

    // BRidge tUNNEL
    private boolean isBrunnel(AtomicReference<Map<String, Object>> ref)
    {
        boolean ret = false;
        Map<String, Object> attributes = ref.get();
        Map<String, Object> attr = new HashMap<>();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            attr.put(entry.getKey(), entry.getValue());
        }
        try {
            String brunnel = attributes.get("brunnel").toString();
            if (brunnel.equals("bridge")) {
                String _class = attributes.get("class").toString();
                switch (_class) {
                    case ("path"):
                        _class = "bridge_path";
                        break;
                    case ("steps"):
                        _class = "bridge_path";
                        break;
                    case ("rail"):
                        _class = "bridge_rail";
                        break;
                    default:
                        _class = "bridge";
                }
                attr.put("class", _class);
                ref.set(attr);
                ret = true;
            }
        }catch(Exception ignored){}
        return ret;
    }

    private void draw(Geometry geometry, String layerName, Map<String, Object> attributes, BufferedImage bimage)
    {
        if (geometry.getClass().equals(GeometryCollection.class)) {
            for (int n = 0; n < geometry.getNumGeometries(); n++) {
                Geometry geometryN = geometry.getGeometryN(n);
                draw(geometryN, layerName, attributes, bimage);
            }
        }

        if (geometry.getClass().equals(MultiPolygon.class)) {
            for (int n = 0; n < geometry.getNumGeometries(); n++) {
                Geometry geometryN = geometry.getGeometryN(n);
                draw(geometryN, layerName, attributes, bimage);
            }
        }

        if (geometry.getClass().equals(MultiLineString.class)) {
            for (int n = 0; n < geometry.getNumGeometries(); n++) {
                Geometry geometryN = geometry.getGeometryN(n);
                draw(geometryN, layerName, attributes, bimage);
            }
        }

        AtomicReference<String> ref = new AtomicReference<>("");
        setStyleName(layerName, ref, attributes, geometry.getGeometryType());
        String styleName = ref.get();

        java.awt.Polygon p = new java.awt.Polygon();
        Coordinate[] coordinates;
        Graphics2D graphics = (Graphics2D)bimage.getGraphics();
        //graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (geometry.getClass().equals(Polygon.class) && isDrawable(styleName)) {
            StyleMap.getOrDefault(ref.get(), StyleMap.get("polygon_default")).Apply(graphics);
            Boolean fill = !layerName.equals("park");
            Polygon poly = (Polygon)geometry;
            //LineString exteriorRing = poly.getExteriorRing();
            LinearRing exteriorRing = poly.getExteriorRing();
            coordinates = exteriorRing.getCoordinates();
            for (Coordinate coordinate : coordinates) {
                p.addPoint((int)(coordinate.x * scale), (int)(coordinate.y * scale));
            }
            if (fill)
                graphics.fillPolygon(p);
            else // outline
                draw(tilePoly.intersection(new GeometryFactory().createLineString(coordinates)), layerName, attributes, bimage);

            graphics.setColor(background);
            for (int n = 0; n < poly.getNumInteriorRing(); n++) {
                //LineString interiorRingN = poly.getInteriorRingN(n);
                LinearRing interiorRingN = poly.getInteriorRingN(n);
                coordinates = interiorRingN.getCoordinates();
                p.reset();
                for (Coordinate coordinate : coordinates) {
                    p.addPoint((int)(coordinate.x * scale), (int)(coordinate.y * scale));
                }
                graphics.fillPolygon(p);
            }
        }

        if (geometry.getClass().equals(LineString.class) && isDrawable(styleName)) {
            coordinates = geometry.getCoordinates();
            int[] coordGridX = new int[coordinates.length];
            int[] coordGridY = new int[coordinates.length];
            for (int i = 0; i < coordinates.length; i++) {
                coordGridX[i] = (int)(coordinates[i].x * scale);
                coordGridY[i] = (int)(coordinates[i].y * scale);
            }
            boolean done = false;
            while(!done)
            {
                StyleMap.getOrDefault(ref.get(), StyleMap.get("line_default")).Apply(graphics);
                graphics.drawPolyline(coordGridX, coordGridY, coordinates.length);
                done = !isSandwichStyle(ref);
            }
        }
        graphics.dispose();
    }

    // https://openmaptiles.org/schema/
    private void setStyleName(String layerName, AtomicReference<String> ref, Map<String, Object> attributes, String GeometryType)
    {
        String _class = "";
        try{_class = attributes.get("class").toString();}catch(Exception ex){}

        switch (layerName)
        {
            case "boundary":
                try{
                    String admin_level = attributes.get("admin_level").toString();
                    int al = Integer.parseInt(admin_level);
                    if(al<=2){
                        ref.set("boundary_solid");
                    }else{
                        ref.set("boundary_dash");
                    }
                }catch(Exception ex)
                {
                    ref.set("nodraw");
                }
                break;
            case "transportation":
                switch (GeometryType)
                {
                    case "Polygon":
                        switch (_class)
                        {
                            case "path":
                                ref.set("polygon_path");
                                break;
                        }
                        break;
                    case "LineString":
                        ref.set(_class);
                        break;
                }
                break;
            case "landcover":
                ref.set(_class);
                break;
            default:
                ref.set(layerName);
        }
    }

    private boolean isSandwichStyle(AtomicReference<String> stylename)
    {
        switch (stylename.get())
        {
            case "rail":
                stylename.set("rail_dash");
                return true;
            case "boundary_dash":
                stylename.set("boundary_dot");
                return true;
        }
        return false;
    }

    private boolean isDrawable(String styleName)
    {
        switch (styleName)
        {
            case "nodraw":
                return false;
            case "path":
                return zoom > 13;
            case "park":
                return zoom > 8;/*
            case "national_park":
                return zoom > 8;
            case "protected_area":
                return zoom > 8;
            case "nature_reserve":
                return zoom > 8;/**/
        }
        return true;
    }

    private void drawPlaceNames(int Z, int X, int Y, HashMap<String, byte[]> tiles, BufferedImage bimage)
    {
        Graphics graphics = bimage.getGraphics();
        graphics.setColor(Color.black);

        List<int[]> tile = Arrays.asList (
                new int[] {  X,   Y,    0,    0}, // current
                // retrieving names from adjacent
                // tiles to avoid breaking names.
                // 'bbbike.org' typical issue.
                new int[] {  X, Y-1,    0, -256}, // top
                new int[] {X-1, Y-1, -256, -256}, // top left
                new int[] {X-1,   Y, -256,    0}, // left
                new int[] {X-1, Y+1, -256,  256}, // bottom left
                new int[] {  X, Y+1,    0,  256}  // bottom
        );

        VectorTileDecoder vectorTileDecoder = new VectorTileDecoder();
        for(int[] t : tile)
        {
            String id = Z + "_" + t[0] + "_" + ((1 << Z) - t[1] - 1);
            byte[] ctile = tiles.get(id);
            if (ctile != null) {
                VectorTileDecoder.FeatureIterable decode;
                try {
                    decode = vectorTileDecoder.decode(ctile, "place");
                    for (VectorTileDecoder.Feature Feature : decode) {
                        Geometry geometry = Feature.getGeometry();
                        Map<String, Object> attributes = Feature.getAttributes();
                        String _class = attributes.get("class").toString();
                        int size = 0;
                        switch(_class)
                        {
                            case("city"):
                                size = 16;
                                break;
                            case("town"):
                                if (zoom >  9) size = 14;
                                break;
                            case("village"):
                                if (zoom > 10) size = 12;
                                break;
                            case("hamlet"):
                                if (zoom > 11) size = 12;
                                break;
                            case("suburb"):
                                if (zoom > 12) size = 11;
                                break;
                            case("quarter"):
                                if (zoom > 12) size = 11;
                                break;
                            case("neighbourhood"):
                                if (zoom > 12) size = 11;
                                break;
                            case("isolated_dwelling"):
                                if (zoom > 12) size = 10;
                                break;
                            case("island"):
                                if (zoom >  8) size = 12;
                                break;
                            default:
                        }
                        if(size > 0)
                        {
                            try {
                                String str = attributes.get("name").toString();
                                int tx = (int)(scale * (geometry.getCoordinate().x + t[2]));
                                int ty = (int)(scale * (geometry.getCoordinate().y + t[3]));

                                Font font = new Font("Serif", Font.LAYOUT_LEFT_TO_RIGHT, (int)(scale * size));
                                graphics.setFont(font);
                                graphics.drawString(str, tx, ty);
                            }catch(Exception ignored){}
                        }
                    }
                }catch(IOException ignored){}
            }
        }
        graphics.dispose();
    }

    private void drawAddresses(int Z, int X, int Y, HashMap<String, byte[]> tiles, BufferedImage bimage)
    {
        String id = Z + "_" + X + "_" + ((1 << Z) - Y - 1);
        byte[] tile = tiles.get(id);

        VectorTileDecoder vectorTileDecoder = new VectorTileDecoder();
        VectorTileDecoder.FeatureIterable decode;

        try {
            decode = vectorTileDecoder.decode(tile, "transportation_name");
            for (VectorTileDecoder.Feature Feature : decode) {
                Geometry geometry = Feature.getGeometry();
                Map<String, Object> attributes = Feature.getAttributes();
                try{
                    String name = attributes.get("name").toString();

                    Graphics2D graphics = (Graphics2D)bimage.getGraphics();
                    Coordinate[] coordinates = geometry.getCoordinates();
                    if (coordinates.length > 1)
                        drawStreetName(graphics, name, coordinates);
                    graphics.dispose();
                }catch(Exception ignored) {}
            }
        }catch(IOException ignored){}

        try {
            decode = vectorTileDecoder.decode(tile, "housenumber");
            for (VectorTileDecoder.Feature Feature : decode) {
                Geometry geometry = Feature.getGeometry();
                Map<String, Object> attributes = Feature.getAttributes();
                try{
                    String str = attributes.get("housenumber").toString();

                    Graphics2D graphics = (Graphics2D)bimage.getGraphics();
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Font font = new Font("Serif", Font.PLAIN, (int)(scale * 1.2));
                    graphics.setFont(font);
                    graphics.setColor(Color.darkGray);

                    int x = (int)(scale * (geometry.getCoordinate().x - 1));
                    int y = (int)(scale * (geometry.getCoordinate().y + 0.5));

                    graphics.drawString(str, x, y);
                    graphics.dispose();
                }catch(Exception ignored){}
            }
        }catch(IOException ignored){}
    }

    private void drawStreetName(Graphics2D graphics, String s, Coordinate[] coords)
    {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.darkGray);
        int FontSize = 20;
        Font font = new Font("Serif", Font.PLAIN, FontSize);
        FontRenderContext frc = graphics.getFontRenderContext();

        Coordinate[] coordinates = new Coordinate[0];
        int coordslength = 0;

        LineString path = getPath(coords);
        if (path != null) {
            ArrayList arr = new ArrayList();
            Coordinate[] pathCoords = path.getCoordinates();
            Vector2d prev = new Vector2d();
            Vector2d curr;
            for (int i = 0; i < pathCoords.length; i++) {
                Point2D p1 = new Point2D.Double(pathCoords[i].x, pathCoords[i].y);
                if(i > 0)
                {
                    Point2D p2 = new Point2D.Double(pathCoords[i-1].x, pathCoords[i-1].y);
                    curr = new Vector2d(p1.getX()-p2.getX(),p1.getY()-p2.getY());
                    curr.normalize();
                    if (prev.length() > 0)
                        if (prev.dot(curr) < -0.1736) break; // avoid sharp turns
                    prev = new Vector2d(curr.x, curr.y);
                }
                arr.add(new Coordinate(p1.getX() * scale,p1.getY() * scale));
            }
            coordslength = arr.size();
            coordinates = new Coordinate[coordslength];
            for (int i = 0; i < coordslength; i++) {
                coordinates[i] = (Coordinate)arr.get(i);
            }
        }

        Vector2d v0 = new Vector2d(1,0);
        Vector2d dir = new Vector2d(
                coordinates[coordslength-1].x - coordinates[0].x,
                coordinates[coordslength-1].y - coordinates[0].y
        );
        double angle = dir.angle(v0);
        if (angle > (Math.PI / 2)) // reverse the array to avoid having text appear upside down
            ArrayUtils.reverse(coordinates);
        double[] pts = new double[2*coordslength];
        int j = 0;
        for (Coordinate coord : coordinates) {
            pts[j] = coord.x; pts[j+1] = coord.y;
            j=j+2;
        }
        PathTrans pathTrans = new PathTrans(pts);
        GlyphVector gv = font.createGlyphVector(frc, s);
        int length = gv.getNumGlyphs();
        double space = 1.2;
        double advance = gv.getGlyphPosition(length).getX() * space;
        if (advance>pathTrans.totalLength) return;

        double rate = 1/pathTrans.totalLength;
        double startPos = rate * (pathTrans.totalLength - advance)/2;

        for(int i = 0; i < length; i++)
        {
            String substring = s.substring(i, i+1);
            GlyphVector gv1 = font.createGlyphVector(frc, substring);
            Rectangle2D Bounds = gv1.getLogicalBounds();
            PathTrans.trans interpolate = pathTrans.interpolate(startPos + gv.getGlyphPosition(i).getX() * rate * space);
            double theta = interpolate.rotateAngle * Math.PI / 180;
            AffineTransform newTX = new AffineTransform();
            newTX.rotate(theta); gv1.setGlyphTransform(0, newTX);
            Vector2D v = new Vector2D(0, Bounds.getHeight()/4);
            v.rotate(theta);
            graphics.drawGlyphVector(gv1, (float)(interpolate.x+v.x), (float)(interpolate.y+v.y));
        }
    }

    private LineString getPath(Coordinate[] coordinates)
    {
        GeometryFactory geometryFactory = new GeometryFactory();
        Polygon tilePoly = geometryFactory.createPolygon(edges);
        LineString lineStr = geometryFactory.createLineString(coordinates);

        Geometry geometry = tilePoly.intersection(lineStr);
        AtomicReference<Geometry> ref = new AtomicReference<>(geometry);
        setGeom(ref, null);
        geometry = ref.get();
        if (geometry.getClass().equals(LineString.class)) {
            return (LineString)geometry;
        }
        return null;
    }

    private boolean setGeom(AtomicReference<Geometry> ref, Geometry geom)
    {
        Geometry geometry = geom != null ? geom : ref.get();

        if (geometry.getClass().equals(GeometryCollection.class)) {
            for (int n = 0; n < geometry.getNumGeometries(); n++) {
                Geometry geometryN = geometry.getGeometryN(n);
                if (setGeom(ref, geometryN)) break;
            }
        }

        if (geometry.getClass().equals(MultiLineString.class)) {
            ref.set(geometry.getGeometryN(0));
            return true;
        }

        if (geometry.getClass().equals(LineString.class)) {
            ref.set(geometry);
            return true;
        }
        return false;
    }

    private void updStreetBounds()
    {
        int edgeOff = (int)(10 / scale);
        edges = new Coordinate[] {
                new Coordinate(edgeOff, edgeOff),
                new Coordinate(256 - edgeOff * 2, edgeOff * 2),
                new Coordinate(256 - edgeOff * 2, 256 - edgeOff * 2),
                new Coordinate(edgeOff, 256 - edgeOff * 2),
                new Coordinate(edgeOff, edgeOff),
        };
    }

    private class Vector2D
    {
        public double x = 0;
        public double y = 0;
        Vector2D(double x, double y)
        {
            this.x = x;
            this.y = y;
        }

        void rotate(double n)
        {
            this.x = (this.x * java.lang.Math.cos(n)) - (this.y * java.lang.Math.sin(n));
            this.y = (this.x * java.lang.Math.sin(n)) + (this.y * java.lang.Math.cos(n));
        }
    }

    static class tile {
        private int zoom_level;
        private int tile_column;
        private int tile_row;
        private byte[] blob;

        public tile(){}
        public tile(int zoom_level, int tile_column, int tile_row, byte[] blob)
        {
            this.zoom_level = zoom_level;
            this.tile_column = tile_column;
            this.tile_row = tile_row;
            this.blob = blob;
        }

        public String getId()
        {
            return "" + zoom_level + "_" + tile_column + "_" + tile_row;
        }

        public byte[] getBlob()
        {
            try {
                InputStream is = new ByteArrayInputStream(blob);
                GZIPInputStream gis = new GZIPInputStream(is);
                return IOUtils.toByteArray(gis);
            } catch (IOException ex) {
                return blob;
            }
        }
    }

    private class Style {
        private float strokewidth = 1.f;
        private boolean useScale = false;
        private float alpha = -1;
        private int cap = BasicStroke.CAP_ROUND;
        private float miterlimit = 10.f;
        private float[] dash = null;
        private float dashPhase = 0f;
        private Color color = Color.white;

        public Style(){}
        public Style(Color color)
        {
            this.color = color;
        }
        public Style(Color color, float strokewidth)
        {
            this.color = color;
            this.strokewidth = strokewidth;
        }
        public Style(Color color, float strokewidth, boolean useScale)
        {
            this.color = color;
            this.strokewidth = strokewidth;
            this.useScale = useScale;
        }
        public Style(Color color, float strokewidth, boolean useScale, float alpha)
        {
            this.color = color;
            this.strokewidth = strokewidth;
            this.useScale = useScale;
            this.alpha = alpha;
        }
        public Style(Color color, float strokewidth, boolean useScale, float alpha, int cap)
        {
            this.color = color;
            this.strokewidth = strokewidth;
            this.useScale = useScale;
            this.alpha = alpha;
            this.cap = cap;
        }
        public Style(Color color, float strokewidth, boolean useScale, float alpha, int cap, float[] dash)
        {
            this.color = color;
            this.strokewidth = strokewidth;
            this.useScale = useScale;
            this.alpha = alpha;
            this.cap = cap;
            this.dash = dash;
        }
        public Style(Color color, float strokewidth, boolean useScale, float alpha, int cap, float[] dash, float dashPhase)
        {
            this.color = color;
            this.strokewidth = strokewidth;
            this.useScale = useScale;
            this.alpha = alpha;
            this.cap = cap;
            this.dash = dash;
            this.dashPhase = dashPhase;
        }
        public Style(Color color, float strokewidth, boolean useScale, float alpha, int cap, float miterlimit, float[] dash, float dashPhase)
        {
            this.color = color;
            this.strokewidth = strokewidth;
            this.useScale = useScale;
            this.alpha = alpha;
            this.cap = cap;
            this.miterlimit = miterlimit;
            this.dash = dash;
            this.dashPhase = dashPhase;
        }

        public void Apply(Graphics2D graphics)
        {
            float linesize = useScale ? strokewidth * widthScale : strokewidth;
            graphics.setStroke(new BasicStroke(linesize, cap, BasicStroke.JOIN_ROUND, miterlimit, dash, dashPhase));
            graphics.setColor(color);
            if(alpha >= 0 && alpha <= 1)
                graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
    }
}
