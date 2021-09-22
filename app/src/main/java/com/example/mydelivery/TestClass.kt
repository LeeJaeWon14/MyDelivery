package com.example.mydelivery

fun main() {
    println("hello world")
//    for(i in 1 until 10 step(2)) {
//        println("$i")
//    }

//    val arr = arrayOf("1", "2", "3")
//    val list = listOf(*arr)
//    list.forEach {
//        println(it)
//    }
//    val mutable = list.toMutableList()
//    mutable.add("4")
//    mutable.forEach {
//        println(it)
//    }

    val array = mutableListOf<Int>()
    while(true) {
        val input = readLine()!!
        if(input == "z") break
        else array.add(input.toInt())
    }
    array.sort()
    array.forEach {
        println(it)
    }
}