package com.anjunar.technologyspeaks.security

import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.{JavaMailSender, MimeMessageHelper}
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Service
class RegisterService(val mailSender: JavaMailSender, val templateEngine: TemplateEngine) {

  def register(to: String, code: String, nickName: String): Unit = {
    val mimeMessage: MimeMessage = mailSender.createMimeMessage()
    val helper = new MimeMessageHelper(mimeMessage, true, "UTF-8")

    val context = new Context()
    context.setVariable("code", code)
    context.setVariable("nickName", nickName)

    val renderedTemplate = templateEngine.process("RegisterTemplate.html", context)

    helper.setTo(to)
    helper.setFrom("anjunar@gmx.de")
    helper.setSubject("Registrierung bei technologyspeaks.com")
    helper.setText(renderedTemplate, true)

    mailSender.send(mimeMessage)
  }

}
