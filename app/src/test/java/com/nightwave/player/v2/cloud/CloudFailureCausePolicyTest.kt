package com.nightwave.player.v2.cloud

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.CompletionException

class CloudFailureCausePolicyTest {
    private class CodedFailure(val code: Int) : Exception("synthetic private payload")

    @Test fun taskAndCompletionWrappersRetainTheTypedOriginalFailure() {
        val original = CodedFailure(10)
        val wrapped = ExecutionException(CompletionException(original))
        assertSame(original, CloudFailureCausePolicy.find(wrapped, CodedFailure::class.java))
        assertEquals(10, CloudFailureCausePolicy.find(wrapped, CodedFailure::class.java)!!.code)
    }

    @Test fun directFailuresAreRetainedAndUnrelatedCausesReturnNull() {
        val original = CodedFailure(401)
        assertSame(original, CloudFailureCausePolicy.find(original, CodedFailure::class.java))
        assertNull(CloudFailureCausePolicy.find(IllegalStateException("private detail"), CodedFailure::class.java))
    }

    @Test fun cyclicCauseGraphsTerminateWithoutLeakingMessages() {
        val a = Exception("private A")
        val b = Exception("private B")
        a.initCause(b)
        b.initCause(a)
        assertNull(CloudFailureCausePolicy.find(a, CodedFailure::class.java))
    }

    @Test fun adversarialDeepChainsAreBounded() {
        var chain: Throwable = CodedFailure(403)
        repeat(7) { chain = ExecutionException(chain) }
        assertNotNull(CloudFailureCausePolicy.find(chain, CodedFailure::class.java))
        chain = ExecutionException(chain)
        assertNull(CloudFailureCausePolicy.find(chain, CodedFailure::class.java))
    }
}
