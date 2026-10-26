import com.nightwave.player.offline.OfflineFileRegistration
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

private fun hash(file: File): ByteArray = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
private fun fixture(): Triple<File, File, File> {
    val root = Files.createTempDirectory("nightwave-nw92-").toFile()
    val source = File(root, "original.pending").apply { writeBytes(ByteArray(200_000) { (it % 251).toByte() }) }
    val target = File(root, "offline").apply { mkdir() }.resolve("new.mp3")
    return Triple(root, source, target)
}
private inline fun mustReject(block: () -> Unit) {
    var rejected = false
    try { block() } catch (_: Exception) { rejected = true }
    check(rejected) { "Expected registration to fail closed" }
}

fun main() {
    var scenarios = 0
    run {
        val (root, source, target) = fixture()
        val digest = hash(source)
        var commits = 0
        OfflineFileRegistration.copyVerifiedAndRegister(source, target,
            { hash(it).contentEquals(digest) }, { commits++; true })
        check(commits == 1 && target.isFile && source.isFile)
        check(hash(source).contentEquals(hash(target)))
        check(!File(target.path + ".pending").exists())
        root.deleteRecursively(); scenarios++
    }
    run {
        val (root, source, target) = fixture()
        val digest = hash(source)
        mustReject { OfflineFileRegistration.copyVerifiedAndRegister(source, target,
            { hash(it).contentEquals(digest) }, { false }) }
        check(source.isFile && !target.exists() && !File(target.path + ".pending").exists())
        root.deleteRecursively(); scenarios++
    }
    run {
        val (root, source, target) = fixture()
        var commits = 0
        mustReject { OfflineFileRegistration.copyVerifiedAndRegister(source, target,
            { false }, { commits++; true }) }
        check(source.isFile && !target.exists() && commits == 0)
        root.deleteRecursively(); scenarios++
    }
    run {
        val (root, source, target) = fixture()
        val old = byteArrayOf(4,5,6)
        target.writeBytes(old)
        mustReject { OfflineFileRegistration.copyVerifiedAndRegister(source, target, { true }, { true }) }
        check(target.readBytes().contentEquals(old) && source.isFile)
        root.deleteRecursively(); scenarios++
    }
    run {
        val (root, source, target) = fixture()
        val old = byteArrayOf(8,9)
        val priorPending = File(target.path + ".pending").apply { writeBytes(old) }
        mustReject { OfflineFileRegistration.copyVerifiedAndRegister(source, target, { true }, { true }) }
        check(priorPending.readBytes().contentEquals(old) && source.isFile && !target.exists())
        root.deleteRecursively(); scenarios++
    }
    run {
        val (root, source, target) = fixture()
        mustReject { OfflineFileRegistration.copyVerifiedAndRegister(source, target,
            { true }, { throw IllegalStateException("Synthetic index error") }) }
        check(source.isFile && !target.exists() && !File(target.path + ".pending").exists())
        root.deleteRecursively(); scenarios++
    }
    run {
        val (root, source, target) = fixture()
        var registerCalled = false
        val srcDigest = hash(source)
        OfflineFileRegistration.copyVerifiedAndRegister(source, target,
            { hash(it).contentEquals(srcDigest) }, {
                registerCalled = true
                check(target.isFile && hash(target).contentEquals(srcDigest))
                true
            })
        check(registerCalled && source.exists())
        root.deleteRecursively(); scenarios++
    }
    println("NW92_OFFLINE_COPY_REGISTRATION_SMOKE_PASS scenarios=$scenarios")
}
