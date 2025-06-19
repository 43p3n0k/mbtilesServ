package com.example.mbtilesServ.Model;


import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.lang.Contract;

@Entity
@Table(name="tiles")
public class Tile {
    @EmbeddedId
    private tilesID id;
    private Integer zoom_level;
    private Integer tile_column;
    private Integer tile_row;
    private byte[] tile_data;

    public int getZoom(){
        return zoom_level;
    }
    public int getColumn(){
        return tile_column;
    }
    public int getRow(){
        return tile_row;
    }
    public byte[] getTileData(){
        return tile_data;
    }
}
