package com.lqx.dudu.springboot;

import org.junit.jupiter.api.Test;

class SpringbootApplicationTests {
    //记录器
    @Test
    public void contextLoads() {
        String token  = "ttm.JUynkSkTQzKRtYyRl_rofR3Js04";
        String substring = token.substring(4, 31);
        System.out.println(substring);

    }

}
