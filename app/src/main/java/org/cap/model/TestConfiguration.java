package org.cap.model;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;

public class TestConfiguration {
    @Expose 
    public Map<Integer, String> idNameMap;

    @Expose
    public List<Core> mappingInfo;
    
    public TestConfiguration() {
    }
    
}
