package app.domain.security

import jfx.core.meta.PackageClassLoader

object SecurityRegistry {
  
  def init(): Unit = {
    val loader = PackageClassLoader("app.domain.security")
    
    loader.register(() => new Account())
    loader.register(() => new PasswordLogin())
    loader.register(() => new PasswordRegister())
    loader.register(() => new PasswordChange())
    loader.register(() => new ConfirmCode())
    loader.register(() => new WebAuthnLogin())
    loader.register(() => new WebAuthnRegister())
  }
}
