import java.util.Base64

val ensureDebugKeystore = tasks.register("ensureDebugKeystore") {
  description = "Auto-generates or restores missing debug.keystore for CI/CD builds"
  group = "build setup"
  val targetKeystore = file("${rootDir}/debug.keystore")
  val targetBase64 = file("${rootDir}/debug.keystore.base64")
  outputs.file(targetKeystore)
  doFirst {
    if (!targetKeystore.exists()) {
      if (targetBase64.exists()) {
        try {
          val bytes = Base64.getDecoder().decode(targetBase64.readText().trim())
          targetKeystore.writeBytes(bytes)
        } catch (_: Exception) {
        }
      }
      if (!targetKeystore.exists()) {
        try {
          val process = ProcessBuilder(
            "keytool",
            "-genkeypair",
            "-alias", "androiddebugkey",
            "-keypass", "android",
            "-keystore", targetKeystore.absolutePath,
            "-storepass", "android",
            "-dname", "CN=Android Debug,O=Android,C=US",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000"
          ).start()
          process.waitFor()
        } catch (_: Exception) {
        }
      }
      if (!targetKeystore.exists()) {
        logger.warn("debug.keystore does not exist.")
      }
    }
  }
}

tasks.matching { it.name.startsWith("validateSigning") || it.name == "preBuild" }.configureEach {
  dependsOn(ensureDebugKeystore)
}
