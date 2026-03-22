package app.domain.security

import jFx2.state.Property

data class WebAuthnLogin(val email : Property<String> = Property(""))