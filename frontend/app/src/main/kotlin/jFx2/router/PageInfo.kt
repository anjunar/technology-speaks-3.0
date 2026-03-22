package jFx2.router

interface PageInfo {

    val name : String

    val width : Int

    val height : Int

    val resizable : Boolean
    
    var close : () -> Unit

}