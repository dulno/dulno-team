import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.net.URI
import java.util.zip.GZIPInputStream

plugins {
  id("java")
  id("io.freefair.lombok") version "9.1.0"
  id("org.springframework.boot") version "3.5.7"
}

group = "com.dulno"
version = "1.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

repositories {
  mavenCentral()
  maven {
    url = uri("https://git.dulno.com/api/v4/projects/51/packages/maven")
    credentials(HttpHeaderCredentials::class) {
      name = "Private-Token"
      value = System.getenv("DULNO_GITLAB_PRIVATE_TOKEN") ?:
        findProperty("dulnoGitlabPrivateToken") as String?
    }
    authentication {
      create("header", HttpHeaderAuthentication::class)
    }
  }
}

dependencies {
  testCompileOnly(platform("org.junit:junit-bom:6.0.1"))
  testCompileOnly("org.junit.jupiter:junit-jupiter:6.0.1")

  implementation("com.dulno:core:1.0.0-SNAPSHOT")

  implementation("com.google.inject:guice:7.0.0")

  implementation("com.google.guava:guava:33.5.0-jre")

  implementation("org.projectlombok:lombok:1.18.42")
  annotationProcessor("org.projectlombok:lombok:1.18.42")
  testImplementation("org.projectlombok:lombok:1.18.42")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.42")

  implementation("com.datastax.oss:java-driver-core:4.17.0")

  implementation("org.json:json:20250517")
  implementation("commons-io:commons-io:2.21.0")

  implementation("org.apache.tomcat.embed:tomcat-embed-core:11.0.13")

  implementation("org.springframework.boot:spring-boot-starter-web:3.5.7")
  implementation("org.springframework:spring-core:7.0.0")

  implementation("io.jsonwebtoken:jjwt:0.13.0")

  implementation("com.sun.mail:javax.mail:1.6.2")

  implementation("com.stripe:stripe-java:29.1.0")

  implementation("dev.samstevens.totp:totp:1.7.1")

  implementation("io.kubernetes:client-java:23.0.0")

  implementation("com.maxmind.geoip2:geoip2:4.4.0")

  implementation("com.google.zxing:core:3.5.4")
  implementation("com.google.zxing:javase:3.5.3")
}

tasks.test {
  useJUnitPlatform()
}

tasks.bootJar {
  mainClass = "com.dulno.team.TeamApplication"
}

tasks.register("downloadGeoLite2Database") {
  val licenseKey = "***REMOVED***"
  val databaseUrl = "https://download.maxmind.com/app/geoip_download?" +
    "edition_id=GeoLite2-City&license_key=$licenseKey&suffix=tar.gz"
  val resourcesDir = File("geo")
  val downloadFile = layout.buildDirectory.file("GeoLite2-City.tar.gz").get().asFile
  doLast {
    resourcesDir.mkdirs()
    downloadFile.parentFile.mkdirs()
    if (downloadFile.exists()) {
      downloadFile.delete()
    }
    URI(databaseUrl).toURL().openStream().use { input ->
      downloadFile.outputStream().use { output ->
        input.copyTo(output)
      }
    }
    extract(downloadFile, resourcesDir)
    downloadFile.delete()
  }
}

fun extract(file: File, destination: File) {
  GZIPInputStream(file.inputStream()).use { gis ->
    TarArchiveInputStream(gis).use { tis ->
      var entry = tis.nextEntry
      while (entry != null) {
        if (!entry.isDirectory && entry.name.endsWith(".mmdb")) {
          val outputFile = File(destination, "GeoLite2-City.mmdb")
          outputFile.outputStream().use { os ->
            tis.copyTo(os)
          }
        }
        entry = tis.nextEntry
      }
    }
  }
}