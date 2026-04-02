package com.anjunar.technologyspeaks.security

import jakarta.validation.constraints.{Email, Size}

import scala.annotation.meta.field

class PasswordRegistration(@(Email @field) var email: String,
                           @(Size @field)(min = 2, max = 80) var nickName: String,
                           @(Size @field)(min = 4, max = 128) var password: String)
