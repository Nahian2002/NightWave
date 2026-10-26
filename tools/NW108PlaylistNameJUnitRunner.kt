import org.junit.Test
fun main() {
    val klass=Class.forName("com.nightwave.player.v2.data.PlaylistNameIdentityTest")
    val methods=klass.declaredMethods.filter { it.isAnnotationPresent(Test::class.java) }.sortedBy { it.name }
    check(methods.size==6) { "Expected 6 methods, found ${methods.size}" }
    val instance=klass.getDeclaredConstructor().newInstance()
    for(m in methods){try{m.invoke(instance);println("NW108_CASE_PASS ${m.name}")}catch(e:java.lang.reflect.InvocationTargetException){throw RuntimeException("FAILED ${m.name}",e.cause)}}
    println("NW108_REAL_NAME_POLICY_JUNIT_PASS cases=${methods.size}")
}
