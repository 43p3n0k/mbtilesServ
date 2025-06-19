package com.example.mbtilesServ.Model;

import jakarta.persistence.Column;

import java.util.Objects;

public class metadataID {
    @Column(insertable=false, updatable=false)
    private String name;
    @Column(insertable=false, updatable=false)
    private String value;
    @Override
    public boolean equals(Object o)
    {
        if ( this == o ) return true;
        return o != null && getClass() == o.getClass();
    }
    @Override
    public int hashCode() {
        return Objects.hash( name,value );
    }
}
