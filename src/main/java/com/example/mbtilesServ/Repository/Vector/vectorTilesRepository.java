package com.example.mbtilesServ.Repository.Vector;

import com.example.mbtilesServ.Model.tilesID;
import com.example.mbtilesServ.Model.Tile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface vectorTilesRepository extends Repository<Tile, tilesID> {
    @Query("Select t from Tile t where t.zoom_level = :z AND t.tile_column IN (:x-1, :x) AND t.tile_row IN (:y+1,:y,:y-1)")
    public List<Tile> getTile(Integer z, Integer x, Integer y);
}
