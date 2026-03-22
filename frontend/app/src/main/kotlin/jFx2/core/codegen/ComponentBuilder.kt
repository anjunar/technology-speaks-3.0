package jFx2.core.codegen

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class JfxComponentBuilder(

    val name: String = "",

    val tag: String = "",

    val classes: Array<String> = [],

    val afterBuild: AfterBuildMode = AfterBuildMode.SCHEDULED
)

enum class AfterBuildMode {
    NONE,
    SCHEDULED,
    EAGER
}

