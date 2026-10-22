package com.teraha.sales.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix="sales.paging")
@Data
public class PagingConfigs {
    //default page size will be 20.
    //Overridable with sales.paging.page-size in application.properties
    private int pageSize = 20;
}
