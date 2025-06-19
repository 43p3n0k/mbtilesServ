package com.example.mbtilesServ.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.example.mbtilesServ.Service.mbtilesResource;

@Controller
public class mbtilesController {

    @Autowired
    private mbtilesResource mbtilesResource;

    @GetMapping("/getCenterZoom")
    public ResponseEntity<Object[]>getCenterZoom(@RequestParam Integer wndWidth){
        return ResponseEntity
                .ok()
                //.header("content-disposition","attachment; filename = getCenterZoom.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(mbtilesResource.getCenterZoom(wndWidth));

    }

    @GetMapping("/mvt/{z}/{x}/{y}.png")
    @ResponseBody
    public ResponseEntity<byte[]> mbtiles(@PathVariable int z, @PathVariable int x, @PathVariable int y){
        return ResponseEntity
                .ok()
                .header("content-disposition","attachment; filename = " + z + "_" + x + "_" + y + ".png")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(mbtilesResource.getTile(z,x,y));
    }
}
