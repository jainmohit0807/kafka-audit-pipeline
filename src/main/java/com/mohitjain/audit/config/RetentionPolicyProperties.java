package com.mohitjain.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "audit.retention")
public class RetentionPolicyProperties {

    private int yearsToRetain = 7;

    private String partitionCheckCron = "0 0 1 1 1 *";

    private boolean autoCreatePartitions = true;

    private boolean autoDropExpiredPartitions = true;
}
