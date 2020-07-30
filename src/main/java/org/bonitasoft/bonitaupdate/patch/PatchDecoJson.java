package org.bonitasoft.bonitaupdate.patch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PatchDecoJson {

    
    public static List<Map<String,Object>> toJson(List<Patch> listPatch) {
        List<Map<String,Object>> listPatchResult = new ArrayList<>();
        for (Patch patch : listPatch) {
            listPatchResult.add( patch.getMap());
        }
        return listPatchResult;
    }
}
