package com.example.mbtilesServ.Service;

import com.example.mbtilesServ.Model.Tile;
import com.example.mbtilesServ.Repository.Raster.rasterTilesRepository;
import com.example.mbtilesServ.Repository.Vector.vectorTilesRepository;
import com.example.mbtilesServ.Repository.Vector.vectorMetaDataRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Service
public class mbtilesResource {

    private final int vectorTilesMaxZoom;
    private final int rasterTilesMaxZoom;

    private final vectorMetaDataRepo metaData;

    private final rasterTilesRepository rasterTilesRepository;

    @Autowired
    private vectorTilesRepository vectorTilesRepository;

    @Autowired
    private Processor proc;

    public mbtilesResource(
            vectorMetaDataRepo metaData,
            rasterTilesRepository rasterTilesRepository
    ){
        this.metaData = metaData;
        this.rasterTilesRepository = rasterTilesRepository;

        this.vectorTilesMaxZoom = Integer.parseInt(this.metaData.getMaxZoom().get(0).getValue());
        this.rasterTilesMaxZoom = rasterTilesRepository.getMaxZoom().orElse(20);
    }

    public Object[] getCenterZoom(@RequestParam Integer wndWidth){
        try{
            String[] split = metaData.getBounds().get(0).getValue().split(",");
            int minZoom = Integer.parseInt(metaData.getMinZoom().get(0).getValue());
            float span = Math.abs(Float.parseFloat(split[2]) - Float.parseFloat(split[0]));
            float tileWidth = 256.f * span / wndWidth;

            float ref = 360;
            int i = 0;
            while(ref > tileWidth)
            {
                ref /= 2;
                i++;
            }
            return new Object[] {
                    new Float[] {
                            0.5f * (Float.parseFloat(split[1]) + Float.parseFloat(split[3])),
                            0.5f * (Float.parseFloat(split[0]) + Float.parseFloat(split[2]))
                    },
                    Math.max(i, minZoom)
            };
        }catch(Exception e){
            return new Object[] {new Float[]{0.f, 0.f}, 2};
        }
    }

    public byte[] getTile(int z, int x, int y){
        try {
            List<Tile> tile = rasterTilesRepository.getTile(z, x, (1 << z) - y - 1);
            return tile.get(0).getTileData();
        } catch (Exception e) {
            try {
                Processor.OverZoom oz = new Processor.OverZoom(z, x, y, vectorTilesMaxZoom);
                int Z = oz.z;
                int X = oz.x;
                int Y = oz.y;
                List<Tile> tile = vectorTilesRepository.getTile(Z, X, (1 << Z) - Y - 1);
                return proc.vectorToRaster(oz, tile);
            } catch (Exception ignored) {
                try {
                    Processor.OverZoom oz = new Processor.OverZoom(z,x,y,rasterTilesMaxZoom);
                    int Z = oz.z;
                    int X = oz.x;
                    int Y = oz.y;
                    List<Tile> tile = rasterTilesRepository.getTile(Z, X, (1 << Z) - Y - 1);
                    return proc.cropImage(oz, tile.get(0).getTileData());
                } catch (Exception ex) {
                    return proc.getBytes(null);
                }
            }
        }
    }
}
