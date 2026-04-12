package com.mmy.nxsh.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mmy.nxsh.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("family_elderly_bind")
public class FamilyElderlyBind extends BaseEntity {
    private Long familyId;
    private Long elderlyId;
    private String relationName;  // 关系名称，如"女儿"
    private Integer isPrimary;    // 是否为主要联系人
}