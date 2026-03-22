package app.domain.security

import jFx2.state.Property

data class WebAuthnRegister(val email : Property<String> = Property(""), val nickName : Property<String> = Property(""))