package com.anjunar.technologyspeaks

import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class SpringApplication

object SpringApplication {

  def main(args: Array[String]): Unit = {
    SpringContext.context = org.springframework.boot.SpringApplication.run(classOf[SpringApplication], args*)

    SpringContext.context.getBean(classOf[StartUpRunner]).run()
  }

}
