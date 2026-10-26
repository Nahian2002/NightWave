import org.junit.Test
fun main() {
 val suites=listOf(
  "com.nightwave.player.offline.OfflineManualNextAvailabilityTest" to 6,
  "com.nightwave.player.offline.OfflineShuffleCycleIntegrityTest" to 8
 )
 var passed=0
 for ((name,expected) in suites) {
  val cls=Class.forName(name)
  val methods=cls.declaredMethods.filter{it.isAnnotationPresent(Test::class.java)}.sortedBy{it.name}
  check(methods.size==expected){"NW105 method-count mismatch: "+name}
  val instance=cls.getDeclaredConstructor().newInstance()
  for (method in methods) {
   try {method.invoke(instance);passed++}
   catch(e:java.lang.reflect.InvocationTargetException){
    throw RuntimeException("NW105_JUNIT_CASE_FAILED "+name+"."+method.name,e.cause)}
  }
  println("NW105_REAL_OFFLINE_SUITE_PASS name="+name+" count="+methods.size)
 }
 check(passed==14)
 println("NW105_REAL_OFFLINE_JUNIT_SOURCE_PASS cases="+passed)
}
