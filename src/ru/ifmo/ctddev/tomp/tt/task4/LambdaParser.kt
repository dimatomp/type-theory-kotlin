package ru.ifmo.ctddev.tomp.tt.task4

import java.util.*
import Lambda
import Var
import Lam
import App

class LambdaFormatException: RuntimeException()

fun parseLambda(s: String): Lambda {
    var cPos = 0
    val reused: MutableMap<Lambda, Lambda> = HashMap()
    fun spaces(): Unit { while (cPos < s.length() && Character.isWhitespace(s.charAt(cPos))) cPos++ }
    fun reuse(lam: Lambda): Lambda = reused.getOrPut(lam) { lam }
    fun lexeme(): String {
        val begin = cPos
        while (cPos < s.length() && (Character.isLetterOrDigit(s.charAt(cPos)) || s.charAt(cPos) == '\'')) cPos++
        return s.substring(begin, cPos)
    }
    fun recursive(): Lambda {
        var result: Lambda? = null
        while (cPos < s.length()) {
            when (s.charAt(cPos)) {
                '\\' -> {
                    cPos++
                    spaces()
                    val p = lexeme()
                    if (cPos == s.length() || s.charAt(cPos) != '.')
                        throw LambdaFormatException()
                    cPos++
                    val res = reuse(Lam(p, recursive()))
                    return res
                }
                '(' -> {
                    cPos++
                    val nested = recursive()
                    result = if (result == null) nested else reuse(App(result, nested))
                }
                ')' -> {
                    cPos++
                    if (result == null)
                        throw LambdaFormatException()
                    else
                        return result
                }
                in 'a'..'z', in '0'..'9', '\'' -> {
                    val name = lexeme()
                    result = if (result == null) Var(name) else reuse(App(result, Var(name)))
                }
                else ->  {
                    if (Character.isWhitespace(s.charAt(cPos)))
                        spaces()
                    else
                        throw LambdaFormatException()
                }
            }
        }
        if (result == null)
            throw LambdaFormatException()
        else
            return result
    }
    return recursive()
}
