package com.example.mbtilesServ.Model;

import jakarta.persistence.Column;

import java.util.Objects;

public class tilesID {
    @Column(insertable=false, updatable=false)
    private Integer zoom_level;
    @Column(insertable=false, updatable=false)
    private Integer tile_column;
    @Column(insertable=false, updatable=false)
    private Integer tile_row;
    @Override
    public boolean equals(Object o)
    {
        if ( this == o ) return true;
        return o != null && getClass() == o.getClass();
    }
    @Override
    public int hashCode() {
        return Objects.hash( zoom_level,tile_column,tile_row );
    }
}
