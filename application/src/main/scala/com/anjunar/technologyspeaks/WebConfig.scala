package com.anjunar.technologyspeaks

import com.anjunar.technologyspeaks.rest.{EntityConverter, JsonHttpMessageConverter, MapperHttpMessageConverter}
import com.anjunar.technologyspeaks.security.SecurityInterceptor
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.core.Ordered
import org.springframework.format.FormatterRegistry
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter
import org.springframework.web.servlet.config.annotation.{InterceptorRegistry, WebMvcConfigurer}

@Configuration
class WebConfig(val securityInterceptor: SecurityInterceptor, val entityConverter: EntityConverter) extends WebMvcConfigurer {

  override def configureMessageConverters(builder: HttpMessageConverters.ServerBuilder): Unit = {
    builder.addCustomConverter(new MapperHttpMessageConverter())
    builder.addCustomConverter(new JsonHttpMessageConverter())
    builder.build()
  }

  override def addFormatters(registry: FormatterRegistry): Unit = {
    registry.addConverter(entityConverter)
  }

  override def addInterceptors(registry: InterceptorRegistry): Unit = {
    registry.addInterceptor(securityInterceptor)
      .addPathPatterns("/**")
      .order(Ordered.HIGHEST_PRECEDENCE + 10)
  }

}
