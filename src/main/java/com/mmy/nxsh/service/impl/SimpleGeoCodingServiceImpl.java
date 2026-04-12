package com.mmy.nxsh.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mmy.nxsh.service.GeoCodingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SimpleGeoCodingServiceImpl implements GeoCodingService {

    private final RestTemplate restTemplate;
    private final String amapKey;
    private final String amapUrl;

    public SimpleGeoCodingServiceImpl(RestTemplateBuilder builder,
                                      @Value("${geo.amap.key:}") String amapKey,
                                      @Value("${geo.amap.url:https://restapi.amap.com/v3/geocode/regeo}") String amapUrl) {
        this.restTemplate = builder.build();
        this.amapKey = amapKey;
        this.amapUrl = amapUrl;
    }

    @Override
    public String reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return "未知地点";
        }
        if (StrUtil.isBlank(amapKey)) {
            return formatAddress(latitude, longitude);
        }
        try {
            String location = longitude.toPlainString() + "," + latitude.toPlainString();
            String url = amapUrl + "?location=" + location + "&key=" + amapKey + "&radius=100&extensions=base";
            String body = restTemplate.getForObject(url, String.class);
            String address = parseAmapAddress(body);
            if (StrUtil.isNotBlank(address)) {
                return address;
            }
        } catch (Exception ignored) {
        }
        return formatAddress(latitude, longitude);
    }

    private String parseAmapAddress(String body) {
        if (StrUtil.isBlank(body)) {
            return null;
        }
        JSONObject json = JSONUtil.parseObj(body);
        if (!"1".equals(json.getStr("status"))) {
            return null;
        }
        JSONObject regeocode = json.getJSONObject("regeocode");
        if (regeocode == null) {
            return null;
        }
        return regeocode.getStr("formatted_address");
    }

    private String formatAddress(BigDecimal latitude, BigDecimal longitude) {
        return "纬度" + formatCoordinate(latitude) + "，经度" + formatCoordinate(longitude) + "附近";
    }

    private String formatCoordinate(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
