package com.anjunar.technologyspeaks.security

import com.webauthn4j.WebAuthnManager
import com.webauthn4j.data.client.challenge.Challenge

import java.util.concurrent.ConcurrentHashMap

object WebAuthnManagerProvider {

  val webAuthnManager: WebAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager()

  val challengeStore: ConcurrentHashMap[String, Challenge] = new ConcurrentHashMap[String, Challenge]()

  var ORIGIN: String = "https://technologyspeaks.com"

  var RP_ID: String = "technologyspeaks.com"

  val RP_NAME: String = "Technology Speaks"

}
