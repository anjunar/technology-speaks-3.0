package com.anjunar.json.mapper

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.cfg.DateTimeFeature

object ObjectMapperProvider {

  val mapper: tools.jackson.databind.json.JsonMapper =
    tools.jackson.databind.json.JsonMapper
      .builder()
      .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
      .changeDefaultPropertyInclusion(_ =>
        JsonInclude.Value.construct(
          JsonInclude.Include.NON_EMPTY,
          JsonInclude.Include.NON_EMPTY
        )
      )
      .build()

}
