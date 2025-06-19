package com.example.mbtilesServ.Repository.Raster;

import com.example.mbtilesServ.Model.tilesID;
import com.example.mbtilesServ.Model.Tile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface rasterTilesRepository extends Repository<Tile, tilesID> {
    @Query("Select t from Tile t where t.zoom_level = :z AND t.tile_column = :x AND t.tile_row = :y")
    List<Tile> getTile(Integer z, Integer x, Integer y);

    @Query("SELECT MAX(t.zoom_level) FROM Tile t")
    Optional<Integer> getMaxZoom();
}
