package org.bonitasoft.bonitaupdate.patch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PatchDecoJson {

    public static PatchDecoJson getInstance() {
        return new PatchDecoJson();
    }
    /**
     * 
     * @param listPatches
     * @return
     */
    public  List<Map<String,Object>> toJson(List<Patch> listPatches) {
        List<Map<String,Object>> listPatchResult = new ArrayList<>();
        for (Patch patch : listPatches) {
            listPatchResult.add( patch.getMap());
        }
        
        return listPatchResult;
    }
    
    /**
     * 
     * @param listPatches
     * @return
     */
    public  List<Map<String,Object>> toJsonSortedBySequence(List<Patch> listPatches) {
         
        Collections.sort(listPatches, new Comparator<Patch>()
        {
          public int compare(Patch s1,
                  Patch s2)
          {
            return Integer.valueOf(s1.sequence).compareTo(s2.sequence);
          }
        });
        return toJson( listPatches);
    }
    
    
}
