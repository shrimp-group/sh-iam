package com.wkclz.auth.filter;

/**
 * Filter 排序常量
 */
public final class FilterOrder {

    public static final int LOGGING = Integer.MIN_VALUE + 1000;
    public static final int SEC_HEADER = Integer.MIN_VALUE + 2000;
    public static final int AUTH = Integer.MIN_VALUE + 3000;
    public static final int AUTHZ = Integer.MIN_VALUE + 4000;

    private FilterOrder() {}
}
