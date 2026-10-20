package com.roampass.history.domain;

// 상태를 바꾼 주체. status_history.actor에 이 문자열이 그대로 저장된다
public record Actor(String value) {

    public static final Actor USER = new Actor("USER");
    public static final Actor SYSTEM = new Actor("SYSTEM");
    public static final Actor PG = new Actor("PG");
    public static final Actor SUPPLIER = new Actor("SUPPLIER");

    public static Actor admin(String adminId) {
        return new Actor("ADMIN:" + adminId);
    }
}