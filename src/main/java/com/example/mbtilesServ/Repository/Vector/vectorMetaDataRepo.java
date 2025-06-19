package com.example.mbtilesServ.Repository.Vector;

import com.example.mbtilesServ.Model.metadata;
import com.example.mbtilesServ.Model.metadataID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface vectorMetaDataRepo extends Repository<metadata, metadataID> {
    @Query("Select d from metadata d where d.name = 'bounds' ")
    List<metadata> getBounds();

    @Query("Select d from metadata d where d.name = 'minzoom' ")
    List<metadata> getMinZoom();

    @Query("Select d from metadata d where d.name = 'maxzoom' ")
    List<metadata> getMaxZoom();
}
