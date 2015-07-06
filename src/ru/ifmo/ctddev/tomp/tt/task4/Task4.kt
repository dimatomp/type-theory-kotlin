package ru.ifmo.ctddev.tomp.tt.task4

import java.io.BufferedReader
import java.io.FileReader
import java.io.PrintWriter

fun main(args: Array<String>) {
    val str = BufferedReader(FileReader("task4.in")).readLine()
    val out = PrintWriter("task4.out")
    val src = parseLambda(str)
    out.println(src.normalize())
    out.close()
}
