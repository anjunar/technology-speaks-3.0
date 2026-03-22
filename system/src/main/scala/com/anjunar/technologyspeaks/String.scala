package com.anjunar.technologyspeaks

extension (value: String)
  def toKebabCase(): String =
    value
      .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
      .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2")
      .toLowerCase()
