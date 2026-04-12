package com.mmy.nxsh.controller.dto;

import lombok.Data;

@Data
public class ElderlyLoginResponse {
    private String token;
    private Long elderlyId;
    private String name;
    private String nickname;

    public static ElderlyLoginResponse of(String token, Long elderlyId, String name, String nickname) {
        ElderlyLoginResponse resp = new ElderlyLoginResponse();
        resp.setToken(token);
        resp.setElderlyId(elderlyId);
        resp.setName(name);
        resp.setNickname(nickname);
        return resp;
    }
}
