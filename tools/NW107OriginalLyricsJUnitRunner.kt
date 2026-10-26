import org.junit.Test

fun main() {
    val clazz = Class.forName("com.nightwave.player.v2.lyrics.LyricsReadingPolicyTest")
    val methods = clazz.declaredMethods.filter {
        it.isAnnotationPresent(Test::class.java)
    }.sortedBy { it.name }
    check(methods.size == 8) { "NW107 JUnit method count drift: ${methods.size}" }
    val instance = clazz.getDeclaredConstructor().newInstance()
    for (method in methods) {
        try {
            method.invoke(instance)
            println("NW107_JUNIT_CASE_PASS " + method.name)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw RuntimeException("NW107_JUNIT_CASE_FAILED " + method.name, e.cause)
        }
    }
    println("NW107_ORIGINAL_LYRICS_JUNIT_PASS methods=" + methods.size)
}
