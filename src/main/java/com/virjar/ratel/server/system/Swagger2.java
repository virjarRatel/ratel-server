package com.virjar.ratel.server.system;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by virjar on 2018/8/4.<br>
 * swagger集成
 */
@Configuration
public class Swagger2 {

    private List<Parameter> parameter() {
        List<Parameter> params = new ArrayList<>();
        params.add(new ParameterBuilder().name("operatorToken")
                .description("请求令牌，访问登陆接口获得(登陆注册接口不需要这个参数): /api/ratel/user/login")
                .modelRef(new ModelRef("string"))
                .parameterType("query")
                .required(false).build());
        return params;
    }

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.virjar.ratel.server.controller"))
                .paths(PathSelectors.any())

                .build().globalOperationParameters(parameter());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("ratel管理系统")
                .description("免root开启app上帝模式")
                .termsOfServiceUrl("http://git.virjar.com/ratel/ratel-doc")
                .version("1.0")
                .build();
    }
}
