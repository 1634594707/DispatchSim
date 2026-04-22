package com.dispatchsim.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dispatchSimOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DispatchSim API")
                        .description("园区无人车调度仿真平台后端接口文档，涵盖订单、车辆、仿真控制与统计查询。")
                        .version("v1")
                        .contact(new Contact()
                                .name("DispatchSim")
                                .url("https://github.com/dispatchsim"))
                        .license(new License()
                                .name("Internal Project Use")));
    }
}
