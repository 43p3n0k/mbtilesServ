package com.example.mbtilesServ.Model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity
public class metadata {
    @EmbeddedId
    private metadataID id;
    private String name;
    private String value;

    public String getValue(){
        return value;
    }
}
