package com.chronomon.analysis.address;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 层级地址
 *
 * @author yuzisheng
 * @date 2023-11-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Region {
    /**
     * 节点标识
     */
    private String regionId;
    /**
     * 父节点标识
     */
    private String parentRegionId;
    /**
     * 节点名称
     */
    private String regionName;
    /**
     * 节点层级
     */
    private Integer regionLevel;
    /**
     * 节点地址
     */
    private String regionAddress;
}
