package com.anjunar.json.mapper.intermediate

import com.anjunar.json.mapper.intermediate.model.{JsonArray, JsonBoolean, JsonNode, JsonNull, JsonNumber, JsonObject, JsonString}

object JsonParser {

  def parse(json: String): JsonNode = {
    val parser = new FastParser(json)
    val node = parser.parseValue()
    parser.skipWhitespace()
    if (!parser.eof()) {
      throw new IllegalStateException(s"Unexpected trailing data at pos ${parser.pos}")
    }
    node
  }

  private class FastParser(input: String) {
    private val chars: Array[Char] = input.toCharArray
    private val len: Int = chars.length
    var pos: Int = 0

    def eof(): Boolean = pos >= len

    private def peek(): Char =
      if (pos < len) chars(pos) else 0.toChar

    private def nextChar(): Char = {
      val current = chars(pos)
      pos += 1
      current
    }

    def skipWhitespace(): Unit =
      while (pos < len) {
        val current = chars(pos)
        if (current == ' ' || current == '\n' || current == '\r' || current == '\t') {
          pos += 1
        } else {
          return
        }
      }

    def parseValue(): JsonNode = {
      skipWhitespace()
      if (eof()) {
        throw new IllegalStateException("Unexpected end of input")
      }

      peek() match {
        case '{' =>
          parseObject()
        case '[' =>
          parseArray()
        case '"' =>
          new JsonString(parseString())
        case 't' =>
          expectLiteral("true")
          new JsonBoolean(true)
        case 'f' =>
          expectLiteral("false")
          new JsonBoolean(false)
        case 'n' =>
          expectLiteral("null")
          JsonNull()
        case '-' =>
          parseNumber()
        case current if current >= '0' && current <= '9' =>
          parseNumber()
        case current =>
          throw new IllegalStateException(s"Unexpected char '$current' at pos $pos")
      }
    }

    private def expectLiteral(literal: String): Unit = {
      val start = pos
      val end = start + literal.length
      if (end > len) {
        throw new IllegalStateException(s"Unexpected end (expect '$literal')")
      }

      var index = 0
      while (index < literal.length) {
        if (chars(pos + index) != literal.charAt(index)) {
          throw new IllegalStateException(s"Unexpected literal at pos $pos, expected $literal")
        }
        index += 1
      }

      pos = end
    }

    private def parseObject(): JsonObject = {
      if (nextChar() != '{') {
        throw new IllegalStateException("Expected '{'")
      }

      val map = new java.util.LinkedHashMap[String, JsonNode]()
      skipWhitespace()
      if (!eof() && peek() == '}') {
        pos += 1
        return new JsonObject(map)
      }

      var continueLoop = true
      while (continueLoop) {
        skipWhitespace()
        if (eof() || peek() != '"') {
          throw new IllegalStateException(s"Expected string key at pos $pos")
        }

        val key = parseString()
        skipWhitespace()
        if (eof() || nextChar() != ':') {
          throw new IllegalStateException(s"Expected ':' after key at pos $pos")
        }

        val value = parseValue()
        map.put(key, value)
        skipWhitespace()

        if (eof()) {
          throw new IllegalStateException("Unterminated object")
        }

        peek() match {
          case ',' =>
            pos += 1
          case '}' =>
            pos += 1
            continueLoop = false
          case current =>
            throw new IllegalStateException(s"Expected ',' or '}' in object at pos $pos but found '$current'")
        }
      }

      new JsonObject(map)
    }

    private def parseArray(): JsonArray = {
      if (nextChar() != '[') {
        throw new IllegalStateException("Expected '['")
      }

      val array = new java.util.ArrayList[JsonNode]()
      skipWhitespace()
      if (!eof() && peek() == ']') {
        pos += 1
        return new JsonArray(array)
      }

      var continueLoop = true
      while (continueLoop) {
        val value = parseValue()
        array.add(value)
        skipWhitespace()

        if (eof()) {
          throw new IllegalStateException("Unterminated array")
        }

        peek() match {
          case ',' =>
            pos += 1
          case ']' =>
            pos += 1
            continueLoop = false
          case current =>
            throw new IllegalStateException(s"Expected ',' or ']' in array at pos $pos but found '$current'")
        }
      }

      new JsonArray(array)
    }

    private def parseString(): String = {
      if (nextChar() != '"') {
        throw new IllegalStateException("Expected '\"' at beginning of string")
      }

      val start = pos
      var index = pos
      var hasEscape = false

      while (index < len) {
        val current = chars(index)
        if (current == '"') {
          val end = index
          pos = index + 1
          return if (!hasEscape) {
            new String(chars, start, end - start)
          } else {
            unescapeFromChars(start, end)
          }
        } else if (current == '\\') {
          hasEscape = true
          index += 2
        } else {
          index += 1
        }
      }

      throw new IllegalStateException("Unterminated string")
    }

    private def unescapeFromChars(start: Int, end: Int): String = {
      val builder = new StringBuilder(end - start)
      var index = start

      while (index < end) {
        val current = chars(index)
        if (current == '\\') {
          index += 1
          if (index >= end) {
            throw new IllegalStateException("Invalid escape at end of string")
          }

          chars(index) match {
            case '"' =>
              builder.append('"')
              index += 1
            case '\\' =>
              builder.append('\\')
              index += 1
            case '/' =>
              builder.append('/')
              index += 1
            case 'b' =>
              builder.append('\b')
              index += 1
            case 'f' =>
              builder.append('\f')
              index += 1
            case 'n' =>
              builder.append('\n')
              index += 1
            case 'r' =>
              builder.append('\r')
              index += 1
            case 't' =>
              builder.append('\t')
              index += 1
            case 'u' =>
              if (index + 4 >= end) {
                throw new IllegalStateException("Invalid unicode escape")
              }

              var code = 0
              var hexIndex = 1
              while (hexIndex <= 4) {
                code = (code << 4) + hexValue(chars(index + hexIndex))
                hexIndex += 1
              }

              builder.append(code.toChar)
              index += 5
            case current =>
              throw new IllegalStateException(s"Invalid escape '\\$current' at pos $index")
          }
        } else {
          builder.append(current)
          index += 1
        }
      }

      builder.toString
    }

    private def hexValue(current: Char): Int =
      if (current >= '0' && current <= '9') {
        current.toInt - '0'.toInt
      } else if (current >= 'A' && current <= 'F') {
        10 + (current.toInt - 'A'.toInt)
      } else if (current >= 'a' && current <= 'f') {
        10 + (current.toInt - 'a'.toInt)
      } else {
        throw new IllegalStateException(s"Invalid hex char '$current' in unicode escape")
      }

    private def parseNumber(): JsonNumber = {
      val start = pos
      if (peek() == '-') {
        pos += 1
      }

      if (eof()) {
        throw new IllegalStateException("Unexpected end in number")
      }

      if (peek() == '0') {
        pos += 1
      } else if (peek() >= '1' && peek() <= '9') {
        while (!eof() && chars(pos) >= '0' && chars(pos) <= '9') {
          pos += 1
        }
      } else {
        throw new IllegalStateException(s"Invalid number at pos $pos")
      }

      if (!eof() && chars(pos) == '.') {
        pos += 1
        if (eof() || chars(pos) < '0' || chars(pos) > '9') {
          throw new IllegalStateException("Invalid fractional part in number")
        }
        while (!eof() && chars(pos) >= '0' && chars(pos) <= '9') {
          pos += 1
        }
      }

      if (!eof() && (chars(pos) == 'e' || chars(pos) == 'E')) {
        pos += 1
        if (!eof() && (chars(pos) == '+' || chars(pos) == '-')) {
          pos += 1
        }
        if (eof() || chars(pos) < '0' || chars(pos) > '9') {
          throw new IllegalStateException("Invalid exponent in number")
        }
        while (!eof() && chars(pos) >= '0' && chars(pos) <= '9') {
          pos += 1
        }
      }

      new JsonNumber(new String(chars, start, pos - start))
    }
  }

}
