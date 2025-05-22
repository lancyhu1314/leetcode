package com.suning.fab.loan.service;


import com.suning.rsf.provider.annotation.Contract;
import com.suning.rsf.provider.annotation.Method;

import java.util.Map;

@Contract(
        name = "com.suning.api.rsf.service.IGrayRsfService",
        internal = false,
        description = "old gray service",
        warningPhones = "18512580053"
)

public interface IGrayRsfService {
    @Method(
            idempotent = false,
            timeout = 5000L,
            retryTimes = 0,
            priority = "H",
            description = "execute service"
    )
    Map<String, Object> execute(Map<String, Object> var1);
}
