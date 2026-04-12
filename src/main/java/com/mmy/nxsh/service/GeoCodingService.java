package com.mmy.nxsh.service;

import java.math.BigDecimal;

public interface GeoCodingService {

    String reverseGeocode(BigDecimal latitude, BigDecimal longitude);
}
