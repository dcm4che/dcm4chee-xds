package org.dcm4chee.xds2.src.metadata.exception;

import java.util.ArrayList;
import java.util.List;

public class MetadataConfigurationException extends Exception {

    private static final long serialVersionUID = 2015096383019921411L;
    
    private List<String> missingAttributes;
    private List<MetadataConfigurationException> subErrors;
    
    public MetadataConfigurationException(String msg) {
        super(msg);
    }
    
    public MetadataConfigurationException(String msg, List<String> missingAttributes) {
        super(msg);
        this.missingAttributes = missingAttributes;
    }

    public List<String> getMissingAttributes() {
        return missingAttributes;
    }
    
    public List<MetadataConfigurationException> getSubErrors() {
        return subErrors;
    }
    
    public void addSubError(MetadataConfigurationException x) {
        if (subErrors == null)
            subErrors = new ArrayList<MetadataConfigurationException>();
        subErrors.add(x);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        append(sb, this);
        if (subErrors != null) {
            for (int i = 0, len = subErrors.size() ; i < len ; i++) {
                append(sb, subErrors.get(i));
            }
        }
        return sb.toString();
    }

    private void append(StringBuilder sb, MetadataConfigurationException x) {
        List<String> missingAttrs = x.getMissingAttributes();
        sb.append(x.getMessage());
        if (missingAttrs != null && missingAttrs.size() > 0) {
            sb.append("\nMissing required attributes:(").append(missingAttrs.get(0));
            for (int i = 1, len = missingAttrs.size() ; i < len ; i++) {
                sb.append(", ").append(missingAttrs);
            }
            sb.append(")");
        }
        sb.append("\n");
    }
}
