package org.cdisc.tools;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
public class IDCardinality {
    @Getter @Setter
    private Map<String, String> cardinalities;
}
