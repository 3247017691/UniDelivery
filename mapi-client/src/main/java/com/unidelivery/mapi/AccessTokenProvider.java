package com.unidelivery.mapi;

/**
 * 按投放账户提供 access_token；实现方负责续期与缓存（app 模块落库 + 定时续期）。
 */
public interface AccessTokenProvider {

    String token(long advertiserId);

    /** 平台返回 40102（token 已过期）时作废本地缓存，下次 token() 强制续期。 */
    default void invalidate(long advertiserId) {
    }
}
