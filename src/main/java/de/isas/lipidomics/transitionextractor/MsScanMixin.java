/*
 * 
 */
package de.isas.lipidomics.transitionextractor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Range;
import io.github.msdk.datamodel.IsolationInfo;
import io.github.msdk.datamodel.RawDataFile;
import io.github.msdk.io.mzml.data.MzMLBinaryDataInfo;
import io.github.msdk.io.mzml.data.MzMLCVGroup;
import io.github.msdk.io.mzml.data.MzMLPrecursorList;
import io.github.msdk.io.mzml.data.MzMLProductList;
import io.github.msdk.io.mzml.data.MzMLScanList;
import java.io.InputStream;
import java.util.List;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
public abstract class MsScanMixin {
    
    @JsonIgnore
    public abstract RawDataFile getRawDataFile();
    
//    @JsonIgnore
//    public abstract MzMLCVGroup getCVParams();
    
    @JsonIgnore
    public abstract MzMLBinaryDataInfo getMzBinaryDataInfo();
    
    @JsonIgnore
    public abstract MzMLBinaryDataInfo getIntensityBinaryDataInfo();
    
    @JsonIgnore
    public abstract InputStream getInputStream();
    
//    @JsonIgnore
//    public abstract String getId();
    
//    @JsonIgnore
//    public abstract List<IsolationInfo> getIsolations();
    
//    @JsonIgnore
//    public abstract Range<Double> getMzRange();
    
//    @JsonIgnore
//    public abstract MzMLPrecursorList getPrecursorList();
    
//    @JsonIgnore
//    public abstract MzMLProductList getProductList();
    
//    @JsonIgnore
//    public abstract MzMLScanList getScanList();
    
//    @JsonIgnore
//    public abstract Range<Double> getScanningRange();
    
    @JsonIgnore
    public abstract double[] getMzValues();
    
    @JsonIgnore
    public abstract float[] getIntensityValues();
}
