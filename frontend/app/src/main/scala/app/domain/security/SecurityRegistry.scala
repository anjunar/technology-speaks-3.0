package app.domain.security

import jfx.core.meta.PackageClassLoader

object SecurityRegistry {
  
  def init(): Unit = {
    val loader = PackageClassLoader("app.domain.security")
    
    loader.register(() => new Account(), classOf[Account])
    loader.register(() => new PasswordLogin(), classOf[PasswordLogin])
    loader.register(() => new PasswordRegister(), classOf[PasswordRegister])
    loader.register(() => new PasswordChange(), classOf[PasswordChange])
    loader.register(() => new ConfirmCode(), classOf[ConfirmCode])
    loader.register(() => new WebAuthnLogin(), classOf[WebAuthnLogin])
    loader.register(() => new WebAuthnRegister(), classOf[WebAuthnRegister])
  }
}
